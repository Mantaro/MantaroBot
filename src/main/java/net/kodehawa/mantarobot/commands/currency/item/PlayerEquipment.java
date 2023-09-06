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

import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Breakable;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Axe;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.FishRod;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Pickaxe;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Wrench;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.MongoUser;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class PlayerEquipment {
    //int = itemId
    private final Map<EquipmentType, Integer> equipment;
    private final Map<EquipmentType, PotionEffect> effects;
    private final Map<EquipmentType, Integer> durability;
    @BsonIgnore
    public Map<String, Object> fieldTracker = new HashMap<>();

    @BsonCreator
    public PlayerEquipment(@BsonProperty("equipment") Map<EquipmentType, Integer> equipment, @BsonProperty("effects") Map<EquipmentType, PotionEffect> effects, @BsonProperty("durability") Map<EquipmentType, Integer> durability) {
        this.equipment = equipment == null ? new HashMap<>() : equipment;
        this.effects = effects == null ? new HashMap<>() : effects;
        this.durability = durability == null ? new HashMap<>() : durability;
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

    @BsonIgnore
    public boolean equipItem(Item item) {
        EquipmentType type = getTypeFor(item);
        if (type == null || type.getType() != 0) {
            return false;
        }

        equipment.put(type, ItemHelper.idOf(item));
        if (item instanceof Breakable it) {
            durability.put(type, it.getMaxDurability());
        }

        fieldTracker.put("equippedItems.durability", durability);
        fieldTracker.put("equippedItems.equipment", equipment);
        return true;
    }

    @BsonIgnore
    public void applyEffect(PotionEffect effect) {
        EquipmentType type = getTypeFor(ItemHelper.fromId(effect.getPotion()));
        if (type == null || type.getType() != 1) {
            return;
        }

        effects.put(type, effect);
        fieldTracker.put("equippedItems.effects", effects);
    }

    //Convenience methods start here.
    @BsonIgnore
    public void resetOfType(EquipmentType type) {
        equipment.remove(type);
        durability.remove(type);
        fieldTracker.put("equippedItems.durability", durability);
        fieldTracker.put("equippedItems.equipment", equipment);
    }

    @BsonIgnore
    public void resetEffect(EquipmentType type) {
        effects.remove(type);
    }

    @BsonIgnore
    public void incrementEffectUses(EquipmentType type) {
        effects.computeIfPresent(type, (i, effect) -> {
            effect.setTimesUsed(effect.getTimesUsed() + 1);

            return effect;
        });

        fieldTracker.put("equippedItems.effects", effects);

    }

    @BsonIgnore
    public boolean useEffect(EquipmentType type) {
        var use = getCurrentEffect(type).use();
        fieldTracker.put("equippedItems.effects", effects);
        return use;
    }

    @BsonIgnore
    public void equipEffect(EquipmentType type, int amount) {
        getCurrentEffect(type).equip(amount);
        fieldTracker.put("equippedItems.effects", effects);
    }

    @BsonIgnore
    public boolean isEffectActive(EquipmentType type, int maxUses) {
        PotionEffect effect = effects.get(type);
        if (effect == null) {
            return false;
        }

        return effect.getTimesUsed() < maxUses;
    }

    @BsonIgnore
    public PotionEffect getCurrentEffect(EquipmentType type) {
        return effects.get(type);
    }

    @BsonIgnore
    public Item getEffectItem(EquipmentType type) {
        PotionEffect effect = effects.get(type);
        return effect == null ? null : ItemHelper.fromId(effect.getPotion());
    }

    @BsonIgnore
    public Integer of(EquipmentType type) {
        Integer id = equipment.get(type);
        return id == null ? 0 : id;
    }

    @BsonIgnore
    public EquipmentType getTypeFor(Item item) {
        for (EquipmentType type : EquipmentType.values()) {
            if (type.getPredicate().test(item)) {
                return type;
            }
        }

        return null;
    }

    @BsonIgnore
    public void resetDurabilityTo(EquipmentType type, int amount) {
        durability.put(type, amount);
        fieldTracker.put("equippedItems.durability", durability);
    }

    @BsonIgnore
    public int reduceDurability(EquipmentType type, int amount) {
        //noinspection DataFlowIssue
        int dur = durability.computeIfPresent(type, (t, a) -> a - amount);
        fieldTracker.put("equippedItems.durability", durability);

        return dur;
    }

    @BsonIgnore
    public void updateAllChanged(MongoUser database) {
        MantaroData.db().updateFieldValues(database, fieldTracker);
    }

    public enum EquipmentType {
        ROD(FishRod.class::isInstance, 0),
        PICK(Pickaxe.class::isInstance, 0),
        AXE(Axe.class::isInstance, 0),
        WRENCH(Wrench.class::isInstance, 0),
        POTION(item -> item.getItemType() == ItemType.POTION, 1),
        BUFF(item -> item.getItemType() == ItemType.BUFF, 1);

        private final Predicate<Item> predicate;
        private final int type;

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

        @Override
        public String toString() {
            return this.name();
        }
    }
}
