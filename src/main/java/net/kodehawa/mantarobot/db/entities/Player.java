package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;

import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.Map;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;
import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.Resolver.serialize;
import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.Resolver.unserialize;

public class  Player implements ManagedObject {
	public static final String DB_TABLE = "players";

	public static Player of(User user) {
		return of(user.getId());
	}

	public static Player of(Member member) {
		return of(member.getUser());
	}

	public static Player of(String userId) {
		return new Player(userId + ":g", 0L, 0L, 0L, new HashMap<>(), new PlayerData());
	}

	@Getter
	private final PlayerData data;
	@Getter
	private final String id;
	private transient Inventory inventory = new Inventory();
	@Getter
	private Long level = null;
	@Getter
	private Long money = null;
	@Getter
	@Setter
	private Long reputation = null;

	@ConstructorProperties({"id", "level", "money", "reputation", "inventory", "data"})
	public Player(String id, Long level, Long money, Long reputation, Map<Integer, Integer> inventory, PlayerData data) {
		this.id = id;
		this.level = level == null ? 0 : level;
		this.money = money == null ? 0 : money;
		this.reputation = reputation == null ? 0 : reputation;
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
			this.money = 0L;
			this.getInventory().process(new ItemStack(Items.STAR, 1));
			return false;
		}
	}

	/**
	 * Adds x amount of reputation to a player. Normally 1.
	 *
	 * @param rep how much?
	 */
	public void addReputation(long rep) {
		this.reputation += rep;
		this.setReputation(reputation);
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

	//it's 3am and i cba to replace usages of this so whatever
    @JsonIgnore
	public boolean isLocked() {
        return data.getLockedUntil() - System.currentTimeMillis() > 0;
	}

	@JsonIgnore
	public void setLocked(boolean locked) {
        data.setLockedUntil(locked ? System.currentTimeMillis() + 35000 : 0);
	}
}
