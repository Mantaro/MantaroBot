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

package net.kodehawa.mantarobot.commands.custom.legacy;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Pattern;

import static net.kodehawa.mantarobot.utils.Utils.iterate;
import static org.apache.commons.lang3.StringUtils.capitalize;

public class DynamicModifiers extends LinkedHashMap<String, String> {
    private static final long serialVersionUID = 1;
    private static final Pattern GETTER_MODIFIER = Pattern.compile("\\$\\([A-Za-z0-9.]+?\\)");
    
    private static String k(String... parts) {
        return String.join(".", parts);
    }
    
    public String resolve(String string) {
        if(!string.contains("$("))
            return string;
        
        Set<String> dejaVu = new HashSet<>();
        for(String key : iterate(GETTER_MODIFIER, string)) {
            if(dejaVu.contains(key)) continue;
            String mapKey = key.substring(2, key.length() - 1);
            String value = get(mapKey);
            if(value == null) {
                value = "{Unresolved variable " + mapKey + "}";
            }
            string = string.replace(key, value.replaceAll("[^\\\\]\\\\[^\\\\]", "\\\\"));
            if(!string.contains("$(")) break;
            dejaVu.add(key);
        }
        
        return string;
    }
    
    public DynamicModifiers mapGuild(String prefix, Guild guild) {
        return this
                       .set(prefix, guild.getName())
                       .set(prefix, "name", guild.getName())
                       .mapMember(k(prefix, "owner"), guild.getOwner())
                       .set(prefix, "region", guild.getRegion().getName())
                       .set(prefix, "totalusers", String.valueOf(guild.getMembers().size()));
    }
    
    public DynamicModifiers mapUser(String prefix, User member) {
        return this
                       .set(prefix, member.getAsMention())
                       .set(prefix, "tag", member.getAsTag())
                       .set(prefix, "username", member.getName())
                       .set(prefix, "discriminator", member.getDiscriminator())
                       .set(prefix, "name", member.getName())
                       .set(prefix, "mention", member.getAsMention())
                       .set(prefix, "avatar", member.getEffectiveAvatarUrl())
                       .set(prefix, "id", member.getId());
    }
    
    public DynamicModifiers mapMember(String prefix, Member member) {
        return this
                       .mapUser(prefix, member.getUser())
                       .set(prefix, "name", member.getEffectiveName())
                       //TODO: i want this to compile already come on
                       //.set(prefix, "game", member.getGame() != null ? member.getGame().getName() : "None")
                       .set(prefix, "status", capitalize(member.getOnlineStatus().getKey()));
    }
    
    public DynamicModifiers mapEvent(String botPrefix, String prefix, GuildMessageReceivedEvent event) {
        return this.mapEvent(botPrefix, prefix, (GenericGuildMessageEvent) event)
                       .set(prefix, event.getMember().getAsMention() + "@" + event.getChannel().getAsMention())
                       .mapMember(k(prefix, "author"), event.getMember())
                       .mapMessage(k(prefix, "message"), new CustomMessage(event.getMessage(), botPrefix));
    }
    
    public DynamicModifiers mapEvent(String botPrefix, String prefix, GenericGuildMessageEvent event) {
        return this.set(prefix, "timestamp", new Date(System.currentTimeMillis()).toString())
                       .mapChannel(k(prefix, "channel"), event.getChannel())
                       .mapGuild(k(prefix, "guild"), event.getGuild())
                       .mapMember(k(prefix, "me"), event.getGuild().getSelfMember());
    }
    
    public DynamicModifiers mapEvent(String prefix, GenericGuildMemberEvent event) {
        return this
                       .set(prefix, event.getMember().getAsMention() + "@" + event.getGuild().getName())
                       .mapGuild(k(prefix, "guild"), event.getGuild())
                       .mapMember(k(prefix, "me"), event.getGuild().getSelfMember())
                       .mapMember(k(prefix, "user"), event.getMember());
    }
    
    public DynamicModifiers mapEvent(String prefix, GenericGuildEvent event) {
        return this
                       .mapGuild(k(prefix, "guild"), event.getGuild())
                       .mapMember(k(prefix, "me"), event.getGuild().getSelfMember());
    }
    
    public DynamicModifiers mapMessage(String prefix, Message message) {
        return mapMessage(prefix, new CustomMessage(message, ""));
    }
    
    public DynamicModifiers mapMessage(String prefix, CustomMessage message) {
        return this
                       .set(prefix, message.getContentRaw())
                       .set(prefix, "raw", message.getContentRaw())
                       .set(prefix, "textual", message.getContentDisplay())
                       .set(prefix, "stripped", message.getContentStripped());
    }
    
    public DynamicModifiers mapChannel(String prefix, TextChannel channel) {
        return this
                       .set(prefix, channel.getAsMention())
                       .set(prefix, "topic", channel.getTopic())
                       .set(prefix, "name", channel.getName())
                       .set(prefix, "id", channel.getId())
                       .set(prefix, "mention", channel.getAsMention());
    }
    
    public DynamicModifiers set(String key, String value) {
        if(!containsKey(key))
            put(key, value);
        
        return this;
    }
    
    public DynamicModifiers set(String prefix, String key, String value) {
        return set(k(prefix, key), value);
    }
}
