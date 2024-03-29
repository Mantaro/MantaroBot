/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.currency.item;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.UUID;

@SuppressWarnings("unused")
public class PotionEffect {
    private String uuid;
    private int potion; //item id
    private long until;
    private ItemType.PotionType type;
    private long timesUsed;
    private long amountEquipped = 1;

    @BsonCreator
    public PotionEffect(@BsonProperty("potion") int potionId, @BsonProperty("until") long until, @BsonProperty("type") ItemType.PotionType type) {
        uuid = UUID.randomUUID().toString();
        this.potion = potionId;
        this.until = until;
        this.type = type;
    }

    @BsonIgnore
    public boolean use() {
        long newAmount = amountEquipped - 1;
        if (newAmount < 1) {
            return false;
        } else {
            setAmountEquipped(newAmount);
            setTimesUsed(0);
            return true;
        }
    }

    @BsonIgnore
    public void equip(int amount) {
        long newAmount = amountEquipped + amount;
        if (newAmount > 15) {
            setAmountEquipped(15);
        } else {
            setAmountEquipped(newAmount);
        }
    }

    @BsonIgnore
    public void equip() {
        equip(1);
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
}
