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

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ItemStack {
    public static int MAX_STACK_SIZE = 5000;
    private final int amount;
    private final Item item;

    public ItemStack(Item item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    public ItemStack(int i, int amount) {
        this(Items.fromId(i), amount);
    }

    public static Map<Item, ItemStack> mapped(List<ItemStack> list) {
        return list.stream().filter(stack -> stack.getAmount() != 0).collect(Collectors.toMap(ItemStack::getItem, UnaryOperator.identity(), ItemStack::join));
    }

    public static List<ItemStack> reduce(List<ItemStack> list) {
        return new ArrayList<>(mapped(list).values());
    }

    public static String toString(List<ItemStack> list) {
        if (list.isEmpty()) return "There's only dust.";

        return list.stream().filter(stack -> stack.getAmount() != 0)
                .sorted(Comparator.comparingInt(ItemStack::getAmount).reversed())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }

    public static String toString(ItemStack... stacks) {
        return toString(Arrays.asList(stacks));
    }

    @Override
    public String toString() {
        return getItem().getEmoji() + " x " + getAmount();
    }

    public int getAmount() {
        return amount;
    }

    public Item getItem() {
        return item;
    }

    public ItemStack join(ItemStack stack) {
        if (!stack.getItem().equals(this.getItem()))
            throw new UnsupportedOperationException("Not the same Items");
        if (!canJoin(stack))
            throw new UnsupportedOperationException("Add a check for canJoin before this");

        return new ItemStack(this.getItem(), this.getAmount() + stack.getAmount());
    }

    public boolean canJoin(ItemStack other) {
        int amountAfter = amount + other.amount;
        return amountAfter >= 0 && amountAfter <= MAX_STACK_SIZE;
    }
}
