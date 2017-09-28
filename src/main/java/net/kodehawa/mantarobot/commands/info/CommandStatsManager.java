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

package net.kodehawa.mantarobot.commands.info;

import net.dv8tion.jda.core.EmbedBuilder;
import net.jodah.expiringmap.ExpiringMap;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CommandStatsManager {
    public static final Map<String, AtomicInteger> TOTAL_CMDS = new HashMap<>();

    public static final ExpiringMap<String, AtomicInteger> DAY_CMDS = ExpiringMap.<String, AtomicInteger>builder()
            .expiration(1, TimeUnit.DAYS)
            .build();

    public static final ExpiringMap<String, AtomicInteger> HOUR_CMDS = ExpiringMap.<String, AtomicInteger>builder()
            .expiration(1, TimeUnit.HOURS)
            .build();

    public static final ExpiringMap<String, AtomicInteger> MINUTE_CMDS = ExpiringMap.<String, AtomicInteger>builder()
            .expiration(1, TimeUnit.MINUTES)
            .build();
    private static final char ACTIVE_BLOCK = '\u2588';
    private static final char EMPTY_BLOCK = '\u200b';

    public static String bar(int percent, int total) {
        int activeBlocks = (int) ((float) percent / 100f * total);
        StringBuilder builder = new StringBuilder().append('`').append(EMPTY_BLOCK);
        for(int i = 0; i < total; i++) builder.append(activeBlocks > i ? ACTIVE_BLOCK : ' ');
        return builder.append(EMPTY_BLOCK).append('`').toString();
    }

    public static EmbedBuilder fillEmbed(Map<String, AtomicInteger> commands, EmbedBuilder builder) {
        int total = commands.values().stream().mapToInt(AtomicInteger::get).sum();

        if(total == 0) {
            builder.addField("Nothing Here.", "Just dust.", false);
            return builder;
        }

        commands.entrySet().stream()
                .filter(entry -> entry.getValue().get() > 0)
                .sorted(Comparator.comparingInt(entry -> total - entry.getValue().get()))
                .limit(12)
                .forEachOrdered(entry -> {
                    int percent = entry.getValue().get() * 100 / total;
                    builder.addField(entry.getKey(), String.format("%s %d%% (%d)", bar(percent, 15), percent, entry.getValue().get()), false);
                });

        return builder;
    }

    public static void log(String cmd) {
        if(cmd.isEmpty()) return;
        TOTAL_CMDS.computeIfAbsent(cmd, k -> new AtomicInteger(0)).incrementAndGet();
        DAY_CMDS.computeIfAbsent(cmd, k -> new AtomicInteger(0)).incrementAndGet();
        HOUR_CMDS.computeIfAbsent(cmd, k -> new AtomicInteger(0)).incrementAndGet();
        MINUTE_CMDS.computeIfAbsent(cmd, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public static String resume(Map<String, AtomicInteger> commands) {
        int total = commands.values().stream().mapToInt(AtomicInteger::get).sum();

        return (total == 0) ? ("No Commands issued.") : ("Count: " + total + "\n" + commands.entrySet().stream()
                .filter(entry -> entry.getValue().get() > 0)
                .sorted(Comparator.comparingInt(entry -> total - entry.getValue().get()))
                .limit(5)
                .map(entry -> {
                    int percent = Math.round((float) entry.getValue().get() * 100 / total);
                    return String.format("%s %d%% **%s** (%d)", bar(percent, 15), percent, entry.getKey(), entry.getValue().get());
                })
                .collect(Collectors.joining("\n")));
    }

    public static int getTotalValueFor(Map<String, AtomicInteger> map) {
        return map.values().stream().mapToInt(AtomicInteger::get).sum();
    }
}
