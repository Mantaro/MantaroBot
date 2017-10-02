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

package net.kodehawa.mantarobot.commands.info.stats;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

public class StatsHelper {
    public static <T> DoubleSummaryStatistics calculateDouble(Collection<T> collection, ToDoubleFunction<T> toDouble) {
        return collection.stream().mapToDouble(toDouble).summaryStatistics();
    }

    public static <T> IntSummaryStatistics calculateInt(Collection<T> collection, ToIntFunction<T> toInt) {
        return collection.stream().mapToInt(toInt).summaryStatistics();
    }

    public static void sendStatsMessageAndThen(GuildMessageReceivedEvent e, String message) {
        e.getChannel().sendMessage(EmoteReference.EYES + "**[Stats]** Y-Yeah... gathering info, hold on for a bit...").queue(message1 -> message1.editMessage(message).queue());
    }

    public static void sendStatsMessageAndThen(GuildMessageReceivedEvent e, MessageEmbed message) {
        e.getChannel().sendMessage(EmoteReference.EYES + "**[Stats]** Y-Yeah... gathering info, hold on for a bit...").queue(message1 -> message1.editMessage(message).queue());
    }
}
