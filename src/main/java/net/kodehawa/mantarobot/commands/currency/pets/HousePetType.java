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
    DOG(EmoteReference.DOG, "Dog", List.of(HousePetAbility.CATCH, HousePetAbility.CHEER), 40000, 200, true),
    CAT(EmoteReference.CAT, "Cat", List.of(HousePetAbility.FISH, HousePetAbility.CHEER), 35000, 150, true),
    RAT(EmoteReference.HAMSTER, "Hamster", List.of(HousePetAbility.CHOP, HousePetAbility.CHEER), 30000, 30, true),
    KODE(EmoteReference.DEV, "Kodehawa",
            // All of them?
            List.of(HousePetAbility.CHEER, HousePetAbility.FISH, HousePetAbility.CATCH, HousePetAbility.CHOP),
            3000000, 300, true
    ),
    ROCK(EmoteReference.ROCK, "Rock", List.of(HousePetAbility.CHEER), 1000, 1, true),
    ALL(EmoteReference.PENCIL, "All Placeholder", List.of(HousePetAbility.values()), 100000, 10000, false);

    public enum HousePetAbility {
        FISH(HousePet.ActivityResult.PASS_FISH),
        CATCH(HousePet.ActivityResult.PASS_MINE),
        CHOP(HousePet.ActivityResult.PASS_CHOP),
        CHEER(HousePet.ActivityResult.PASS);

        private final HousePet.ActivityResult passActivity;
        HousePetAbility(HousePet.ActivityResult passActivity) {
            this.passActivity = passActivity;
        }

        public HousePet.ActivityResult getPassActivity() {
            return passActivity;
        }
    }

    public enum PatReaction {
        CHEER("commands.pet.pet_reactions.cheer"), SCARE("commands.pet.pet_reactions.scare"),
        CUTE("commands.pet.pet_reactions.cute"), CUTE_2("commands.pet.pet_reactions.cute_not_animal"),
        NOTHING("commands.pet.pet_reactions.nothing");

        final String message;
        PatReaction(String i18n) {
            this.message = i18n;
        }

        public String getMessage() {
            return message;
        }
    }

    private final EmoteReference emoji;
    private final String name;
    private final List<HousePetAbility> abilities;
    private final int cost;
    private final int maxCoinBuildup;
    private final boolean buyable;

    HousePetType(EmoteReference emoji, String name, List<HousePetAbility> ability, int cost, int maxCoinBuildup, boolean buyable) {
        this.emoji = emoji;
        this.name = name;
        this.abilities = ability;
        this.cost = cost;
        this.maxCoinBuildup = maxCoinBuildup;
        this.buyable = buyable;
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

    public int getMaxCoinBuildup(long level) {
        if (this == ROCK) {
            return 0;
        }

        return (int) (maxCoinBuildup + (4 * level));
    }

    public int getMaxItemBuildup(long level) {
        if (this == ROCK) {
            return 0;
        }

        return (int) (3 + (0.1 * level));
    }

    public String getStringAbilities() {
        return getAbilities().stream()
                .map(ability -> Utils.capitalize(ability.toString().toLowerCase()))
                .collect(Collectors.joining(", "));
    }

    public boolean isBuyable() {
        return buyable;
    }

    /**
     * Looks up the HousePet based on a String value, if nothing is found returns null.
     *
     * @param name The String value to match
     * @return The pet, or null if nothing is found.
     */
    public static HousePetType lookupFromString(String name) {
        for (HousePetType type : HousePetType.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type.isBuyable() ? type : null;
            }

            if (type.getName().equalsIgnoreCase(name)) {
                return type.isBuyable() ? type : null;
            }
        }

        return null;
    }
}
