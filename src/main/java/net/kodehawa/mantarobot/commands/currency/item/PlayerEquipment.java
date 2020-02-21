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
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kodehawa.mantarobot.commands.currency.item.special.FishRod;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Breakable;

import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class PlayerEquipment {
    //int = itemId
    private Map<EquipmentType, Integer> equipment;
    private Map<EquipmentType, PotionEffect> effects;
    private Map<EquipmentType, Integer> durability;

    @JsonCreator
    @ConstructorProperties({"equipment, effects"})
    public PlayerEquipment(@JsonProperty("equipment") Map<EquipmentType, Integer> equipment, @JsonProperty("effects") Map<EquipmentType, PotionEffect> effects, @JsonProperty("durability") Map<EquipmentType, Integer> durability) {
        this.equipment = equipment;
        this.effects = effects;
        this.durability = durability == null ? new HashMap<>() : durability; //Workaround because some people will not have this property.
    }

    @JsonIgnore
    public boolean equipItem(Item item) {
        EquipmentType type = getTypeFor(item);
        if (type == null || type.getType() != 0)
            return false;

        equipment.put(type, Items.idOf(item));
        if (item instanceof Breakable) //should always be?
            durability.put(type, ((Breakable) item).getMaxDurability());
        return true;
    }

    @JsonIgnore
    public void applyEffect(PotionEffect effect) {
        EquipmentType type = getTypeFor(Items.fromId(effect.getPotion()));
        if (type == null || type.getType() != 1)
            return;

        effects.put(type, effect);
    }

    //Convenience methods start here.
    @JsonIgnore
    public void resetOfType(EquipmentType type) {
        equipment.remove(type);
        durability.remove(type);
    }

    @JsonIgnore
    public void resetEffect(EquipmentType type) {
        effects.remove(type);
    }

    @JsonIgnore
    public void incrementEffectUses(EquipmentType type) {
        effects.computeIfPresent(type, (i, effect) -> {
            effect.setTimesUsed(effect.getTimesUsed() + 1);

            return effect;
        });
    }

    @JsonIgnore
    public boolean isEffectActive(EquipmentType type, int maxUses) {
        PotionEffect effect = effects.get(type);
        if (effect == null) {
            return false;
        }

        return effect.getTimesUsed() < maxUses;
    }

    @JsonIgnore
    public PotionEffect getCurrentEffect(EquipmentType type) {
        return effects.get(type);
    }

    @JsonIgnore
    public Item getEffectItem(EquipmentType type) {
        PotionEffect effect = effects.get(type);
        return effect == null ? null : Items.fromId(effect.getPotion());
    }

    @JsonIgnore
    public Integer of(EquipmentType type) {
        Integer id = equipment.get(type);
        return id == null ? 0 : id;
    }

    @JsonIgnore
    public EquipmentType getTypeFor(Item item) {
        for (EquipmentType type : EquipmentType.values()) {
            if (type.getPredicate().test(item)) {
                return type;
            }
        }

        return null;
    }

    @JsonIgnore
    public int reduceDurability(EquipmentType type, int amount) {
        return durability.computeIfPresent(type, (t, a) -> a - amount);
    }

    public Map<EquipmentType, Integer> getEquipment() {
        return this.equipment;
    }

    public Map<EquipmentType, PotionEffect> getEffects() {
        return this.effects;
    }

    public Map<EquipmentType, Integer> getDurability() {
        return durability;
    }

    public enum EquipmentType {
        ROD(FishRod.class::isInstance, 0),
        PICK(item -> item.getItemType() == ItemType.MINE_PICK || item.getItemType() == ItemType.MINE_RARE_PICK, 0),
        POTION(item -> item.getItemType() == ItemType.POTION, 1),
        BUFF(item -> item.getItemType() == ItemType.BUFF, 1);

        private Predicate<Item> predicate;
        private int type;

        EquipmentType(Predicate<Item> predicate, int type) {
            this.predicate = predicate;
            this.type = type;
        }

        public static EquipmentType fromString(String text) {
            for (EquipmentType b : EquipmentType.values()) {
                if (b.name().equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }

        public Predicate<Item> getPredicate() {
            return this.predicate;
        }

        public int getType() {
            return this.type;
        }
    }
}
