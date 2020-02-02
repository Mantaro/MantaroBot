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

package net.kodehawa.mantarobot.db.entities.helpers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class LocalExperienceData {
    
    private String userId;
    private long experience;
    private long level;
    @JsonCreator
    public LocalExperienceData(@JsonProperty("userId") String userId, @JsonProperty("experience") long experience, @JsonProperty("level") long level) {
        this.userId = userId;
        this.experience = experience;
        this.level = level;
    }
    @JsonIgnore
    public LocalExperienceData(String userId) {
        this.userId = userId;
    }
    
    public LocalExperienceData() {
    }
    
    public String getUserId() {
        return this.userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public long getExperience() {
        return this.experience;
    }
    
    public void setExperience(long experience) {
        this.experience = experience;
    }
    
    public long getLevel() {
        return this.level;
    }
    
    public void setLevel(long level) {
        this.level = level;
    }
    
    protected boolean canEqual(final Object other) {
        return other instanceof LocalExperienceData;
    }
    
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final long $experience = this.getExperience();
        result = result * PRIME + (int) ($experience >>> 32 ^ $experience);
        final long $level = this.getLevel();
        result = result * PRIME + (int) ($level >>> 32 ^ $level);
        return result;
    }
    
    public boolean equals(final Object o) {
        if(o == this) return true;
        if(!(o instanceof LocalExperienceData)) return false;
        final LocalExperienceData other = (LocalExperienceData) o;
        if(!other.canEqual(this)) return false;
        final Object this$userId = this.getUserId();
        final Object other$userId = other.getUserId();
        if(!Objects.equals(this$userId, other$userId)) return false;
        if(this.getExperience() != other.getExperience()) return false;
        return this.getLevel() == other.getLevel();
    }
    
    public String toString() {
        return "LocalExperienceData(userId=" + this.getUserId() + ", experience=" + this.getExperience() + ", level=" + this.getLevel() + ")";
    }
}
