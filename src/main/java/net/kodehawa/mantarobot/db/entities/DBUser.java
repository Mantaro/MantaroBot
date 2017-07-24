package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
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
		r.table(DB_TABLE).get(getId()).delete().run(conn());
	}

	@Override
	public void save() {
		r.table(DB_TABLE).insert(this)
			.optArg("conflict", "replace")
			.run(conn());
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
