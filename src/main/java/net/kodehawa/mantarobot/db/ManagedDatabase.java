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

import com.google.common.collect.Lists;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.ExtraRuntimeOptions;
import net.kodehawa.mantarobot.db.entities.*;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ManagedDatabase {
    private static final Logger log = LoggerFactory.getLogger(ManagedDatabase.class);
    private final MongoClient mongoClient;

    public ManagedDatabase(@Nonnull MongoClient mongoClient) {
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
        var id = guildId + ":" + name;
        log("Requesting Custom Command {} from MongoDB", id);

        MongoCollection<CustomCommand> collection = dbMantaro().getCollection(CustomCommand.DB_TABLE, CustomCommand.class);
        return collection.find().filter(Filters.eq(id)).first();
    }

    @Nullable
    @CheckReturnValue
    public CustomCommand getCustomCommand(@Nonnull Guild guild, @Nonnull String name) {
        return getCustomCommand(guild.getId(), name);
    }

    @Nullable
    @CheckReturnValue
    public CustomCommand getCustomCommand(@Nonnull GuildDatabase guild, @Nonnull String name) {
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
        log("Requesting all Custom Commands from MongoDB");
        return Lists.newArrayList(dbMantaro().getCollection(CustomCommand.DB_TABLE, CustomCommand.class).find());
    }

    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands(@Nonnull String guildId) {
        log("Requesting all Custom Commands from MongoDB on guild {}", guildId);
        var collection = dbMantaro().getCollection(CustomCommand.DB_TABLE, CustomCommand.class);
        return Lists.newArrayList(collection.find(Filters.eq("guildId", guildId)));
    }

    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands(@Nonnull Guild guild) {
        return getCustomCommands(guild.getId());
    }

    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands(@Nonnull GuildDatabase guild) {
        return getCustomCommands(guild.getId());
    }

    @Nonnull
    @CheckReturnValue
    public GuildDatabase getGuild(@Nonnull String guildId) {
        log("Requesting Guild {} from MongoDB", guildId);
        var collection = dbMantaro().getCollection(GuildDatabase.DB_TABLE, GuildDatabase.class);
        var guild = collection.find().filter(Filters.eq(guildId)).first();
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
    public MantaroObject getMantaroData() {
        log("Requesting MantaroObject from MongoDB");
        var collection = dbMantaro().getCollection(MantaroObject.DB_TABLE, MantaroObject.class);
        var obj = collection.find().first();
        if (obj == null) {
            obj = MantaroObject.create();
            obj.save();
        }

        return obj;
    }

    @Nonnull
    @CheckReturnValue
    public Player getPlayer(@Nonnull String userId) {
        log("Requesting Player {} from MongoDB", userId);
        var collection = dbMantaro().getCollection(Player.DB_TABLE, Player.class);
        var player = collection.find().filter(Filters.eq(userId)).first();

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
        log("Requesting Player {} from MongoDB", userId);
        var collection = dbMantaro().getCollection(PlayerStats.DB_TABLE, PlayerStats.class);
        var stats = collection.find().filter(Filters.eq(userId)).first();

        return stats == null ? PlayerStats.of(userId) : stats;
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

    //Can be null and it's perfectly valid.
    public Marriage getMarriage(String marriageId) {
        if (marriageId == null) {
            return null;
        }

        log("Requesting marriage {} from MongoDB", marriageId);
        return dbMantaro().getCollection(Marriage.DB_TABLE, Marriage.class).find(Filters.eq(marriageId)).first();
    }

    @Nonnull
    @CheckReturnValue
    public List<Marriage> getMarriages() {
        log("Requesting all marriages from MongoDB");
        return Lists.newArrayList(dbMantaro().getCollection(Marriage.DB_TABLE, Marriage.class).find());
    }

    @Nonnull
    @CheckReturnValue
    public List<PremiumKey> getPremiumKeys() {
        log("Requesting all premium keys from MongoDB");
        var collection = dbMantaro().getCollection(PremiumKey.DB_TABLE, PremiumKey.class);
        return Lists.newArrayList(collection.find());
    }

    //Also tests if the key is valid or not!
    @Nullable
    @CheckReturnValue
    public PremiumKey getPremiumKey(@Nullable String id) {
        log("Requesting Premium Key {} from MongoDB", id);
        if (id == null) return null;

        var collection = dbMantaro().getCollection(PremiumKey.DB_TABLE, PremiumKey.class);
        return collection.find().filter(Filters.eq(id)).first();
    }

    @Nonnull
    @CheckReturnValue
    public UserDatabase getUser(@Nonnull String userId) {
        log("Requesting User {} from MongoDB", userId);
        var collection = dbMantaro().getCollection(UserDatabase.DB_TABLE, UserDatabase.class);
        var user = collection.find().filter(Filters.eq(userId)).first();

        return user == null ? UserDatabase.of(userId) : user;
    }

    @Nonnull
    @CheckReturnValue
    public UserDatabase getUser(@Nonnull User user) {
        return getUser(user.getId());
    }

    @Nonnull
    @CheckReturnValue
    public UserDatabase getUser(@Nonnull Member member) {
        return getUser(member.getUser());
    }

    public <T extends ManagedMongoObject> void saveMongo(@Nonnull T object, Class<T> clazz) {
        log("Saving {} {}:{} to MongoDB (replacing whole)", object.getClass().getSimpleName(), object.getTableName(), object.getDatabaseId());

        var collection = dbMantaro().getCollection(object.getTableName(), clazz);
        var returnDoc = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER);
        var found = collection.findOneAndReplace(Filters.eq(object.getId()), object, returnDoc);
        if (found == null) { // New document?
            collection.insertOne(object);
        }
    }

    public <T extends ManagedMongoObject> void deleteMongo(@Nonnull T object, Class<T> clazz) {
        log("Deleting {} {}:{} from MongoDB (whole)", object.getClass().getSimpleName(), object.getTableName(), object.getDatabaseId());

        MongoCollection<T> collection = dbMantaro().getCollection(object.getTableName(), clazz);
        collection.deleteOne(Filters.eq(object.getId()));
    }

    public void updateFieldValue(ManagedMongoObject object, String key, Object value) {
        log("Updating id {} key {} (from db {}) to {} (single value)", object.getId(), key, object.getTableName(), value);

        var collection = dbMantaro().getCollection(object.getTableName());
        collection.updateOne(Filters.eq(object.getId()), Updates.set(key, value), new UpdateOptions().upsert(true));
    }

    public void updateFieldValues(ManagedMongoObject object, Map<String, Object> map) {
        log("Updating tracked set for id {} (db: {}, set size: {}) (batch values)", object.getId(), object.getTableName(), map.size(), object.getTableName());

        if (map.isEmpty()) {
            log("Empty tracked set when requesting update!");
            return;
        }

        var collection = dbMantaro().getCollection(object.getTableName());
        List<Bson> updateCollection = map.entrySet().stream().map((entry) -> {
            // This is MASSIVE jank. Why isn't it using the Codec it should use?
            if (entry.getValue() instanceof Map<?, ?> e) {
                var keySet = e.keySet();
                if (!keySet.isEmpty() && keySet.iterator().next() instanceof Enum<?>) {
                    return Updates.set(
                            entry.getKey(),
                            e.entrySet().stream().collect(Collectors.toMap(k -> k.getKey().toString(), Map.Entry::getValue))
                    );
                }
            }

            return Updates.set(entry.getKey(), entry.getValue());
        }).collect(Collectors.toList());

        collection.updateOne(Filters.eq(object.getId()), updateCollection, new UpdateOptions().upsert(true));
    }
}
