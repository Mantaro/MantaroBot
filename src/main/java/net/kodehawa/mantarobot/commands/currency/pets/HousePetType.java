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

import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.List;
import java.util.stream.Collectors;

public enum HousePetType {
    DOG(EmoteReference.DOG, "Dog", List.of(HousePetAbility.CATCH, HousePetAbility.CHEER), 40000, 200),
    CAT(EmoteReference.CAT, "Cat", List.of(HousePetAbility.FISH, HousePetAbility.CHEER), 30000, 100),
    RAT(EmoteReference.HAMSTER, "Hamster", List.of(HousePetAbility.CHEER), 4000, 30),
    KODE(EmoteReference.DEV, "Kodehawa", List.of(HousePetAbility.CHEER, HousePetAbility.FISH), 3000000, 300);

    public static enum HousePetAbility {
        FISH, CATCH, CHEER;
    }

    public static enum PatReaction {
        CHEER("commands.pet.pet_reactions.cheer"), SCARE("commands.pet.pet_reactions.scare"), CUTE("commands.pet.pet_reactions.cute");

        String message;
        PatReaction(String i18n) {
            this.message = i18n;
        }

        public String getMessage() {
            return message;
        }
    }

    private EmoteReference emoji;
    private String name;
    private List<HousePetAbility> abilities;
    private int cost;
    private int maxCoinBuildup;

    HousePetType(EmoteReference emoji, String name, List<HousePetAbility> ability, int cost, int maxCoinBuildup) {
        this.emoji = emoji;
        this.name = name;
        this.abilities = ability;
        this.cost = cost;
        this.maxCoinBuildup = maxCoinBuildup;
    }

    public EmoteReference getEmoji() {
        return emoji;
    }

    public String getName() {
        return name;
    }

    public List<HousePetAbility> getAbilities() {
        return abilities;
    }

    public int getCost() {
        return cost;
    }

    public int getMaxCoinBuildup() {
        return maxCoinBuildup;
    }

    public String getStringAbilities() {
        return getAbilities().stream().map(ability -> Utils.capitalize(ability.toString().toLowerCase())).collect(Collectors.joining(", "));
    }

    /**
     * Looks up the HousePet based on a String value, if nothing is found returns null.
     *
     * @param name The String value to match
     * @return The pet, or null if nothing is found.
     */
    public static HousePetType lookupFromString(String name) {
        for (HousePetType b : HousePetType.values()) {
            if (b.name().equalsIgnoreCase(name)) {
                return b;
            }

            if (b.getName().equalsIgnoreCase(name)) {
                return b;
            }
        }

        return null;
    }
}
