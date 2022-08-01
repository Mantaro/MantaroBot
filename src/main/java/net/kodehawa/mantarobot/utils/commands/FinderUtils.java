/*
 * Copyright (C) 2016-2022 David Rubio Escares / Kodehawa
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

package net.kodehawa.mantarobot.utils.commands;

import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.OptsCmd.optsCmd;

public class FinderUtils {
    public final static Pattern DISCORD_ID = Pattern.compile("\\d{17,21}");
    public final static Pattern CHANNEL_MENTION = Pattern.compile("<#(\\d{17,21})>");

    private static List<Role> findRole0(MessageReceivedEvent event, String content) {
        List<Role> found = FinderUtil.findRoles(content, event.getGuild());
        if (found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR +
                    "Cannot find any role with that name :( *if the role has spaces try wrapping it in quotes \"like this\"*"
            ).queue();

            return null;
        }

        return found;
    }

    public static Role findRole(MessageReceivedEvent event, String content) {
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

        final var roles = event.getMember().getRoles();
        if (roles.isEmpty() && found.isEmpty()) {
            event.getChannel().sendMessage("I can't find any suitable role with this search.").queue();
            return null;
        }

        return roles.get(0);
    }

    public static Role findRoleSelect(MessageReceivedEvent event,
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

    private static List<StandardGuildMessageChannel> findChannel0(MessageReceivedEvent event, String content) {
        List<StandardGuildMessageChannel> found = findStandardChannels0(content, event.getGuild());
        if (found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot find any text channel with that name :(").queue();
            return null;
        }

        return found;
    }

    public static StandardGuildMessageChannel findChannel(MessageReceivedEvent event, String content) {
        List<StandardGuildMessageChannel> found = findChannel0(event, content);
        if (found == null) {
            return null;
        }

        if (found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format(
                    "%sToo many channels found, maybe refine your search?\n**Text Channel found:** %s",
                    EmoteReference.THINKING,
                    found.stream()
                            .limit(5)
                            .map(StandardGuildMessageChannel::getName)
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

    public static StandardGuildMessageChannel findChannelSelect(MessageReceivedEvent event,
                                                String content, Consumer<StandardGuildMessageChannel> consumer) {
        List<StandardGuildMessageChannel> found = findChannel0(event, content);
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

    public static VoiceChannel findVoiceChannelSelect(MessageReceivedEvent event, String content, Consumer<VoiceChannel> consumer) {
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

    private static <T extends GuildChannel> void selectList(MessageReceivedEvent event, List<T> found, Consumer<T> consumer) {
        DiscordUtils.selectList(event, found.stream().limit(5).collect(Collectors.toList()),
                channel -> "%s%s (ID: %s)".formatted(
                        EmoteReference.BLUE_SMALL_MARKER,
                        channel.getName(),
                        channel.getId()
                ), s -> optsCmd.baseEmbed(event, "Select the Channel:").setDescription(s).build(), consumer
        );
    }

    // The two here are basically taken from the old FinderUtils stuff from jagrosh.
    // not literally, since it had to be modified to work with new stuff.
    public static List<StandardGuildMessageChannel> findStandardChannels0(String query, Guild guild)
    {
        Matcher channelMention = CHANNEL_MENTION.matcher(query);
        if (channelMention.matches())
        {
            var tc = guild.getChannelById(StandardGuildMessageChannel.class, channelMention.group(1));
            if (tc!=null) {
                return Collections.singletonList(tc);
            }
        }
        else if(DISCORD_ID.matcher(query).matches())
        {
            var tc = guild.getChannelById(StandardGuildMessageChannel.class, query);
            if (tc!=null) {
                return Collections.singletonList(tc);
            }
        }

        var channels = guild.getChannels().stream()
                .filter(StandardGuildMessageChannel.class::isInstance)
                .map(StandardGuildMessageChannel.class::cast)
                .toList();

        return genericStandardChannelSearch0(query, channels);
    }

    private static List<StandardGuildMessageChannel> genericStandardChannelSearch0(String query, List<StandardGuildMessageChannel> cache)
    {
        ArrayList<StandardGuildMessageChannel> exact = new ArrayList<>();
        ArrayList<StandardGuildMessageChannel> wrongCase = new ArrayList<>();
        ArrayList<StandardGuildMessageChannel> startsWith = new ArrayList<>();
        ArrayList<StandardGuildMessageChannel> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        cache.forEach((channel) -> {
            String name = channel.getName();
            if (name.equals(query)) {
                exact.add(channel);
            } else if(name.equalsIgnoreCase(query) && exact.isEmpty()) {
                wrongCase.add(channel);
            } else if(name.toLowerCase().startsWith(lowerQuery) && wrongCase.isEmpty()) {
                startsWith.add(channel);
            } else if(name.toLowerCase().contains(lowerQuery) && startsWith.isEmpty()) {
                contains.add(channel);
            }
        });

        if (!exact.isEmpty()) {
            return Collections.unmodifiableList(exact);
        }

        if (!wrongCase.isEmpty()) {
            return Collections.unmodifiableList(wrongCase);
        }

        if (!startsWith.isEmpty()) {
            return Collections.unmodifiableList(startsWith);
        }

        return Collections.unmodifiableList(contains);
    }

}
