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
import com.github.natanbc.discordbotsapi.DiscordBotsAPI;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import lombok.Getter;
import lombok.experimental.Delegate;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.ShardedRateLimiter;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.listener.VoiceChannelListener;
import net.kodehawa.mantarobot.commands.utils.birthday.BirthdayTask;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.ReactionOperations;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.core.shard.jda.reconnect.LazyReconnectQueue;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;
import static net.kodehawa.mantarobot.data.MantaroData.config;
import static net.kodehawa.mantarobot.utils.Utils.pretty;

public class MantaroShard implements JDA {
    public static final DataManager<List<String>> SPLASHES = new SimpleFileDataManager("assets/mantaro/texts/splashes.txt");
    public static final VoiceChannelListener VOICE_CHANNEL_LISTENER = new VoiceChannelListener();
    private static final Random RANDOM = new Random();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final DiscordBotsAPI discordBotsAPI = new DiscordBotsAPI(MantaroData.config().get().dbotsorgToken);

    static {
        if(SPLASHES.get().removeIf(s -> s == null || s.isEmpty())) SPLASHES.save();
    }

    @Getter
    public final MantaroEventManager manager;
    @Getter
    private final ExecutorService commandPool;
    @Getter
    private final ExecutorService threadPool;
    @Getter
    private static LazyReconnectQueue reconnectQueue = new LazyReconnectQueue();
    @Delegate
    private JDA jda;

    private final Logger log;
    private final MantaroListener mantaroListener;
    private final int shardId;
    private final CommandListener commandListener;
    private final int totalShards;
    private static ShardedRateLimiter shardedRateLimiter = new ShardedRateLimiter();



    private BirthdayTask birthdayTask = new BirthdayTask();
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

    public MantaroShard(int shardId, int totalShards, MantaroEventManager manager, ICommandProcessor commandProcessor) throws RateLimitedException, LoginException, InterruptedException {
        this.shardId = shardId;
        this.totalShards = totalShards;
        this.manager = manager;

        ThreadFactory normalTPNamedFactory =
                new ThreadFactoryBuilder()
                        .setNameFormat("MantaroShard-Executor[" + shardId + "/" + totalShards + "] Thread-%d")
                        .build();

        ThreadFactory commandTPNamedFactory =
                new ThreadFactoryBuilder()
                        .setNameFormat("MantaroShard-Command[" + shardId + "/" + totalShards + "] Thread-%d")
                        .build();

        threadPool = Executors.newCachedThreadPool(normalTPNamedFactory);
        commandPool = Executors.newCachedThreadPool(commandTPNamedFactory);

        log = LoggerFactory.getLogger("MantaroShard-" + shardId);
        mantaroListener = new MantaroListener(shardId, this);
        commandListener = new CommandListener(shardId, this, commandProcessor);

        start(false);
    }

    public void start(boolean force) throws RateLimitedException, LoginException, InterruptedException {
        if(jda != null) {
            log.info("Attempting to drop shard #" + shardId);
            if(!force) prepareShutdown();
            jda.shutdownNow();
            log.info("Dropped shard #" + shardId);
            removeListeners();
        }

        JDABuilder jdaBuilder = new JDABuilder(AccountType.BOT)
                .setToken(config().get().token)
                .setAutoReconnect(true)
                .setCorePoolSize(15)
                .setAudioSendFactory(new NativeAudioSendFactory())
                .setEventManager(manager)
                .setShardedRateLimiter(shardedRateLimiter)
                .setReconnectQueue(reconnectQueue)
                .setGame(Game.of("Hold on to your seatbelts!"));

        if(totalShards > 1) jdaBuilder.useSharding(shardId, totalShards);
        jda = jdaBuilder.buildBlocking(Status.AWAITING_LOGIN_CONFIRMATION);
        if(totalShards > 1) Thread.sleep(5000);

        //Assume everything is alright~
        addListeners();
    }

    private void addListeners() {
        jda.addEventListener(mantaroListener, commandListener, VOICE_CHANNEL_LISTENER, InteractiveOperations.listener(), ReactionOperations.listener());
    }

    private void removeListeners() {
        jda.removeEventListener(mantaroListener, commandListener, VOICE_CHANNEL_LISTENER, InteractiveOperations.listener(), ReactionOperations.listener());
    }

    public void startBirthdayTask(long millisecondsUntilTomorrow) {
        log.debug("Started birthday task for shard {}, scheduled to run in {} ms more", shardId, millisecondsUntilTomorrow);

        executorService.scheduleWithFixedDelay(() -> birthdayTask.handle(shardId),
                millisecondsUntilTomorrow, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }

    public void updateServerCount() {
        OkHttpClient httpClient = new OkHttpClient();
        Config config = config().get();

        String dbotsToken = config.dbotsToken;
        String dbotsOrgToken = config.dbotsorgToken;

        if(dbotsToken != null) {
            Async.task("Dbots update Thread", () -> {
                try {
                    int count = jda.getGuilds().size();
                    RequestBody body = RequestBody.create(
                            JSON,
                            new JSONObject().put("server_count", count).put("shard_id", getId()).put("shard_count", totalShards).toString()
                    );

                    Request request = new Request.Builder()
                            .url("https://bots.discord.pw/api/bots/" + jda.getSelfUser().getId() + "/stats")
                            .addHeader("Authorization", dbotsToken)
                            .addHeader("Content-Type", "application/json")
                            .post(body)
                            .build();
                    httpClient.newCall(request).execute().close();
                } catch(Exception ignored) { }
            }, 1, TimeUnit.HOURS);
        }

        if(dbotsOrgToken != null) {
            Async.task("dbots.org update thread", () -> {
                try {
                    discordBotsAPI.postStats(getId(), totalShards, jda.getGuilds().size());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 1, TimeUnit.HOURS);
        }
    }

    public void updateStatus() {
        Runnable changeStatus = () -> {
            AtomicInteger users = new AtomicInteger(0), guilds = new AtomicInteger(0);
            if(MantaroBot.getInstance() != null) {
                Arrays.stream(MantaroBot.getInstance().getShardedMantaro().getShards()).filter(Objects::nonNull).map(MantaroShard::getJDA).forEach(jda -> {
                    users.addAndGet((int) jda.getUserCache().size());
                    guilds.addAndGet((int) jda.getGuildCache().size());
                });
            }
            String newStatus = random(SPLASHES.get(), RANDOM)
                    .replace("%ramgb%", String.valueOf(((long) (Runtime.getRuntime().maxMemory() * 1.2D)) >> 30L))
                    .replace("%usercount%", users.toString())
                    .replace("%guildcount%", guilds.toString())
                    .replace("%shardcount%", String.valueOf(getTotalShards()))
                    .replace("%prettyusercount%", pretty(users.get()))
                    .replace("%prettyguildcount%", pretty(guilds.get()));

            getJDA().getPresence().setGame(Game.of(config().get().prefix[0] + "help | " + newStatus + " | [" + getId() + "]"));
            log.debug("Changed status to: " + newStatus);
        };

        changeStatus.run();
        Async.task("Splash Thread", changeStatus, 600, TimeUnit.SECONDS);
    }

    public MantaroEventManager getEventManager() {
        return manager;
    }

    public int getId() {
        return shardId;
    }

    public JDA getJDA() {
        return jda;
    }

    private int getTotalShards() {
        return totalShards;
    }

    public void prepareShutdown() {
        jda.removeEventListener(jda.getRegisteredListeners().toArray());
    }

    @Override
    public String toString() {
        return "MantaroShard [" + (getId()) + " / " + totalShards + "]";
    }
}
