/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.events.EventUtils;
import net.kodehawa.mantarobot.core.listeners.events.ShardMonitorEvent;
import net.kodehawa.mantarobot.core.shard.MantaroShard;
import net.kodehawa.mantarobot.core.shard.ShardedMantaro;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;

import java.util.Arrays;
import java.util.concurrent.*;

/**
 * This class acts as a Watcher for all the {@link MantaroShard} instances.
 * The always-running ShardWatcherThread looks over all the shards and listeners to make sure no shard is either not receiving events or deadlocked.
 * This works by sending a foreign event to all the shards, asking for a response back.
 * There are two ways for a Shard to send a signal that's dead: by having one or more listeners deadlocked, or by having a {@link MantaroEventManager#getLastJDAEventTimeDiff()}
 * time of over 30000ms (30 seconds without receiving any event).
 * <p>
 * After acknowledging the dead shards, the ShardWatcherThread will proceed to attempt to RESUME all of the dead shards. If this doesn't work,
 * it will restart the dead shards by sending a signal to {@link MantaroShard#start(boolean)} with a value of "true", which will send {@link JDA#shutdownNow()}
 * to the old shard instance, and attempt to build a completely new one. This sends a timeout after two minutes of wait.
 * There is a backoff of 6 seconds between rebooting shards, to avoid OP2 spam during this procedure (5 seconds from the {@link MantaroShard#start(boolean)} call, and one extra second on this procedure).
 * <p>
 * After rebooting the shard, everything on it *should* go back to normal and it should be able to listen to events and dispatch messages again without issues.
 * <p>
 * There are two loops on this code. The first one handles checking the restart queue (populated after a RESUME request fails) and the second one handles the actual procedure
 * for detecting dead shards and attempts to send a RESUME on them.
 */
@Slf4j
public class ShardWatcher implements Runnable {

    //The pool that will run the FutureTask to wait for the shard to finish its pre-load phase.
    //No longer needed?
    private final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();
    //The scheduler that manages the wait between one shard being resumed and the backoff period to check if it successfully revived.
    private final ScheduledExecutorService RESUME_WAITER = Executors.newSingleThreadScheduledExecutor();
    //The queue where shards that didn't get revived used a RESUME get added. Here they get completely scrapped and re-built when they get polled from the queue.
    private final ConcurrentLinkedQueue<MantaroShard> RESTART_QUEUE = new ConcurrentLinkedQueue<>();

    //Mantaro's sharded instance
    private ShardedMantaro shardedMantaro;

    @Override
    public void run() {
        LogUtils.shard("ShardWatcherThread started");
        //Executes the restart queue handler. For the actual logic behind all this, check the next while(true) loop.
        THREAD_POOL.execute(()->{
            while(true) {
                MantaroShard shard = RESTART_QUEUE.poll();
                if(shard == null) {
                    //poll the queue every 10 seconds
                    try {
                        Thread.sleep(10000);
                    } catch(InterruptedException e) {
                        LogUtils.shard("Shard restarter task interrupted");
                        return;
                    }

                    //Continue to the next loop cycle if no shard is on the restart queue.
                    continue;
                }

                //Alert us, plz no panic
                LogUtils.shard(
                        String.format("(Resume request failed) Dead shard? Starting automatic shard restart on shard #%d due to it being inactive for longer than 30 seconds.", shard.getId())
                );

                try {
                    //Reboot the shard.
                    shard.start(true);
                } catch(Exception e) {
                    //If the shard wasn't able to restart by itself, alert us so we can reboot manually later.
                    LogUtils.shard(String.format("Shard %d was unable to be restarted: %s", shard.getId(), e));
                }

                try {
                    //Wait 5 seconds as a backoff.
                    Thread.sleep(5000);
                } catch(InterruptedException e) {
                    LogUtils.shard("Shard restarter task interrupted");
                    return;
                }
            }
        });

        final int wait = MantaroData.config().get().shardWatcherWait;
        while(true) {
            try {
                //Run every x ms (usually every 10 minutes unless changed)
                Thread.sleep(wait);
                MantaroEventManager.getLog().info("Checking shards...");

                //Just in case...
                if(shardedMantaro == null)
                    shardedMantaro = MantaroBot.getInstance().getShardedMantaro();

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
                    //Under the hood this basically calls for a RESUME JDA instance and if it fails, it adds it to the restart queue to replace it with a completely new one.
                    for(int id : dead) {
                        try {
                            MantaroShard shard = MantaroBot.getInstance().getShard(id);

                            //If we are dealing with a shard reconnecting, don't make its job harder by rebooting it twice.
                            //But, if the shard has been inactive for too long, we're better off scrapping this session as the shard might be stuck on connecting.
                            if((shard.getStatus() == JDA.Status.RECONNECT_QUEUED || shard.getStatus() == JDA.Status.ATTEMPTING_TO_RECONNECT ||
                                    shard.getStatus() == JDA.Status.SHUTDOWN) && shard.getEventManager().getLastJDAEventTimeDiff() < 200000) {
                                LogUtils.shard(String.format("Skipping shard %d due to it being currently reconnecting to the websocket or was shutdown manually...", id));
                                continue;
                            }

                            LogUtils.shard(
                                    String.format("Found dead shard (#%d)... attempting RESUME request and waiting 20 seconds to validate.", id)
                            );

                            //Send the RESUME request.
                            ((JDAImpl)(shard.getJDA())).getClient().close(4000);

                            RESUME_WAITER.schedule(() -> {
                                if(shard.getEventManager().getLastJDAEventTimeDiff() > 18000) {
                                    RESTART_QUEUE.add(shard);
                                }
                            }, 20, TimeUnit.SECONDS);
                        } catch(Exception e) {
                            //Somehow we couldn't reboot the shard.
                            LogUtils.shard(String.format("Cannot restart shard %d. Try to do it manually.", id));
                        }
                    }
                } else {
                    //yay
                    MantaroEventManager.getLog().info("No dead shards found");
                    long ping = MantaroBot.getInstance().getPing();

                    //We might have a few soft-dead shards on here... (or internet went to shit)
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
