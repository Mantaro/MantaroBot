/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.currency.item.special.gems;

import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.attributes.Tiered;

public class CastedGem extends Item implements Tiered {
    private final int tier;
    public CastedGem(String emoji, String name, String translatedName,
                String desc, long value, int tier, boolean sellable, boolean buyable, String recipe, int... recipeTypes) {
        super(ItemType.CAST_OBTAINABLE, emoji, name, null, translatedName, desc, value, sellable, buyable, false,
                100, null, recipe, false, recipeTypes
        );

        this.tier = tier;
    }

    @Override
    public int getTier() {
        return tier;
    }
}
