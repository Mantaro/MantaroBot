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

package net.kodehawa.mantarobot.commands.currency.item.special.tools;

import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.attributes.Attribute;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Castable;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Salvageable;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.attributes.ItemUsage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Pickaxe extends Item implements Castable, Salvageable, Attribute {
    // Wrench level, basically.
    private final int castLevelRequired;
    private final int maximumCastAmount;
    private final int maxDurability;
    private final int moneyIncrease;
    private final List<Integer> salvageReturns;
    private final String explanation;
    private final int rarity;

    // This ones have default attributes.
    private int diamondIncrease = 2;
    private int sparkleLuck = 401; // Bound: 400
    private int gemLuck = 340; // Bound: 400

    // I can barely read this, so let me break it down:
    // Item type, item emoji, name, localized name, description, wrench tier to cast it, maximum amount to cast at once
    // item value, sellable, buyable, item recipe (amount), max durability, amount of money more it can give,
    // amount of extra diamonds it can give, sparkle find rate, gem find rate, item rarity, explanation, item recipe (items)
    public Pickaxe(ItemType type, String emoji, String name, String translatedName, String description,
                   int castLevelRequired, int maximumCastAmount, long value, boolean sellable, boolean buyable,
                   String recipe, int maxDurability, int moneyIncrease, int diamondIncrease, int sparkleLuck,
                   int gemLuck, int rarity, String explanation, int... recipeTypes) {
        super(type, emoji, name, translatedName, description, value, sellable, buyable, recipe, recipeTypes);
        this.castLevelRequired = castLevelRequired;
        this.maximumCastAmount = maximumCastAmount;
        this.maxDurability = maxDurability;
        this.moneyIncrease = moneyIncrease;
        this.salvageReturns = Arrays.stream(recipeTypes).filter(id -> id > 1).boxed().collect(Collectors.toList());
        this.diamondIncrease = diamondIncrease;
        this.sparkleLuck = sparkleLuck;
        this.gemLuck = gemLuck;
        this.rarity = rarity;
        this.explanation = explanation;
    }

    public Pickaxe(ItemType type, int castLevelRequired, int maximumCastAmount,
                   String emoji, String name, String translatedName,
                   String description, long value, boolean sellable, boolean buyable, String recipe,
                   int maxDurability, int moneyIncrease, int gemLuck, int rarity,
                   String explanation, int... recipeTypes) {
        super(type, emoji, name, translatedName, description, value, sellable, buyable, recipe, recipeTypes);
        this.castLevelRequired = castLevelRequired;
        this.maximumCastAmount = maximumCastAmount;
        this.maxDurability = maxDurability;
        this.moneyIncrease = moneyIncrease;
        this.salvageReturns = Arrays.stream(recipeTypes).filter(id -> id > 1).boxed().collect(Collectors.toList());
        this.rarity = rarity;
        this.gemLuck = gemLuck;
        this.explanation = explanation;
    }

    public Pickaxe(ItemType type, int castLevelRequired, int maximumCastAmount,
                   String emoji, String name, String translatedName,
                   String desc, long value, boolean sellable, boolean buyable, String recipe,
                   int maxDurability, int moneyIncrease, int rarity, String explanation, int... recipeTypes) {
        super(type, emoji, name, translatedName, desc, value, sellable, buyable, recipe, recipeTypes);
        this.castLevelRequired = castLevelRequired;
        this.maximumCastAmount = maximumCastAmount;
        this.maxDurability = maxDurability;
        this.moneyIncrease = moneyIncrease;
        this.salvageReturns = Arrays.stream(recipeTypes).filter(id -> id > 1).boxed().collect(Collectors.toList());
        this.rarity = rarity;
        this.explanation = explanation;
    }

    public Pickaxe(ItemType type, String emoji, String name, String translatedName,
                   String description, long value, boolean buyable, int maxDurability, int moneyIncrease,
                   int rarity, String explanation) {
        super(type, emoji, name, translatedName, description, value, true, buyable);
        this.castLevelRequired = -1;
        this.maximumCastAmount = -1;
        this.maxDurability = maxDurability;
        this.moneyIncrease = moneyIncrease;
        this.salvageReturns = Collections.emptyList();
        this.rarity = rarity;
        this.explanation = explanation;
    }

    @Override
    public int getTier() {
        return rarity;
    }

    @Override
    // TODO: Localize
    public String buildAttributes() {
        return """
                **Wrench Tier (to craft):**\u2009 %s
                **Money Increase:**\u2009 %,d - %,d credits
                **Diamond Drop Range:**\u2009 0 - %,d
                **Sparkle / Gem Luck (%%):**\u2009 %,.1f%% / %,.1f%%
                """.formatted(
                        getTierStars(getCastLevelRequired()), (getMoneyIncrease() / 4),
                        getMoneyIncrease(), getDiamondIncrease(),
                        getChance(0, 400, getSparkleLuck()),
                        getChance(0, 400, getGemLuck())
                );
    }

    @Override
    public String getExplanation() {
        return explanation;
    }

    @Override
    public ItemUsage getType() {
        return ItemUsage.MINING;
    }

    @Override
    public List<Integer> getReturns() {
        return salvageReturns;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public int getCastLevelRequired() {
        return this.castLevelRequired;
    }

    public int getMaximumCastAmount() {
        return this.maximumCastAmount;
    }

    public int getMoneyIncrease() {
        return moneyIncrease;
    }

    public int getDiamondIncrease() {
        return diamondIncrease;
    }

    public int getSparkleLuck() {
        return sparkleLuck;
    }

    public int getGemLuck() {
        return gemLuck;
    }

    public double getChance(int min, int max, int target) {
        if (target > max) {
            return 0f;
        }

        return ((double) (max - target) / (max - min)) * 100;
    }
}
