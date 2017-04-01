package net.kodehawa.mantarobot.data.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import net.kodehawa.mantarobot.data.db.ManagedObject;
import net.kodehawa.mantarobot.utils.URLEncoding;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.stream.Collectors;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
public class CustomCommand implements ManagedObject {
	public static final String DB_TABLE = "commands";

	public static CustomCommand of(String guildId, String cmdName, List<String> responses) {
		return new CustomCommand(guildId + ":" + cmdName, responses.stream().map(URLEncoding::encode).collect(Collectors.toList()));
	}

	private final String id;
	private final List<String> values;

	@ConstructorProperties({"id", "values"})
	public CustomCommand(String id, List<String> values) {
		this.id = id;
		this.values = values.stream().map(URLEncoding::decode).collect(Collectors.toList());
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

	@JsonProperty("values")
	public List<String> encodedValues() {
		return values.stream().map(URLEncoding::encode).collect(Collectors.toList());
	}

	@JsonIgnore
	public String getGuildId() {
		return getId().split(":", 2)[0];
	}

	@JsonIgnore
	public String getName() {
		return getId().split(":", 2)[1];
	}

	@JsonIgnore
	public List<String> getValues() {
		return values;
	}
}
