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
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import gnu.trove.impl.unmodifiable.TUnmodifiableLongSet;
import gnu.trove.set.hash.TLongHashSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.kodehawa.mantarobot.commands.moderation.MuteTask;
import net.kodehawa.mantarobot.commands.moderation.TempBanManager;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.commands.utils.birthday.BirthdayCacher;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.core.shard.MantaroShard;
import net.kodehawa.mantarobot.core.shard.ShardedMantaro;
import net.kodehawa.mantarobot.core.shard.jda.ShardedJDA;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogFilter;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.CompactPrintStream;
import net.kodehawa.mantarobot.utils.SentryHelper;
import org.apache.commons.collections4.iterators.ArrayIterator;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.utils.ShutdownCodes.FATAL_FAILURE;
import static net.kodehawa.mantarobot.utils.ShutdownCodes.REBOOT_FAILURE;

@Slf4j
public class MantaroBot extends ShardedJDA {
    @Getter
    private static MantaroBot instance;
    @Getter
    private static TempBanManager tempBanManager;
    @Getter
    private final MantaroAudioManager audioManager;
    @Getter
    private final MantaroCore core;
    @Getter
    private final DiscordBotsAPI discordBotsAPI;
    @Getter
    private final ShardedMantaro shardedMantaro;
    @Getter
    private final StatsDClient statsClient;
    @Getter
    private BirthdayCacher birthdayCacher;
    @Getter
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);

    //just in case
    static {
        if(ExtraRuntimeOptions.VERBOSE) {
            System.setOut(new CompactPrintStream(System.out));
            System.setErr(new CompactPrintStream(System.err));
        }

        if(ExtraRuntimeOptions.DEBUG) {
            log.info("Running in debug mode!");
        }

        log.info("Filtering all logs below " + LogFilter.LEVEL);
    }

    private MantaroBot() throws Exception {
        instance = this;
        Config config = MantaroData.config().get();
        core = new MantaroCore(config, true, true, ExtraRuntimeOptions.DEBUG);
        discordBotsAPI = new DiscordBotsAPI(config.dbotsorgToken);

        statsClient = new NonBlockingStatsDClient(
                config.isPremiumBot() ? "mantaro-patreon" : "mantaro",
                "localhost",
                8125,
                "tag:value"
        );

        LogUtils.log("Startup", String.format("Starting up MantaroBot %s\n" + "Hold your seatbelts! <3", MantaroInfo.VERSION));

        long start = System.currentTimeMillis();

        core.setCommandsPackage("net.kodehawa.mantarobot.commands")
                .setOptionsPackage("net.kodehawa.mantarobot.options")
                .startMainComponents(false);

        shardedMantaro = core.getShardedInstance();
        audioManager = new MantaroAudioManager();
        tempBanManager = new TempBanManager(MantaroData.db().getMantaroData().getTempBans());
        long end = System.currentTimeMillis();

        System.out.println("Finished loading basic components. Current status: " + MantaroCore.getLoadState());
        MantaroData.config().save();

        LogUtils.log("Startup",
                String.format("Loaded %d commands in %d seconds.\n" +
                        "Shards are still waking up!", DefaultCommandProcessor.REGISTRY.commands().size(), (end - start) / 1000));

        birthdayCacher = new BirthdayCacher();
        final MuteTask muteTask = new MuteTask();
        Async.task("Mute Handler", muteTask::handle, 1, TimeUnit.MINUTES);
    }

    public static void main(String[] args) {
        try {
            new MantaroBot();
        } catch(Exception e) {
            SentryHelper.captureException("Couldn't start Mantaro at all, so something went seriously wrong", e, MantaroBot.class);
            log.error("Could not complete Main Thread routine!", e);
            log.error("Cannot continue! Exiting program...");
            System.exit(FATAL_FAILURE);
        }
    }

    public static boolean isDebug() {
        return ExtraRuntimeOptions.DEBUG;
    }

    public static boolean isVerbose() {
        return ExtraRuntimeOptions.VERBOSE;
    }

    public Guild getGuildById(String guildId) {
        return getShardForGuild(guildId).getGuildById(guildId);
    }

    public MantaroShard getShard(int id) {
        return Arrays.stream(shardedMantaro.getShards()).filter(Objects::nonNull).filter(shard -> shard.getId() == id).findFirst().orElse(null);
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

    public void startCheckingBirthdays() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

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
        for(MantaroShard shard : core.getShardedInstance().getShards()) {
            shard.startBirthdayTask(millisecondsUntilTomorrow);
        }

        //Start the birthday cacher.
        executorService.scheduleWithFixedDelay(birthdayCacher::cache, 22, 22, TimeUnit.HOURS);
    }

    @Override
    public void restartShard(int shardId, boolean force) {
        try {
            getShardList().get(shardId).start(force);
        } catch(Exception e) {
            LogUtils.shard("Error while restarting shard " + shardId);
            e.printStackTrace();
        }
    }

    public void forceRestartShard(int shardId) {
        restartShard(shardId, true);
    }

    @Override
    public void restartAll() {
        log.warn("Restarting bot...");
        System.exit(REBOOT_FAILURE);
    }
}
