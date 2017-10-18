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
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.*;

import java.util.List;

import static com.rethinkdb.RethinkDB.r;

public class ManagedDatabase {
    private final Config c = MantaroData.config().get();

    public CustomCommand getCustomCommand(String guildId, String name) {

        CustomCommand custom;

        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            custom = r.table(CustomCommand.DB_TABLE).get(guildId + ":" + name).run(conn, CustomCommand.class);
        }

        return custom;
    }

    public CustomCommand getCustomCommand(Guild guild, String name) {
        return getCustomCommand(guild.getId(), name);
    }

    public CustomCommand getCustomCommand(DBGuild guild, String name) {
        return getCustomCommand(guild.getId(), name);
    }

    public CustomCommand getCustomCommand(GuildMessageReceivedEvent event, String cmd) {
        return getCustomCommand(event.getGuild(), cmd);
    }

    public List<CustomCommand> getCustomCommands() {
        Cursor<CustomCommand> ccs;

        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            ccs = r.table(CustomCommand.DB_TABLE).run(conn, CustomCommand.class);
        }
        return ccs.toList();
    }

    public List<CustomCommand> getCustomCommands(String guildId) {
        String pattern = '^' + guildId + ':';
        Cursor<CustomCommand> cc;
        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            cc = r.table(CustomCommand.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, CustomCommand.class);
        }

        return cc.toList();
    }

    public List<CustomCommand> getCustomCommands(Guild guild) {
        return getCustomCommands(guild.getId());
    }

    public List<CustomCommand> getCustomCommands(DBGuild guild) {
        return getCustomCommands(guild.getId());
    }

    public List<CustomCommand> getCustomCommandsByName(String name) {
        String pattern = ':' + name + '$';
        Cursor<CustomCommand> cc;

        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            cc = r.table(CustomCommand.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, CustomCommand.class);
        }

        return cc.toList();
    }

    public DBGuild getGuild(String guildId) {
        DBGuild guild;

        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            guild = r.table(DBGuild.DB_TABLE).get(guildId).run(conn, DBGuild.class);
        }

        return guild == null ? DBGuild.of(guildId) : guild;
    }

    public DBGuild getGuild(Guild guild) {
        return getGuild(guild.getId());
    }

    public DBGuild getGuild(Member member) {
        return getGuild(member.getGuild());
    }

    public DBGuild getGuild(GuildMessageReceivedEvent event) {
        return getGuild(event.getGuild());
    }

    public MantaroObj getMantaroData() {
        MantaroObj obj;

        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            obj = r.table(MantaroObj.DB_TABLE).get("mantaro").run(conn, MantaroObj.class);
        }

        return obj == null ? MantaroObj.create() : obj;
    }

    public Player getPlayer(String userId) {
        Player player;

        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            player = r.table(Player.DB_TABLE).get(userId + ":g").run(conn, Player.class);
        }

        return player == null ? Player.of(userId) : player;
    }

    public Player getPlayer(User user) {
        return getPlayer(user.getId());
    }

    public Player getPlayer(Member member) {
        return getPlayer(member.getUser());
    }

    public List<Player> getPlayers() {
        String pattern = ":g$";
        Cursor<Player> playerCursor;
        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            playerCursor = r.table(Player.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, Player.class);
        }

        return playerCursor.toList();
    }

    public List<PremiumKey> getPremiumKeys() {
        Cursor<PremiumKey> keyCursor;

        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            keyCursor = r.table(PremiumKey.DB_TABLE).run(conn, PremiumKey.class);
        }

        return keyCursor.toList();
    }

    //Also tests if the key is valid or not!
    public PremiumKey getPremiumKey(String id){
        if(id == null) return null;

        PremiumKey key;

        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            key = r.table(PremiumKey.DB_TABLE).get(id).run(conn, PremiumKey.class);
        }

        return key;
    }

    public DBUser getUser(String userId) {
        DBUser user;

        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            user = r.table(DBUser.DB_TABLE).get(userId).run(conn, DBUser.class);
        }

        return user == null ? DBUser.of(userId) : user;
    }

    public DBUser getUser(User user) {
        return getUser(user.getId());
    }

    public DBUser getUser(Member member) {
        return getUser(member.getUser());
    }
}
