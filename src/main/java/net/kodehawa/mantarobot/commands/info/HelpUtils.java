/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.info;

import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.kodehawa.mantarobot.core.command.processor.CommandProcessor;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.MongoGuild;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class HelpUtils {
    public static String forType(GuildMessageChannel channel, MongoGuild guildData, CommandCategory category) {
        return forType(
                CommandProcessor.REGISTRY.commands().entrySet().stream()
                        .filter(entry -> entry.getValue().category() == category)
                        .filter(entry -> !guildData.getDisabledCategories().contains(entry.getValue().category()))
                        .filter(c -> !guildData.getDisabledCommands().contains(c.getKey()))
                        .filter(c -> guildData.getChannelSpecificDisabledCommands().get(channel.getId()) == null || !guildData.getChannelSpecificDisabledCommands().get(channel.getId()).contains(c.getKey()))
                        .filter(c -> !guildData.getChannelSpecificDisabledCategories().computeIfAbsent(channel.getId(), wew -> new ArrayList<>()).contains(category))
                        .map(Entry::getKey)
                        .collect(Collectors.toList())
        );
    }

    public static String forTypeSlash(GuildMessageChannel channel, MongoGuild guildData, CommandCategory category) {
        return forType(
                CommandProcessor.REGISTRY.getCommandManager().slashCommands().entrySet().stream()
                        .filter(entry -> entry.getValue().getCategory() == category)
                        .filter(entry -> !guildData.getDisabledCategories().contains(entry.getValue().getCategory()))
                        .filter(c -> !guildData.getDisabledCommands().contains(c.getKey()))
                        .filter(c -> guildData.getChannelSpecificDisabledCommands().get(channel.getId()) == null || !guildData.getChannelSpecificDisabledCommands().get(channel.getId()).contains(c.getKey()))
                        .filter(c -> !guildData.getChannelSpecificDisabledCategories().computeIfAbsent(channel.getId(), wew -> new ArrayList<>()).contains(category))
                        .map(Entry::getKey)
                        .collect(Collectors.toList())
        );
    }

    public static String forType(List<String> values) {
        if (values.isEmpty()) {
            return "`Disabled`";
        }

        return "\u2009\u2009`" + values.stream().sorted()
                .collect(Collectors.joining("` `")) + "`";
    }

    public static String getCommandMention(String cmd, String sub) {
        String mention = cmd;
        if (!sub.isBlank()) {
            mention += " " + sub;
        }
        try (var jedis = MantaroData.getDefaultJedisPool().getResource()) {
            String id = jedis.hget("command-ids", cmd);
            if (id != null) {
                if (!sub.isBlank()) {
                    mention ="</" + cmd + " " + sub + ":" + id + ">";
                } else {
                    mention ="</" + cmd + ":" + id + ">";
                }
            }
        }
        return mention;
    }
}
