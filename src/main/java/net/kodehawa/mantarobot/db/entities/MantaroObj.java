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
import lombok.Data;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedObject;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class MantaroObj implements ManagedObject {
    public static final String DB_TABLE = "mantaro";
    public final String id = "mantaro";
    public List<String> blackListedGuilds = null;
    public List<String> blackListedUsers = null;
    public List<String> patreonUsers = null;
    private Map<Long, Pair<String, Long>> mutes = null;
    private Map<String, Long> tempBans = null;

    @ConstructorProperties({"blackListedGuilds", "blackListedUsers", "patreonUsers", "tempbans", "mutes"})
    @JsonCreator
    public MantaroObj(@JsonProperty("blackListedGuilds") List<String> blackListedGuilds,
                      @JsonProperty("blackListedUsers") List<String> blackListedUsers,
                      @JsonProperty("patreonUsers") List<String> patreonUsers,
                      @JsonProperty("tempBans") Map<String, Long> tempBans,
                      @JsonProperty("mutes") Map<Long, Pair<String, Long>> mutes) {
        this.blackListedGuilds = blackListedGuilds;
        this.blackListedUsers = blackListedUsers;
        this.patreonUsers = patreonUsers;
        this.tempBans = tempBans;
        this.mutes = mutes;
    }

    public static MantaroObj create() {
        return new MantaroObj(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new ConcurrentHashMap<>());
    }

    @JsonIgnore
    @Override
    @Nonnull
    public String getTableName() {
        return DB_TABLE;
    }
}
