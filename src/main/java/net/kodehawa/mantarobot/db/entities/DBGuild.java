package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;

import java.beans.ConstructorProperties;

import static com.rethinkdb.RethinkDB.r;
import static java.lang.System.currentTimeMillis;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
@ToString
@EqualsAndHashCode
public class DBGuild implements ManagedObject {
	public static final String DB_TABLE = "guilds";

	public static DBGuild of(String id) {
		return new DBGuild(id, 0, new GuildData());
	}

	private final GuildData data;
	private final String id;
	private long premiumUntil;

	@ConstructorProperties({"id", "premiumUntil", "data"})
	public DBGuild(String id, long premiumUntil, GuildData data) {
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

	public Guild getGuild(JDA jda) {
		return jda.getGuildById(getId());
	}

	@JsonIgnore
	public long getPremiumLeft() {
		return isPremium() ? this.premiumUntil - currentTimeMillis() : 0;
	}

	public DBGuild incrementPremium(long milliseconds) {
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
