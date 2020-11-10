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

package net.kodehawa.mantarobot.commands.currency.item;

public enum ItemType {
    // Commons
    COLLECTABLE(false),
    COMMON(false),
    RARE(false),
    INTERACTIVE(false),

    // Not really used.
    PREMIUM(false),

    // Cast-able items.
    CAST_MINE(true),
    CAST_OBTAINABLE(true),

    // Drops.
    MINE(false),
    FISHING(false),
    FISHING_RARE(false),
    MINE_RARE(true),
    CRATE(false),
    CHOP_DROP(false),

    // Buffs
    POTION(false),
    BUFF(false),

    // Wrenches
    WRENCH(true),

    // Waifu stuff, only has one little type tho.
    WAIFU(false),

    // Picks
    MINE_PICK(true),
    MINE_RARE_PICK(true),
    MINE_RARE_PICK_NODROP(true),

    // Rods
    FISHROD(true),
    FISHROD_RARE(true),
    FISHROD_RARE_NODROP(true),

    // Axes
    CHOP_AXE(true),
    CHOP_RARE_AXE(true),
    CHOP_RARE_AXE_NODROP(true),

    // Broken items
    BROKEN(false),

    //Pet items don't appear in the normal market.
    PET(true),
    PET_FOOD(false),

    // Not used anymore
    DEPRECATED(false);

    private final boolean cast;

    ItemType(boolean cast) {
        this.cast = cast;
    }

    public boolean isCastable() {
        return cast;
    }

    public enum LootboxType {
        COMMON,
        RARE,
        MINE,
        MINE_PREMIUM,
        FISH,
        FISH_PREMIUM,
        PREMIUM,
        //-insert ea logo here-
        EPIC
    }

    public enum PotionType {
        PLAYER,
        CHANNEL,
        SPECIAL
    }
}
