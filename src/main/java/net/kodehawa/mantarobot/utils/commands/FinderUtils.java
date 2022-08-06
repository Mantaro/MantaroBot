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

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.kodehawa.mantarobot.core.command.slash.IContext;

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
    public final static Pattern ROLE_MENTION = Pattern.compile("<@&(\\d{17,21})>"); // $1 -> ID

    private static List<Role> findRole0(IContext ctx, String content) {
        List<Role> found = findRole0(content, ctx.getGuild());
        if (found.isEmpty() && !content.isEmpty()) {
            ctx.sendLocalized("general.find_roles_failure", EmoteReference.ERROR);
            return null;
        }

        return found;
    }

    public static Role findRole(IContext ctx, String content) {
        List<Role> found = findRole0(ctx, content);
        // Ah yes, null return null.
        if (found == null) {
            return null;
        }

        if (found.size() > 1 && !content.isEmpty()) {
            ctx.sendLocalized("general.too_many_roles", EmoteReference.THINKING,
                    found.stream()
                            .limit(5)
                            .map(Role::getName)
                            .collect(Collectors.joining(", "))
            );

            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        }

        final var roles = ctx.getMember().getRoles();
        if (roles.isEmpty() && found.isEmpty()) {
            ctx.sendLocalized("general.find_roles_failure", EmoteReference.ERROR);
            return null;
        }

        return roles.get(0);
    }

    public static Role findRoleSelect(IContext ctx,
                                      String content, Consumer<Role> consumer) {
        List<Role> found = findRole0(ctx, content);
        if (found == null) {
            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        } else {
            DiscordUtils.selectListButton(ctx, found.stream().limit(5).collect(Collectors.toList()),
                    role -> "%s%s (ID: %s)".formatted(
                            EmoteReference.BLUE_SMALL_MARKER,
                            role.getName(), role.getId()
                    ),
                    s -> optsCmd.baseEmbed(ctx, ctx.getLanguageContext().get("general.role_select")).
                            setDescription(s)
                            .build(),
                    consumer
            );
        }

        return null;
    }

    private static List<StandardGuildMessageChannel> findChannel0(IContext ctx, String content) {
        List<StandardGuildMessageChannel> found = findStandardChannels0(content, ctx.getGuild());
        if (found.isEmpty() && !content.isEmpty()) {
            ctx.sendLocalized("general.find_channels_failure", EmoteReference.ERROR);
            return null;
        }

        return found;
    }

    public static StandardGuildMessageChannel findChannel(IContext ctx, String content) {
        List<StandardGuildMessageChannel> found = findChannel0(ctx, content);
        if (found == null) {
            return null;
        }

        if (found.size() > 1 && !content.isEmpty()) {
            ctx.sendLocalized("general.too_many_channels", EmoteReference.THINKING,
                    found.stream()
                            .limit(5)
                            .map(StandardGuildMessageChannel::getName)
                            .collect(Collectors.joining(", "))
            );

            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        }

        return null;
    }

    public static StandardGuildMessageChannel findChannelSelect(IContext ctx,
                                                String content, Consumer<StandardGuildMessageChannel> consumer) {
        List<StandardGuildMessageChannel> found = findChannel0(ctx, content);
        // This feels a little weird, but found can return null here.
        if (found == null) {
            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        } else {
            selectList(ctx, found, consumer);
        }

        return null;
    }

    public static VoiceChannel findVoiceChannelSelect(IContext ctx, String content, Consumer<VoiceChannel> consumer) {
        List<VoiceChannel> found = findVoiceChannels0(content, ctx.getGuild());
        if (found.isEmpty() && !content.isEmpty()) {
            ctx.sendLocalized("general.find_voice_channels_failure", EmoteReference.ERROR);
            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        } else {
            selectList(ctx, found, consumer);
        }

        return null;
    }

    private static <T extends GuildChannel> void selectList(IContext ctx, List<T> found, Consumer<T> consumer) {
        DiscordUtils.selectListButton(ctx, found.stream().limit(5).collect(Collectors.toList()),
                channel -> "%s%s (ID: %s)".formatted(
                        EmoteReference.BLUE_SMALL_MARKER,
                        channel.getName(),
                        channel.getId()
                ), s -> optsCmd.baseEmbed(ctx, ctx.getLanguageContext().get("general.channel_select"))
                        .setDescription(s).build(), consumer);
    }

    // !! The stuff below here is basically taken from the old FinderUtils stuff from jagrosh.
    // !! not literally, since it had to be modified to work with new stuff.
    // !! Because of this, you can apply the original FinderUtils license to anything below this message (Apache 2.0).
    // !! License for the code following this message will be pasted below
    /*
     * Copyright 2016-2018 John Grosh (jagrosh) & Kaidan Gustave (TheMonitorLizard)
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
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

    public static List<Role> findRole0(String query, Guild guild)
    {
        Matcher roleMention = ROLE_MENTION.matcher(query);
        if (roleMention.matches())
        {
            Role role = guild.getRoleById(roleMention.group(1));
            if (role!=null) {
                return Collections.singletonList(role);
            }
        } else if(DISCORD_ID.matcher(query).matches()) {
            Role role = guild.getRoleById(query);
            if (role!=null) {
                return Collections.singletonList(role);
            }
        }

        ArrayList<Role> exact = new ArrayList<>();
        ArrayList<Role> wrongCase = new ArrayList<>();
        ArrayList<Role> startsWith = new ArrayList<>();
        ArrayList<Role> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        guild.getRoleCache().forEach((role) -> {
            String name = role.getName();
            if (name.equals(query)) {
                exact.add(role);
            } else if(name.equalsIgnoreCase(query) && exact.isEmpty()) {
                wrongCase.add(role);
            } else if(name.toLowerCase().startsWith(lowerQuery) && wrongCase.isEmpty()) {
                startsWith.add(role);
            } else if(name.toLowerCase().contains(lowerQuery) && startsWith.isEmpty()) {
                contains.add(role);
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

    private static List<VoiceChannel> genericVoiceChannelSearch(String query, SnowflakeCacheView<VoiceChannel> cache)
    {
        ArrayList<VoiceChannel> exact = new ArrayList<>();
        ArrayList<VoiceChannel> wrongCase = new ArrayList<>();
        ArrayList<VoiceChannel> startsWith = new ArrayList<>();
        ArrayList<VoiceChannel> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        cache.forEach((vc) -> {
            String name = vc.getName();
            if (name.equals(query)) {
                exact.add(vc);
            } else if (name.equalsIgnoreCase(query) && exact.isEmpty()) {
                wrongCase.add(vc);
            } else if (name.toLowerCase().startsWith(lowerQuery) && wrongCase.isEmpty()) {
                startsWith.add(vc);
            } else if (name.toLowerCase().contains(lowerQuery) && startsWith.isEmpty()) {
                contains.add(vc);
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

    public static List<VoiceChannel> findVoiceChannels0(String query, Guild guild)
    {
        if (DISCORD_ID.matcher(query).matches())
        {
            VoiceChannel vc = guild.getVoiceChannelById(query);
            if (vc != null) {
                return Collections.singletonList(vc);
            }
        }

        return genericVoiceChannelSearch(query, guild.getVoiceChannelCache());
    }
}
