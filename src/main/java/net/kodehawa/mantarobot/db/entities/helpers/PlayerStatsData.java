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

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PlayerStatsData {
    private long looted;
    private long mined;
    private long gambleLose;
    private long slotsLose;
    
    public PlayerStatsData() {
    }
    
    @JsonIgnore
    public void incrementGambleLose() {
        gambleLose += 1;
    }
    
    @JsonIgnore
    public void incrementSlotsLose() {
        slotsLose += 1;
    }
    
    public long getLooted() {
        return this.looted;
    }
    
    public void setLooted(long looted) {
        this.looted = looted;
    }
    
    public long getMined() {
        return this.mined;
    }
    
    public void setMined(long mined) {
        this.mined = mined;
    }
    
    public long getGambleLose() {
        return this.gambleLose;
    }
    
    public void setGambleLose(long gambleLose) {
        this.gambleLose = gambleLose;
    }
    
    public long getSlotsLose() {
        return this.slotsLose;
    }
    
    public void setSlotsLose(long slotsLose) {
        this.slotsLose = slotsLose;
    }
}
