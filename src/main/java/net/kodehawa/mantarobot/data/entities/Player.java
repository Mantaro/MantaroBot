package net.kodehawa.mantarobot.data.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.commands.rpg.item.ItemStack;
import net.kodehawa.mantarobot.data.db.ManagedObject;
import net.kodehawa.mantarobot.data.entities.helpers.Inventory;
import net.kodehawa.mantarobot.data.entities.helpers.PlayerData;

import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.Map;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;
import static net.kodehawa.mantarobot.data.MantaroData.db;
import static net.kodehawa.mantarobot.data.entities.helpers.Inventory.Resolver.serialize;
import static net.kodehawa.mantarobot.data.entities.helpers.Inventory.Resolver.unserialize;

public class Player implements ManagedObject {
	public static final String DB_TABLE = "players";

	public static Player of(User user) {
		return of(user.getId());
	}

	public static Player of(Member member) {
		return of(member.getUser().getId(), member.getGuild().getId());
	}

	public static Player of(String userId) {
		return new Player(userId + ":g", 0L, 250L, 0L, new HashMap<>(), new PlayerData());
	}

	public static Player of(String userId, String guildId) {
		boolean local = db().getGuild(guildId).getData().isRpgLocalMode();
		return new Player(userId + ":" + (local ? guildId : "g"), 0L, 0L, 0L, new HashMap<>(), new PlayerData());
	}

	@Getter
	private final PlayerData data;
	@Getter
	private final String id;
	private transient Inventory inventory = new Inventory();
	@Getter
	private long level = 0;
	@Getter
	private long money = 0;
	@Getter
	private transient boolean processing;
	@Getter
	private long reputation = 0;

	@ConstructorProperties({"id", "level", "money", "reputation", "inventory", "data"})
	public Player(String id, long level, long money, long reputation, Map<Integer, Integer> inventory, PlayerData data) {
		this.id = id;
		this.level = level;
		this.money = money;
		this.reputation = reputation;
		this.data = data;
		this.inventory.replaceWith(unserialize(inventory));
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

	/**
	 * Adds x amount of money from the player.
	 *
	 * @param money How much?
	 * @return pls dont overflow.
	 */
	public boolean addMoney(long money) {
		if (money < 0) return false;
		try {
			this.money = Math.addExact(this.money, money);
			return true;
		} catch (ArithmeticException ignored) {
			this.money = 0;
			this.getInventory().process(new ItemStack(9, 1));
			return false;
		}
	}

	/**
	 * Adds x amount of reputation to a player. Normally 1.
	 *
	 * @param rep how much?
	 * @return are you less than 400?
	 */
	public boolean addReputation(long rep) {
		if (this.reputation + rep > 4000) return false;
		this.reputation += rep;
		return true;
	}

	@JsonIgnore
	public String getGuildId() {
		return getId().split(":")[1];
	}

	@JsonIgnore
	public Inventory getInventory() {
		return inventory;
	}

	@JsonIgnore
	public String getUserId() {
		return getId().split(":")[0];
	}

	@JsonIgnore
	public boolean isGlobal() {
		return getGuildId().equals("g");
	}

	@JsonProperty("inventory")
	public Map<Integer, Integer> rawInventory() {
		return serialize(inventory.asList());
	}

	/**
	 * Removes x amount of money from the player. Only goes though if money removed sums more than zero (avoids negative values).
	 *
	 * @param money How much?
	 * @return well if the sum negative it won't pass through, you little fucker.
	 */
	public boolean removeMoney(long money) {
		if (this.money - money < 0) return false;
		this.money -= money;
		return true;
	}

	public Player setLevel(long level) {
		this.level = level;
		return this;
	}

	public Player setMoney(long money) {
		this.money = money < 0 ? 0 : money;
		return this;
	}

	/**
	 * Set the preparation for receive data.
	 * This is done to prevent it to receive data twice and also to prevent duplication of data.
	 *
	 * @param processing is it receiving data?
	 */
	public void setProcessing(boolean processing) {
		this.processing = processing;
	}

	public Player setReputation(int reputation) {
		this.reputation = reputation < 0 ? 0 : reputation;
		return this;
	}
}
