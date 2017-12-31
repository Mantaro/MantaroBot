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

package net.kodehawa.mantarobot.core.modules.commands.base;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.options.Option;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.concurrent.TimeUnit;

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

    default EmbedBuilder helpEmbed(GuildMessageReceivedEvent event, String name) {
        return baseEmbed(event, name)
                .setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png")
                .addField("Permission required", permission().toString(), true);
    }

    default String checkString(String s) {
        if(s.length() > 2040) {
            return Utils.paste(s);
        } else {
            return s;
        }
    }

    default void onError(GuildMessageReceivedEvent event) {
        MessageEmbed helpEmbed = help(event);

        if(helpEmbed == null) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "There's no extended help set for this command.").queue();
            return;
        }

        event.getChannel().sendMessage(EmoteReference.ERROR + "You executed this command incorrectly, help for it will be shown below.").queue(
                message -> message.delete().queueAfter(5, TimeUnit.SECONDS)
        );

        event.getChannel().sendMessage(help(event)).queue(
                message -> message.delete().queueAfter(30, TimeUnit.SECONDS)
        );
    }

    default void onHelp(GuildMessageReceivedEvent event) {
        MessageEmbed helpEmbed = help(event);

        if(helpEmbed == null) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "There's no extended help set for this command.").queue(
                    message -> message.delete().queueAfter(20, TimeUnit.SECONDS)
            );
            return;
        }

        event.getChannel().sendMessage(help(event)).queue();
    }

    @Override
    default Command addOption(String call, Option option) {
        Option.addOption(call, option);
        return this;
    }
}
