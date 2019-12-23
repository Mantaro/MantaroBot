/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
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
 *
 */

package net.kodehawa.mantarobot.db.entities.helpers;

public class PremiumKeyData {
    private String linkedTo = null;
    
    public PremiumKeyData() {
    }
    
    public String getLinkedTo() {
        return this.linkedTo;
    }
    
    public void setLinkedTo(String linkedTo) {
        this.linkedTo = linkedTo;
    }
    
    protected boolean canEqual(final Object other) {
        return other instanceof PremiumKeyData;
    }
    
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $linkedTo = this.getLinkedTo();
        result = result * PRIME + ($linkedTo == null ? 43 : $linkedTo.hashCode());
        return result;
    }
    
    public boolean equals(final Object o) {
        if(o == this) return true;
        if(!(o instanceof PremiumKeyData)) return false;
        final PremiumKeyData other = (PremiumKeyData) o;
        if(!other.canEqual(this)) return false;
        final Object this$linkedTo = this.getLinkedTo();
        final Object other$linkedTo = other.getLinkedTo();
        return this$linkedTo == null ? other$linkedTo == null : this$linkedTo.equals(other$linkedTo);
    }
    
    public String toString() {
        return "PremiumKeyData(linkedTo=" + this.getLinkedTo() + ")";
    }
}
