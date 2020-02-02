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

public class Broken extends Item {
    //Repair cost it's usually Item value / 3
    private int mainItem;
    //EXAMPLE: 2,1;3,2 will mean require two items of type 1 and 3 of type 2. For example a pick will require 2 of type 1 and 1 of type 2.
    //You can have as many types as you want.
    //If the recipe it's an empty string (or null), it means the item has no recipe.
    private String recipe;
    
    
    public Broken(int mainItem, String emoji, String name, String translatedName, String desc, long value, String recipe) {
        super(ItemType.BROKEN, emoji, name, translatedName, desc, value, true, false);
        this.mainItem = mainItem;
        //Repair recipe
        this.recipe = recipe;
    }
    
    public Broken(ItemType type, int mainItem, String emoji, String name, String translatedName, String desc, long value, String recipe) {
        super(type, emoji, name, translatedName, desc, value, true, false);
        this.mainItem = mainItem;
        //Repair recipe
        this.recipe = recipe;
    }
    
    public int getMainItem() {
        return this.mainItem;
    }
    
    public String getRecipe() {
        return this.recipe;
    }
}
