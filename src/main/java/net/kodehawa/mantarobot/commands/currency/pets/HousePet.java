/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.currency.pets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;

import java.beans.ConstructorProperties;
import java.security.SecureRandom;

public class HousePet {
    @JsonIgnore
    private static final SecureRandom random = new SecureRandom();

    private String name;
    private HousePetType type;
    private int stamina = 100;
    private int health = 100;
    private int hunger = 100;
    private int thirst = 100;
    private int patCounter;
    private long experience;
    private long level = 1;

    @JsonCreator
    @ConstructorProperties({"name", "type"})
    public HousePet(String name, HousePetType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HousePetType getType() {
        return type;
    }

    public void setType(HousePetType type) {
        this.type = type;
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public void decreaseHealth() {
        var defaultDecrease = 2;
        if (health < 1) {
            return;
        }

        this.health = Math.max(1, health - defaultDecrease);
    }

    public void decreaseStamina() {
        var defaultDecrease = 10;
        if (stamina < 1) {
            return;
        }

        this.stamina = Math.max(1, stamina - defaultDecrease);
    }

    public void decreaseHunger() {
        var defaultDecrease = 10;
        if (hunger < 1) {
            return;
        }

        this. hunger = Math.max(1, hunger - defaultDecrease);
    }

    public void decreaseThirst() {
        var defaultDecrease = 15;
        if (thirst < 1) {
            return;
        }

        this.thirst = Math.max(1, thirst - defaultDecrease);
    }

    public void increaseHealth() {
        if (health >= 100) {
            this.health = 100;
            return;
        }

        var defaultIncrease = 10;
        this.health = Math.min(100, health + defaultIncrease);
    }

    public void increaseStamina() {
        if (stamina >= 100) {
            this.stamina = 100;
            return;
        }

        var defaultIncrease = 30;
        this.stamina = Math.min(100, stamina + defaultIncrease);
    }

    public void increaseHunger(int by) {
        if (hunger >= 100) {
            this.hunger = 100;
            return;
        }

        this.hunger = Math.min(100, hunger + by);
    }

    public void increaseThirst(int by) {
        if (thirst >= 100) {
            this.thirst = 100;
            return;
        }

        this.thirst = Math.min(100, thirst + by);
    }

    public int getHunger() {
        return hunger;
    }

    public void setHunger(int hunger) {
        this.hunger = hunger;
    }

    public int getThirst() {
        return thirst;
    }

    public void setThirst(int thirst) {
        this.thirst = thirst;
    }

    public int getPatCounter() {
        return patCounter;
    }

    public void increasePats() {
        this.patCounter += 1;
    }

    public long getExperience() {
        return experience;
    }

    public void setExperience(long experience) {
        this.experience = experience;
    }

    public long getLevel() {
        return level;
    }

    public void setLevel(long level) {
        this.level = level;
    }

    @JsonIgnore
    public double experienceToNextLevel() {
        return (getLevel() * Math.log10(getLevel()) * 1000) + (50 * getLevel() / 2D);
    }

    @JsonIgnore
    public void increaseExperience() {
        this.experience += Math.max(10, random.nextInt(50));
        var toNextLevel = experienceToNextLevel();
        if (experience > toNextLevel)
            level += 1;
    }

    @JsonIgnore
    public ActivityResult handleAbility(HousePetType.HousePetAbility neededAbility) {
        if (!type.getAbilities().contains(neededAbility))
            return ActivityResult.NO_ABILITY;

        if (getStamina() < 40)
            return ActivityResult.LOW_STAMINA;

        if (getHealth() < 30)
            return ActivityResult.LOW_HEALTH;

        if (getHunger() < 10)
            return ActivityResult.LOW_HUNGER;

        if (getThirst() < 20)
            return ActivityResult.LOW_THIRST;

        decreaseStamina();
        decreaseHealth();
        decreaseHunger();
        decreaseThirst();
        increaseExperience();

        return neededAbility.getPassActivity();
    }

    @JsonIgnore
    public String buildMessage(ActivityResult result, I18nContext language, int money, int items) {
        return String.format(language.get(result.getLanguageString()), getType().getEmoji(), getName(), money, items);
    }

    @JsonIgnore
    public HousePetType.PatReaction handlePat() {
        if (getType() == HousePetType.CAT) {
            return random.nextBoolean() ? HousePetType.PatReaction.CUTE : HousePetType.PatReaction.SCARE;
        }

        if (getType() == HousePetType.KODE) {
            return HousePetType.PatReaction.CUTE_2;
        }

        if (getType() == HousePetType.ROCK) {
            return HousePetType.PatReaction.NOTHING;
        }

        return HousePetType.PatReaction.CUTE;
    }

    public static class ActivityReward {
        private int items;
        private int money;
        private String result;

        public ActivityReward(int items, int money, String result) {
            this.items = items;
            this.money = money;
            this.result = result;
        }

        public int getItems() {
            return items;
        }

        public void setItems(int items) {
            this.items = items;
        }

        public int getMoney() {
            return money;
        }

        public void setMoney(int money) {
            this.money = money;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

    public enum ActivityResult {
        LOW_STAMINA(false, "commands.pet.activity.low_stamina"),
        LOW_HEALTH(false, "commands.pet.activity.low_health"),
        LOW_HUNGER(false, "commands.pet.activity.low_hunger"),
        LOW_THIRST(false, "commands.pet.activity.low_thirst"),
        PASS(true, "commands.pet.activity.success"),
        PASS_MINE(true, "commands.pet.activity.success_mine"),
        PASS_CHOP(true, "commands.pet.activity.success_chop"),
        PASS_FISH(true, "commands.pet.activity.success_fish"),
        NO_ABILITY(false, ""); // No need, as it'll just be skipped.

        final boolean pass;
        final String i18n;
        ActivityResult(boolean pass, String i18n) {
            this.pass = pass;
            this.i18n = i18n;
        }

        public boolean passed() {
            return pass;
        }

        public String getLanguageString() {
            return i18n;
        }
    }
}
