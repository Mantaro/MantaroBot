/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
 */

package net.kodehawa.mantarobot.commands.currency.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.beans.ConstructorProperties;
import java.util.UUID;

@Data
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
}
