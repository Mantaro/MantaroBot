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

package net.kodehawa.mantarobot.commands.currency.item.special;

import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Castable;
import net.kodehawa.mantarobot.commands.currency.pets.HousePetType;

// Only castable sometimes... no matter, though. If the recipe is empty it won't show on cast list, nor be castable.
public class Food extends Item implements Castable {
    private final int hungerLevel;
    private final FoodType type;

    public Food(FoodType type, int hungerLevel, String emoji, String name, String translatedName,
                String desc, long value, boolean buyable) {
        super(ItemType.PET_FOOD, emoji, name, translatedName, desc, value, true, buyable);
        this.hungerLevel = hungerLevel;
        this.type = type;
    }

    public Food(FoodType type, int hungerLevel, String emoji, String name, String translatedName,
                String desc, long value, boolean buyable, String recipe, int... recipeTypes) {
        super(ItemType.PET_FOOD_CAST, emoji, name, translatedName, desc, value, true, buyable, recipe, recipeTypes);
        this.hungerLevel = hungerLevel;
        this.type = type;
    }

    public int getHungerLevel() {
        return this.hungerLevel;
    }

    public FoodType getType() {
        return type;
    }

    @SuppressWarnings("unused")
    public enum FoodType {
        CAT(HousePetType.CAT), DOG(HousePetType.DOG), HAMSTER(HousePetType.RAT), DEV(HousePetType.KODE), GENERAL(HousePetType.ALL);

        final HousePetType applicableType;

        FoodType(HousePetType applicable) {
            this.applicableType = applicable;
        }

        public HousePetType getApplicableType() {
            return applicableType;
        }
    }
}
