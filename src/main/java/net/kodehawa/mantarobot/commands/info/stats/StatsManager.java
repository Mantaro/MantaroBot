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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.info.stats;

import net.dv8tion.jda.api.EmbedBuilder;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class StatsManager<T> {
    private static final char ACTIVE_BLOCK = '\u2588';
    private static final char EMPTY_BLOCK = '\u200b';

    public static String bar(long percent, long total) {
        var activeBlocks = (int) ((float) percent / 100f * total);
        var builder = new StringBuilder().append('`').append(EMPTY_BLOCK);

        for (long i = 0; i < total; i++) {
            builder.append(activeBlocks > i ? ACTIVE_BLOCK : ' ');
        }

        return builder.append(EMPTY_BLOCK).append('`').toString();
    }

    public EmbedBuilder fillEmbed(Map<T, AtomicInteger> values, EmbedBuilder builder) {
        var total = values.values().stream().mapToInt(AtomicInteger::get).sum();

        if (total == 0) {
            builder.addField("Nothing Here.", "Just dust.", false);
            return builder;
        }

        values.entrySet().stream()
                .filter(entry -> entry.getValue().get() > 0)
                .sorted(Comparator.comparingInt(entry -> total - entry.getValue().get()))
                .limit(12)
                .forEachOrdered(entry -> {
                    int percent = entry.getValue().get() * 100 / total;
                    builder.addField(String.valueOf(entry.getKey()), String.format("%s %d%% (%d)", bar(percent, 15), percent, entry.getValue().get()), false);
                });

        return builder;
    }

    public String resume(Map<T, AtomicInteger> commands) {
        var total = commands.values().stream().mapToInt(AtomicInteger::get).sum();

        return (total == 0) ? ("No Events Logged.") : ("Count: " + total + "\n" + commands.entrySet().stream()
                .filter(entry -> entry.getValue().get() > 0)
                .sorted(Comparator.comparingInt(entry -> total - entry.getValue().get()))
                .limit(5)
                .map(entry -> {
                    int percent = Math.round((float) entry.getValue().get() * 100 / total);
                    return String.format("%s %d%% **%s** (%d)", bar(percent, 15), percent, entry.getKey(), entry.getValue().get());
                })
                .collect(Collectors.joining("\n")));
    }

    public int getTotalValueFor(Map<T, AtomicInteger> map) {
        return map.values().stream().mapToInt(AtomicInteger::get).sum();
    }
}
