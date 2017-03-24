package net.kodehawa.mantarobot.data.db;

import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.data.entities.*;

import java.util.List;

import static com.rethinkdb.RethinkDB.r;

public class ManagedDatabase {
	/*
	TODO BUG HANDLING
	 */
	private final Connection conn;

	public ManagedDatabase(Connection conn) {
		this.conn = conn;
	}

	public List<CustomCommand> getCustomCommands() {
		Cursor<CustomCommand> c = r.table(CustomCommand.DB_TABLE).run(conn, CustomCommand.class);
		return c.toList();
	}

	public Player getGlobalPlayer(String userId) {
		String id = userId + ":g";
		Player player = r.table(Player.DB_TABLE).get(id).run(conn, Player.class);
		return player == null ? Player.of(userId) : player;
	}

	public Player getGlobalPlayer(User user) {
		return getGlobalPlayer(user.getId());
	}

	public Player getGlobalPlayer(Member member) {
		return getGlobalPlayer(member.getUser());
	}

	public DBGuild getGuild(String guildId) {
		DBGuild guild = r.table(DBGuild.DB_TABLE).get(guildId).run(conn, DBGuild.class);
		return guild == null ? DBGuild.of(guildId) : guild;
	}

	public DBGuild getGuild(Guild guild) {
		return getGuild(guild.getId());
	}

	public DBGuild getGuild(Member member) {
		return getGuild(member.getGuild());
	}

	public Player getPlayer(String userId, String guildId) {
		boolean local = getGuild(guildId).getData().isRpgLocalMode();
		String id = userId + ":" + (local ? guildId : "g");
		Player player = r.table(Player.DB_TABLE).get(id).run(conn, Player.class);
		return player == null ? Player.of(userId, guildId) : player;
	}

	public Player getPlayer(User user, Guild guild) {
		return getPlayer(user.getId(), guild.getId());
	}

	public Player getPlayer(Member member) {
		return getPlayer(member.getUser(), member.getGuild());
	}

	public List<PremiumKey> getPremiumKeys() {
		Cursor<PremiumKey> c = r.table(PremiumKey.DB_TABLE).run(conn, PremiumKey.class);
		return c.toList();
	}

	public List<Quote> getQuotes(String guildId) {
		String pattern = "^" + guildId + ":";
		Cursor<Quote> c = r.table(Quote.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, Quote.class);
		return c.toList();
	}

	public List<Quote> getQuotes(Guild guild) {
		return getQuotes(guild.getId());
	}

	public List<Quote> getQuotes(DBGuild guild) {
		return getQuotes(guild.getId());
	}

	public DBUser getUser(String userId) {
		DBUser user = r.table(DBUser.DB_TABLE).get(userId).run(conn, DBUser.class);
		return user == null ? DBUser.of(userId) : user;
	}

	public DBUser getUser(User user) {
		return getUser(user.getId());
	}

	public DBUser getUser(Member member) {
		return getUser(member.getUser());
	}
}
