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

package net.kodehawa.mantarobot;

import br.com.brjdevs.java.utils.async.Async;
import com.github.natanbc.discordbotsapi.DiscordBotsAPI;
import com.github.natanbc.discordbotsapi.PostingException;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import gnu.trove.impl.unmodifiable.TUnmodifiableLongSet;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.kodehawa.mantarobot.commands.moderation.MuteTask;
import net.kodehawa.mantarobot.commands.moderation.TempBanManager;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.commands.utils.birthday.BirthdayCacher;
import net.kodehawa.mantarobot.commands.utils.birthday.BirthdayTask;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.core.shard.MantaroShard;
import net.kodehawa.mantarobot.core.shard.ShardedMantaro;
import net.kodehawa.mantarobot.core.shard.jda.ShardedJDA;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.services.Carbonitex;
import net.kodehawa.mantarobot.utils.CompactPrintStream;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.data.ConnectionWatcherDataManager;
import net.kodehawa.mantarobot.utils.rmq.RabbitMQDataManager;
import net.kodehawa.mantarobot.web.MantaroAPI;
import net.kodehawa.mantarobot.web.MantaroAPISender;
import okhttp3.*;
import org.apache.commons.collections4.iterators.ArrayIterator;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.utils.ShutdownCodes.API_HANDSHAKE_FAILURE;
import static net.kodehawa.mantarobot.utils.ShutdownCodes.FATAL_FAILURE;

@Slf4j
public class MantaroBot extends ShardedJDA {

    public static int cwport;
    private static boolean DEBUG = false;
    @Getter
    private static ConnectionWatcherDataManager connectionWatcher;
    @Getter
    private static MantaroBot instance;
    @Getter
    private static TempBanManager tempBanManager;
    @Getter
    private final MantaroAudioManager audioManager;
    @Getter
    private final MantaroCore core;
    @Getter
    private final MantaroAPI mantaroAPI = new MantaroAPI();
    @Getter
    private final RabbitMQDataManager rabbitMQDataManager;
    @Getter
    private final ShardedMantaro shardedMantaro;
    @Getter
    private final StatsDClient statsClient;
    @Getter
    private final DiscordBotsAPI discordBotsAPI;
    @Getter
    private TUnmodifiableLongSet discordBotsUpvoters = new TUnmodifiableLongSet(new TLongHashSet());
    @Getter
    private BirthdayCacher birthdayCacher;
    @Getter
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
    private final MuteTask muteTask = new MuteTask();
    private final BirthdayTask birthdayTask = new BirthdayTask();
    private final Carbonitex carbonitex = new Carbonitex();

    private MantaroBot() throws Exception {
        instance = this;
        Config config = MantaroData.config().get();
        core = new MantaroCore(config, true, true, DEBUG);
        discordBotsAPI = new DiscordBotsAPI(config.dbotsorgToken);

        statsClient = new NonBlockingStatsDClient(
                config.isPremiumBot() ? "mantaro-patreon" : "mantaro",
                "localhost",
                8125,
                "tag:value"
        );

        if(!config.isPremiumBot() && !config.isBeta() && !mantaroAPI.configure()) {
            SentryHelper.captureMessage("Cannot send node data to the remote server or ping timed out. Mantaro will exit", MantaroBot.class);
            System.exit(API_HANDSHAKE_FAILURE);
        }

        LogUtils.log("Startup", String.format("Starting up MantaroBot %s\n" + "Hold your seatbelts! <3", MantaroInfo.VERSION));

        rabbitMQDataManager = new RabbitMQDataManager(config);
        if(!config.isPremiumBot() && !config.isBeta()) sendSignal();
        long start = System.currentTimeMillis();

        core.setCommandsPackage("net.kodehawa.mantarobot.commands")
                .setOptionsPackage("net.kodehawa.mantarobot.options")
                .startMainComponents(false);

        shardedMantaro = core.getShardedInstance();
        audioManager = new MantaroAudioManager();
        tempBanManager = new TempBanManager(MantaroData.db().getMantaroData().getTempBans());

        System.out.println("[-=-=-=-=-=- MANTARO STARTED -=-=-=-=-=-]");

        MantaroData.config().save();

        log.info("Starting update managers...");
        shardedMantaro.startUpdaters();

        core.markAsReady();
        long end = System.currentTimeMillis();

        System.out.println("Finished loading basic components. Current status: " + MantaroCore.getLoadState());

        LogUtils.log("Startup",
                String.format("Loaded %d commands in %d shards.\nI took %d seconds to wake up!",
                        DefaultCommandProcessor.REGISTRY.commands().size(), shardedMantaro.getTotalShards(), (end - start) / 1000));

        if(!config.isPremiumBot() && !config.isBeta()) {
            mantaroAPI.startService();
            MantaroAPISender.startService();
        }

        birthdayCacher = new BirthdayCacher();
        this.startCheckingBirthdays();

        Async.task("discordbots.org post task", () -> {
            if(config.dbotsorgToken == null) return;
            MantaroShard[] shards = shardedMantaro.getShards();
            int[] payload = new int[shards.length];
            for(int i = 0; i < shards.length; i++) {
                payload[i] = (int)shards[i].getGuildCache().size();
            }
            try {
                discordBotsAPI.postStats(payload);
            } catch(PostingException e) {
                log.error("Error posting stats to discordbots.org", e);
            }
        }, 30, TimeUnit.MINUTES);

        Async.task("Mute Handler", muteTask::handle, 1, TimeUnit.MINUTES);
        Async.task("Carbonitex post task", carbonitex::handle, 30, TimeUnit.MINUTES);

        //TODO Do something with this.
        /*Async.task("discordbots.org upvotes task", ()->{
            if(config.dbotsorgToken == null) return;
            try {
                long[] upvoters = discordBotsAPI.getUpvoterIds();
                TLongSet set = new TLongHashSet();
                set.addAll(upvoters);
                discordBotsUpvoters = new TUnmodifiableLongSet(set);
            } catch(PostingException e) {
                log.error("Error getting upvoters from discordbots.org", e);
            }
        }, 10, TimeUnit.MINUTES);*/
    }

    public static void main(String[] args) {
        if(System.getProperty("mantaro.verbose") != null) {
            System.setOut(new CompactPrintStream(System.out));
            System.setErr(new CompactPrintStream(System.err));
        }

        if(System.getProperty("mantaro.debug") != null) {
            DEBUG = true;
            System.out.println("Running in debug mode!");
        }

        if(args.length > 0) {
            try {
                cwport = Integer.parseInt(args[0]);
            } catch(Exception e) {
                log.info("Invalid connection watcher port specified in arguments, using value in config");
                cwport = MantaroData.config().get().connectionWatcherPort;
            }
        } else {
            log.info("No connection watcher port specified, using value in config");
            cwport = MantaroData.config().get().connectionWatcherPort;
        }

        log.info("Using port " + cwport + " to communicate with connection watcher");

        if(cwport > 0) {
            new Thread(() -> {
                try {
                    connectionWatcher = MantaroData.connectionWatcher();
                } catch(Exception e) {
                    //Don't log this to sentry!
                    log.error("Error connecting to Connection Watcher", e);
                }
            });
        }

        try {
            new MantaroBot();
        } catch(Exception e) {
            SentryHelper.captureException("Couldn't start Mantaro at all, so something went seriously wrong", e, MantaroBot.class);
            log.error("Could not complete Main Thread routine!", e);
            log.error("Cannot continue! Exiting program...");
            System.exit(FATAL_FAILURE);
        }
    }

    public Guild getGuildById(String guildId) {
        return getShardForGuild(guildId).getGuildById(guildId);
    }

    public MantaroShard getShard(int id) {
        return Arrays.stream(shardedMantaro.getShards()).filter(shard -> shard.getId() == id).findFirst().orElse(null);
    }

    @Override
    public int getShardAmount() {
        return shardedMantaro.getTotalShards();
    }

    @Nonnull
    @Override
    public Iterator<JDA> iterator() {
        return new ArrayIterator<>(shardedMantaro.getShards());
    }

    public int getId(JDA jda) {
        return jda.getShardInfo() == null ? 0 : jda.getShardInfo().getShardId();
    }

    public MantaroShard getShardForGuild(String guildId) {
        return getShardForGuild(Long.parseLong(guildId));
    }

    public MantaroShard getShardForGuild(long guildId) {
        return getShard((int) ((guildId >> 22) % shardedMantaro.getTotalShards()));
    }

    public List<MantaroShard> getShardList() {
        return Arrays.asList(shardedMantaro.getShards());
    }

    private void sendSignal() {
        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    String.format("{\"content\": \"**Received startup trigger on Node %s", mantaroAPI.nodeUniqueIdentifier)
            );
            Request request = new Request.Builder()
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            response.close();
        } catch(Exception ignored) {
        }
    }

    private void startCheckingBirthdays() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);

        //How much until tomorrow? That's the initial delay, then run it once a day.
        ZoneId z = ZoneId.of("America/Chicago");
        ZonedDateTime now = ZonedDateTime.now(z);
        LocalDate tomorrow = now.toLocalDate().plusDays(1);
        ZonedDateTime tomorrowStart = tomorrow.atStartOfDay(z);
        Duration duration = Duration.between(now, tomorrowStart);
        long millisecondsUntilTomorrow = duration.toMillis();

        executorService.scheduleWithFixedDelay(birthdayTask::handle, millisecondsUntilTomorrow, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
        executorService.scheduleWithFixedDelay(birthdayCacher::cache, 22, 22, TimeUnit.HOURS);
    }
}
