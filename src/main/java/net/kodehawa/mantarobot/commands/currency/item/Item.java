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

import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiPredicate;

public class Item {
    private static final Logger log = LoggerFactory.getLogger(Item.class);
    protected final long value;
    private final boolean buyable;
    private final String emoji, name, desc;
    private final boolean hidden;
    private final long maxSize;
    private final boolean sellable;
    //EXAMPLE: 1;3 will mean require two items of type 1 and 3 of type 2. For example a pick will require 2 of type 1 and 1 of type 2.
    //You can have as many types as you want.
    //If the recipe it's an empty string (or null), it means the item has no recipe.
    private final String recipe;
    private final int[] recipeTypes;
    private final long price;
    private BiPredicate<Context, Boolean> action;
    private final ItemType itemType;
    private final String translatedName;
    private final String alias;
    private final boolean petOnly;

    public Item(ItemType type, String emoji, String name, String alias, String translatedName,
                String desc, long value, boolean sellable, boolean buyable, boolean hidden, long maxSize,
                BiPredicate<Context, Boolean> action,
                String recipe, boolean petOnly, int... recipeTypes) {
        this.emoji = emoji;
        this.name = name;
        this.desc = desc;
        this.value = value;
        this.price = value;
        this.sellable = sellable;
        this.buyable = buyable;
        this.maxSize = maxSize;
        this.hidden = hidden;
        this.action = action;
        this.itemType = type;
        this.recipe = recipe;
        this.recipeTypes = recipeTypes;
        this.translatedName = translatedName;
        this.alias = alias;
        this.petOnly = petOnly;
        log.debug("Registered item {}: {}", name, this.toVerboseString());
    }

    public Item(ItemType type, String emoji, String name, String alias, String translatedName,
                String desc, long value) {
        this(type, emoji, name, alias,
                translatedName, desc, value,
                true, true, false,
                100, null, "", false
        );
    }

    public Item(ItemType type, String emoji, String name, String translatedName,
                String desc, long value) {
        this(type, emoji, name, null,
                translatedName, desc, value,
                true, true, false,
                100, null, "", false
        );
    }

    public Item(ItemType type, String emoji, String name, String alias, String translatedName,
                String desc, long value, boolean sellable, boolean buyable) {
        this(type, emoji, name, alias,
                translatedName, desc, value,
                sellable, buyable, false,
                100, null, "", false
        );
    }

    public Item(ItemType type, String emoji, String name, String translatedName,
                String desc, long value, boolean sellable, boolean buyable) {
        this(type, emoji, name, null,
                translatedName, desc, value,
                sellable, buyable, false,
                100, null, "", false
        );
    }

    public Item(ItemType type, String emoji, String name, String alias, String translatedName,
                String desc, long value, boolean sellable, boolean buyable, String recipe, int... recipeTypes) {
        this(type, emoji, name, alias,
                translatedName, desc, value,
                sellable, buyable, false,
                100, null,
                recipe, false, recipeTypes
        );
    }

    public Item(ItemType type, String emoji, String name, String translatedName,
                String desc, long value, boolean sellable, boolean buyable, String recipe, int... recipeTypes) {
        this(type, emoji, name, null,
                translatedName, desc, value,
                sellable, buyable, false,
                100, null,
                recipe, false, recipeTypes
        );
    }

    public Item(ItemType type, String emoji, String name, String translatedName,
                String desc, long value, boolean buyable) {
        this(type, emoji, name, null,
                translatedName, desc, value,
                true, buyable, false,
                100, null, "", false
        );
    }

    public Item(ItemType type, String emoji, String name, String alias, String translatedName,
                String desc, long value, boolean buyable) {
        this(type, emoji, name, alias,
                translatedName, desc, value,
                true, buyable, false,
                100, null, "", false
        );
    }

    public Item(ItemType type, String emoji, String name, String translatedName,
                String desc, long value, boolean sellable, boolean buyable, boolean hidden) {
        this(type, emoji, name, null,
                translatedName, desc, value,
                sellable, buyable, hidden,
                100, null, "", false
        );
    }

    public Item(ItemType type, String emoji, String name, String translatedName,
                String desc, long value, boolean sellable, boolean buyable, BiPredicate<Context, Boolean> action) {
        this(type, emoji, name, null,
                translatedName, desc, value,
                sellable, buyable, false,
                100, action, "", false
        );
    }

    public Item(ItemType type, String emoji, String name, String translatedName,
                String desc, long value, boolean buyable, BiPredicate<Context, Boolean> action) {
        this(type, emoji, name, null,
                translatedName, desc, value,
                true, buyable, false,
                100, action, "", false
        );
    }

    public Item(ItemType type, String emoji, String name, String translatedName,
                String desc, long value, boolean sellable, boolean buyable, boolean hidden,
                BiPredicate<Context, Boolean> action) {
        this(type, emoji, name, null,
                translatedName, desc, value,
                sellable, buyable, hidden,
                100, action, "", false
        );
    }

    public Item(ItemType type, String emoji, String name, String translatedName,
                String desc, long value, boolean sellable, boolean buyable, boolean hidden, boolean petOnly) {
        this(type, emoji, name, null,
                translatedName, desc, value,
                sellable, buyable, hidden,
                100, null, "", petOnly
        );
    }

    public Item(ItemType type, String emoji, String name, String translatedName,
                String desc, long value, boolean sellable, boolean buyable, boolean hidden, boolean petOnly,
                String recipe, int... recipeTypes) {
        this(type, emoji, name, null,
                translatedName, desc, value,
                sellable, buyable, hidden,
                100, null,
                recipe, petOnly, recipeTypes
        );
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
        this(ItemType.COLLECTABLE, emoji, name, null,
                "", desc, 0,
                false, false, true,
                100, null,
                "", false
        );
    }

    @Override
    public String toString() {
        return "**" + name + "** ($" + value + ")";
    }

    public String toDisplayString() {
        return emoji + " " + name;
    }

    public String toVerboseString() {
        return String.format(
                "Item{name:%s, type:%s, value:%s, buyable:%s, sellable:%s}",
                name, itemType, value, buyable, sellable
        );
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

    public boolean isHidden() {
        return hidden || (!sellable && !buyable);
    }

    public long maxSize() {
        return maxSize;
    }

    public String getRecipe() {
        return this.recipe;
    }

    public int[] getRecipeTypes() {
        return this.recipeTypes;
    }

    public BiPredicate<Context, Boolean> getAction() {
        return this.action;
    }

    public void setAction(BiPredicate<Context, Boolean> action) {
        this.action = action;
    }

    public ItemType getItemType() {
        return this.itemType;
    }

    public String getTranslatedName() {
        return this.translatedName;
    }

    public String getAlias() {
        return this.alias;
    }

    public boolean isPetOnly() {
        return this.petOnly;
    }
}
