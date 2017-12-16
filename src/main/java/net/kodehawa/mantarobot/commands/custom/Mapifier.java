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

package net.kodehawa.mantarobot.commands.custom;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;
import static net.kodehawa.mantarobot.utils.Utils.iterate;
import static org.apache.commons.lang3.StringUtils.capitalize;

public class Mapifier {
    private static final Pattern GETTER_MODIFIER = Pattern.compile("\\$\\([A-Za-z0-9.]+?\\)");

    public static String dynamicResolve(String string, Map<String, String> dynamicMap) {
        if(!string.contains("$(")) return string;

        Set<String> skipIfIterated = new HashSet<>();
        for(String key : iterate(GETTER_MODIFIER, string)) {
            if(skipIfIterated.contains(key)) continue;
            String mapKey = key.substring(2, key.length() - 1);
            string = string.replace(key, dynamicMap.getOrDefault(mapKey, mapKey));
            if(!string.contains("$(")) break;
            skipIfIterated.add(key);
        }

        return string;
    }

    public static void map(String prefix, Map<String, String> map, Guild guild) {
        map.put(prefix, guild.getName());
        prefix = prefix + ".";
        map.put(prefix + "name", guild.getName());
        map(prefix + "owner", map, guild.getOwner());
        map.put(prefix + "region", guild.getRegion().getName());
        map.put(prefix + "totalusers", String.valueOf(guild.getMembers().size()));
    }

    public static void map(String prefix, Map<String, String> map, Member member) {
        map.put(prefix, member.getAsMention());
        prefix = prefix + ".";
        map.put(prefix + "username", member.getUser().getName());
        map.put(prefix + "discriminator", member.getUser().getDiscriminator());
        map.put(prefix + "name", member.getEffectiveName());
        map.put(prefix + "game", member.getGame() != null ? member.getGame().getName() : "None");
        map.put(prefix + "status", capitalize(member.getOnlineStatus().getKey()));
        map.put(prefix + "mention", member.getAsMention());
        map.put(prefix + "avatar", member.getUser().getEffectiveAvatarUrl());
        map.put(prefix + "id", member.getUser().getId());
    }

    public static void map(String prefix, Map<String, String> map, GuildMessageReceivedEvent event) {
        map.put(prefix, event.getMember().getAsMention() + "@" + event.getChannel().getAsMention());
        prefix = prefix + ".";
        map(prefix + "channel", map, event.getChannel());
        map(prefix + "guild", map, event.getGuild());
        map(prefix + "me", map, event.getGuild().getSelfMember());
        map(prefix + "author", map, event.getMember());
        map(prefix + "message", map, event.getMessage());
    }

    public static void map(String prefix, Map<String, String> map, GenericGuildMemberEvent event) {
        map.put(prefix, event.getMember().getAsMention() + "@" + event.getGuild().getName());
        prefix = prefix + ".";
        map(prefix + "guild", map, event.getGuild());
        map(prefix + "me", map, event.getGuild().getSelfMember());
        map(prefix + "user", map, event.getMember());
    }

    public static void map(String prefix, Map<String, String> map, Message message) {
        map.put(prefix, splitArgs(message.getContentRaw(), 2)[1]);
        prefix = prefix + ".";
        map.put(prefix + "raw", splitArgs(message.getContentRaw(), 2)[1]);
        map.put(prefix + "textual", splitArgs(message.getContentDisplay(), 2)[1]);
        map.put(prefix + "stripped", splitArgs(message.getContentStripped(), 2)[1]);
    }

    public static void map(String prefix, Map<String, String> map, TextChannel channel) {
        map.put(prefix, channel.getAsMention());
        prefix = prefix + ".";
        map.put(prefix + "topic", channel.getTopic());
        map.put(prefix + "name", channel.getName());
        map.put(prefix + "id", channel.getId());
        map.put(prefix + "mention", channel.getAsMention());
    }
}
