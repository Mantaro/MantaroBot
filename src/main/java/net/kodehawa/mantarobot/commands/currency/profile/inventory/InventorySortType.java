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

public enum InventorySortType {
    VALUE(InventorySort.SORT_VALUE), AMOUNT(InventorySort.SORT_AMOUNT), TYPE(InventorySort.SORT_TYPE),
    RANDOM(InventorySort.SORT_RANDOM), VALUE_TOTAL(InventorySort.SORT_VALUE_SUM);

    // We don't know the type on enums, shouldn't matter.
    final InventorySort<? super ItemStack> sort;

    InventorySortType(final InventorySort<? super ItemStack> sort) {
        this.sort = sort;
    }

    public InventorySort<? super ItemStack> getSort() {
        return sort;
    }
}
