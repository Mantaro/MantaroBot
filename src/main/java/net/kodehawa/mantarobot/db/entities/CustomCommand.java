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
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class CustomCommand implements ManagedMongoObject {
    @BsonIgnore
    public static final String DB_TABLE = "commands";

    private String id;
    private String guildId;
    private List<String> values;
    private String owner;
    private boolean nsfw;
    private boolean locked;

    @BsonCreator
    public CustomCommand(@BsonId String id, @BsonProperty("guildId") String guildId, @BsonProperty("values") List<String> values,
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

    @BsonIgnore
    public String getName() {
        return getId().split(":", 2)[1];
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    public List<String> getValues() {
        return values;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isNsfw() {
        return nsfw;
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
    @Nonnull
    @Override
    public String getTableName() {
        return DB_TABLE;
    }

    @BsonIgnore
    @Override
    public void save() {
        MantaroData.db().saveMongo(this, CustomCommand.class);
    }

    @BsonIgnore
    @Override
    public void delete() {
        MantaroData.db().deleteMongo(this, CustomCommand.class);
    }
}
