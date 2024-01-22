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

import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class PlayerEquipment {
    //int = itemId
    private final Map<EquipmentType, Integer> equipment;
    private final Map<PotionEffectType, PotionEffect> effectList;
    private final Map<EquipmentType, Integer> durability;
    @BsonIgnore
    public Map<String, Object> fieldTracker = new HashMap<>();

    @SuppressWarnings("unused")
    @BsonCreator
    public PlayerEquipment(@BsonProperty("equipment") Map<EquipmentType, Integer> equipment, @BsonProperty("effects") Map<EquipmentType, PotionEffect> effects, @BsonProperty("durability") Map<EquipmentType, Integer> durability, @BsonProperty("effectList") List<PotionEffect> effectList) {
        this.equipment = equipment == null ? new HashMap<>() : equipment;
        this.effectList = new HashMap<>();
        List<PotionEffect> list = new ArrayList<>(); // ensure mutability
        if (effectList != null) {
            list.addAll(effectList);
        }
        if (effects != null) { // legacy
            list.addAll(effects.values());
        }
        for (var value : list) {
            var item = ItemHelper.fromId(value.getPotion());
            if (!(item instanceof Potion potion)) continue;
            this.effectList.put(potion.getEffectType(), value);
        }
        this.durability = durability == null ? new HashMap<>() : durability;
    }

    public Collection<PotionEffect> getEffectList() {
        // we only save this as the list as we can create keys from the list itself
        // collections are treated as lists by mongo (surprisingly)
        return effectList.values();
    }

    public List<PotionEffect> getEffectListSorted() {
        return effectList.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(a -> a.getKey().ordinal()))
                .map(Map.Entry::getValue)
                .toList();
    }

    public Collection<PotionEffectType> getEffectTypes() {
        return effectList.keySet();
    }

    public Map<EquipmentType, Integer> getEquipment() {
        return this.equipment;
    }

    @SuppressWarnings("unused")
    public Map<EquipmentType, PotionEffect> getEffects() {
        return null; // legacy, pojo needs this
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
            fieldTracker.put("equippedItems.durability", durability);
        }

        fieldTracker.put("equippedItems.equipment", equipment);
        return true;
    }

    @BsonIgnore
    public void applyEffect(PotionEffect effect) {
        var item = ItemHelper.fromId(effect.getPotion());
         if (item == null || !item.getItemType().isPotion() || !(item instanceof Potion potion)) {
             return;
         }

        effectList.put(potion.getEffectType(), effect);
        fieldTracker.put("equippedItems.effectList", getEffectList());
        fieldTracker.put("equippedItems.effects", null); // ensures we dont duplicate buffs
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
    public void resetEffect(PotionEffectType effectType) {
        effectList.keySet().removeIf(e -> e == effectType);
        fieldTracker.put("equippedItems.effectList", getEffectList());
        fieldTracker.put("equippedItems.effects", null); // ensures we dont duplicate buffs
    }

    @BsonIgnore
    public void incrementEffectUses(PotionEffectType type) {
        effectList.computeIfPresent(type, (i, effect) -> {
            effect.setTimesUsed(effect.getTimesUsed() + 1);

            return effect;
        });

        fieldTracker.put("equippedItems.effectList", getEffectList());
        fieldTracker.put("equippedItems.effects", null); // ensures we dont duplicate buffs
    }

    @BsonIgnore
    public boolean useEffect(PotionEffectType type) {
        var use = getCurrentEffect(type).use();
        fieldTracker.put("equippedItems.effectList", getEffectList());
        fieldTracker.put("equippedItems.effects", null); // ensures we dont duplicate buffs
        return use;
    }

    @BsonIgnore
    public void equipEffect(PotionEffectType type, int amount) {
        getCurrentEffect(type).equip(amount);
        fieldTracker.put("equippedItems.effectList", getEffectList());
        fieldTracker.put("equippedItems.effects", null); // ensures we dont duplicate buffs
    }

    @BsonIgnore
    public boolean isEffectActive(PotionEffectType type, int maxUses) {
        PotionEffect effect = effectList.get(type);
        if (effect == null) {
            return false;
        }

        return effect.getTimesUsed() < maxUses;
    }

    @BsonIgnore
    public PotionEffect getCurrentEffect(PotionEffectType type) {
        return effectList.get(type);
    }

    @BsonIgnore
    public Item getEffectItem(PotionEffectType type) {
        PotionEffect effect = effectList.get(type);
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
        ROD(FishRod.class::isInstance),
        PICK(Pickaxe.class::isInstance),
        AXE(Axe.class::isInstance),
        WRENCH(Wrench.class::isInstance),
        POTION(item -> item.getItemType() == ItemType.POTION || item.getItemType() == ItemType.POTION_CASTABLE), // legacy
        BUFF(item -> item.getItemType() == ItemType.BUFF); // legacy

        private final Predicate<Item> predicate;

        EquipmentType(Predicate<Item> predicate) {
            this.predicate = predicate;
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
            // 1 is legacy type
            return this == BUFF || this == POTION ? 1 : 0;
        }

        @Override
        public String toString() {
            return this.name();
        }
    }
}
