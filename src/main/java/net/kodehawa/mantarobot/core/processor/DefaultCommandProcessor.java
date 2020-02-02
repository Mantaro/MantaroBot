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

package net.kodehawa.mantarobot.core.processor;

import io.prometheus.client.Histogram;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import org.slf4j.Logger;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public class DefaultCommandProcessor implements ICommandProcessor {
    public static final CommandRegistry REGISTRY = new CommandRegistry();
    private static final Histogram commandTime = Histogram.build()
                                                         .name("command_time").help("Time it takes for a command to be ran.")
                                                         .register();
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DefaultCommandProcessor.class);
    
    @Override
    public boolean run(GuildMessageReceivedEvent event) {
        //When did we start processing this command?...
        long start = System.currentTimeMillis();
        //The command executed, in raw form.
        String rawCmd = event.getMessage().getContentRaw();
        //Mantaro prefixes.
        String[] prefix = MantaroData.config().get().prefix;
        //Guild-specific prefix.
        String customPrefix = MantaroData.db().getGuild(event.getGuild()).getData().getGuildCustomPrefix();
        //What prefix did this person use.
        String usedPrefix = null;
        //Lower-case raw cmd check, only used for prefix checking.
        String lowerRawCmd = rawCmd.toLowerCase();
        
        for(String s : prefix) {
            if(lowerRawCmd.startsWith(s))
                usedPrefix = s;
        }
        
        if(usedPrefix != null && lowerRawCmd.startsWith(usedPrefix.toLowerCase())) {
            rawCmd = rawCmd.substring(usedPrefix.length());
        } else if(customPrefix != null && lowerRawCmd.startsWith(customPrefix.toLowerCase())) {
            rawCmd = rawCmd.substring(customPrefix.length());
        } else if(usedPrefix == null) {
            return false;
        }
        
        String[] parts = splitArgs(rawCmd, 2);
        String cmdName = parts[0], content = parts[1];
        
        REGISTRY.process(event, cmdName, content, usedPrefix);
        
        long end = System.currentTimeMillis();
        commandTime.observe(end - start);
        
        return true;
    }
}
