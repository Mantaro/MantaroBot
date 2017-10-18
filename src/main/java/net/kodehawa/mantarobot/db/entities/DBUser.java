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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;

import java.beans.ConstructorProperties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.rethinkdb.RethinkDB.r;
import static java.lang.System.currentTimeMillis;

@Getter
@ToString
@EqualsAndHashCode
public class DBUser implements ManagedObject {
    public static final String DB_TABLE = "users";
    private final UserData data;
    private final String id;
    private long premiumUntil;
    @ConstructorProperties({"id", "premiumUntil", "data"})
    public DBUser(String id, long premiumUntil, UserData data) {
        this.id = id;
        this.premiumUntil = premiumUntil;
        this.data = data;
    }

    public static DBUser of(String id) {
        return new DBUser(id, 0, new UserData());
    }

    @Override
    public void delete() {
        Config c = MantaroData.config().get();
        try(Connection conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect()) {
            r.table(DB_TABLE).get(getId()).delete().runNoReply(conn);
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

    public User getUser(JDA jda) {
        return jda.getUserById(getId());
    }

    @JsonIgnore
    public long getPremiumLeft() {
        return isPremium() ? this.premiumUntil - currentTimeMillis() : 0;
    }

    public DBUser incrementPremium(long milliseconds) {
        if(isPremium()) {
            this.premiumUntil += milliseconds;
        } else {
            this.premiumUntil = currentTimeMillis() + milliseconds;
        }
        return this;
    }

    @JsonIgnore
    public boolean isPremium() {
        PremiumKey key = MantaroData.db().getPremiumKey(data.getPremiumKey());
        return currentTimeMillis() < premiumUntil || (key != null && currentTimeMillis() < key.getExpiration() && key.getParsedType().equals(PremiumKey.Type.USER));
    }

    @JsonIgnore
    public PremiumKey generateAndApplyPremiumKey(int days, String owner){
        String premiumId = UUID.randomUUID().toString();
        PremiumKey newKey = new PremiumKey(premiumId, TimeUnit.DAYS.toMillis(days), currentTimeMillis() + TimeUnit.DAYS.toMillis(days), PremiumKey.Type.USER, true, owner);
        data.setPremiumKey(premiumId);
        newKey.save();
        save();
        return newKey;
    }
}
