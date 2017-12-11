/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.core.shard.watcher;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.events.EventUtils;
import net.kodehawa.mantarobot.core.listeners.events.ShardMonitorEvent;
import net.kodehawa.mantarobot.core.shard.MantaroShard;
import net.kodehawa.mantarobot.core.shard.ShardedMantaro;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * This class acts as a Watcher for all the {@link MantaroShard} instances.
 * The always-running ShardWatcherThread looks over all the shards and listeners to make sure no shard is either not receiving events or deadlocked.
 * This works by sending a foreign event to all the shards, asking for a response back.
 * There are two ways for a Shard to send a signal that's dead: by having one or more listeners deadlocked, or by having a {@link MantaroEventManager#getLastJDAEventTimeDiff()}
 * time of over 30000ms (30 seconds without receiving any event).
 * <p>
 * After acknowledging the dead shards, the ShardWatcherThread will proceed to reboot all of the dead shards by sending a signal to {@link MantaroShard#start(boolean)} with a value of
 * "true", which will send {@link JDA#shutdownNow()} to the old shard instance, and attempt to build a completely new one. This times out after two minutes of wait.
 * There is a backoff of 6 seconds between rebooting shards, to avoid OP2 spam during this procedure (5 seconds from the {@link MantaroShard#start(boolean)} call, and one extra second on this procedure).
 * <p>
 * After rebooting the shard, everything on it *should* go back to normal and it should be able to listen to events and dispatch messages again without issues.
 */
@Slf4j
public class ShardWatcher implements Runnable {

    //The pool that will run the FutureTask to wait for the shard to finish its pre-load phase.
    //No longer needed?
    private final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    //Mantaro's sharded instance
    private ShardedMantaro shardedMantaro;

    @Override
    public void run() {
        LogUtils.shard("ShardWatcherThread started");
        final int wait = MantaroData.config().get().shardWatcherWait;
        while(true) {
            try {
                //Run every x ms (usually every 10 minutes unless changed)
                Thread.sleep(wait);
                MantaroEventManager.getLog().info("Checking shards...");

                //Just in case...
                if(shardedMantaro == null) shardedMantaro = MantaroBot.getInstance().getShardedMantaro();

                //Get and propagate the shard event.
                //This event will propagate over all Mantaro-specific listeners, and see if the shards are responding accordingly.
                ShardMonitorEvent sme = new ShardMonitorEvent(shardedMantaro.getTotalShards());
                EventUtils.propagateEvent(sme);

                //Start the procedure...
                int[] dead = sme.getDeadShards();

                //Oh well... we can try to recover them now!
                if(dead.length != 0) {
                    MantaroEventManager.getLog().error("Dead shards found: {}", Arrays.toString(dead));

                    //Alert us in case a massive amount of dead shards is found.
                    //This COULD be caused by discord dying and reconnecting a bunch of shards, so we don't need to worry until we get a bunch of "starting automatic shard
                    //restart on..." kinda message.
                    if(dead.length > 15) {
                        LogUtils.shard("Seems like Megumin struck our castle and we got a horribly high amount of dead shards (" + dead.length + ")\n" +
                                "This could be just due to them reconnecting though, if nothing appears down there talking about how the shards are rebooting " +
                                "you might aswell ignore this warning.");
                    }

                    //Start scrapping and rebooting shards.
                    //Under the hood this basically calls JDA#shutdownNow on the old JDA instance and replaces it with a completely new one.
                    for(int id : dead) {
                        try {
                            FutureTask<Integer> restartJDA = new FutureTask<>(() -> {
                                try {
                                    MantaroShard shard = MantaroBot.getInstance().getShard(id);

                                    //If we are dealing with a shard reconnecting, don't make its job harder by rebooting it twice.
                                    //But, if the shard has been inactive for too long, we're better off scrapping this session as the shard might be stuck on connecting.
                                    if((shard.getStatus() == JDA.Status.RECONNECT_QUEUED || shard.getStatus() == JDA.Status.ATTEMPTING_TO_RECONNECT ||
                                            shard.getStatus() == JDA.Status.SHUTDOWN) && shard.getEventManager().getLastJDAEventTimeDiff() < 200000) {
                                        LogUtils.shard("Skipping shard " + id + " due to it being currently reconnecting to the websocket or was shutdown manually...");
                                        return 1;
                                    }

                                    //Alert us, plz no panic
                                    LogUtils.shard(
                                            "Dead shard? Starting automatic shard restart on shard #" + id + " due to it being inactive for longer than 30 seconds."
                                    );

                                    //Reboot the shard.
                                    shard.start(true);
                                    Thread.sleep(1000); //5 seconds on the start method + 1 second of extra backoff.
                                    return 1;
                                } catch(Exception e) {
                                    //Something went wrong :(
                                    LogUtils.shard(String.format("Cannot restart shard %d Try to do it manually.", id));
                                    return 0;
                                }
                            });

                            //Execute, if this blocks for longer than 2m throw exception (shouldn't happen anymore?)
                            THREAD_POOL.execute(restartJDA);
                            restartJDA.get(2, TimeUnit.MINUTES);
                        } catch(Exception e) {
                            //Somehow we couldn't reboot the shard.
                            LogUtils.shard(String.format("Cannot restart shard %d Try to do it manually.", id));
                        }
                    }
                } else {
                    //yay
                    MantaroEventManager.getLog().info("No dead shards found");
                    long ping = MantaroBot.getInstance().getPing();

                    //We might have a few soft-dead shards on here...
                    if(ping > 400) {
                        LogUtils.shard(String.format("No dead shards found, but average ping is high (%dms). Ping breakdown: %s",
                                ping, Arrays.toString(MantaroBot.getInstance().getPings())));
                    }
                }
            } catch(InterruptedException e) {
                //Just in case we stop this for any reason, we want to know that we interrupted this, just so we know we won't have a shard watcher running on the background.
                log.error("ShardWatcher interrupted, stopping...");
                LogUtils.shard("ShardWatcher interrupted, stopping...");
                return;
            }
        }
    }
}
