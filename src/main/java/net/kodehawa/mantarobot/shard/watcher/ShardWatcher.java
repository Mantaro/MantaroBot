package net.kodehawa.mantarobot.shard.watcher;


import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.ShardMonitorEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.shard.ShardedMantaro;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ShardWatcher implements Runnable {

    private final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();
    private ShardedMantaro shardedMantaro = MantaroBot.getInstance().getShardedMantaro();

    @Override
    public void run() {
        LogUtils.shard("ShardWatcherThread started");
        final int wait = MantaroData.config().get().shardWatcherWait;
        while(true) {
            try {
                Thread.sleep(wait);
                MantaroEventManager.getLog().info("Checking shards...");
                ShardMonitorEvent sme = new ShardMonitorEvent(shardedMantaro.getTotalShards());
                shardedMantaro.getManagers().forEach(manager -> manager.handle(sme));
                int[] dead = sme.getDeadShards();
                if(dead.length != 0) {
                    MantaroEventManager.getLog().error("Dead shards found: {}", Arrays.toString(dead));
                    for(int id : dead) {
                        try {
                            FutureTask<Integer> restartJDA = new FutureTask<>(() -> {
                                try {

                                    LogUtils.shard(
                                            "Dead shard? Starting automatic shard restart on shard #" + id + " due to it being inactive for longer than 2 minutes."
                                    );

                                    MantaroBot.getInstance().getShard(id).restartJDA(true);
                                    Thread.sleep(1000);
                                    return 1;
                                } catch(Exception e) {
                                    log.warn("Cannot restart shard #{} <@155867458203287552> try to do it manually.", id);
                                    return 0;
                                }
                            });
                            THREAD_POOL.execute(restartJDA);
                            restartJDA.get(2, TimeUnit.MINUTES);
                        } catch(Exception e) {
                            log.warn("Cannot restart shard #{} <@155867458203287552> try to do it manually.", id);
                        }
                    }
                } else {
                    MantaroEventManager.getLog().info("No dead shards found");
                }
            } catch(InterruptedException e) {
                log.error("ShardWatcher interrupted, stopping...");
                return;
            }
        }
    }
}
