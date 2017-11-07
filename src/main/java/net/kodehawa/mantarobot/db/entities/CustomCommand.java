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

package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.utils.URLEncoding;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class CustomCommand implements ManagedObject {
    public static final String DB_TABLE = "commands";
    private final String id;
    private final List<String> values;

    @ConstructorProperties({"id", "values"})
    @JsonCreator
    public CustomCommand(@JsonProperty("id") String id, @JsonProperty("values") List<String> values) {
        this.id = id;
        this.values = values.stream().map(URLEncoding::decode).collect(Collectors.toList());
    }

    public static CustomCommand of(String guildId, String cmdName, List<String> responses) {
        return new CustomCommand(guildId + ":" + cmdName, responses.stream().map(URLEncoding::encode).collect(Collectors.toList()));
    }

    @Override
    public void delete() {
        MantaroData.db().delete(this);
    }

    @Override
    public void save() {
        MantaroData.db().save(this);
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
}
