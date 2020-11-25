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

package net.kodehawa.mantarobot.core.command.processor;

import io.prometheus.client.Histogram;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.data.MantaroData;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public class CommandProcessor {
    public static final CommandRegistry REGISTRY = new CommandRegistry();
    private static final Histogram commandTime = Histogram.build()
            .name("command_time").help("Time it takes for a command to be ran.")
            .register();

    public boolean run(GuildMessageReceivedEvent event) {
        final long start = System.currentTimeMillis();

        // The command executed, in raw form.
        var rawCmd = event.getMessage().getContentRaw();
        // Lower-case raw cmd check, only used for prefix checking.
        final var lowerRawCmd = rawCmd.toLowerCase();

        // Mantaro prefixes.
        String[] prefix = MantaroData.config().get().prefix;
        // Guild-specific prefix.
        final var dbGuild = MantaroData.db().getGuild(event.getGuild());
        var customPrefix = dbGuild.getData().getGuildCustomPrefix();

        // What prefix did this person use.
        String usedPrefix = null;
        for (String s : prefix) {
            if (lowerRawCmd.startsWith(s)) {
                usedPrefix = s;
            }
        }

        // Remove prefix from arguments.
        if (usedPrefix != null && lowerRawCmd.startsWith(usedPrefix.toLowerCase())) {
            rawCmd = rawCmd.substring(usedPrefix.length());
        } else if (customPrefix != null && lowerRawCmd.startsWith(customPrefix.toLowerCase())) {
            rawCmd = rawCmd.substring(customPrefix.length());
            usedPrefix = customPrefix;
        } else if (usedPrefix == null) {
            return false;
        }

        // This could be done using a lock, but that would be a little too blocking. So just set a flag.
        try (var jedis = MantaroData.getDefaultJedisPool().getResource()) {
            jedis.set("commands-running-" + event.getAuthor().getId(), String.valueOf(1));
        }

        // The command arguments to parse.
        String[] parts = splitArgs(rawCmd, 2);
        String cmdName = parts[0], content = parts[1];

        // Run the actual command here.
        REGISTRY.process(event, dbGuild, cmdName, content, usedPrefix);

        final long end = System.currentTimeMillis();
        commandTime.observe(end - start);
        return true;
    }
}
