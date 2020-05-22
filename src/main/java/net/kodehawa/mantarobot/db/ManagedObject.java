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

package net.kodehawa.mantarobot.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.kodehawa.mantarobot.data.MantaroData;

import javax.annotation.Nonnull;

public interface ManagedObject {
    @SuppressWarnings("NullableProblems")
    @Nonnull
    String getId();

    @JsonIgnore
    @Nonnull
    String getTableName();

    @JsonIgnore
    @Nonnull
    default String getDatabaseId() {
        return getId();
    }

    default void delete() {
        MantaroData.db().delete(this);
    }

    default void save() {
        MantaroData.db().save(this);
    }

    default void deleteAsync() {
        MantaroData.queue(this::delete);
    }

    default void saveAsync() {
        MantaroData.queue(this::save);
    }
}
