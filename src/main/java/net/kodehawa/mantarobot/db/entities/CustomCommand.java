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

package net.kodehawa.mantarobot.db.entities;

import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedMongoObject;
import net.kodehawa.mantarobot.utils.Utils;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.stream.Collectors;

public class CustomCommand implements ManagedMongoObject {
    public static final String DB_TABLE = "commands";
    private final String id;
    private final String guildId;
    private final List<String> values;
    private String owner = "";
    private boolean nsfw = false;
    private boolean locked = false;

    @ConstructorProperties({"id", "values", "owner", "nsfw", "locked"})
    @BsonCreator
    public CustomCommand(@BsonProperty("id") String id, @BsonProperty("guildId") String guildId, @BsonProperty("values") List<String> values,
                         @BsonProperty("owner") String owner, @BsonProperty("nsfw") boolean nsfw, @BsonProperty("locked") boolean locked) {
        this.id = id;
        this.guildId = guildId;
        this.values = values.stream().map(Utils::decodeURL).collect(Collectors.toList());
        this.owner = owner;
        this.nsfw = nsfw;
        this.locked = locked;
    }

    @BsonIgnore
    public static CustomCommand of(String guildId, String cmdName, List<String> responses) {
        return new CustomCommand(guildId + ":" + cmdName, guildId, responses.stream().map(Utils::encodeURL).collect(Collectors.toList()),
                "", false, false);
    }

    @BsonIgnore
    public static CustomCommand transfer(String guildId, CustomCommand command) {
        return new CustomCommand(guildId + ":" + command.getName(), guildId, command.getValues(),
                command.getOwner(), command.isNsfw(), command.isLocked());
    }

    @BsonProperty("values")
    public List<String> encodedValues() {
        return values.stream().map(Utils::encodeURL).collect(Collectors.toList());
    }

    @BsonIgnore
    public String getGuildId() {
        return getId().split(":", 2)[0];
    }

    @BsonIgnore
    public String getName() {
        return getId().split(":", 2)[1];
    }

    @BsonIgnore
    public List<String> getValues() {
        return values;
    }

    @Nonnull
    public String getId() {
        return this.id;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isNsfw() {
        return this.nsfw;
    }

    public void setNsfw(boolean nsfw) {
        this.nsfw = nsfw;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @BsonIgnore
    @Override
    @Nonnull
    public String getTableName() {
        return DB_TABLE;
    }

    @Override
    public void save() {
        MantaroData.db().saveMongo(this, CustomCommand.class);
    }

    @Override
    public void delete() {
        MantaroData.db().deleteMongo(this, CustomCommand.class);
    }
}
