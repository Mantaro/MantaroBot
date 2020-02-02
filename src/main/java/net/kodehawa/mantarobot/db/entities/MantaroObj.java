/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.utils.Pair;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    
    protected boolean canEqual(final Object other) {
        return other instanceof MantaroObj;
    }
    
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $blackListedGuilds = this.getBlackListedGuilds();
        result = result * PRIME + ($blackListedGuilds == null ? 43 : $blackListedGuilds.hashCode());
        final Object $blackListedUsers = this.getBlackListedUsers();
        result = result * PRIME + ($blackListedUsers == null ? 43 : $blackListedUsers.hashCode());
        final Object $patreonUsers = this.getPatreonUsers();
        result = result * PRIME + ($patreonUsers == null ? 43 : $patreonUsers.hashCode());
        final Object $mutes = this.getMutes();
        result = result * PRIME + ($mutes == null ? 43 : $mutes.hashCode());
        final Object $tempBans = this.getTempBans();
        result = result * PRIME + ($tempBans == null ? 43 : $tempBans.hashCode());
        return result;
    }
    
    public boolean equals(final Object o) {
        if(o == this) return true;
        if(!(o instanceof MantaroObj)) return false;
        final MantaroObj other = (MantaroObj) o;
        if(!other.canEqual(this)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if(!Objects.equals(this$id, other$id)) return false;
        final Object this$blackListedGuilds = this.getBlackListedGuilds();
        final Object other$blackListedGuilds = other.getBlackListedGuilds();
        if(!Objects.equals(this$blackListedGuilds, other$blackListedGuilds))
            return false;
        final Object this$blackListedUsers = this.getBlackListedUsers();
        final Object other$blackListedUsers = other.getBlackListedUsers();
        if(!Objects.equals(this$blackListedUsers, other$blackListedUsers))
            return false;
        final Object this$patreonUsers = this.getPatreonUsers();
        final Object other$patreonUsers = other.getPatreonUsers();
        if(!Objects.equals(this$patreonUsers, other$patreonUsers))
            return false;
        final Object this$mutes = this.getMutes();
        final Object other$mutes = other.getMutes();
        if(!Objects.equals(this$mutes, other$mutes)) return false;
        final Object this$tempBans = this.getTempBans();
        final Object other$tempBans = other.getTempBans();
        return Objects.equals(this$tempBans, other$tempBans);
    }
    
    public String toString() {
        return "MantaroObj(id=" + this.getId() + ", blackListedGuilds=" + this.getBlackListedGuilds() + ", blackListedUsers=" + this.getBlackListedUsers() + ", patreonUsers=" + this.getPatreonUsers() + ", mutes=" + this.getMutes() + ", tempBans=" + this.getTempBans() + ")";
    }
}
