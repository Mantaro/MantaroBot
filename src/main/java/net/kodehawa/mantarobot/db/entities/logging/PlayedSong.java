package net.kodehawa.mantarobot.db.entities.logging;

import lombok.Data;
import net.kodehawa.mantarobot.db.ManagedObject;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Data
public class PlayedSong implements ManagedObject {
	public static final String DB_TABLE = "playedSongs";

	public static void log(String songId) {
		PlayedSong song = new PlayedSong(songId);
		song.setTimesPlayed(1);

		r.table(DB_TABLE).insert(song)
			.optArg("conflict", (id, arg1, arg2) -> {
				arg1.g("timesPlayed").add(1);
				return arg1;
			})
			.runNoReply(conn());
	}

	private final String id;
	private int timesPlayed;

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
