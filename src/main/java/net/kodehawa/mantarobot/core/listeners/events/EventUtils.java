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

package net.kodehawa.mantarobot.core.listeners.events;

import net.dv8tion.jda.api.events.Event;
import net.kodehawa.mantarobot.MantaroBot;

public class EventUtils {
    public static void propagateEvent(MantaroEvent event) {
        for (var shard : MantaroBot.getInstance().getShardManager().getShards()) {
            shard.getEventManager().handle((Event) event);
        }

        event.onPropagation();
    }
}
