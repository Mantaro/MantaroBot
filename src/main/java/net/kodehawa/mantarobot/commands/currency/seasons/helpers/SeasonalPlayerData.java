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

package net.kodehawa.mantarobot.commands.currency.seasons.helpers;

public class SeasonalPlayerData {
    private long gamesWon = 0;
    private long waifuCachedValue = 0;
    private long lockedUntil = 0;
    
    public SeasonalPlayerData() {
    }
    
    public long getGamesWon() {
        return this.gamesWon;
    }
    
    public void setGamesWon(long gamesWon) {
        this.gamesWon = gamesWon;
    }
    
    public long getWaifuCachedValue() {
        return this.waifuCachedValue;
    }
    
    public void setWaifuCachedValue(long waifuCachedValue) {
        this.waifuCachedValue = waifuCachedValue;
    }
    
    public long getLockedUntil() {
        return this.lockedUntil;
    }
    
    public void setLockedUntil(long lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
    
    protected boolean canEqual(final Object other) {
        return other instanceof SeasonalPlayerData;
    }
    
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $gamesWon = this.gamesWon;
        result = result * PRIME + (int) ($gamesWon >>> 32 ^ $gamesWon);
        final long $waifuCachedValue = this.waifuCachedValue;
        result = result * PRIME + (int) ($waifuCachedValue >>> 32 ^ $waifuCachedValue);
        final long $lockedUntil = this.lockedUntil;
        result = result * PRIME + (int) ($lockedUntil >>> 32 ^ $lockedUntil);
        return result;
    }
    
    public boolean equals(final Object o) {
        if(o == this) return true;
        if(!(o instanceof SeasonalPlayerData)) return false;
        final SeasonalPlayerData other = (SeasonalPlayerData) o;
        if(!other.canEqual(this)) return false;
        if(this.gamesWon != other.gamesWon) return false;
        if(this.waifuCachedValue != other.waifuCachedValue) return false;
        return this.lockedUntil == other.lockedUntil;
    }
    
    public String toString() {
        return "SeasonalPlayerData(gamesWon=" + this.gamesWon + ", waifuCachedValue=" + this.waifuCachedValue + ", lockedUntil=" + this.lockedUntil + ")";
    }
}
