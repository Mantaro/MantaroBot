package net.kodehawa.mantarobot.data.entities;

import com.google.gson.JsonParser;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.commands.rpg.item.ItemStack;
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
		return new Player(userId + ":g", 0L, 250L, 0L, 0L, 100L, "");
	}

	public static Player of(String userId, String guildId) {
		boolean local = db().getGuild(guildId).getData().getRpgLocalMode();
		return new Player(userId + ":" + (local ? guildId : "g"), 0L, 250L, 0L, 0L, 100L, "");
	}

	@Getter
	private final String id;
	@Getter
	private long health = 250;
	@Getter
	private transient long maxHealth = 250;
	@Getter
	private transient long maxStamina = 100;
	@Getter
	private long money = 0;
	@Getter
	private transient boolean processing;
	@Getter
	private long reputation = 0;
	@Getter
	private long stamina = 100;
	@Getter
	private long level = 0;

	private transient Inventory inventory = new Inventory();

	public Player(String id, Long level, Long health, Long money, Long reputation, Long stamina, String inventory) {
		this.id = id;
		this.health = health;
		this.level = level;
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

	/**
	 * Adds x amount of health to the entity. Used in recovery and potion process.
	 *
	 * @param amount How much?
	 * @return Did it pass through? Please? (aka, did it not overflow?)
	 */
	public boolean addHealth(long amount) {
		if (getHealth() + amount < 0 || getHealth() + amount > getMaxHealth()) return false;
		setHealth(getHealth() + amount);
		return true;
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
			this.inventory().process(new ItemStack(9, 1));
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

	/**
	 * Adds x amount of stamina to the entity. Used in recovery and potion process.
	 *
	 * @param amount How much?
	 * @return Did it pass through? Please? (aka, did it not overflow?)
	 */
	public boolean addStamina(long amount) {
		if (getStamina() + amount < 0 || getStamina() + amount > getMaxStamina()) return false;
		setStamina(getStamina() + amount);
		return true;
	}

	/**
	 * Makes a player a little bit sicker. Normally the result of sick-inducing activities like mining.
	 *
	 * @param amount how much am I gonna consume?
	 * @return if it's more than zero.
	 */
	public boolean consumeHealth(int amount) {
		return this.health - amount >= 0 && addHealth(-amount);
	}

	/**
	 * Makes a player tired. If stamina reaches a critical point, you cannot do much action in the RPG.
	 *
	 * @param amount how much am I gonna consume?
	 * @return if it's more than zero.
	 */
	public boolean consumeStamina(int amount) {
		return this.stamina - amount >= 0 && addStamina(-amount);
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

	public Player setHealth(long health) {
		this.health = health < 0 ? 0 : health;
		return this;
	}

	public Player setMoney(long money) {
		this.money = money < 0 ? 0 : money;
		return this;
	}

	public Player setLevel(long level){
		this.level = level;
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

	public Player setStamina(long stamina) {
		this.stamina = stamina < 0 ? 0 : stamina;
		return this;
	}
}
