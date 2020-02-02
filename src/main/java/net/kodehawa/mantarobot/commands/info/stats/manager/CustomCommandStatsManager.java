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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomCommandStatsManager extends StatsManager<String> {
    public static final Map<String, AtomicInteger> TOTAL_CUSTOM_CMDS = new HashMap<>();
    
    public static void log(String cmd) {
        if(cmd.isEmpty()) return;
        TOTAL_CUSTOM_CMDS.computeIfAbsent(cmd, k -> new AtomicInteger(0)).incrementAndGet();
    }
}
