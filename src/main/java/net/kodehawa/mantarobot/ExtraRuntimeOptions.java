/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.OptionalInt;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ExtraRuntimeOptions {
    public static final boolean DISABLE_NON_ALLOCATING_BUFFER = getValue("mantaro.disable-non-allocating-buffer") != null;
    public static final boolean DEBUG = getValue("mantaro.debug") != null;
    public static final boolean DEBUG_LOGS = getValue("mantaro.debug_logs") != null;
    public static final boolean LOG_DB_ACCESS = getValue("mantaro.log_db_access") != null;
    public static final boolean TRACE_LOGS = getValue("mantaro.trace_logs") != null;
    public static final boolean VERBOSE = getValue("mantaro.verbose") != null;
    
    public static final OptionalInt FROM_SHARD = maybeInt("mantaro.from-shard");
    public static final OptionalInt TO_SHARD = maybeInt("mantaro.to-shard");
    public static final OptionalInt SHARD_COUNT = maybeInt("mantaro.shard-count");
    public static final boolean SHARD_SUBSET = FROM_SHARD.isPresent() && TO_SHARD.isPresent() && SHARD_COUNT.isPresent();
    public static final boolean SHARD_SUBSET_MISSING = !SHARD_SUBSET && (
            FROM_SHARD.isPresent() || TO_SHARD.isPresent()
    );
    
    private static OptionalInt maybeInt(String name) {
        var value = getValue(name);
        if(value == null) return OptionalInt.empty();
        try {
            return OptionalInt.of(Integer.parseInt(value));
        } catch(NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
    
    @Nullable
    private static String getValue(@Nonnull String name) {
        return System.getProperty(name, System.getenv(name.replace(".", "_").toUpperCase()));
    }
}
