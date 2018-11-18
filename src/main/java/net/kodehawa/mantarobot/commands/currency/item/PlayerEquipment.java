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
import lombok.Getter;
import net.kodehawa.mantarobot.commands.currency.item.special.FishRod;

import java.beans.ConstructorProperties;
import java.util.Map;
import java.util.function.Predicate;

public class PlayerEquipment {
    @Getter
    //int = itemId
    private Map<EquipmentType, Integer> equipment;

    @JsonCreator
    @ConstructorProperties({"equipment"})
    public PlayerEquipment(Map<EquipmentType, Integer> equipment) {
        this.equipment = equipment;
    }

    @JsonIgnore
    public boolean equipItem(Item item) {
        for(EquipmentType type : EquipmentType.values()) {
            if(type.getPredicate().test(item)) {
                equipment.put(type, Items.idOf(item));
                return true;
            }
        }

        return false;
    }

    @JsonIgnore
    public void resetOfType(EquipmentType type) {
        equipment.put(type, 0);
    }

    @JsonIgnore
    public Integer of(EquipmentType type) {
        Integer id = equipment.get(type);
        return id == null ? 0 : id;
    }

    public enum EquipmentType {
        ROD(item -> item instanceof FishRod), PICK(item -> item.getItemType() == ItemType.CAST_MINE || item.getItemType() == ItemType.MINE_RARE_PICK),
        POTION(item -> item.getItemType() == ItemType.POTION), BUFF(item -> item.getItemType() == ItemType.BUFF);

        @Getter
        private Predicate<Item> predicate;

        EquipmentType(Predicate<Item> predicate) {
            this.predicate = predicate;
        }
    }
}
