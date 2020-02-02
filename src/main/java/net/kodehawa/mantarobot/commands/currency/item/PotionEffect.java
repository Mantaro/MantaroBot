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

package net.kodehawa.mantarobot.commands.currency.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.beans.ConstructorProperties;
import java.util.Objects;
import java.util.UUID;

public class PotionEffect {
    private String uuid;
    private int potion; //item id
    private long until;
    private ItemType.PotionType type;
    private long timesUsed;
    private long amountEquipped = 1;
    
    @JsonCreator
    @ConstructorProperties({"potionId", "until", "type"})
    public PotionEffect(int potionId, long until, ItemType.PotionType type) {
        uuid = UUID.randomUUID().toString();
        this.potion = potionId;
        this.until = until;
        this.type = type;
    }
    
    public PotionEffect() {
    }
    
    @JsonIgnore
    public boolean use() {
        long newAmount = amountEquipped - 1;
        if(newAmount < 1) {
            return false;
        } else {
            setAmountEquipped(newAmount);
            setTimesUsed(0);
            return true;
        }
    }
    
    @JsonIgnore
    public boolean equip(int amount) {
        long newAmount = amountEquipped + amount;
        if(newAmount >= 10) {
            setAmountEquipped(9);
        } else {
            setAmountEquipped(newAmount);
        }
        
        return true;
    }
    
    @JsonIgnore
    public boolean equip() {
        return equip(1);
    }
    
    public String getUuid() {
        return this.uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public int getPotion() {
        return this.potion;
    }
    
    public void setPotion(int potion) {
        this.potion = potion;
    }
    
    public long getUntil() {
        return this.until;
    }
    
    public void setUntil(long until) {
        this.until = until;
    }
    
    public ItemType.PotionType getType() {
        return this.type;
    }
    
    public void setType(ItemType.PotionType type) {
        this.type = type;
    }
    
    public long getTimesUsed() {
        return this.timesUsed;
    }
    
    public void setTimesUsed(long timesUsed) {
        this.timesUsed = timesUsed;
    }
    
    public long getAmountEquipped() {
        return this.amountEquipped;
    }
    
    public void setAmountEquipped(long amountEquipped) {
        this.amountEquipped = amountEquipped;
    }
    
    protected boolean canEqual(final Object other) {
        return other instanceof PotionEffect;
    }
    
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $uuid = this.uuid;
        result = result * PRIME + ($uuid == null ? 43 : $uuid.hashCode());
        result = result * PRIME + this.potion;
        final long $until = this.until;
        result = result * PRIME + (int) ($until >>> 32 ^ $until);
        final Object $type = this.type;
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final long $timesUsed = this.timesUsed;
        result = result * PRIME + (int) ($timesUsed >>> 32 ^ $timesUsed);
        final long $amountEquipped = this.amountEquipped;
        result = result * PRIME + (int) ($amountEquipped >>> 32 ^ $amountEquipped);
        return result;
    }
    
    public boolean equals(final Object o) {
        if(o == this) return true;
        if(!(o instanceof PotionEffect)) return false;
        final PotionEffect other = (PotionEffect) o;
        if(!other.canEqual(this)) return false;
        final Object this$uuid = this.uuid;
        final Object other$uuid = other.uuid;
        if(!Objects.equals(this$uuid, other$uuid)) return false;
        if(this.potion != other.potion) return false;
        if(this.until != other.until) return false;
        final Object this$type = this.type;
        final Object other$type = other.type;
        if(!Objects.equals(this$type, other$type)) return false;
        if(this.timesUsed != other.timesUsed) return false;
        return this.amountEquipped == other.amountEquipped;
    }
    
    public String toString() {
        return "PotionEffect(uuid=" + this.uuid + ", potion=" + this.potion + ", until=" + this.until + ", type=" + this.type + ", timesUsed=" + this.timesUsed + ", amountEquipped=" + this.amountEquipped + ")";
    }
}
