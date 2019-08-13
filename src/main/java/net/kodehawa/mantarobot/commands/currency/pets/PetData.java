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

package net.kodehawa.mantarobot.commands.currency.pets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.PotionEffect;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
public class PetData {
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
    private Map<Item, AtomicLong> collected = new HashMap<>();
    private long collectRate;
    private long lastCollectedAt; //to handle increasing

    //Hunger (every type except fire)
    private long hunger;
    private long saturation;
    private long lastFedAt;

    //Effect (for battles/collecting buffs)
    private PotionEffect potionEffect;
    private PotionEffect effectAppliedAt;

    //Current pet upgrade level
    public long upgradeLevel = 1; //The bigger this number, the easier it is to gain XP.

    public enum PetSkill {
        FISH, MINE, COLLECT, FIGHT;

        @JsonIgnore
        static Random random = new Random();

        public static PetSkill getRandom() {
            int x = random.nextInt(PetSkill.values().length);
            return PetSkill.values()[x];
        }
    }
}
