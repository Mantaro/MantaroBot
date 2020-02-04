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

package net.kodehawa.mantarobot.commands.currency.item.special;

import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Breakable;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Castable;

public class Pickaxe extends Item implements Castable, Breakable {
    private float chance;
    //Wrench level, basically.
    private int castLevelRequired;
    private int maximumCastAmount;
    private int maxDurability;
    
    public Pickaxe(ItemType type, float chance, int castLevelRequired, int maximumCastAmount, String emoji, String name, String translatedName, String desc, long value, boolean sellable, boolean buyable, String recipe, int maxDurability, int... recipeTypes) {
        super(type, emoji, name, translatedName, desc, value, sellable, buyable, recipe, recipeTypes);
        this.chance = chance;
        this.castLevelRequired = castLevelRequired;
        this.maximumCastAmount = maximumCastAmount;
        this.maxDurability = maxDurability;
    }
    
    public Pickaxe(ItemType type, float chance, String emoji, String name, String translatedName, String desc, long value, boolean buyable, int maxDurability) {
        super(type, emoji, name, translatedName, desc, value, true, buyable);
        this.chance = chance;
        this.castLevelRequired = -1;
        this.maximumCastAmount = -1;
        this.maxDurability = maxDurability;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public float getChance() {
        return this.chance;
    }
    
    public int getCastLevelRequired() {
        return this.castLevelRequired;
    }
    
    public int getMaximumCastAmount() {
        return this.maximumCastAmount;
    }
}
