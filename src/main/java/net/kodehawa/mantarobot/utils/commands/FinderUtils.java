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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.utils.commands;

import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.OptsCmd.optsCmd;

public class FinderUtils {
    private static List<Role> findRole0(GuildMessageReceivedEvent event, String content) {
        List<Role> found = FinderUtil.findRoles(content, event.getGuild());
        if (found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR +
                    "Cannot find any role with that name :( *if the role has spaces try wrapping it in quotes \"like this\"*"
            ).queue();

            return null;
        }

        return found;
    }

    public static Role findRole(GuildMessageReceivedEvent event, String content) {
        List<Role> found = findRole0(event, content);
        // Ah yes, null return null.
        if (found == null) {
            return null;
        }

        if (found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format(
                    "%sToo many roles found, maybe refine your search?\n**Roles found:** %s",
                    EmoteReference.THINKING,
                    found.stream()
                            .limit(5)
                            .map(Role::getName)
                            .collect(Collectors.joining(", ")))
            ).queue();

            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        }

        return event.getMember().getRoles().get(0);
    }

    public static Role findRoleSelect(GuildMessageReceivedEvent event,
                                      String content, Consumer<Role> consumer) {
        List<Role> found = findRole0(event, content);
        if (found == null) {
            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        } else {
            DiscordUtils.selectList(event, found.stream().limit(5).collect(Collectors.toList()),
                    role -> "%s%s (ID: %s)".formatted(
                            EmoteReference.BLUE_SMALL_MARKER,
                            role.getName(), role.getId()
                    ),
                    s -> optsCmd.baseEmbed(event, "Select the Role:").
                            setDescription(s)
                            .build(),
                    consumer
            );
        }

        return null;
    }

    private static List<TextChannel> findChannel0(GuildMessageReceivedEvent event, String content) {
        List<TextChannel> found = FinderUtil.findTextChannels(content, event.getGuild());
        if (found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot find any text channel with that name :(").queue();
            return null;
        }

        return found;
    }

    public static TextChannel findChannel(GuildMessageReceivedEvent event, String content) {
        List<TextChannel> found = findChannel0(event, content);
        if (found == null) {
            return null;
        }

        if (found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format(
                    "%sToo many channels found, maybe refine your search?\n**Text Channel found:** %s",
                    EmoteReference.THINKING,
                    found.stream()
                            .limit(5)
                            .map(TextChannel::getName)
                            .collect(Collectors.joining(", "))
                    )
            ).queue();

            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        }

        return null;
    }

    public static TextChannel findChannelSelect(GuildMessageReceivedEvent event,
                                                String content, Consumer<TextChannel> consumer) {
        List<TextChannel> found = findChannel0(event, content);
        // This feels a little weird, but found can return null here.
        if (found == null) {
            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        } else {
            selectList(event, found, consumer);
        }

        return null;
    }

    public static VoiceChannel findVoiceChannelSelect(GuildMessageReceivedEvent event, String content, Consumer<VoiceChannel> consumer) {
        List<VoiceChannel> found = FinderUtil.findVoiceChannels(content, event.getGuild());
        if (found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot find any voice channel with that name :(").queue();
            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        } else {
            selectList(event, found, consumer);
        }

        return null;
    }

    private static <T extends GuildChannel> void selectList(GuildMessageReceivedEvent event, List<T> found, Consumer<T> consumer) {
        DiscordUtils.selectList(event, found.stream().limit(5).collect(Collectors.toList()),
                channel -> "%s%s (ID: %s)".formatted(
                        EmoteReference.BLUE_SMALL_MARKER,
                        channel.getName(),
                        channel.getId()
                ), s -> optsCmd.baseEmbed(event, "Select the Channel:").setDescription(s).build(), consumer
        );
    }
}
