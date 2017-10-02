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
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.utils.Utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GuildStatsManager {
    public static final Map<LoggedEvent, AtomicInteger> TOTAL_EVENTS = new HashMap<>();

    public static final ExpiringMap<LoggedEvent, AtomicInteger> DAY_EVENTS = ExpiringMap.<String, AtomicInteger>builder()
            .expiration(1, TimeUnit.DAYS)
            .expirationPolicy(ExpirationPolicy.CREATED)
            .build();

    public static final ExpiringMap<LoggedEvent, AtomicInteger> HOUR_EVENTS = ExpiringMap.<String, AtomicInteger>builder()
            .expiration(1, TimeUnit.HOURS)
            .expirationPolicy(ExpirationPolicy.CREATED)
            .build();

    public static final ExpiringMap<LoggedEvent, AtomicInteger> MINUTE_EVENTS = ExpiringMap.<String, AtomicInteger>builder()
            .expiration(1, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.CREATED)
            .build();

    private static final char ACTIVE_BLOCK = '\u2588';
    private static final char EMPTY_BLOCK = '\u200b';
    public static int MILESTONE = 0;

    public static String bar(int percent, int total) {
        int activeBlocks = (int) ((float) percent / 100f * total);
        StringBuilder builder = new StringBuilder().append('`').append(EMPTY_BLOCK);
        for(int i = 0; i < total; i++) builder.append(activeBlocks > i ? ACTIVE_BLOCK : ' ');
        return builder.append(EMPTY_BLOCK).append('`').toString();
    }

    public static EmbedBuilder fillEmbed(Map<LoggedEvent, AtomicInteger> events, EmbedBuilder builder) {
        int total = events.values().stream().mapToInt(AtomicInteger::get).sum();

        if(total == 0) {
            builder.addField("Nothing Here.", "Just dust.", false);
            return builder;
        }

        events.entrySet().stream()
                .filter(entry -> entry.getValue().get() > 0)
                .sorted(Comparator.comparingInt(entry -> total - entry.getValue().get()))
                .limit(12)
                .forEachOrdered(entry -> {
                    int percent = entry.getValue().get() * 100 / total;
                    builder.addField(entry.getKey().toString(), String.format("%s %d%% (%d)", bar(percent, 15), percent, entry.getValue().get()), true);
                });

        return builder.setFooter("Guilds: " + MantaroBot.getInstance().getGuildCache().size(), null);
    }

    public static void log(LoggedEvent loggedEvent) {
        TOTAL_EVENTS.computeIfAbsent(loggedEvent, k -> new AtomicInteger(0)).incrementAndGet();
        DAY_EVENTS.computeIfAbsent(loggedEvent, k -> new AtomicInteger(0)).incrementAndGet();
        HOUR_EVENTS.computeIfAbsent(loggedEvent, k -> new AtomicInteger(0)).incrementAndGet();
        MINUTE_EVENTS.computeIfAbsent(loggedEvent, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public static String resume(Map<LoggedEvent, AtomicInteger> commands) {
        int total = commands.values().stream().mapToInt(AtomicInteger::get).sum();

        return (total == 0) ? ("No Events Logged.") : ("Count: " + total + "\n" + commands.entrySet().stream()
                .filter(entry -> entry.getValue().get() > 0)
                .sorted(Comparator.comparingInt(entry -> total - entry.getValue().get()))
                .limit(5)
                .map(entry -> {
                    int percent = Math.round((float) entry.getValue().get() * 100 / total);
                    return String.format("%s %d%% **%s** (%d)", bar(percent, 15), percent, entry.getKey().toString(), entry.getValue().get());
                })
                .collect(Collectors.joining("\n")));
    }

    public enum LoggedEvent {
        JOIN, LEAVE;

        @Override
        public String toString() {
            return Utils.capitalize(name().toLowerCase());
        }
    }
}
