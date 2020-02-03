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
}
