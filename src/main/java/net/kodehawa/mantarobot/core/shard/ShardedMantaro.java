/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
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
 *
 */

package net.kodehawa.mantarobot.core.shard;

import com.github.natanbc.discordbotsapi.DiscordBotsAPI;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.SessionControllerAdapter;
import net.kodehawa.mantarobot.ExtraRuntimeOptions;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.LoadState;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.events.PostLoadEvent;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.core.shard.jda.BucketedController;
import net.kodehawa.mantarobot.core.shard.watcher.ShardWatcher;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.services.Carbonitex;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.Utils;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.utils.ShutdownCodes.SHARD_FETCH_FAILURE;

/**
 * Represents a Sharded bot.
 * This class will still be used whether we have zero or a billion shards tho, but Mantaro is more optimized to run with shards.
 * It holds all the necessary info for the bot to correctly function in a sharded environment, while also providing access to {@link ICommandProcessor} and
 * other extremely important stuff.
 */
public class ShardedMantaro {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ShardedMantaro.class);
    private final Carbonitex carbonitex = new Carbonitex();
    private final Config config = MantaroData.config().get();
    private final DiscordBotsAPI discordBotsAPI = new DiscordBotsAPI.Builder().setToken(MantaroData.config().get().dbotsorgToken).build();
    private final List<MantaroEventManager> managers = new ArrayList<>();
    private final ICommandProcessor processor;
    private final MantaroShard[] shards;
    private final int totalShards;
    private final int fromShard;
    private final int toShard;
    private final SessionController sessionController;
    
    public ShardedMantaro(int totalShards, boolean isDebug, boolean auto, String token, ICommandProcessor commandProcessor, int fromShard, int toShard) {
        int shardAmount = totalShards;
        if(auto)
            shardAmount = getRecommendedShards(token);
        if(isDebug)
            shardAmount = 2;
        
        if(isDebug || shardAmount < 16) {
            sessionController = new SessionControllerAdapter();
        } else {
            //If you're self-hosting you shouldn't have this many shards, but keep this one in mind, lol.
            //If you're another big bot looking how to do batch login, refer to BucketedController
            int bucketFactor = config.getBucketFactor();
            log.info("Using buckets of {} shards to start the bot! Assuming we're on big bot :tm: sharding.", bucketFactor);
            sessionController = new BucketedController(bucketFactor);
        }
        this.totalShards = shardAmount;
        this.processor = commandProcessor;
        this.fromShard = fromShard == 0 ? ExtraRuntimeOptions.FROM_SHARD.orElse(fromShard) : fromShard;
        this.toShard = toShard == 0 ? ExtraRuntimeOptions.TO_SHARD.orElse(toShard) : toShard;
        this.shards = new MantaroShard[this.totalShards];
    }
    
    private static int getRecommendedShards(String token) {
        if(MantaroData.config().get().totalShards != 0) {
            return MantaroData.config().get().totalShards;
        }
        
        try {
            Request shards = new Request.Builder()
                                     .url("https://discordapp.com/api/gateway/bot")
                                     .header("Authorization", "Bot " + token)
                                     .header("Content-Type", "application/json")
                                     .build();
            
            Response response = Utils.httpClient.newCall(shards).execute();
            JSONObject shardObject = new JSONObject(response.body().string());
            response.close();
            return shardObject.getInt("shards");
        } catch(JSONException e) {
            log.error("Unable to fetch shard count, using default value (1)");
        } catch(Exception e) {
            SentryHelper.captureExceptionContext(
                    "Exception thrown when trying to get shard count, discord isn't responding?", e, MantaroBot.class, "Shard Count Fetcher"
            );
            log.error("Unable to fetch shard count", e);
            System.exit(SHARD_FETCH_FAILURE);
        }
        return 1;
    }
    
    /**
     * Starts building all the necessary Shards to start this bot instance.
     * After finishing loading all the necessary shards, this will call {@link ShardedMantaro#startPostLoadProcedure(long)} and set everything so the bot
     * will be functioning completely (sends {@link PostLoadEvent}, marks the core as ready, starts posting stats to most popular discord bot lists,
     * starts the birthday checker and starts the ShardWatcherThread both on all the started shards.
     */
    public void shard() {
        try {
            MantaroCore.setLoadState(LoadState.LOADING_SHARDS);
            log.info("Spawning shards...");
            long start = System.currentTimeMillis();
            for(int i = fromShard; i < (toShard == 0 ? totalShards : toShard); i++) {
                if(MantaroData.config().get().upToShard != 0 && i > MantaroData.config().get().upToShard)
                    continue;
                
                log.info("Starting shard #" + i + " of " + (toShard == 0 ? totalShards : toShard - fromShard));
                
                //The custom event manager instance is important so we can track when we received the last event, or if we're receiving events at all.
                MantaroEventManager manager = new MantaroEventManager();
                managers.add(manager);
                
                //Builds the new MantaroShard instance, which will start the shard.
                shards[i] = new MantaroShard(i, totalShards, manager, processor, sessionController);
                log.debug("Finished loading shard #" + i + ".");
            }
            
            //Beep-boop, we finished loading!
            this.startPostLoadProcedure(start);
        } catch(Exception e) {
            e.printStackTrace();
            SentryHelper.captureExceptionContext("Shards failed to initialize!", e, this.getClass(), "Shard Loader");
        }
    }
    
    private void startPostLoadProcedure(long start) {
        long end = System.currentTimeMillis();
        MantaroBot bot = MantaroBot.getInstance();
        
        //Start the reconnect queue.
        bot.getCore().markAsReady();
        
        System.out.println("[-=-=-=-=-=- MANTARO STARTED -=-=-=-=-=-]");
        LogUtils.shard(String.format("Loaded all %d (of a total of %d) shards in %d seconds.", config.upToShard, totalShards, (end - start) / 1000));
        log.info("Loaded all shards successfully... Starting ShardWatcher! Status: {}", MantaroCore.getLoadState());
        
        new Thread(new ShardWatcher(), "ShardWatcherThread").start();
        bot.getCore().getShardEventBus().post(new PostLoadEvent());
        
        startUpdaters();
        bot.startCheckingBirthdays();
    }
    
    private void startUpdaters() {
        Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Carbonitex post task"))
                .scheduleAtFixedRate(carbonitex::handle, 0, 30, TimeUnit.MINUTES);
        
        if(config.dbotsorgToken != null) {
            Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "dbots.org update thread")).scheduleAtFixedRate(() -> {
                try {
                    long count = MantaroBot.getInstance().getShardManager().getGuildCache().size();
                    int[] shards = MantaroBot.getInstance().getShardList().stream().mapToInt(shard -> (int) shard.getJDA().getGuildCache().size()).toArray();
                    discordBotsAPI.postStats(shards).execute();
                    log.debug("Updated server count ({}) for discordbots.org", count);
                } catch(Exception ignored) {
                }
            }, 0, 1, TimeUnit.HOURS);
        } else {
            log.warn("discordbots.org token not set in config, cannot start posting stats!");
        }
        
        for(MantaroShard shard : getShards()) {
            shard.updateStatus();
        }
    }
    
    public List<MantaroEventManager> getManagers() {
        return this.managers;
    }
    
    public MantaroShard[] getShards() {
        return this.shards;
    }
    
    public int getTotalShards() {
        return this.totalShards;
    }
}
