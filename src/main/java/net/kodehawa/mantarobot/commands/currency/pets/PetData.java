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

package net.kodehawa.mantarobot.commands.currency.pets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.kodehawa.mantarobot.commands.currency.item.PlayerEquipment;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PetData {
    //Current pet upgrade level
    public Level upgradeLevel = Level.BASIC; //The bigger this number, the easier it is to gain XP.
    //lol
    private String test;
    private String id; //Why isn't this on the main class?
    private long xp; //Increased through collecting, training and battles.
    private long level; //Same as above.
    private double affection; //Increases randomly with actions that involve "loving" or taking care of your pet.
    private long affectionLevel;
    private long timesPetted;
    private long timesCollected;
    //How many battles
    private long battles;
    //To calculate win/lose ratio
    private long battlesWon;
    private long battlesLost;
    private long battlesDraw;
    //Skills learned -> Skill XP
    private Map<PetSkill, AtomicLong> petSkills = new HashMap<>();
    //Hydration (water type)
    private long hydrationLevel;
    private long lastHydratedAt; //to handle decreasing
    //Collect stats
    private Map<Long, AtomicLong> collected = new HashMap<>();
    private long collectRate;
    private long lastCollectedAt; //to handle increasing
    //Hunger (every type except fire)
    private long hunger;
    private float saturation;
    private long lastFedAt;
    //not so much of "player" anymore
    private PlayerEquipment equippedItems = new PlayerEquipment(new HashMap<>(), new HashMap<>(), new HashMap<>()); //hashmap is type -> itemId

    @JsonIgnore
    public long getCurrentHydration() {
        long hoursSince = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - lastHydratedAt);
        if (hoursSince > 3) {
            hydrationLevel = Math.min(10, hydrationLevel * hoursSince / 2);
        }
        return hydrationLevel;
    }

    @JsonIgnore
    public long increaseHydration() {
        hydrationLevel += 10;

        if (hydrationLevel > 100) {
            hydrationLevel = 100;
            return 100;
        }

        return hydrationLevel;
    }

    @JsonIgnore
    public long increaseHunger(long by) {
        hunger += by;
        if (hunger > 100) {
            hunger = 100;
            return 100;
        }

        return hunger;
    }

    @JsonIgnore
    //The calculations used to decrease hunger use a negative factor on saturation: the lower, the better.
    public float increaseSaturation(float by) {
        saturation -= by;

        if (saturation < 1) {
            saturation = 1;
            return 1;
        }

        return saturation;
    }

    @JsonIgnore
    public float updateSaturation() {
        long hoursSince = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - lastFedAt);
        if (hoursSince > 5) {
            saturation = Math.min(10, saturation * hoursSince / 3);
        }
        return saturation;
    }

    @JsonIgnore
    public long checkCurrentHunger() {
        //very scientific formula
        if (saturation < 3 && TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - lastFedAt) < 10) {
            return hunger;
        }

        long reduction = (long) (Math.max(1, TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() / lastFedAt)) / -saturation);

        return hunger - reduction;
    }

    public String getTest() {
        return this.test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getXp() {
        return this.xp;
    }

    public void setXp(long xp) {
        this.xp = xp;
    }

    public long getLevel() {
        return this.level;
    }

    public void setLevel(long level) {
        this.level = level;
    }

    public double getAffection() {
        return this.affection;
    }

    public void setAffection(double affection) {
        this.affection = affection;
    }

    public long getAffectionLevel() {
        return this.affectionLevel;
    }

    public void setAffectionLevel(long affectionLevel) {
        this.affectionLevel = affectionLevel;
    }

    public long getTimesPetted() {
        return this.timesPetted;
    }

    public void setTimesPetted(long timesPetted) {
        this.timesPetted = timesPetted;
    }

    public long getTimesCollected() {
        return this.timesCollected;
    }

    public void setTimesCollected(long timesCollected) {
        this.timesCollected = timesCollected;
    }

    public long getBattles() {
        return this.battles;
    }

    public void setBattles(long battles) {
        this.battles = battles;
    }

    public long getBattlesWon() {
        return this.battlesWon;
    }

    public void setBattlesWon(long battlesWon) {
        this.battlesWon = battlesWon;
    }

    public long getBattlesLost() {
        return this.battlesLost;
    }

    public void setBattlesLost(long battlesLost) {
        this.battlesLost = battlesLost;
    }

    public long getBattlesDraw() {
        return this.battlesDraw;
    }

    public void setBattlesDraw(long battlesDraw) {
        this.battlesDraw = battlesDraw;
    }

    public Map<PetSkill, AtomicLong> getPetSkills() {
        return this.petSkills;
    }

    public void setPetSkills(Map<PetSkill, AtomicLong> petSkills) {
        this.petSkills = petSkills;
    }

    public long getHydrationLevel() {
        return this.hydrationLevel;
    }

    public void setHydrationLevel(long hydrationLevel) {
        this.hydrationLevel = hydrationLevel;
    }

    public long getLastHydratedAt() {
        return this.lastHydratedAt;
    }

    public void setLastHydratedAt(long lastHydratedAt) {
        this.lastHydratedAt = lastHydratedAt;
    }

    public Map<Long, AtomicLong> getCollected() {
        return this.collected;
    }

    public void setCollected(Map<Long, AtomicLong> collected) {
        this.collected = collected;
    }

    public long getCollectRate() {
        return this.collectRate;
    }

    public void setCollectRate(long collectRate) {
        this.collectRate = collectRate;
    }

    public long getLastCollectedAt() {
        return this.lastCollectedAt;
    }

    public void setLastCollectedAt(long lastCollectedAt) {
        this.lastCollectedAt = lastCollectedAt;
    }

    public long getHunger() {
        return this.hunger;
    }

    public void setHunger(long hunger) {
        this.hunger = hunger;
    }

    public float getSaturation() {
        return this.saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public long getLastFedAt() {
        return this.lastFedAt;
    }

    public void setLastFedAt(long lastFedAt) {
        this.lastFedAt = lastFedAt;
    }

    public PlayerEquipment getEquippedItems() {
        return this.equippedItems;
    }

    public void setEquippedItems(PlayerEquipment equippedItems) {
        this.equippedItems = equippedItems;
    }

    public Level getUpgradeLevel() {
        return this.upgradeLevel;
    }

    public void setUpgradeLevel(Level upgradeLevel) {
        this.upgradeLevel = upgradeLevel;
    }

    public enum PetSkill {
        FISH, MINE, COLLECT, FIGHT;

        @JsonIgnore
        static Random random = new Random();

        public static PetSkill getRandom() {
            int x = random.nextInt(PetSkill.values().length);
            return PetSkill.values()[x];
        }
    }

    public enum Level {
        BASIC("", 0), NORMAL("", 5), ADVANCED("", 20), LEGENDARY("", 50);

        String recipe;
        long levelRequired;

        Level(String recipe, long level) {
            this.recipe = recipe;
            this.levelRequired = level;
        }
    }
}
