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

@Slf4j
public class ShardWatcher implements Runnable {

    private final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();
    private ShardedMantaro shardedMantaro;

    @Override
    public void run() {
        LogUtils.shard("ShardWatcherThread started");
        final int wait = MantaroData.config().get().shardWatcherWait;
        while(true) {
            try {
                //Run every x ms
                Thread.sleep(wait);
                MantaroEventManager.getLog().info("Checking shards...");

                //Just in case...
                if(shardedMantaro == null) shardedMantaro = MantaroBot.getInstance().getShardedMantaro();

                //Get and propagate the shard event.
                ShardMonitorEvent sme = new ShardMonitorEvent(shardedMantaro.getTotalShards());
                EventUtils.propagateEvent(sme);

                //Start the procedure...
                int[] dead = sme.getDeadShards();
                if(dead.length != 0) {
                    MantaroEventManager.getLog().error("Dead shards found: {}", Arrays.toString(dead));
                    for(int id : dead) {
                        try {
                            FutureTask<Integer> restartJDA = new FutureTask<>(() -> {
                                try {
                                    MantaroShard shard = MantaroBot.getInstance().getShard(id);

                                    //If we are dealing with a shard reconnecting, don't make its job harder by rebooting it twice.
                                    if(shard.getStatus() == JDA.Status.RECONNECT_QUEUED || shard.getStatus() == JDA.Status.SHUTDOWN) {
                                        return 1;
                                    }

                                    LogUtils.shard(
                                            "Dead shard? Starting automatic shard restart on shard #" + id + " due to it being inactive for longer than 30 seconds."
                                    );

                                    //Reboot the shard.
                                    shard.start(true);
                                    Thread.sleep(1000); //5.5 seconds + 1 second backoff.
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
                            LogUtils.shard(String.format("Cannot restart shard %d Try to do it manually.", id));
                        }
                    }
                } else {
                    //yay
                    MantaroEventManager.getLog().info("No dead shards found");
                    long ping = MantaroBot.getInstance().getPing();

                    if(ping > 400) {
                        LogUtils.shard(String.format("No dead shards found, but average ping is high (%dms). Ping breakdown: %s",
                                ping, Arrays.toString(MantaroBot.getInstance().getPings())));
                    }
                }
            } catch(InterruptedException e) {
                log.error("ShardWatcher interrupted, stopping...");
                LogUtils.shard("ShardWatcher interrupted, stopping...");
                return;
            }
        }
    }
}
