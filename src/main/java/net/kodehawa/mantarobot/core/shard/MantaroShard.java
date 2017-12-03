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

/**
 * Represents a Discord shard.
 * This class and contains all the logic necessary to build, start and configure shards.
 * The logic for configuring sharded instances of the bot is on {@link net.kodehawa.mantarobot.core.MantaroCore}.
 *
 * This also handles posting stats to dbots/dbots.org/carbonitex. Because uh... no other class was fit for it.
 */
public class MantaroShard implements JDA {
    //Random stuff that gets in Mantaro's status that I wonder if anyone reads.
    public static final DataManager<List<String>> SPLASHES = new SimpleFileDataManager("assets/mantaro/texts/splashes.txt");

    public static final VoiceChannelListener VOICE_CHANNEL_LISTENER = new VoiceChannelListener();

    private static final Random RANDOM = new Random();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    //Natan's DBL API sender instance.
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
    //A instance of a ReconnectQueue that accounts for startup reconnects, avoiding OP2 spam on Shard reconnection during the startup procedure.
    private static LazyReconnectQueue reconnectQueue = new LazyReconnectQueue();
    @Delegate
    private JDA jda;

    private final Logger log;

    private final MantaroListener mantaroListener;
    private final CommandListener commandListener;

    private final int shardId;
    private final int totalShards;

    //A RateLimiter that keeps track of global ratelimits between shards.
    private static ShardedRateLimiter shardedRateLimiter = new ShardedRateLimiter();

    private BirthdayTask birthdayTask = new BirthdayTask();
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

    /**
     * Builds a new instance of a MantaroShard.
     * @param shardId The id of the newly-created shard.
     * @param totalShards The total quantity of shards that the bot will startup with.
     * @param manager The event manager.
     * @param commandProcessor The {@link ICommandProcessor} used to process upcoming Commands.
     *
     * @throws RateLimitedException
     * @throws LoginException
     * @throws InterruptedException
     */
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

    /**
     * Starts a new Shard.
     * This method builds a {@link JDA} instance and then attempts to start it up.
     * This locks until the shard finds a status of AWAITING_LOGIN_CONFIRMATION + 5 seconds.
     *
     * The newly-started shard will have auto reconnect enabled, a core pool size of 18 and a new NAS instance. The rest is defined either on global or instance
     * variables.
     *
     * @param force Whether we will call {@link JDA#shutdown()} or {@link JDA#shutdownNow()}
     * @throws RateLimitedException
     * @throws LoginException
     * @throws InterruptedException
     */
    public void start(boolean force) throws RateLimitedException, LoginException, InterruptedException {
        if(jda != null) {
            log.info("Attempting to drop shard {}...", shardId);
            prepareShutdown();

            if(!force)
                jda.shutdown();
            else
                jda.shutdownNow();

            log.info("Dropped shard #{} successfully!", shardId);
            removeListeners();
        }

        JDABuilder jdaBuilder = new JDABuilder(AccountType.BOT)
                .setToken(config().get().token)
                .setAutoReconnect(true)
                .setCorePoolSize(18)
                .setAudioSendFactory(new NativeAudioSendFactory())
                .setEventManager(manager)
                .setShardedRateLimiter(shardedRateLimiter)
                .setReconnectQueue(reconnectQueue)
                .setGame(Game.playing("Hold on to your seatbelts!"));

        if(totalShards > 1) jdaBuilder.useSharding(shardId, totalShards);
        jda = jdaBuilder.buildBlocking(Status.AWAITING_LOGIN_CONFIRMATION);
        if(totalShards > 1) Thread.sleep(5000);

        //Assume everything is alright~
        addListeners();
    }

    private void addListeners() {
        log.debug("Added all listeners for shard {}", shardId);
        jda.addEventListener(mantaroListener, commandListener, VOICE_CHANNEL_LISTENER, InteractiveOperations.listener(), ReactionOperations.listener());
    }

    private void removeListeners() {
        log.debug("Removed all listeners for shard {}", shardId);
        jda.removeEventListener(mantaroListener, commandListener, VOICE_CHANNEL_LISTENER, InteractiveOperations.listener(), ReactionOperations.listener());
    }

    /**
     * Starts the birthday task wait until tomorrow. When 00:00 arrives, this will call {@link BirthdayTask#handle(int)} every 24 hours.
     * Every shard has one birthday task.
     * @param millisecondsUntilTomorrow The amount of milliseconds until 00:00.
     */
    public void startBirthdayTask(long millisecondsUntilTomorrow) {
        log.debug("Started birthday task for shard {}, scheduled to run in {} ms more", shardId, millisecondsUntilTomorrow);

        executorService.scheduleWithFixedDelay(() -> birthdayTask.handle(shardId),
                millisecondsUntilTomorrow, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }

    /**
     * Handles updating the server count to most of the popular bot lists.
     */
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
                    log.debug("Updated server count ({}) for bots.discord.pw on Shard {}", count, shardId);
                } catch(Exception ignored) { }
            }, 1, TimeUnit.HOURS);
        } else {
            log.warn("bots.discord.pw token not set in config, cannot start posting stats!");
        }

        if(dbotsOrgToken != null) {
            Async.task("dbots.org update thread", () -> {
                try {
                    int count = jda.getGuilds().size();
                    discordBotsAPI.postStats(getId(), totalShards, count);
                    log.debug("Updated server count ({}) for discordbots.org on Shard {}", count, shardId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 1, TimeUnit.HOURS);
        } else {
            log.warn("discordbots.org token not set in config, cannot start posting stats!");
        }
    }

    /**
     * Updates Mantaro's "splash".
     * Splashes are random gags like "now seen in theaters!" that show on Mantaro's status.
     * This has been on Mantaro since at least a year, so it's part of its "personality" as a bot.
     */
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

            getJDA().getPresence().setGame(Game.playing(String.format("%shelp | %s | [%d]", config().get().prefix[0], newStatus, getId())));
            log.debug("Changed status to: " + newStatus);
        };

        changeStatus.run();
        Async.task("Splash Thread", changeStatus, 10, TimeUnit.MINUTES);
    }

    /**
     * @return The current {@link MantaroEventManager} for this specific instance.
     */
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

    //This used to be bigger...
    public void prepareShutdown() {
        jda.removeEventListener(jda.getRegisteredListeners().toArray());
    }

    @Override
    public String toString() {
        return "MantaroShard [" + (getId()) + " / " + totalShards + "]";
    }
}
