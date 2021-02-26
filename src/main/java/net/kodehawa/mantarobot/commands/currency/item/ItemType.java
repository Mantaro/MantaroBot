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

package net.kodehawa.mantarobot.commands.currency.item;

public enum ItemType {
    // Commons
    COLLECTABLE(false, "items.categories.collectable"),
    COMMON(false, "items.categories.common"),
    RARE(false, "items.categories.rare"),
    INTERACTIVE(false, "items.categories.interactive"),

    // Not really used.
    PREMIUM(false, "items.categories.premium"),

    // Cast-able items.
    CAST_MINE(true, "items.categories.cast_mine"),
    CAST_OBTAINABLE(true, "items.categories.cast_mine"),

    // Drops.
    MINE(false, "items.categories.mine"),
    MINE_RARE(true, "items.categories.mine_rare"),

    FISHING(false, "items.categories.fish"),
    FISHING_RARE(false, "items.categories.fish_rare"),

    CRATE(false, "items.categories.crate"),
    CHOP_DROP(false, "items.categories.chop_drop"),

    // Buffs
    POTION(false, "items.categories.potion"),
    BUFF(false, "items.categories.buff"),

    // Wrenches
    WRENCH(true, "items.categories.wrench"),

    // Waifu stuff, only has one little type tho.
    WAIFU(false, "items.categories.waifu"),

    // Picks
    MINE_PICK(true, "items.categories.mine_pick"),
    MINE_RARE_PICK(true, "items.categories.mine_rare_pick"),
    MINE_RARE_PICK_NODROP(true, "items.categories.mine_rare_pick"),

    // Rods
    FISHROD(true, "items.categories.fish_rod"),
    FISHROD_RARE(true, "items.categories.fish_rod_rare"),
    FISHROD_RARE_NODROP(true, "items.categories.fish_rod_rare"),

    // Axes
    CHOP_AXE(true, "items.categories.chop_axe"),
    CHOP_RARE_AXE(true, "items.categories.chop_axe_rare"),
    CHOP_RARE_AXE_NODROP(true, "items.categories.chop_axe_rare"),

    // Broken items
    BROKEN(false, "items.categories.broken"),

    //Pet items don't appear in the normal market.
    PET(true, "items.categories.pet"),
    PET_FOOD(false, "items.categories.pet_food"),

    // Not used anymore
    DEPRECATED(false, "items.categories.deprecated");

    private final boolean cast;

    private final String description;

    ItemType(boolean cast, String description) {
        this.cast = cast;
        this.description = description;
    }

    public boolean isCastable() {
        return cast;
    }

    public String getDescription() {
        return description;
    }

    public enum LootboxType {
        COMMON,
        RARE,
        MINE,
        MINE_PREMIUM,
        FISH,
        FISH_PREMIUM,
        CHOP,
        CHOP_PREMIUM,
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
