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

package net.kodehawa.mantarobot.commands.currency.item.special.helpers.attributes;

import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Castable;
import org.apache.commons.lang3.StringUtils;

public interface Tiered extends Castable {
    int getTier();

    default String getTierStars() {
        return StringUtils.repeat('⭐', getTier());
    }

    default String getTierStars(int stars) {
        if (stars == -1 || stars == 0) {
            return "none";
        }

        return StringUtils.repeat('⭐', stars);
    }

    default int getMaximumCastAmount() {
        if (getTier() >= 7) return 1;
        int required = getCastLevelRequired();
        if (required < 0) return -1;
        int offsetTier = Math.max(0, getTier() - 4);
        return 10 - (offsetTier * 2) - (required > 1 ? 1 : 0);
    }
}
