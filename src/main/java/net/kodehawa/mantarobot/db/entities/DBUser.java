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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;

import java.beans.ConstructorProperties;

import static com.rethinkdb.RethinkDB.r;
import static java.lang.System.currentTimeMillis;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
@ToString
@EqualsAndHashCode
public class DBUser implements ManagedObject {
	public static final String DB_TABLE = "users";

	public static DBUser of(String id) {
		return new DBUser(id, 0, new UserData());
	}

	private final UserData data;
	private final String id;
	private long premiumUntil;

	@ConstructorProperties({"id", "premiumUntil", "data"})
	public DBUser(String id, long premiumUntil, UserData data) {
		this.id = id;
		this.premiumUntil = premiumUntil;
		this.data = data;
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

	@JsonIgnore
	public long getPremiumLeft() {
		return isPremium() ? this.premiumUntil - currentTimeMillis() : 0;
	}

	public User getUser(JDA jda) {
		return jda.getUserById(getId());
	}

	public DBUser incrementPremium(long milliseconds) {
		if (isPremium()) {
			this.premiumUntil += milliseconds;
		} else {
			this.premiumUntil = currentTimeMillis() + milliseconds;
		}
		return this;
	}

	@JsonIgnore
	public boolean isPremium() {
		return currentTimeMillis() < premiumUntil;
	}
}
