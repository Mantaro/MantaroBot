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

package net.kodehawa.mantarobot.commands.currency.item;

public enum ItemType {
    //An item without buy value, that can only be obtained through commands.
    COLLECTABLE(false),
    //An item that's common, obtainable through buy/sell.
    COMMON(false),
    //An item that's maybe a collectible, but that's harder to get than the rest.
    RARE(false),
    //An item used in fishing mechanisms.
    FISHING(false),
    //An item used in mining.
    MINE(false),
    //An item that has an action attached to it.
    INTERACTIVE(false),
    //Guess.
    PREMIUM(false),
    //Cast-able item.
    CAST(true),
    //Cast-able item, but you can also use it to mine
    CAST_MINE(true),
    //Cast-able item, but you can also obtain it without casting.
    CAST_OBTAINABLE(true),
    //Rare fishes.
    FISHING_RARE(false),
    MINE_RARE(true),
    MINE_RARE_PICK(true),
    MINE_PICK(true),
    CRATE(false),
    CAST_FISH(true),
    POTION(false),
    BUFF(false),
    WRENCH(true),
    BROKEN(false),
    BROKEN_COMMON(false),
    BROKEN_FISHING(false),
    BROKEN_MINE_COMMON(false), //This is to drop broken picks on the fish crate.
    BROKEN_FISHING_COMMON(false), //This is to drop broken rods on the fish crate.
    WAIFU(false), //So it doesn't drop on a loot crate, lol
    
    //Pet items don't appear in the normal market.
    PET(true),
    PET_FOOD(false);
    
    private boolean cast;
    
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
