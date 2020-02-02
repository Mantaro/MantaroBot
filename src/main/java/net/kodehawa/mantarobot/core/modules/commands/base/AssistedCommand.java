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

package net.kodehawa.mantarobot.core.modules.commands.base;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.options.core.Option;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;

import java.util.Map;

/**
 * "Assisted" version of the {@link Command} interface, providing some "common ground" for all Commands based on it.
 */
public interface AssistedCommand extends Command {
    
    default EmbedBuilder baseEmbed(GuildMessageReceivedEvent event, String name) {
        return baseEmbed(event, name, event.getJDA().getSelfUser().getEffectiveAvatarUrl());
    }
    
    default EmbedBuilder baseEmbed(GuildMessageReceivedEvent event, String name, String image) {
        return new EmbedBuilder()
                       .setAuthor(name, null, image)
                       .setColor(event.getMember().getColor())
                       .setFooter("Requested by " + event.getMember().getEffectiveName(), event.getAuthor().getEffectiveAvatarUrl());
    }
    
    default void doTimes(int times, Runnable runnable) {
        for(int i = 0; i < times; i++) runnable.run();
    }
    
    default Map<String, String> getArguments(String[] args) {
        return StringUtils.parse(args);
    }
    
    default Map<String, String> getArguments(String content) {
        return StringUtils.parse(content.split("\\s+"));
    }
    
    default String checkString(String s) {
        if(s.length() > 1600) {
            return Utils.paste3(s);
        } else {
            return s;
        }
    }
    
    @Override
    default Command addOption(String call, Option option) {
        Option.addOption(call, option);
        return this;
    }
    
    default Config getConfig() {
        return MantaroData.config().get();
    }
}
