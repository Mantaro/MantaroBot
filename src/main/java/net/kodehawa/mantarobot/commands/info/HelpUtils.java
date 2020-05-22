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

package net.kodehawa.mantarobot.commands.info;

import net.dv8tion.jda.api.entities.TextChannel;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class HelpUtils {
    public static String forType(TextChannel channel, GuildData guildData, Category category) {
        return forType(
                DefaultCommandProcessor.REGISTRY.commands().entrySet().stream()
                        .filter(entry -> entry.getValue().category() == category)
                        .filter(entry -> !guildData.getDisabledCategories().contains(entry.getValue().category()))
                        .filter(c -> !guildData.getDisabledCommands().contains(c.getKey()))
                        .filter(c -> guildData.getChannelSpecificDisabledCommands().get(channel.getId()) == null || !guildData.getChannelSpecificDisabledCommands().get(channel.getId()).contains(c.getKey()))
                        .filter(c -> !guildData.getChannelSpecificDisabledCategories().computeIfAbsent(channel.getId(), wew -> new ArrayList<>()).contains(category))
                        .map(Entry::getKey)
                        .collect(Collectors.toList())
        );
    }

    public static String forType(List<String> values) {
        if (values.size() == 0) return "`Disabled`";

        return "``" + values.stream().sorted()
                .collect(Collectors.joining("`` ``")) + "``";
    }
}
