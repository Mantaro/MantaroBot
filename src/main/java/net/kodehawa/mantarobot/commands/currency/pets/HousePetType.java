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

package net.kodehawa.mantarobot.commands.currency.pets;

import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public enum HousePetType {
    DOG(EmoteReference.DOG, "Dog", List.of(HousePetAbility.CATCH, HousePetAbility.CHEER), 40000, 200, 20, true),
    CAT(EmoteReference.CAT, "Cat", List.of(HousePetAbility.FISH, HousePetAbility.CHEER), 35000, 150, 0, true),
    RAT(EmoteReference.HAMSTER, "Hamster", List.of(HousePetAbility.CHOP, HousePetAbility.CHEER), 30000, 30, 0, true),
    KODE(EmoteReference.DEV, "Dev",
            // All of them?
            List.of(HousePetAbility.CHEER, HousePetAbility.FISH, HousePetAbility.CATCH, HousePetAbility.CHOP),
            3000000, 300, 30, true
    ),
    ROCK(EmoteReference.ROCK, "Rock", List.of(HousePetAbility.CHEER), 1000, 1, 0, true),
    ALL(EmoteReference.PENCIL, "All Placeholder", List.of(HousePetAbility.values()), 100000, 10000, 0, false);

    public enum HousePetAbility {
        FISH(HousePet.ActivityResult.PASS_FISH, HousePet.ActivityResult.PASS_FISH_BOOSTED),
        CATCH(HousePet.ActivityResult.PASS_MINE, HousePet.ActivityResult.PASS_MINE_BOOSTED),
        CHOP(HousePet.ActivityResult.PASS_CHOP, HousePet.ActivityResult.PASS_CHOP_BOOSTED),
        CHEER(HousePet.ActivityResult.PASS, HousePet.ActivityResult.PASS_BOOSTED);

        private final HousePet.ActivityResult passActivity;
        private final HousePet.ActivityResult boostedActivity;
        HousePetAbility(HousePet.ActivityResult passActivity, HousePet.ActivityResult boostedActivity) {
            this.passActivity = passActivity;
            this.boostedActivity = boostedActivity;
        }

        public HousePet.ActivityResult getPassActivity(boolean isBoosted) {
            return isBoosted ? boostedActivity : passActivity;
        }
    }

    public enum PatReaction {
        @SuppressWarnings("unused")
        CHEER("commands.pet.pet_reactions.cheer"), SCARE("commands.pet.pet_reactions.scare"),
        CUTE("commands.pet.pet_reactions.cute"), CUTE_2("commands.pet.pet_reactions.cute_not_animal"),
        NOTHING("commands.pet.pet_reactions.nothing");

        final String message;
        @SuppressWarnings("unused") // this is used but bc HousePet is suppressed this would also report
        PatReaction(String i18n) {
            this.message = i18n;
        }

        public String getMessage() {
            return message;
        }
    }

    public enum PlayReaction {
        PLAYFUL(List.of(HousePetType.DOG, HousePetType.CAT), "commands.pet.play.reactions.playful"),
        LOVE(List.of(HousePetType.DOG, HousePetType.CAT, HousePetType.RAT), "commands.pet.play.reactions.love"),
        DEV(List.of(HousePetType.KODE), "commands.pet.play.reactions.dev"),
        NOTHING(List.of(HousePetType.ROCK), "commands.pet.play.reactions.nothing");

        final String message;
        final List<HousePetType> types;
        PlayReaction(List<HousePetType> types, String i18n) {
            this.types = types;
            this.message = i18n;
        }

        public List<HousePetType> getTypes() {
            return types;
        }

        public String getMessage() {
            return message;
        }

        public static PlayReaction getReactionForPlay(HousePetType type) {
            var typeMatch = Arrays.stream(values()).filter(react -> react.getTypes().contains(type)).collect(Collectors.toList());
            Collections.shuffle(typeMatch); // will this be random enough?
            return typeMatch.get(0);
        }
    }

    private final EmoteReference emoji;
    private final String name;
    private final List<HousePetAbility> abilities;
    private final int cost;
    private final int gemLuckIncrease;
    private final int maxCoinBuildup;
    private final boolean buyable;

    HousePetType(EmoteReference emoji, String name, List<HousePetAbility> ability, int cost, int maxCoinBuildup, int gemLuckIncrease, boolean buyable) {
        this.emoji = emoji;
        this.name = name;
        this.abilities = ability;
        this.cost = cost;
        this.maxCoinBuildup = maxCoinBuildup;
        this.buyable = buyable;
        this.gemLuckIncrease = gemLuckIncrease;
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

        return Math.min(2000, (int) (maxCoinBuildup + (4 * level)));
    }

    public int getMaxItemBuildup(long level) {
        if (this == ROCK) {
            return 0;
        }

        return Math.min(30, (int) (3 + (0.1 * level)));
    }

    public String getStringAbilities() {
        return getAbilities().stream()
                .map(ability -> Utils.capitalize(ability.toString().toLowerCase()))
                .collect(Collectors.joining(", "));
    }

    public boolean isBuyable() {
        return buyable;
    }

    public int getGemLuckIncrease() {
        return gemLuckIncrease;
    }


    public String getTranslationKey() {
        return "commands.pet.types." + name().toLowerCase();
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
