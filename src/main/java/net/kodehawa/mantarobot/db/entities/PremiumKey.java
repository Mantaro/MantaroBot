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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rethinkdb.net.Connection;
import lombok.Getter;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedObject;

import java.beans.ConstructorProperties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.rethinkdb.RethinkDB.r;
import static java.lang.System.currentTimeMillis;

@Getter
public class PremiumKey implements ManagedObject {
    public static final String DB_TABLE = "keys";
    private long duration;
    private long expiration;
    private String id;
    private int type;
    private boolean enabled;
    private String owner;

    public enum Type {
        MASTER, USER, GUILD
    }

    @ConstructorProperties({"id", "duration", "expiration", "type", "enabled", "owner"})
    public PremiumKey(String id, long duration, long expiration, Type type, boolean enabled, String owner) {
        this.id = id;
        this.duration = duration;
        this.expiration = expiration;
        this.type = type.ordinal();
        this.enabled = enabled;
        this.owner = owner;
    }

    @JsonIgnore
    public PremiumKey() {}

    @Override
    public void delete() {
        Config c = MantaroData.config().get();
        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            r.table(DB_TABLE).get(id).delete().runNoReply(conn);
        }
    }

    @Override
    public void save() {
        Config c = MantaroData.config().get();
        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            r.table(DB_TABLE).insert(this)
                    .optArg("conflict", "replace")
                    .runNoReply(conn);
        }
    }

    @JsonIgnore
    public static PremiumKey generatePremiumKey(String owner, Type type){
        String premiumId = UUID.randomUUID().toString();
        PremiumKey newKey = new PremiumKey(premiumId, -1, -1, type, false, owner);
        newKey.save();
        return newKey;
    }

    @JsonIgnore
    public Type getParsedType(){
        return Type.values()[type];
    }

    @JsonIgnore
    public long getDurationDays(){
        return TimeUnit.MILLISECONDS.toDays(duration);
    }

    @JsonIgnore
    public long validFor(){
        return TimeUnit.MILLISECONDS.toDays(getExpiration() - currentTimeMillis());
    }

    @JsonIgnore
    public long validForMs(){
        return getExpiration() - currentTimeMillis();
    }

    @JsonIgnore
    public void activate(int days){
        this.enabled = true;
        this.duration = TimeUnit.DAYS.toMillis(days);
        this.expiration = currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
        save();
    }
}
