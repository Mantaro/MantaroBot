/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.info.stats.manager;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import net.kodehawa.mantarobot.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GuildStatsManager extends StatsManager<GuildStatsManager.LoggedEvent> {
    public static final ExpiringMap<LoggedEvent, AtomicInteger> DAY_EVENTS = ExpiringMap.builder()
                                                                                     .expiration(1, TimeUnit.DAYS)
                                                                                     .expirationPolicy(ExpirationPolicy.CREATED)
                                                                                     .build();
    public static final ExpiringMap<LoggedEvent, AtomicInteger> HOUR_EVENTS = ExpiringMap.builder()
                                                                                      .expiration(1, TimeUnit.HOURS)
                                                                                      .expirationPolicy(ExpirationPolicy.CREATED)
                                                                                      .build();
    public static final ExpiringMap<LoggedEvent, AtomicInteger> MINUTE_EVENTS = ExpiringMap.builder()
                                                                                        .expiration(1, TimeUnit.MINUTES)
                                                                                        .expirationPolicy(ExpirationPolicy.CREATED)
                                                                                        .build();
    public static final Map<LoggedEvent, AtomicInteger> TOTAL_EVENTS = new HashMap<>();
    public static int MILESTONE = 0;
    
    public static void log(LoggedEvent loggedEvent) {
        TOTAL_EVENTS.computeIfAbsent(loggedEvent, k -> new AtomicInteger(0)).incrementAndGet();
        DAY_EVENTS.computeIfAbsent(loggedEvent, k -> new AtomicInteger(0)).incrementAndGet();
        HOUR_EVENTS.computeIfAbsent(loggedEvent, k -> new AtomicInteger(0)).incrementAndGet();
        MINUTE_EVENTS.computeIfAbsent(loggedEvent, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    public enum LoggedEvent {
        JOIN, LEAVE;
        
        @Override
        public String toString() {
            return Utils.capitalize(name().toLowerCase());
        }
    }
}
