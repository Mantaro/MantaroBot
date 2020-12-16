/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lavalink.client.io.LavalinkLoadBalancer;
import lavalink.client.io.LessAnnoyingJdaLavalink;
import lavalink.client.io.jda.JdaLavalink;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.kodehawa.lib.imageboards.ImageBoard;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.moderation.MuteTask;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.commands.utils.birthday.BirthdayCacher;
import net.kodehawa.mantarobot.commands.utils.birthday.BirthdayTask;
import net.kodehawa.mantarobot.commands.utils.reminders.ReminderTask;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogFilter;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.Prometheus;
import net.kodehawa.mantarobot.utils.TracingPrintStream;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;
import net.kodehawa.mantarobot.utils.exporters.Metrics;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.kodehawa.mantarobot.utils.ShutdownCodes.API_HANDSHAKE_FAILURE;
import static net.kodehawa.mantarobot.utils.ShutdownCodes.FATAL_FAILURE;

@SuppressWarnings("SameReturnValue")
public class MantaroBot {
    private static final Logger log = LoggerFactory.getLogger(MantaroBot.class);
    private static MantaroBot instance;

    // Just in case
    static {
        log.info("Starting up Mantaro {}, Git revision: {}", MantaroInfo.VERSION, MantaroInfo.GIT_REVISION);
        log.info("Reporting UA {} for HTTP requests.", MantaroInfo.USER_AGENT);

        if (ExtraRuntimeOptions.VERBOSE) {
            System.setOut(new TracingPrintStream(System.out));
            System.setErr(new TracingPrintStream(System.err));
        }

        RestAction.setPassContext(true);
        if (ExtraRuntimeOptions.DEBUG) {
            log.info("Running in debug mode!");
        } else {
            RestAction.setDefaultFailure(ErrorResponseException.ignore(
                    RestAction.getDefaultFailure(),
                    ErrorResponse.UNKNOWN_MESSAGE
            ));
        }

        log.info("Filtering all logs below {}", LogFilter.LEVEL);
    }

    private final MantaroAudioManager audioManager;
    private final MantaroCore core;
    private final JdaLavalink lavaLink;
    private final Config config = MantaroData.config().get();

    private final BirthdayCacher birthdayCacher;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(
            3, new ThreadFactoryBuilder().setNameFormat("Mantaro Scheduled Executor Thread-%d").build()
    );

    private MantaroBot() throws Exception {
        if (ExtraRuntimeOptions.PRINT_VARIABLES || ExtraRuntimeOptions.DEBUG) {
            printStartVariables();
        }

        instance = this;

        if (config.needApi) {
            try {
                Request request = new Request.Builder()
                        .url(config.apiTwoUrl + "/mantaroapi/ping")
                        .build();

                Response httpResponse = Utils.httpClient.newCall(request).execute();
                if (httpResponse.code() != 200) {
                    log.error(
                            "Cannot connect to the API! Wrong status code? Returned: {}, Expected: 200",
                            httpResponse.code()
                    );

                    System.exit(API_HANDSHAKE_FAILURE);
                }

                httpResponse.close();
            } catch (ConnectException e) {
                log.error("Cannot connect to the API! Exiting...", e);
                System.exit(API_HANDSHAKE_FAILURE);
            }
        }

        // Lavalink stuff.
        lavaLink = new LessAnnoyingJdaLavalink(
                config.clientId,
                ExtraRuntimeOptions.SHARD_COUNT.orElse(config.totalShards),
                shardId -> getShardManager().getShardById(shardId)
        );

        for (var node : config.getLavalinkNodes()) {
            lavaLink.addNode(new URI(node), config.lavalinkPass);
        }

        // Choose the server with the lowest player amount
        lavaLink.getLoadBalancer().addPenalty(LavalinkLoadBalancer.Penalties::getPlayerPenalty);
        lavaLink.getLoadBalancer().addPenalty(LavalinkLoadBalancer.Penalties::getCpuPenalty);

        core = new MantaroCore(config, ExtraRuntimeOptions.DEBUG);
        audioManager = new MantaroAudioManager();
        birthdayCacher = new BirthdayCacher();
        ItemHelper.setItemActions();

        LogUtils.log("Startup",
                "Starting up Mantaro %s (Git: %s) in Node %s\nHold your seatbelts! <3"
                        .formatted(MantaroInfo.VERSION, MantaroInfo.GIT_REVISION, getNodeNumber())
        );

        core.setCommandsPackage("net.kodehawa.mantarobot.commands")
                .setOptionsPackage("net.kodehawa.mantarobot.options")
                .start();

        log.info("Finished loading basic components. Current status: {}", MantaroCore.getLoadState());
        MantaroData.config().save();
        ImageBoard.setUserAgent(MantaroInfo.USER_AGENT);

        // Handle the removal of mutes.
        ScheduledExecutorService muteExecutor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("Mantaro Mute Task").build()
        );

        muteExecutor.scheduleAtFixedRate(MuteTask::handle, 0, 1, TimeUnit.MINUTES);

        // Handle the delivery of reminders, assuming this is the master node.
        if (isMasterNode()) {
            ScheduledExecutorService reminderExecutor = Executors.newSingleThreadScheduledExecutor(
                    new ThreadFactoryBuilder().setNameFormat("Mantaro Reminder Handler").build()
            );

            reminderExecutor.scheduleAtFixedRate(ReminderTask::handle, 0, 30, TimeUnit.SECONDS);
        }

        // Yes, this is needed.
        ScheduledExecutorService ratelimitMapExecutor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("Mantaro Ratelimit Clear").build()
        );

        ratelimitMapExecutor.scheduleAtFixedRate(RatelimitUtils.ratelimitedUsers::clear, 0, 24, TimeUnit.HOURS);

        // Handle posting statistics.
        ScheduledExecutorService postExecutor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("Mantaro Statistics Posting").build()
        );

        postExecutor.scheduleAtFixedRate(() -> postStats(getShardManager()), 10, 5, TimeUnit.MINUTES);

        // This is basically done because Andesite doesn't destroy players on shutdown
        // when using LL compat. This causes players to not work on next startup.
        // Work around it by just killing/destroying all players before shutdown ends.
        var thread = new ThreadFactoryBuilder().setNameFormat("Mantaro Shutdown Hook").build();
        Runtime.getRuntime().addShutdownHook(thread.newThread(() -> {
            log.info("Destroying all active players...");
            for (var players : audioManager.getMusicManagers().entrySet()) {
                players.getValue().getLavaLink().destroy();
            }

            log.info("Destroyed all players. Not aware of anything holding off shutdown now");
        }));
    }

    public static void main(String[] args) {
        // Start internal metrics collection.
        try {
            Prometheus.enable();
        } catch (Exception e) {
            log.error("Unable to start prometheus client!", e);
        }

        // Attempt to start the bot process itself.
        try {
            new MantaroBot();
        } catch (Exception e) {
            log.error("Could not complete Main Thread routine!", e);
            log.error("Cannot continue! Exiting program...");
            System.exit(FATAL_FAILURE);
        }

        // Must be registered after MantaroBot.instance is set
        Prometheus.registerPostStartup();
    }

    public static boolean isDebug() {
        return ExtraRuntimeOptions.DEBUG;
    }

    public static boolean isVerbose() {
        return ExtraRuntimeOptions.VERBOSE;
    }

    public static MantaroBot getInstance() {
        return MantaroBot.instance;
    }

    public ShardManager getShardManager() {
        return core.getShardManager();
    }

    public JDA getShard(int id) {
        return getShardManager().getShardById(id);
    }

    public void restartShard(int shardId) {
        getShardManager().restart(shardId);
    }

    public JDA getShardGuild(String guildId) {
        return getShardGuild(MiscUtil.parseSnowflake(guildId));
    }

    public JDA getShardGuild(long guildId) {
        return getShardManager().getShardById(
                (int) ((guildId >> 22) % getShardManager().getShardsTotal())
        );
    }

    public int getShardIdForGuild(long guildId) {
        return (int) ((guildId >> 22) % getShardManager().getShardsTotal());
    }

    // You would ask, doesn't ShardManager#getShardsTotal do that? Absolutely not. It's screwed. Fucked. I dunno why.
    // DefaultShardManager overrides it, nvm, ouch.
    public int getManagedShards() {
        return getShardManager().getShardsRunning() + getShardManager().getShardsQueued();
    }

    public List<JDA> getShardList() {
        return IntStream.range(0, getManagedShards())
                .mapToObj(this::getShard)
                .collect(Collectors.toList());
    }

    public void startCheckingBirthdays() {
        Metrics.THREAD_POOL_COLLECTOR.add("birthday-tracker", executorService);
        log.info("Starting to check birthdays...");
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2,
                new ThreadFactoryBuilder().setNameFormat("Mantaro Birthday Executor Thread-%d").build()
        );

        var random = new Random();
        // How much until tomorrow? That's the initial delay, then run it once a day.
        var zoneId = ZoneId.of("America/Chicago");
        var now = ZonedDateTime.now(zoneId);
        var tomorrow = now.toLocalDate().plusDays(1);
        var tomorrowStart = tomorrow.atStartOfDay(zoneId);
        var duration = Duration.between(now, tomorrowStart);
        var millisecondsUntilTomorrow = duration.toMillis();

        // Start the birthday task on all shards.
        // This is because running them in parallel is way better than running it once for all shards.
        // It actually cut off the time from 50 minutes to 20 seconds.
        for (var shard : core.getShards()) {
            log.debug("Started birthday task for shard {}, scheduled to run in {} ms more", shard.getId(), millisecondsUntilTomorrow);

            // Back off this call up to 300 seconds to avoid sending a bunch of requests to discord at the same time
            // This will happen anywhere from 0 seconds after 00:00 to 300 seconds after or before 00:00 (so 23:57 or 00:03)
            // This back-off is per-shard, so this makes it so the requests are more spaced out.
            // Shouldn't matter much for the end user, but makes so batch requests don't fuck over ratelimits immediately.
            var maxBackoff = 300_000; // In millis
            var randomBackoff = random.nextBoolean() ? -random.nextInt(maxBackoff) : random.nextInt(maxBackoff);
            executorService.scheduleWithFixedDelay(() -> BirthdayTask.handle(shard.getId()),
                    millisecondsUntilTomorrow + randomBackoff, TimeUnit.DAYS.toMillis(1) + randomBackoff, TimeUnit.MILLISECONDS);
        }

        // Start the birthday cacher.
        executorService.scheduleWithFixedDelay(birthdayCacher::cache, 22, 23, TimeUnit.HOURS);
    }

    private void postStats(ShardManager manager) {
        for(var jda : manager.getShardCache()) {
            if (jda.getStatus() == JDA.Status.INITIALIZED || jda.getStatus() == JDA.Status.SHUTDOWN) {
                return;
            }

            try(var jedis = MantaroData.getDefaultJedisPool().getResource()) {
                var json = new JSONObject()
                        .put("guild_count", jda.getGuildCache().size())
                        .put("cached_users", jda.getUserCache().size())
                        .put("gateway_ping", jda.getGatewayPing())
                        .put("shard_status", jda.getStatus())
                        .put("last_ping_diff",
                                ((MantaroEventManager) jda.getEventManager()).lastJDAEventDiff()
                        )
                        .put("node_number", MantaroBot.getInstance().getNodeNumber())
                        .toString();

                jedis.hset("shardstats-" + config.getClientId(),
                        String.valueOf(jda.getShardInfo().getShardId()), json
                );

                log.debug("Sent process shard stats to Redis (Global) [Running Shards: {}] -> {}",
                        manager.getShardsRunning(), json
                );
            }
        }
    }

    public void forceRestartShardFromGuild(String guildId) {
        restartShard(getShardGuild(guildId).getShardInfo().getShardId());
    }

    public MantaroAudioManager getAudioManager() {
        return this.audioManager;
    }

    public MantaroCore getCore() {
        return this.core;
    }

    public BirthdayCacher getBirthdayCacher() {
        return this.birthdayCacher;
    }

    public ScheduledExecutorService getExecutorService() {
        return this.executorService;
    }

    public JdaLavalink getLavaLink() {
        return this.lavaLink;
    }

    public boolean isMasterNode() {
        if (ExtraRuntimeOptions.SHARD_SUBSET && ExtraRuntimeOptions.FROM_SHARD.isPresent()) {
            return ExtraRuntimeOptions.FROM_SHARD.getAsInt() == 0;
        }

        return true;
    }

    public String getShardSlice() {
        if (ExtraRuntimeOptions.SHARD_SUBSET) {
            //noinspection OptionalGetWithoutIsPresent
            return ExtraRuntimeOptions.FROM_SHARD.getAsInt() + " to " + ExtraRuntimeOptions.TO_SHARD.getAsInt();
        } else {
            return "0 to " + getShardManager().getShardsTotal();
        }
    }

    public int getNodeNumber() {
        return ExtraRuntimeOptions.NODE_NUMBER.orElse(0);
    }

    // This will print if the MANTARO_PRINT_VARIABLES env variable is present.
    private void printStartVariables() {
        log.info("""
                Environment variables set on this startup:
                VERBOSE_SHARD_LOGS = {}
                DEBUG = {}
                DEBUG_LOGS = {}
                LOG_DB_ACCESS = {}
                TRACE_LOGS = {}
                VERBOSE = {}
                VERBOSE_SHARD_LOGS = {}
                FROM_SHARD = {}
                TO_SHARD = {}
                SHARD_COUNT = {}
                NODE_NUMBER = {}""",
                ExtraRuntimeOptions.VERBOSE_SHARD_LOGS,
                ExtraRuntimeOptions.DEBUG,
                ExtraRuntimeOptions.DEBUG_LOGS,
                ExtraRuntimeOptions.LOG_DB_ACCESS,
                ExtraRuntimeOptions.TRACE_LOGS,
                ExtraRuntimeOptions.VERBOSE,
                ExtraRuntimeOptions.VERBOSE_SHARD_LOGS,
                ExtraRuntimeOptions.FROM_SHARD,
                ExtraRuntimeOptions.TO_SHARD,
                ExtraRuntimeOptions.SHARD_COUNT,
                ExtraRuntimeOptions.NODE_NUMBER
        );
    }
}
