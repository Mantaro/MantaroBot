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

package net.kodehawa.mantarobot.commands.info.stats.manager;

import net.dv8tion.jda.core.EmbedBuilder;

import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public class CommandStatsManager extends StatsManager<String> {
    public static void log(String cmd) {
        if(cmd.isEmpty()) return;
        UsageTracker.tracker(cmd).increment();
    }

    public static String resume(Bucket bucket) {
        int total = UsageTracker.total(bucket.valueMapper);

        return (total == 0) ? ("No Events Logged.") : ("Count: " + total + "\n" + bucket.supplier.apply(5)
                .stream()
                .map(tracker -> {
                    int percent = Math.round((float) bucket.valueMapper.apply(tracker) * 100 / total);
                    return String.format("%s %d%% **%s** (%d)", bar(percent, 15), percent, tracker.getCommandName(), bucket.valueMapper.apply(tracker));
                })
                .collect(Collectors.joining("\n")));
    }

    public static EmbedBuilder fillEmbed(Bucket bucket, EmbedBuilder builder) {
        int total = UsageTracker.total(bucket.valueMapper);

        if(total == 0) {
            builder.addField("Nothing Here.", "Just dust.", false);
            return builder;
        }

        bucket.supplier.apply(12)
                .forEach(tracker -> {
                    int percent = bucket.valueMapper.apply(tracker) * 100 / total;
                    builder.addField(tracker.getCommandName(), String.format("%s %d%% (%d)", bar(percent, 15), percent, bucket.valueMapper.apply(tracker)), false);
                });

        return builder;
    }

    public enum Bucket {
        MINUTE(UsageTracker::highestMinute, UsageTracker::minuteUsages),
        HOUR(UsageTracker::highestHourly, UsageTracker::hourlyUsages),
        DAY(UsageTracker::highestDaily, UsageTracker::dailyUsages),
        TOTAL(UsageTracker::highestTotal, UsageTracker::totalUsages);

        final IntFunction<List<UsageTracker>> supplier;
        final UsageTracker.TrackerIntFunction valueMapper;

        Bucket(IntFunction<List<UsageTracker>> supplier, UsageTracker.TrackerIntFunction valueMapper) {
            this.supplier = supplier;
            this.valueMapper = valueMapper;
        }
    }
}
