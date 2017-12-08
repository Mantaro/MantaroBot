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

package net.kodehawa.mantarobot.core.shard;

import br.com.brjdevs.java.utils.async.Async;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.LoadState;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.events.PostLoadEvent;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.core.shard.watcher.ShardWatcher;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.services.Carbonitex;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.Utils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.utils.ShutdownCodes.SHARD_FETCH_FAILURE;

/**
 * Represents a Sharded bot.
 * This class will still be used whether we have zero or a billion shards tho, but Mantaro is more optimized to run with shards.
 * It holds all the necessary info for the bot to correctly function in a sharded enviroment, while also providing access to {@link ICommandProcessor} and
 * other extermely important stuff.
 */
@Slf4j
public class ShardedMantaro {

    @Getter
    private final List<MantaroEventManager> managers = new ArrayList<>();
    private final ICommandProcessor processor;
    @Getter
    private final MantaroShard[] shards;
    @Getter
    private final int totalShards;
    private final Carbonitex carbonitex = new Carbonitex();


    public ShardedMantaro(int totalShards, boolean isDebug, boolean auto, String token, ICommandProcessor commandProcessor) {
        int shardAmount = totalShards;
        if(auto) shardAmount = getRecommendedShards(token);
        if(isDebug) shardAmount = 2;
        this.totalShards = shardAmount;
        processor = commandProcessor;
        shards = new MantaroShard[this.totalShards];
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
        } catch(Exception e) {
            SentryHelper.captureExceptionContext(
                    "Exception thrown when trying to get shard count, discord isn't responding?", e, MantaroBot.class, "Shard Count Fetcher"
            );
            System.exit(SHARD_FETCH_FAILURE);
        }
        return 1;
    }

    /**
     * Starts building all the necessary Shards to start this bot instance.
     * After finishing loading all the necessary shards, this will call {@link ShardedMantaro#startPostLoadProcedure(long)} and set everything so the bot
     * will be functioning completely (sends {@link PostLoadEvent}, marks the core as ready, starts posting stats to most popular discord bot lists,
     * starts the birthday checker, starts the ShardWatcherThread both on all the started shards and finally it starts the
     * {@link net.kodehawa.mantarobot.core.shard.jda.reconnect.LazyReconnectQueue} instance so stale shards will start reconnecting.
     */
    public void shard() {
        try {
            MantaroCore.setLoadState(LoadState.LOADING_SHARDS);
            log.info("Spawning shards...");
            long start = System.currentTimeMillis();
            for(int i = 0; i < totalShards; i++) {
                if(MantaroData.config().get().upToShard != 0 && i > MantaroData.config().get().upToShard) continue;

                log.info("Starting shard #" + i + " of " + totalShards);

                //The custom event manager instance is important so we can track when we received the last event, or if we're receiving events at all.
                MantaroEventManager manager = new MantaroEventManager();
                managers.add(manager);

                //Builds the new MantaroShard instance, which will start the shard.
                shards[i] = new MantaroShard(i, totalShards, manager, processor);
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
        MantaroShard.getReconnectQueue().ready();
        bot.getCore().markAsReady();

        System.out.println("[-=-=-=-=-=- MANTARO STARTED -=-=-=-=-=-]");
        LogUtils.shard(String.format("Loaded all %d shards in %d seconds.", totalShards, (end - start) / 1000));
        log.info("Loaded all shards succesfully... Starting ShardWatcher! Status: {}", MantaroCore.getLoadState());

        Async.thread("ShardWatcherThread", new ShardWatcher());
        bot.getCore().getShardEventBus().post(new PostLoadEvent());

        startUpdaters();
        bot.startCheckingBirthdays();
    }

    private void startUpdaters() {
        Async.task("Carbonitex post task", carbonitex::handle, 30, TimeUnit.MINUTES);

        for(MantaroShard shard : getShards()) {
            shard.updateServerCount();
            shard.updateStatus();
        }
    }
}
