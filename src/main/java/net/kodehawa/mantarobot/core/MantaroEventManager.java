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

package net.kodehawa.mantarobot.core;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.InterfacedEventManager;
import net.kodehawa.mantarobot.core.listeners.events.ShardMonitorEvent;
import org.slf4j.Logger;

@Slf4j
public class MantaroEventManager extends InterfacedEventManager {
    private long LAST_JDA_EVENT;

    public static Logger getLog() {
        return log;
    }

    @Override
    public void handle(Event event) {
        if(!(event instanceof ShardMonitorEvent)) {
            LAST_JDA_EVENT = System.currentTimeMillis();
        }

        super.handle(event);
    }

    public long getLastJDAEventTimeDiff() {
        return System.currentTimeMillis() - LAST_JDA_EVENT;
    }
}
