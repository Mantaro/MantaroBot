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
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.moderation.MuteTask;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.commands.utils.birthday.BirthdayCacher;
import net.kodehawa.mantarobot.commands.utils.birthday.BirthdayTask;
import net.kodehawa.mantarobot.commands.utils.reminders.ReminderTask;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.core.shard.Shard;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogFilter;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.Prometheus;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.TracingPrintStream;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.exporters.DiscordLatencyExports;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.kodehawa.mantarobot.utils.ShutdownCodes.API_HANDSHAKE_FAILURE;
import static net.kodehawa.mantarobot.utils.ShutdownCodes.FATAL_FAILURE;

public class MantaroBot {
    private static final Logger log = LoggerFactory.getLogger(MantaroBot.class);
    private static MantaroBot instance;

    //just in case
    static {
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

        log.info("Filtering all logs below " + LogFilter.LEVEL);
    }

    private final MantaroAudioManager audioManager;
    private final MantaroCore core;
    private final JdaLavalink lavaLink;

    private final BirthdayCacher birthdayCacher;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3, new ThreadFactoryBuilder().setNameFormat("Mantaro-ScheduledExecutor Thread-%d").build());

    private MantaroBot() throws Exception {
        if(ExtraRuntimeOptions.PRINT_VARIABLES || ExtraRuntimeOptions.DEBUG)
            printStartVariables();

        instance = this;
        Config config = MantaroData.config().get();

        if (config.needApi) {
            try {
                Request request = new Request.Builder()
                        .url(config.apiTwoUrl + "/mantaroapi/ping")
                        .build();
                Response httpResponse = Utils.httpClient.newCall(request).execute();

                if (httpResponse.code() != 200) {
                    log.error("Cannot connect to the API! Wrong status code...");
                    System.exit(API_HANDSHAKE_FAILURE);
                }

                httpResponse.close();
            } catch (ConnectException e) {
                log.error("Cannot connect to the API! Exiting...", e);
                System.exit(API_HANDSHAKE_FAILURE);
            }
        }

        //Lavalink stuff.
        lavaLink = new LessAnnoyingJdaLavalink(
                config.clientId,
                ExtraRuntimeOptions.SHARD_COUNT.orElse(config.totalShards),
                shardId -> getShardManager().getShardById(shardId)
        );

        for (String node : config.getLavalinkNodes())
            lavaLink.addNode(new URI(node), config.lavalinkPass);

        //Choose the server with the lowest player amount
        lavaLink.getLoadBalancer().addPenalty(LavalinkLoadBalancer.Penalties::getPlayerPenalty);

        core = new MantaroCore(config, true, true, ExtraRuntimeOptions.DEBUG);

        audioManager = new MantaroAudioManager();
        Items.setItemActions();

        birthdayCacher = new BirthdayCacher();

        LogUtils.log("Startup", String.format("Starting up Mantaro %s (Git: %s) in Node %s\nHold your seatbelts! <3",
                MantaroInfo.VERSION, MantaroInfo.GIT_REVISION, getNodeNumber())
        );

        core.setCommandsPackage("net.kodehawa.mantarobot.commands")
                .setOptionsPackage("net.kodehawa.mantarobot.options")
                .start();

        System.out.println("Finished loading basic components. Current status: " + MantaroCore.getLoadState());
        MantaroData.config().save();

        //Handle the removal of mutes.
        Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Mute Handler"))
                .scheduleAtFixedRate(MuteTask::handle, 0, 1, TimeUnit.MINUTES);

        //Handle the delivery of reminders, assuming this is the master node.
        if(isMasterNode()) {
            Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Reminder Handler"))
                    .scheduleAtFixedRate(ReminderTask::handle, 0, 30, TimeUnit.SECONDS);
        }

        //Yes, this is needed.
        Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Ratelimit Map Handler"))
                .scheduleAtFixedRate(Utils.ratelimitedUsers::clear, 0, 36, TimeUnit.HOURS);
    }

    public static void main(String[] args) {
        try {
            Prometheus.enable();
        } catch (Exception e) {
            SentryHelper.captureException("Unable to start prometheus client", e, MantaroBot.class);
            log.error("Unable to start prometheus client!", e);
        }
        try {
            new MantaroBot();
        } catch (Exception e) {
            SentryHelper.captureException("Couldn't start Mantaro at all, so something went seriously wrong", e, MantaroBot.class);
            log.error("Could not complete Main Thread routine!", e);
            log.error("Cannot continue! Exiting program...");
            System.exit(FATAL_FAILURE);
        }
        //must be registered after MantaroBot.instance is set
        DiscordLatencyExports.register();
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
        return getShardManager().getShardById((int) ((guildId >> 22) % getShardManager().getShardsTotal()));
    }

    public int getShardIdGuild(long guildId) {
        return (int) ((guildId >> 22) % getShardManager().getShardsTotal());
    }

    //You would ask, doesn't ShardManager#getShardsTotal do that? Absolutely not. It's screwed. Fucked. I dunno why.
    //DefaultShardManager overrides it, nvm, ouch.
    public int getManagedShards() {
        return getShardManager().getShardsRunning() + getShardManager().getShardsQueued();
    }

    public List<JDA> getShardList() {
        return IntStream.range(0, getManagedShards())
                .mapToObj(this::getShard)
                .collect(Collectors.toList());
    }

    public void startCheckingBirthdays() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder().setNameFormat("Mantaro-BirthdayExecutor Thread-%d").build());
        Prometheus.THREAD_POOL_COLLECTOR.add("birthday-tracker", executorService);

        //How much until tomorrow? That's the initial delay, then run it once a day.
        ZoneId z = ZoneId.of("America/Chicago");
        ZonedDateTime now = ZonedDateTime.now(z);
        LocalDate tomorrow = now.toLocalDate().plusDays(1);
        ZonedDateTime tomorrowStart = tomorrow.atStartOfDay(z);
        Duration duration = Duration.between(now, tomorrowStart);
        long millisecondsUntilTomorrow = duration.toMillis();

        //Start the birthday task on all shards.
        //This is because running them in parallel is way better than running it once for all shards.
        //It actually cut off the time from 50 minutes to 20 seconds.
        for (Shard shard : core.getShards()) {
            log.debug("Started birthday task for shard {}, scheduled to run in {} ms more", shard.getId(), millisecondsUntilTomorrow);

            executorService.scheduleWithFixedDelay(() -> BirthdayTask.handle(shard.getId()),
                    millisecondsUntilTomorrow, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
        }

        //Start the birthday cacher.
        executorService.scheduleWithFixedDelay(birthdayCacher::cache, 22, 23, TimeUnit.HOURS);
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
        if(ExtraRuntimeOptions.SHARD_SUBSET && ExtraRuntimeOptions.FROM_SHARD.isPresent()) {
            return ExtraRuntimeOptions.FROM_SHARD.getAsInt() == 0;
        }

        return true;
    }

    public String getShardSlice() {
        if(ExtraRuntimeOptions.SHARD_SUBSET) {
            //noinspection OptionalGetWithoutIsPresent
            return ExtraRuntimeOptions.FROM_SHARD.getAsInt() + " to " + ExtraRuntimeOptions.TO_SHARD.getAsInt();
        } else {
            return "0 to " + getShardManager().getShardsTotal();
        }
    }

    public int getNodeNumber() {
        return ExtraRuntimeOptions.NODE_NUMBER.orElse(0);
    }

    //This will print if the MANTARO_PRINT_VARIABLES env variable is present.
    private void printStartVariables() {
        log.info("Environment variables set on this startup:\n" +
                        "DISABLE_NON_ALLOCATING_BUFFER = {}\n" +
                        "VERBOSE_SHARD_LOGS = {}\n" +
                        "DEBUG = {}\n" + "DEBUG_LOGS = {}\n" +
                        "LOG_DB_ACCESS = {}\n" + "TRACE_LOGS = {}\n" +
                        "VERBOSE = {}\n" + "VERBOSE_SHARD_LOGS = {}\n" +
                        "FROM_SHARD = {}\n" + "TO_SHARD = {}\n" +
                        "SHARD_COUNT = {}\n" + "NODE_NUMBER = {}",
                ExtraRuntimeOptions.DISABLE_NON_ALLOCATING_BUFFER,
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
