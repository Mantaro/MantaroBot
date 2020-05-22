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
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.utils.Pair;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MantaroObj implements ManagedObject {
    public static final String DB_TABLE = "mantaro";
    public final String id = "mantaro";
    public List<String> blackListedGuilds;
    public List<String> blackListedUsers;
    public List<String> patreonUsers;
    private Map<Long, Pair<String, Long>> mutes;
    private Map<String, Long> tempBans;

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

    public MantaroObj() {
    }

    public static MantaroObj create() {
        return new MantaroObj(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new ConcurrentHashMap<>());
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

    public List<String> getBlackListedGuilds() {
        return this.blackListedGuilds;
    }

    public void setBlackListedGuilds(List<String> blackListedGuilds) {
        this.blackListedGuilds = blackListedGuilds;
    }

    public List<String> getBlackListedUsers() {
        return this.blackListedUsers;
    }

    public void setBlackListedUsers(List<String> blackListedUsers) {
        this.blackListedUsers = blackListedUsers;
    }

    public List<String> getPatreonUsers() {
        return this.patreonUsers;
    }

    public void setPatreonUsers(List<String> patreonUsers) {
        this.patreonUsers = patreonUsers;
    }

    public Map<Long, Pair<String, Long>> getMutes() {
        return this.mutes;
    }

    public void setMutes(Map<Long, Pair<String, Long>> mutes) {
        this.mutes = mutes;
    }

    public Map<String, Long> getTempBans() {
        return this.tempBans;
    }

    public void setTempBans(Map<String, Long> tempBans) {
        this.tempBans = tempBans;
    }
}
