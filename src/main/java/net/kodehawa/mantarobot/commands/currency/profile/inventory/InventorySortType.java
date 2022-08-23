/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.currency.profile.inventory;

import net.kodehawa.mantarobot.commands.currency.item.ItemStack;

public enum InventorySortType {
    VALUE(InventorySort.SORT_VALUE, "commands.inventory.sort.value"), AMOUNT(InventorySort.SORT_AMOUNT, "commands.inventory.sort.amount"),
    TYPE(InventorySort.SORT_TYPE, "commands.inventory.sort.type"), RANDOM(InventorySort.SORT_RANDOM, "commands.inventory.sort.random"),
    VALUE_TOTAL(InventorySort.SORT_VALUE_SUM, "commands.inventory.sort.total");

    // We don't know the type on enums, shouldn't matter.
    final InventorySort<? super ItemStack> sort;
    final String translate;

    InventorySortType(final InventorySort<? super ItemStack> sort, String translateKey) {
        this.sort = sort;
        this.translate = translateKey;
    }

    public String getTranslate() {
        return translate;
    }

    public InventorySort<? super ItemStack> getSort() {
        return sort;
    }
}
