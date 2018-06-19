/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.currency.item;

public enum ItemType {
    //An item without buy value, that can only be obtained through commands.
    COLLECTABLE,
    //An item that's common, obtainable through buy/sell.
    COMMON,
    //An item that's maybe a collectible, but that's harder to get than the rest.
    RARE,
    //An item used in fishing mechanisms.
    FISHING,
    //An item used in mining.
    MINE,
    //An item that has an action attached to it.
    INTERACTIVE,
    //Guess.
    PREMIUM,
    //Cast-able item.
    CAST;

    public enum LootboxType {
        COMMON,
        RARE,
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
