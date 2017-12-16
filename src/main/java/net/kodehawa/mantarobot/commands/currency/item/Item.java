/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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

import lombok.Getter;

public class Item {
    protected final long value;
    private final boolean buyable;
    private final String emoji, name, desc;
    @Getter
    private final boolean hidden;
    private final long maxSize;
    private final boolean sellable;
    private long price;

    public Item(String emoji, String name, String desc, long value) {
        this(emoji, name, desc, value, true, true, false, 100);
    }

    public Item(String emoji, String name, String desc, long value, boolean sellable, boolean buyable, boolean hidden, long maxSize) {
        this.emoji = emoji;
        this.name = name;
        this.desc = desc;
        this.value = value;
        this.price = value;
        this.sellable = sellable;
        this.buyable = buyable;
        this.maxSize = maxSize;
        this.hidden = hidden;
    }

    public Item(String emoji, String name, String desc, long value, boolean sellable, boolean buyable) {
        this(emoji, name, desc, value, sellable, buyable, false, 100);
    }

    public Item(String emoji, String name, String desc, long value, boolean buyable) {
        this(emoji, name, desc, value, true, buyable, false, 100);
    }

    public Item(String emoji, String name, String desc, long value, boolean sellable, boolean buyable, boolean hidden) {
        this(emoji, name, desc, value, sellable, buyable, hidden, 100);
    }

    /**
     * Constructor specifically meant for special items. Assuming it will be hidden, with a market price of 0 and neither buyables or sellables
     * So market price really doesn't matter. The hidden attribute means it won't appear on market.
     *
     * @param emoji The emoji it should it display on market.
     * @param name  Display name.
     * @param desc  A short description, normally used in inventory.
     */
    public Item(String emoji, String name, String desc) {
        this(emoji, name, desc, 0, false, false, true, 100);
    }

    @Override
    public String toString() {
        return "**" + name + "** ($" + value + ")";
    }

    public String getDesc() {
        return desc;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getName() {
        return name;
    }

    public long getValue() {
        return price;
    }

    public boolean isBuyable() {
        return buyable;
    }

    public boolean isSellable() {
        return sellable;
    }

    public long maxSize() {
        return maxSize;
    }
}
