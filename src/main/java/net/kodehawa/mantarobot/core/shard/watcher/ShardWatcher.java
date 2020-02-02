/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core.shard.watcher;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.internal.JDAImpl;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.events.EventUtils;
import net.kodehawa.mantarobot.core.listeners.events.ShardMonitorEvent;
import net.kodehawa.mantarobot.core.shard.Shard;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.Prometheus;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class acts as a Watcher for all the {@link net.kodehawa.mantarobot.core.shard.Shard} instances.
 * The always-running ShardWatcherThread looks over all the shards and listeners to make sure no shard is either not receiving events or deadlocked.
 * This works by sending a foreign event to all the shards, asking for a response back.
 * There are two ways for a Shard to send a signal that's dead: by having one or more listeners deadlocked, or by having a {@link MantaroEventManager#getLastJDAEventTimeDiff()}
 * time of over 30000ms (30 seconds without receiving any event).
 * <p>
 * After acknowledging the dead shards, the ShardWatcherThread will proceed to attempt to RESUME all of the dead shards. If this doesn't work,
 * it will restart the dead shards by sending a signal to {@link net.dv8tion.jda.api.sharding.ShardManager#restart(int)} with a value of "true", which will send {@link JDA#shutdownNow()}
 * to the old shard instance, and attempt to build a completely new one. This sends a timeout after two minutes of wait.
 * There is a backoff of 6 seconds between rebooting shards, to avoid OP2 spam during this procedure (5 seconds from the {@link net.dv8tion.jda.api.sharding.ShardManager#restart(int)} call, and one extra second on this procedure).
 * <p>
 * After rebooting the shard, everything on it *should* go back to normal and it should be able to listen to events and dispatch messages again without issues.
 * <p>
 * There are two loops on this code. The first one handles checking the restart queue (populated after a RESUME request fails) and the second one handles the actual procedure
 * for detecting dead shards and attempts to send a RESUME on them.
 */
public class ShardWatcher implements Runnable {
    
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ShardWatcher.class);
    //The pool that will run the FutureTask to wait for the shard to finish its pre-load phase.
    //No longer needed?
    private final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder()
            .setNameFormat("ShardWatcher-QueueWorker")
            .setDaemon(true)
            .build()
    );
    //The scheduler that manages the wait between one shard being resumed and the backoff period to check if it successfully revived.
    private final ScheduledExecutorService RESUME_WAITER = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder()
            .setNameFormat("ShardWatcher-WaiterThread")
            .setDaemon(true)
            .build()
    );
    //The queue where shards that didn't get revived used a RESUME get added. Here they get completely scrapped and re-built when they get polled from the queue.
    private final ConcurrentLinkedQueue<Shard> RESTART_QUEUE = new ConcurrentLinkedQueue<>();
    
    public ShardWatcher() {
        Prometheus.THREAD_POOL_COLLECTOR.add("shard-watcher-thread-pool", THREAD_POOL);
        Prometheus.THREAD_POOL_COLLECTOR.add("shard-watcher-resume-waiter", RESUME_WAITER);
    }
    
    @Override
    public void run() {
        final int wait = MantaroData.config().get().shardWatcherWait;
        LogUtils.shard(String.format("ShardWatcherThread started.\nConfigured to run every %d minutes on this instance.", (wait / 60000)));
        //Executes the restart queue handler. For the actual logic behind all this, check the next while(true) loop.
        THREAD_POOL.execute(() -> {
            while(true) {
                Shard shard = RESTART_QUEUE.poll();
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
                        String.format("(RESUME request failed) Dead shard? Starting automatic shard restart on shard #%d due to it being inactive for longer than 30 seconds.", shard.getId())
                );
                
                try {
                    //Reboot the shard.
                    MantaroBot.getInstance().getShardManager().restart(shard.getId());
                } catch(Exception e) {
                    //If the shard wasn't able to restart by itself, alert us so we can reboot manually later.
                    LogUtils.shard(String.format("Shard %d was unable to be restarted: %s", shard.getId(), e));
                }
                
                try {
                    //Wait 5 seconds as a backoff.
                    Thread.sleep(Math.max(Math.min(5000, 5000 * RESTART_QUEUE.size() / 4), 15000));
                } catch(InterruptedException e) {
                    LogUtils.shard("Shard restarter task interrupted");
                    return;
                }
            }
        });
        
        while(true) {
            try {
                //Run every x ms (usually every 10 minutes unless changed)
                Thread.sleep(wait);
                MantaroEventManager.getLog().info("Checking shards...");
                
                //Get and propagate the shard event.
                //This event will propagate over all Mantaro-specific listeners, and see if the shards are responding accordingly.
                ShardMonitorEvent sme = new ShardMonitorEvent(MantaroBot.getInstance().getShardManager().getShardsTotal());
                EventUtils.propagateEvent(sme);
                
                //Start the procedure...
                int[] dead = sme.getDeadShards();
                
                //Oh well... we can try to recover them now!
                if(dead.length != 0) {
                    MantaroEventManager.getLog().error("Dead shards found: {}", Arrays.toString(dead));
                    
                    //Start scrapping and rebooting shards.
                    //Under the hood this basically calls for a RESUME JDA instance and if it fails, it adds it to the restart queue to replace it with a completely new one.
                    for(int id : dead) {
                        try {
                            Shard shard = MantaroBot.getInstance().getShard(id);
                            
                            var jda = shard.getNullableJDA();
                            //Silently ignore this.
                            if(jda == null || jda.getStatus() == JDA.Status.SHUTDOWN) {
                                continue;
                            }
                            
                            //If we are dealing with a shard reconnecting, don't make its job harder by rebooting it twice.
                            //But, if the shard has been inactive for too long, we're better off scrapping this session as the shard might be stuck on connecting.
                            if((jda.getStatus() == JDA.Status.RECONNECT_QUEUED || jda.getStatus() == JDA.Status.ATTEMPTING_TO_RECONNECT) &&
                                       ((MantaroEventManager)jda.getEventManager()).getLastJDAEventTimeDiff() < 400000) {
                                LogUtils.shard(String.format("Skipping shard %d due to it being currently reconnecting to the websocket or was shutdown manually...", id));
                                continue;
                            }
                            
                            log.info("Found dead shard (#{})... attempting RESUME request and waiting 30 seconds to validate.", id);
                            
                            //Send the RESUME request.
                            ((JDAImpl) (shard.getJDA())).getClient().close(4000);
                            
                            RESUME_WAITER.schedule(() -> {
                                if(((MantaroEventManager)jda.getEventManager()).getLastJDAEventTimeDiff() > 27000) {
                                    RESTART_QUEUE.add(shard);
                                }
                            }, 30, TimeUnit.SECONDS);
                        } catch(Exception e) {
                            //Print the exception so we can look at it later...
                            e.printStackTrace();
                            //Force add into the queue
                            RESUME_WAITER.schedule(() -> RESTART_QUEUE.add(MantaroBot.getInstance().getShard(id)), 30, TimeUnit.SECONDS);
                            //Somehow we couldn't reboot the shard.
                            LogUtils.shard(String.format("Cannot restart shard %d. Try to do it manually.", id));
                        }
                    }
                } else {
                    //yay
                    MantaroEventManager.getLog().info("No dead shards found");
                    long ping = (long)MantaroBot.getInstance().getShardManager()
                                              .getShards().stream().mapToLong(JDA::getGatewayPing).average()
                                              .orElse(-1);
                    
                    //We might have a few soft-dead shards on here... (or internet went to shit)
                    if(ping > 850) {
                        LogUtils.shard(String.format("No dead shards found, but average ping is high (%dms). Ping breakdown: %s",
                                ping, MantaroBot.getInstance().getShardManager()
                                              .getShards().stream().mapToLong(JDA::getGatewayPing)
                                              .mapToObj(String::valueOf).collect(Collectors.joining(", "))
                        ));
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
