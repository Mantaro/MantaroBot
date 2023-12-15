/*
 * Copyright (C) 2016 Kodehawa
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

package net.kodehawa.mantarobot.core.command.helpers;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.attribute.IAgeRestrictedChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kodehawa.mantarobot.core.command.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.MantaroObject;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.MongoGuild;
import net.kodehawa.mantarobot.db.entities.MongoUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PlayerStats;
import net.kodehawa.mantarobot.utils.commands.UtilsContext;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RateLimitContext;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Collection;

@SuppressWarnings("unused")
public interface IContext {
    Guild getGuild();
    GuildMessageChannel getChannel();
    Member getMember();
    User getAuthor();
    RateLimitContext ratelimitContext();
    UtilsContext getUtilsContext();
    I18nContext getLanguageContext();
    void send(String s);
    void send(MessageCreateData message);
    void sendStripped(String s);
    void send(MessageEmbed e);
    void send(MessageEmbed e, ActionRow... actionRows);
    Message sendResult(String s);
    Message sendResult(MessageEmbed e);
    void sendLocalized(String s, Object... args);
    void sendLocalizedStripped(String s, Object... args);
    void sendFormat(String message, Object... format);
    void sendFormatStripped(String message, Object... format);
    void sendFormat(String message, Collection<ActionRow> actionRow, Object... format);
    ManagedDatabase db();
    ShardManager getShardManager();
    MantaroObject getMantaroData();
    Config getConfig();

    default Player getPlayer() {
        return db().getPlayer(getAuthor());
    }

    default Player getPlayer(User user) {
        return db().getPlayer(user);
    }

    default MongoUser getDBUser() {
        return db().getUser(getAuthor());
    }

    default MongoUser getDBUser(User user) {
        return db().getUser(user);
    }

    default MongoGuild getDBGuild() {
        return db().getGuild(getGuild());
    }

    default Marriage getMarriage(@NotNull MongoUser userData) {
        return db().getMarriage(userData.getMarriageId());
    }

    default PlayerStats getPlayerStats() {
        return db().getPlayerStats(getMember());
    }

    default PlayerStats getPlayerStats(String id) {
        return db().getPlayerStats(id);
    }

    default PlayerStats getPlayerStats(User user) {
        return db().getPlayerStats(user);
    }

    default PlayerStats getPlayerStats(Member member) {
        return db().getPlayerStats(member);
    }

    default Color getMemberColor(@NotNull Member member) {
        return member.getColor() == null ? Color.PINK : member.getColor();
    }

    default Color getMemberColor() {
        return getMemberColor(getMember());
    }

    default I18nContext getGuildLanguageContext() {
        return new I18nContext(getDBGuild(), null);
    }

    default boolean isChannelNSFW() {
        if (getChannel() instanceof IAgeRestrictedChannel txtChannel) {
            return txtChannel.isNSFW();
        }

        return true;
    }

    default EmbedBuilder baseEmbed(IContext ctx, String name, String image) {
        return new EmbedBuilder()
                .setAuthor(name, null, image)
                .setColor(ctx.getMember().getColor())
                .setFooter("Requested by: %s".formatted(ctx.getMember().getEffectiveName()),
                        ctx.getGuild().getIconUrl()
                );
    }

}

