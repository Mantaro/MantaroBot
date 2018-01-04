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

package net.kodehawa.mantarobot.core.shard;

import br.com.brjdevs.java.utils.async.Async;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.natanbc.discordbotsapi.DiscordBotsAPI;
import com.github.natanbc.discordbotsapi.PostingException;
import gnu.trove.impl.unmodifiable.TUnmodifiableLongSet;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.utils.cache.SnowflakeCacheView;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.LoadState;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.events.PostLoadEvent;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.core.shard.watcher.ShardWatcher;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.services.Carbonitex;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.Utils;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.ShutdownCodes.SHARD_FETCH_FAILURE;

/**
 * Represents a Sharded bot.
 * This class will still be used whether we have zero or a billion shards tho, but Mantaro is more optimized to run with shards.
 * It holds all the necessary info for the bot to correctly function in a sharded enviroment, while also providing access to {@link ICommandProcessor} and
 * other extremely important stuff.
 */
@Slf4j
public class ShardedMantaro {

    private final Carbonitex carbonitex = new Carbonitex();
    private final Config config = MantaroData.config().get();
    private final DiscordBotsAPI discordBotsAPI = new DiscordBotsAPI(MantaroData.config().get().dbotsorgToken);
    @Getter
    private final List<MantaroEventManager> managers = new ArrayList<>();
    private final ICommandProcessor processor;
    @Getter
    private final MantaroShard[] shards;
    @Getter
    private final int totalShards;
    @Getter
    private TUnmodifiableLongSet discordBotsUpvoters = new TUnmodifiableLongSet(new TLongHashSet());
    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * starts the birthday checker and starts the ShardWatcherThread both on all the started shards.
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
        bot.getCore().markAsReady();

        System.out.println("[-=-=-=-=-=- MANTARO STARTED -=-=-=-=-=-]");
        LogUtils.shard(String.format("Loaded all %d shards in %d seconds.", totalShards, (end - start) / 1000));
        log.info("Loaded all shards successfully... Starting ShardWatcher! Status: {}", MantaroCore.getLoadState());

        Async.thread("ShardWatcherThread", new ShardWatcher());
        bot.getCore().getShardEventBus().post(new PostLoadEvent());

        startUpdaters();
        bot.startCheckingBirthdays();

        Async.task(() -> {
            try {
                SnowflakeCacheView<VoiceChannel> vc = MantaroBot.getInstance().getVoiceChannelCache();
                MantaroBot.getInstance().getStatsClient().gauge("music_players", vc.stream().filter(voiceChannel -> voiceChannel.getMembers().contains(voiceChannel.getGuild().getSelfMember())).count());
            } catch (Exception ignored) {} //Avoid the scheduled task to unexpectedly end on exception (probably ConcurrentModificationException but let's just catch all errors)
        }, 20, TimeUnit.SECONDS);
    }

    private void startUpdaters() {
        Async.task("Carbonitex post task", carbonitex::handle, 30, TimeUnit.MINUTES);

        if(config.dbotsorgToken != null) {
            Async.task("dbots.org update thread", () -> {
                try {
                    long count = MantaroBot.getInstance().getGuildCache().size();
                    int[] shards = MantaroBot.getInstance().getShardList().stream().mapToInt(shard -> (int) shard.getGuildCache().size()).toArray();
                    discordBotsAPI.postStats(shards);
                    log.debug("Updated server count ({}) for discordbots.org", count);
                } catch(Exception ignored) {}
            }, 1, TimeUnit.HOURS);

            Async.task("discordbots.org upvotes task", () -> {
                if(config.dbotsorgToken == null) return;
                try {
                    Request request = new Request.Builder()
                            .url("https://discordbots.org/api/bots/213466096718708737/votes?onlyids=1")
                            .addHeader("Authorization", config.dbotsorgToken)
                            .build();

                    Response r = Utils.httpClient.newCall(request).execute();

                    ResponseBody body = r.body();
                    if(body == null)
                        return;

                    @SuppressWarnings("unchecked") //It's definitely String.
                    List<String> upvoters = objectMapper.readValue(body.string(), List.class);
                    List<Long> upvotersLong = upvoters.stream().map(Long::parseLong).distinct().collect(Collectors.toList());
                    TLongSet set = new TLongHashSet();
                    set.addAll(upvotersLong);
                    discordBotsUpvoters = new TUnmodifiableLongSet(set);

                    r.close();
                } catch(Exception ignored) {}
            }, 5, TimeUnit.MINUTES);
        } else {
            log.warn("discordbots.org token not set in config, cannot start posting stats!");
        }


        for(MantaroShard shard : getShards()) {
            shard.updateServerCount();
            shard.updateStatus();
        }
    }
}
