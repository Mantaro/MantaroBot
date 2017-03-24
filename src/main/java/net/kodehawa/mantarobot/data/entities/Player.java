package net.kodehawa.mantarobot.data.entities;

import com.google.gson.JsonParser;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.data.db.ManagedObject;
import net.kodehawa.mantarobot.data.entities.helpers.Inventory;
import org.apache.commons.lang3.tuple.Pair;

import java.beans.Transient;
import java.util.stream.Collectors;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;
import static net.kodehawa.mantarobot.data.MantaroData.db;
import static net.kodehawa.mantarobot.data.entities.helpers.Inventory.Resolver.serialize;
import static net.kodehawa.mantarobot.data.entities.helpers.Inventory.Resolver.unserialize;
import static net.kodehawa.mantarobot.utils.data.GsonDataManager.gson;

public class Player implements ManagedObject {
	public static final String DB_TABLE = "players";

	public static Player of(User user) {
		return of(user.getId());
	}

	public static Player of(Member member) {
		return of(member.getUser().getId(), member.getGuild().getId());
	}

	public static Player of(String userId) {
		return new Player(userId + ":g", 250, 0, 0, 100, "");
	}

	public static Player of(String userId, String guildId) {
		boolean local = db().getGuild(guildId).getData().isRpgLocalMode();
		return new Player(userId + ":" + (local ? guildId : "g"), 250, 0, 0, 100, "");
	}

	@Getter
	private final String id;
	@Getter
	private int health = 250;
	private transient Inventory inventory = new Inventory();
	@Getter
	private long money = 0;
	@Getter
	private int reputation = 0;
	@Getter
	private int stamina = 100;

	public Player(String id, int health, long money, int reputation, int stamina, String inventory) {
		this.id = id;
		this.health = health;
		this.money = money;
		this.reputation = reputation;
		this.stamina = stamina;

		this.inventory.replaceWith(
			unserialize(
				new JsonParser().parse('{' + inventory + '}')
					.getAsJsonObject().entrySet().stream()
					.map(entry -> Pair.of(Integer.parseInt(entry.getKey()), entry.getValue().getAsInt()))
					.collect(Collectors.toMap(Pair::getKey, Pair::getValue))
			)
		);
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

	@Transient
	public String getGuildId() {
		return getId().split(":")[1];
	}

	public String getInventory() {
		String s = gson(false).toJson(serialize(inventory.asList()));
		return s.substring(1, s.length() - 1);
	}

	@Transient
	public String getUserId() {
		return getId().split(":")[0];
	}

	public Inventory inventory() {
		return inventory;
	}

	@Transient
	public boolean isGlobal() {
		return getGuildId().equals("g");
	}

	public Player setHealth(int health) {
		this.health = health < 0 ? 0 : health;
		return this;
	}

	public Player setMoney(long money) {
		this.money = money < 0 ? 0 : money;
		return this;
	}

	public Player setReputation(int reputation) {
		this.reputation = reputation < 0 ? 0 : reputation;
		return this;
	}

	public Player setStamina(int stamina) {
		this.stamina = stamina < 0 ? 0 : stamina;
		return this;
	}
}
