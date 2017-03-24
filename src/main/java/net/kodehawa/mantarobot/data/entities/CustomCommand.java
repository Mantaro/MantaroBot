package net.kodehawa.mantarobot.data.entities;

import lombok.Getter;
import net.kodehawa.mantarobot.data.db.ManagedObject;

import java.util.ArrayList;
import java.util.List;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
public class CustomCommand implements ManagedObject {
	public static final String DB_TABLE = "commands";

	public static CustomCommand of(String guildId, String cmdName, List<String> responses) {
		return new CustomCommand(guildId + ":" + cmdName, responses);
	}

	private final String id;
	private final List<String> values;

	public CustomCommand(String id, List<String> values) {
		this.id = id;
		this.values = new ArrayList<>(values);
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

	public String getGuildId() {
		return getId().split(":", 2)[0];
	}

	public String getName() {
		return getId().split(":", 2)[1];
	}
}
