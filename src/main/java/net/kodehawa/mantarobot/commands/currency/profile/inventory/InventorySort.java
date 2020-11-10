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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.currency.profile.inventory;

import net.kodehawa.mantarobot.commands.currency.item.ItemStack;

import java.util.Comparator;

// Enums can't have type values, so I need to make this a class instead
public class InventorySort<T extends ItemStack> {
    // This is the only sane one, LMAO
    public static InventorySort<ItemStack> SORT_AMOUNT = new InventorySort<>(Comparator.comparingInt(ItemStack::getAmount).reversed());

    // Using Comparator.comparingLong here fails horribly and makes a InventorySort<Item>, while I need a InventorySort<ItemStack> :)
    // Why can't I use Comparator#reverse here? It makes it Comparator<Object>... oh well, reverse manually.
    public static InventorySort<ItemStack> SORT_VALUE =
            new InventorySort<>(Comparator.comparing((t) -> t.getItem().getValue(), (o1, o2) -> (int) (o2 - o1)));

    public static InventorySort<ItemStack> SORT_TYPE =
            new InventorySort<>(Comparator.comparing((t) -> t.getItem().getItemType(), Comparator.comparingInt(Enum::ordinal)));

    private final Comparator<T> comparator;

    public InventorySort(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    public Comparator<T> getComparator() {
        return comparator;
    }
}
