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

package net.kodehawa.mantarobot.db;

import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.ExtraRuntimeOptions;
import net.kodehawa.mantarobot.db.entities.*;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.rethinkdb.RethinkDB.r;

@Slf4j
public class ManagedDatabase {
    private final Connection conn;

    public ManagedDatabase(@Nonnull Connection conn) {
        this.conn = conn;
    }

    private static void log(String message, Object... fmtArgs) {
        if(ExtraRuntimeOptions.LOG_DB_ACCESS) {
            log.info(message, fmtArgs);
        }
    }

    private static void log(String message) {
        if(ExtraRuntimeOptions.LOG_DB_ACCESS) {
            log.info(message);
        }
    }

    @Nullable
    @CheckReturnValue
    public CustomCommand getCustomCommand(@Nonnull String guildId, @Nonnull String name) {
        log("Requesting custom command {}:{} from rethink", guildId, name);
        return r.table(CustomCommand.DB_TABLE).get(guildId + ":" + name).run(conn, CustomCommand.class);
    }

    @Nullable
    @CheckReturnValue
    public CustomCommand getCustomCommand(@Nonnull Guild guild, @Nonnull String name) {
        return getCustomCommand(guild.getId(), name);
    }

    @Nullable
    @CheckReturnValue
    public CustomCommand getCustomCommand(@Nonnull DBGuild guild, @Nonnull String name) {
        return getCustomCommand(guild.getId(), name);
    }

    @Nullable
    @CheckReturnValue
    public CustomCommand getCustomCommand(@Nonnull GuildMessageReceivedEvent event, @Nonnull String cmd) {
        return getCustomCommand(event.getGuild(), cmd);
    }

    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands() {
        log("Requesting all custom commands from rethink");
        Cursor<CustomCommand> c = r.table(CustomCommand.DB_TABLE).run(conn, CustomCommand.class);
        return c.toList();
    }

    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands(@Nonnull String guildId) {
        log("Requesting all custom commands from guild {} from rethink", guildId);
        String pattern = '^' + guildId + ':';
        Cursor<CustomCommand> c = r.table(CustomCommand.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, CustomCommand.class);
        return c.toList();
    }

    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands(@Nonnull Guild guild) {
        return getCustomCommands(guild.getId());
    }

    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands(@Nonnull DBGuild guild) {
        return getCustomCommands(guild.getId());
    }

    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommandsByName(@Nonnull String name) {
        log("Requesting all custom commands named {} from rethink", name);
        String pattern = ':' + name + '$';
        Cursor<CustomCommand> c = r.table(CustomCommand.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, CustomCommand.class);
        return c.toList();
    }

    @Nullable
    @CheckReturnValue
    public DBGuild getGuild(@Nonnull String guildId) {
        log("Requesting guild {} from rethink", guildId);
        DBGuild guild = r.table(DBGuild.DB_TABLE).get(guildId).run(conn, DBGuild.class);
        return guild == null ? DBGuild.of(guildId) : guild;
    }

    @Nullable
    @CheckReturnValue
    public DBGuild getGuild(@Nonnull Guild guild) {
        return getGuild(guild.getId());
    }

    @Nullable
    @CheckReturnValue
    public DBGuild getGuild(@Nonnull Member member) {
        return getGuild(member.getGuild());
    }

    @Nullable
    @CheckReturnValue
    public DBGuild getGuild(@Nonnull GuildMessageReceivedEvent event) {
        return getGuild(event.getGuild());
    }

    @Nonnull
    @CheckReturnValue
    public MantaroObj getMantaroData() {
        log("Requesting MantaroObj from rethink");
        MantaroObj obj = r.table(MantaroObj.DB_TABLE).get("mantaro").run(conn, MantaroObj.class);
        return obj == null ? MantaroObj.create() : obj;
    }

    @Nullable
    @CheckReturnValue
    public Player getPlayer(@Nonnull String userId) {
        log("Requesting player {} from rethink", userId);
        Player player = r.table(Player.DB_TABLE).get(userId + ":g").run(conn, Player.class);
        return player == null ? Player.of(userId) : player;
    }

    @Nullable
    @CheckReturnValue
    public Player getPlayer(@Nonnull User user) {
        return getPlayer(user.getId());
    }

    @Nullable
    @CheckReturnValue
    public Player getPlayer(@Nonnull Member member) {
        return getPlayer(member.getUser());
    }

    @Nonnull
    @CheckReturnValue
    public List<Player> getPlayers() {
        log("Requesting all players from rethink");
        String pattern = ":g$";
        Cursor<Player> c = r.table(Player.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, Player.class);
        return c.toList();
    }

    @Nonnull
    @CheckReturnValue
    public List<PremiumKey> getPremiumKeys() {
        log("Requesting all premium keys from rethink");
        Cursor<PremiumKey> c = r.table(PremiumKey.DB_TABLE).run(conn, PremiumKey.class);
        return c.toList();
    }

    //Also tests if the key is valid or not!
    @Nullable
    @CheckReturnValue
    public PremiumKey getPremiumKey(@Nullable String id) {
        log("Requesting premium key {} from rethink", id);
        if(id == null) return null;
        return r.table(PremiumKey.DB_TABLE).get(id).run(conn, PremiumKey.class);
    }

    @Nullable
    @CheckReturnValue
    public DBUser getUser(@Nonnull String userId) {
        log("Requesting user {} from rethink", userId);
        DBUser user = r.table(DBUser.DB_TABLE).get(userId).run(conn, DBUser.class);
        return user == null ? DBUser.of(userId) : user;
    }

    @Nullable
    @CheckReturnValue
    public DBUser getUser(@Nonnull User user) {
        return getUser(user.getId());
    }

    @Nullable
    @CheckReturnValue
    public DBUser getUser(@Nonnull Member member) {
        return getUser(member.getUser());
    }

    public void save(@Nonnull ManagedObject object) {
        log("Saving {} {}:{} to rethink", object.getClass().getSimpleName(), object.getTableName(), object.getDatabaseId());
        r.table(object.getTableName())
                .insert(object)
                .optArg("conflict", "replace")
                .runNoReply(conn);
    }

    public void delete(@Nonnull ManagedObject object) {
        log("Deleting {} {}:{} from rethink", object.getClass().getSimpleName(), object.getTableName(), object.getDatabaseId());
        r.table(object.getTableName())
                .get(object.getId())
                .delete()
                .runNoReply(conn);
    }

    /*public void save(CustomCommand command) {
        r.table(CustomCommand.DB_TABLE).insert(command)
                .optArg("conflict", "replace")
                .runNoReply(conn);
    }

    public void save(DBGuild guild) {
        r.table(DBGuild.DB_TABLE).insert(guild)
                .optArg("conflict", "replace")
                .runNoReply(conn);
    }

    public void save(DBUser user) {
        r.table(DBUser.DB_TABLE).insert(user)
                .optArg("conflict", "replace")
                .runNoReply(conn);
    }

    public void save(MantaroObj obj) {
        r.table(MantaroObj.DB_TABLE).insert(obj)
                .optArg("conflict", "replace")
                .runNoReply(conn);
    }

    public void save(Player player) {
        r.table(Player.DB_TABLE).insert(player)
                .optArg("conflict", "replace")
                .runNoReply(conn);
    }

    public void save(PremiumKey key) {
        r.table(PremiumKey.DB_TABLE).insert(key)
                .optArg("conflict", "replace")
                .runNoReply(conn);
    }

    public void delete(CustomCommand command) {
        r.table(CustomCommand.DB_TABLE).get(command.getId()).delete().runNoReply(conn);
    }

    public void delete(DBGuild guild) {
        r.table(DBGuild.DB_TABLE).get(guild.getId()).delete().runNoReply(conn);
    }

    public void delete(DBUser user) {
        r.table(DBUser.DB_TABLE).get(user.getId()).delete().runNoReply(conn);
    }

    public void delete(MantaroObj obj) {
        r.table(MantaroObj.DB_TABLE).get(obj.getId()).delete().runNoReply(conn);
    }

    public void delete(Player player) {
        r.table(Player.DB_TABLE).get(player.getId()).delete().runNoReply(conn);
    }

    public void delete(PremiumKey key) {
        r.table(PremiumKey.DB_TABLE).get(key.getId()).delete().runNoReply(conn);
    }*/
}
