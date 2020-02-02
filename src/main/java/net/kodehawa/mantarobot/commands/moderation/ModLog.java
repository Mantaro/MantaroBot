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

package net.kodehawa.mantarobot.commands.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.utils.Utils;

public class ModLog {
    private static ManagedDatabase db = MantaroData.db();
    
    public static void log(Member author, User target, String reason, String channel, ModAction action, long caseNumber, int messagesDeleted) {
        DBGuild guildDB = db.getGuild(author.getGuild());
        Player player = db.getPlayer(author);
        PlayerData playerData = player.getData();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        
        embedBuilder.addField("Responsible Moderator", author.getEffectiveName(), true);
        
        if(target != null) {
            embedBuilder.addField("Member", target.getName(), true);
            embedBuilder.setThumbnail(target.getEffectiveAvatarUrl());
        } else {
            embedBuilder.setThumbnail(author.getUser().getEffectiveAvatarUrl());
        }
        
        embedBuilder.addField("Reason", reason, false);
        embedBuilder.addField("Channel", channel, true);
        
        if(action == ModAction.PRUNE) {
            embedBuilder.addField("Messages Deleted", String.valueOf(messagesDeleted), true);
        }
        
        //Why was this a giant switch statement?
        embedBuilder.setAuthor(String.format("%s | Case #%s", Utils.capitalize(action.name()), caseNumber),
                null, author.getUser().getEffectiveAvatarUrl());
        
        if(!playerData.hasBadge(Badge.POWER_USER)) {
            playerData.addBadgeIfAbsent(Badge.POWER_USER);
            player.saveAsync();
        }
        
        if(guildDB.getData().getGuildLogChannel() != null) {
            TextChannel logChannel = MantaroBot.getInstance().getShardManager().getTextChannelById(guildDB.getData().getGuildLogChannel());
            if(logChannel != null) {
                logChannel.sendMessage(embedBuilder.build()).queue();
            }
        }
    }
    
    //Overload
    public static void log(Member author, User target, String reason, String channel, ModAction action, long caseNumber) {
        log(author, target, reason, channel, action, caseNumber, 0);
    }
    
    public enum ModAction {
        TEMP_BAN, BAN, KICK, MUTE, UNMUTE, WARN, PRUNE
    }
}

