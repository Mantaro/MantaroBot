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

package net.kodehawa.mantarobot.db.entities;

import lombok.Getter;
import net.kodehawa.mantarobot.db.ManagedObject;

import java.beans.ConstructorProperties;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
public class PremiumKey implements ManagedObject {
    public static final String DB_TABLE = "keys";
    private final long duration;
    private final long expiration;
    private final String id;

    @ConstructorProperties({"id", "duration", "expiration"})
    public PremiumKey(String id, long duration, long expiration) {
        this.id = id;
        this.duration = duration;
        this.expiration = expiration;
    }

    @Override
    public void delete() {
        r.table(DB_TABLE).get(getId()).delete().runNoReply(conn());
    }

    @Override
    public void save() {
        r.table(DB_TABLE).insert(this)
                .optArg("conflict", "replace")
                .runNoReply(conn());
    }
}
