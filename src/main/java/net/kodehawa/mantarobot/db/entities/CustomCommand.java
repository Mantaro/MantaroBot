/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.CustomCommandData;
import net.kodehawa.mantarobot.utils.URLEncoding;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomCommand implements ManagedObject {
    public static final String DB_TABLE = "commands";
    private final String id;
    private final List<String> values;
    //Setting a default to avoid backwards compat issues.
    private CustomCommandData data = new CustomCommandData();

    @ConstructorProperties({"id", "values"})
    @JsonCreator
    public CustomCommand(@JsonProperty("id") String id, @JsonProperty("values") List<String> values, @JsonProperty("data") CustomCommandData data) {
        this.id = id;
        this.values = values.stream().map(URLEncoding::decode).collect(Collectors.toList());
        if (data != null)
            this.data = data;
    }

    public static CustomCommand of(String guildId, String cmdName, List<String> responses) {
        return new CustomCommand(guildId + ":" + cmdName, responses.stream().map(URLEncoding::encode).collect(Collectors.toList()), new CustomCommandData());
    }

    public static CustomCommand transfer(String guildId, CustomCommand command) {
        return new CustomCommand(guildId + ":" + command.getName(), command.getValues(), command.getData());
    }

    @JsonProperty("values")
    public List<String> encodedValues() {
        return values.stream().map(URLEncoding::encode).collect(Collectors.toList());
    }

    @JsonIgnore
    public String getGuildId() {
        return getId().split(":", 2)[0];
    }

    @JsonIgnore
    public String getName() {
        return getId().split(":", 2)[1];
    }

    @JsonIgnore
    public List<String> getValues() {
        return values;
    }

    public String getId() {
        return this.id;
    }

    @JsonIgnore
    @Override
    @Nonnull
    public String getTableName() {
        return DB_TABLE;
    }

    public CustomCommandData getData() {
        return this.data;
    }
}
