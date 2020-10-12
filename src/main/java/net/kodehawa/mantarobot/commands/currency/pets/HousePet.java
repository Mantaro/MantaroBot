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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

public class HousePet {
    @JsonIgnore
    private static SecureRandom random = new SecureRandom();

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
        this.health -= 1;
    }

    public void decreaseStamina() {
        this.stamina -= 10;
    }

    public void decreaseHunger() {
        this.hunger -= 10;
    }

    public void decreaseThirst() {
        this.thirst -= 15;
    }

    public void increaseHealth() {
        this.health += 10;
    }

    public void increaseStamina() {
        this.stamina += 30;
    }

    public void increaseHunger(int by) {
        this.hunger += by;
    }

    public void increaseThirst() {
        this.thirst += 15;
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
        this.experience += random.nextInt(15);
        var toNextLevel = experienceToNextLevel();
        if (experience > toNextLevel)
            level += 1;
    }

    @JsonIgnore
    public boolean isSleepy(String timezone) {
        TimeZone tz = TimeZone.getDefault();
        if(timezone != null) {
            tz = TimeZone.getTimeZone(timezone);
        }

        var time = LocalDateTime.now().atZone(tz.toZoneId());

        return time.getHour() < 7;
    }

    @JsonIgnore
    public ActivityResult handleAbility(HousePetType.HousePetAbility neededAbility, String marriageTz) {
        if(!type.getAbilities().contains(neededAbility))
            return ActivityResult.NO_ABILITY;

        if(getStamina() < 40)
            return ActivityResult.LOW_STAMINA;

        if(getHealth() < 30)
            return ActivityResult.LOW_HEALTH;

        if(getHunger() < 10)
            return ActivityResult.LOW_HUNGER;

        if(getThirst() < 20)
            return ActivityResult.LOW_THRIST;

        if(isSleepy(marriageTz))
            return ActivityResult.SLEEPY;

        decreaseStamina();
        decreaseHealth();
        decreaseHunger();
        decreaseThirst();
        increaseExperience();

        return ActivityResult.PASS;
    }

    @JsonIgnore
    public String buildMessage(ActivityResult result, I18nContext language) {
        return String.format(language.get(result.getLanguageString()), getType().getEmoji(), getType().getName(), getName());
    }

    @JsonIgnore
    public HousePetType.PatReaction handlePat() {
        if(getType() == HousePetType.CAT) {
            return random.nextBoolean() ? HousePetType.PatReaction.CUTE : HousePetType.PatReaction.SCARE;
        }

        return HousePetType.PatReaction.CUTE;
    }

    public static enum ActivityResult {
        LOW_STAMINA(false, "commands.pet.activity.low_stamina"),
        LOW_HEALTH(false, "commands.pet.activity.low_health"),
        LOW_HUNGER(false, "commands.pet.activity.low_hunger"),
        LOW_THRIST(false, "commands.pet.activity.low_thrist"),
        SLEEPY(false, "commands.pet.activity.sleepy"),
        NO_ABILITY(false, ""), // No need, as it'll just be skipped.
        PASS(true, "commands.pet.activity.success");

        boolean pass;
        String i18n;
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
