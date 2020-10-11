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

package net.kodehawa.mantarobot.commands.info.stats.manager;

import com.github.natanbc.usagetracker.Bucket;
import com.github.natanbc.usagetracker.TrackerGroup;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.stream.Collectors;

public class CommandStatsManager extends StatsManager<String> {
    private static final TrackerGroup<String> TRACKERS = new TrackerGroup<>();

    public static void log(String cmd) {
        if (cmd.isEmpty()) return;
        TRACKERS.tracker(cmd).increment();
    }

    public static String resume(Bucket bucket) {
        long total = TRACKERS.total(bucket);

        return (total == 0) ? ("No Events Logged.") : ("Count: " + total + "\n" + TRACKERS.highest(bucket, 5)
                .map(tracker -> {
                    int percent = Math.round((float) bucket.amount(tracker) * 100 / total);
                    return String.format("%s %d%% **%s** (%d)", bar(percent, 15), percent, tracker.getKey(), bucket.amount(tracker));
                })
                .collect(Collectors.joining("\n")));
    }

    public static EmbedBuilder fillEmbed(Bucket bucket, EmbedBuilder builder) {
        long total = TRACKERS.total(bucket);

        if (total == 0) {
            builder.addField("Nothing Here.", "Just dust.", false);
            return builder;
        }

        TRACKERS.highest(bucket, 12)
                .forEach(tracker -> {
                    long percent = bucket.amount(tracker) * 100 / total;
                    builder.addField(tracker.getKey(), String.format("%s %d%% (%d)", bar(percent, 15), percent, bucket.amount(tracker)), false);
                });

        return builder;
    }
}
