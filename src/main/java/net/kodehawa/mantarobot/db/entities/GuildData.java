package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.dv8tion.jda.core.entities.Guild;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.ExtraGuildData;

import static com.rethinkdb.RethinkDB.r;
import static java.lang.System.currentTimeMillis;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class GuildData implements ManagedObject {
	public static final String DB_TABLE = "guilds";
	private final ExtraGuildData data;
	private final String id;
	private long premiumUntil;

	public GuildData(Guild guild) {
		this(guild.getId());
	}

	public GuildData(String guildId) {
		this.id = guildId;
		this.data = new ExtraGuildData();
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

	public void incrementPremium(long milliseconds) {
		if (isPremium()) {
			this.premiumUntil += milliseconds;
		} else {
			this.premiumUntil = currentTimeMillis() + milliseconds;
		}
	}

	@JsonIgnore
	public boolean isPremium() {
		return currentTimeMillis() < premiumUntil;
	}
}
