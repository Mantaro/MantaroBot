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

package net.kodehawa.mantarobot.core.processor;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public class DefaultCommandProcessor implements ICommandProcessor {

    public static final CommandRegistry REGISTRY = new CommandRegistry();
    private static final Logger LOGGER = LoggerFactory.getLogger("CommandProcessor");

    @Override
    public boolean run(GuildMessageReceivedEvent event) {
        long start = System.currentTimeMillis();
        String rawCmd = event.getMessage().getContentRaw();
        String[] prefix = MantaroData.config().get().prefix;
        String customPrefix = MantaroData.db().getGuild(event.getGuild()).getData().getGuildCustomPrefix();

        String usedPrefix = null;
        for(String s : prefix) {
            if(rawCmd.startsWith(s)) usedPrefix = s;
        }

        if(usedPrefix != null && rawCmd.startsWith(usedPrefix)) rawCmd = rawCmd.substring(usedPrefix.length());
        else if(customPrefix != null && rawCmd.startsWith(customPrefix))
            rawCmd = rawCmd.substring(customPrefix.length());
        else if(usedPrefix == null) return false;

        String[] parts = splitArgs(rawCmd, 2);
        String cmdName = parts[0], content = parts[1];

        if(!event.getGuild().getSelfMember().getPermissions(event.getChannel()).contains(Permission.MESSAGE_EMBED_LINKS)) {
            event.getChannel().sendMessage(EmoteReference.STOP + "I require the permission ``Embed Links``. " +
                    "All Commands will be refused until you give me that permission.\n" +
                    "http://i.imgur.com/Ydykxcy.gifv Refer to this on instructions on how to give the bot the permissions. " +
                    "Also check all the other roles the bot has have that permissions and remember to check channel-specific permissions. Thanks you.").queue();
            return false;
        }

        REGISTRY.process(event, cmdName, content);
        LOGGER.debug("Command invoked: {}, by {}#{} with timestamp {}", cmdName, event.getAuthor().getName(), event.getAuthor().getDiscriminator(), new Date(System.currentTimeMillis()));

        long end = System.currentTimeMillis();
        MantaroBot.getInstance().getStatsClient().histogram("command_query_time", end - start);
        return true;
    }
}
