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

package net.kodehawa.mantarobot.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Result;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.ExtraRuntimeOptions;
import net.kodehawa.mantarobot.db.entities.*;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.rethinkdb.RethinkDB.r;

public class ManagedDatabase {
    private static final Logger log = LoggerFactory.getLogger(ManagedDatabase.class);
    private final Connection conn;
    private final MongoClient mongoClient;

    public ManagedDatabase(@Nonnull Connection conn, @Nonnull MongoClient mongoClient) {
        this.conn = conn;
        this.mongoClient = mongoClient;
    }

    private static void log(String message, Object... fmtArgs) {
        if (ExtraRuntimeOptions.LOG_DB_ACCESS) {
            log.info(message, fmtArgs);
        }
    }

    private static void log(String message) {
        if (ExtraRuntimeOptions.LOG_DB_ACCESS) {
            log.info(message);
        }
    }

    public MongoDatabase dbMantaro() {
        return mongoClient.getDatabase("mantaro");
    }

    @Nullable
    @CheckReturnValue
    public CustomCommand getCustomCommand(@Nonnull String guildId, @Nonnull String name) {
        log("Requesting custom command {}:{} from rethink", guildId, name);
        return r.table(CustomCommand.DB_TABLE).get(guildId + ":" + name).runAtom(conn, CustomCommand.class);
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
    public CustomCommand getCustomCommand(@Nonnull MessageReceivedEvent event, @Nonnull String cmd) {
        return getCustomCommand(event.getGuild(), cmd);
    }

    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands() {
        log("Requesting all custom commands from rethink");
        Result<CustomCommand> c = r.table(CustomCommand.DB_TABLE).run(conn, CustomCommand.class);
        return c.toList();
    }

    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands(@Nonnull String guildId) {
        log("Requesting all custom commands from guild {} from rethink", guildId);
        Result<CustomCommand> c = r.table(CustomCommand.DB_TABLE)
                .getAll(guildId)
                .optArg("index", "guild")
                .run(conn, CustomCommand.class);
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
        Result<CustomCommand> c = r.table(CustomCommand.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, CustomCommand.class);
        return c.toList();
    }


    @Nonnull
    @CheckReturnValue
    public GuildDatabase getGuild(@Nonnull String guildId) {
        log("Requesting guild {} from MongoDB", guildId);
        MongoCollection<GuildDatabase> collection = dbMantaro().getCollection(GuildDatabase.DB_TABLE, GuildDatabase.class);
        GuildDatabase guild = collection.find().filter(new Document("_id", guildId)).first();
        return guild == null ? GuildDatabase.of(guildId) : guild;
    }

    @Nonnull
    @CheckReturnValue
    public GuildDatabase getGuild(@Nonnull Guild guild) {
        return getGuild(guild.getId());
    }

    @Nonnull
    @CheckReturnValue
    public GuildDatabase getGuild(@Nonnull Member member) {
        return getGuild(member.getGuild());
    }

    @Nonnull
    @CheckReturnValue
    public GuildDatabase getGuild(@Nonnull MessageReceivedEvent event) {
        return getGuild(event.getGuild());
    }

    @Nonnull
    @CheckReturnValue
    public MantaroObj getMantaroData() {
        log("Requesting MantaroObj from rethink");
        MantaroObj obj = r.table(MantaroObj.DB_TABLE).get("mantaro").runAtom(conn, MantaroObj.class);
        return obj == null ? MantaroObj.create() : obj;
    }

    @Nonnull
    @CheckReturnValue
    public Player getPlayer(@Nonnull String userId) {
        log("Requesting player {} from rethink", userId);
        Player player = r.table(Player.DB_TABLE).get(userId + ":g").runAtom(conn, Player.class);
        return player == null ? Player.of(userId) : player;
    }

    @Nonnull
    @CheckReturnValue
    public Player getPlayer(@Nonnull User user) {
        return getPlayer(user.getId());
    }

    @Nonnull
    @CheckReturnValue
    public Player getPlayer(@Nonnull Member member) {
        return getPlayer(member.getUser());
    }

    @Nonnull
    @CheckReturnValue
    public PlayerStats getPlayerStats(@Nonnull String userId) {
        log("Requesting player STATS {} from rethink", userId);
        PlayerStats playerStats = r.table(PlayerStats.DB_TABLE).get(userId).runAtom(conn, PlayerStats.class);
        return playerStats == null ? PlayerStats.of(userId) : playerStats;
    }

    @Nonnull
    @CheckReturnValue
    public PlayerStats getPlayerStats(@Nonnull User user) {
        return getPlayerStats(user.getId());
    }

    @Nonnull
    @CheckReturnValue
    public PlayerStats getPlayerStats(@Nonnull Member member) {
        return getPlayerStats(member.getUser());
    }

    @Nonnull
    @CheckReturnValue
    public List<Player> getPlayers() {
        log("Requesting all players from rethink");
        String pattern = ":g$";
        Result<Player> c = r.table(Player.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, Player.class);
        return c.toList();
    }

    //Can be null and it's perfectly valid.
    public Marriage getMarriage(String marriageId) {
        if (marriageId == null) {
            return null;
        }

        log("Requesting marriage {} from rethink", marriageId);
        return r.table(Marriage.DB_TABLE).get(marriageId).runAtom(conn, Marriage.class);
    }

    @Nonnull
    @CheckReturnValue
    public List<Marriage> getMarriages() {
        log("Requesting all marriages from rethink");
        Result<Marriage> c = r.table(Marriage.DB_TABLE).run(conn, Marriage.class);
        return c.toList();
    }

    @Nonnull
    @CheckReturnValue
    public List<PremiumKey> getPremiumKeys() {
        log("Requesting all premium keys from rethink");
        Result<PremiumKey> c = r.table(PremiumKey.DB_TABLE).run(conn, PremiumKey.class);
        return c.toList();
    }

    //Also tests if the key is valid or not!
    @Nullable
    @CheckReturnValue
    public PremiumKey getPremiumKey(@Nullable String id) {
        log("Requesting premium key {} from rethink", id);
        if (id == null) return null;
        return r.table(PremiumKey.DB_TABLE).get(id).runAtom(conn, PremiumKey.class);
    }

    @Nonnull
    @CheckReturnValue
    public DBUser getUser(@Nonnull String userId) {
        log("Requesting user {} from rethink", userId);
        DBUser user = r.table(DBUser.DB_TABLE).get(userId).runAtom(conn, DBUser.class);
        return user == null ? DBUser.of(userId) : user;
    }

    @Nonnull
    @CheckReturnValue
    public DBUser getUser(@Nonnull User user) {
        return getUser(user.getId());
    }

    @Nonnull
    @CheckReturnValue
    public DBUser getUser(@Nonnull Member member) {
        return getUser(member.getUser());
    }

    public <T extends ManagedMongoObject> void saveMongo(@Nonnull T object, Class<T> clazz) {
        log("Saving {} {}:{} to MongoDB (replacing)", object.getClass().getSimpleName(), object.getTableName(), object.getDatabaseId());

        Document filter = new Document("_id", object.getId());
        FindOneAndReplaceOptions returnDocAfterReplace = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER);
        MongoCollection<T> collection = dbMantaro().getCollection(object.getTableName(), clazz);
        var found = collection.findOneAndReplace(filter, object, returnDocAfterReplace);
        if (found == null) { // New document?
            collection.insertOne(object);
        }
    }

    public void save(@Nonnull ManagedObject object) {
        log("Saving {} {}:{} to rethink (replacing)", object.getClass().getSimpleName(), object.getTableName(), object.getDatabaseId());

        r.table(object.getTableName())
                .insert(object)
                .optArg("conflict", "replace")
                .runNoReply(conn);
    }

    public void saveUpdating(@Nonnull ManagedObject object) {
        log("Saving {} {}:{} to rethink (updating)", object.getClass().getSimpleName(), object.getTableName(), object.getDatabaseId());

        r.table(object.getTableName())
                .insert(object)
                .optArg("conflict", "update")
                .runNoReply(conn);
    }

    public void delete(@Nonnull ManagedObject object) {
        log("Deleting {} {}:{} from rethink", object.getClass().getSimpleName(), object.getTableName(), object.getDatabaseId());

        r.table(object.getTableName())
                .get(object.getId())
                .delete()
                .runNoReply(conn);
    }
}
