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

import java.util.Objects;

public class CustomCommandData {
    private String owner = "";
    private boolean nsfw = false;
    
    public CustomCommandData() {
    }
    
    public String getOwner() {
        return this.owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    public boolean isNsfw() {
        return this.nsfw;
    }
    
    public void setNsfw(boolean nsfw) {
        this.nsfw = nsfw;
    }
    
    protected boolean canEqual(final Object other) {
        return other instanceof CustomCommandData;
    }
    
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $owner = this.getOwner();
        result = result * PRIME + ($owner == null ? 43 : $owner.hashCode());
        result = result * PRIME + (this.isNsfw() ? 79 : 97);
        return result;
    }
    
    public boolean equals(final Object o) {
        if(o == this) return true;
        if(!(o instanceof CustomCommandData)) return false;
        final CustomCommandData other = (CustomCommandData) o;
        if(!other.canEqual(this)) return false;
        final Object this$owner = this.getOwner();
        final Object other$owner = other.getOwner();
        if(!Objects.equals(this$owner, other$owner)) return false;
        return this.isNsfw() == other.isNsfw();
    }
    
    public String toString() {
        return "CustomCommandData(owner=" + this.getOwner() + ", nsfw=" + this.isNsfw() + ")";
    }
}
