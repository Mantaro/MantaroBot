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
    
    protected boolean canEqual(final Object other) {
        return other instanceof PlayerStatsData;
    }
    
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $looted = this.getLooted();
        result = result * PRIME + (int) ($looted >>> 32 ^ $looted);
        final long $mined = this.getMined();
        result = result * PRIME + (int) ($mined >>> 32 ^ $mined);
        final long $gambleLose = this.getGambleLose();
        result = result * PRIME + (int) ($gambleLose >>> 32 ^ $gambleLose);
        final long $slotsLose = this.getSlotsLose();
        result = result * PRIME + (int) ($slotsLose >>> 32 ^ $slotsLose);
        return result;
    }
    
    public boolean equals(final Object o) {
        if(o == this) return true;
        if(!(o instanceof PlayerStatsData)) return false;
        final PlayerStatsData other = (PlayerStatsData) o;
        if(!other.canEqual(this)) return false;
        if(this.getLooted() != other.getLooted()) return false;
        if(this.getMined() != other.getMined()) return false;
        if(this.getGambleLose() != other.getGambleLose()) return false;
        return this.getSlotsLose() == other.getSlotsLose();
    }
    
    public String toString() {
        return "PlayerStatsData(looted=" + this.getLooted() + ", mined=" + this.getMined() + ", gambleLose=" + this.getGambleLose() + ", slotsLose=" + this.getSlotsLose() + ")";
    }
}
