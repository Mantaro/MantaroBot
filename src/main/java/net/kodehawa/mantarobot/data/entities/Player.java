package net.kodehawa.mantarobot.data.entities;

import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.commands.rpg.item.ItemStack;
import net.kodehawa.mantarobot.data.db.ManagedObject;
import net.kodehawa.mantarobot.data.entities.helpers.Inventory;
import net.kodehawa.mantarobot.data.entities.helpers.PlayerData;
import org.apache.commons.lang3.tuple.Pair;

import java.beans.ConstructorProperties;
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
		return new Player(userId + ":g", 0L, 250L, 0L, "", new PlayerData());
	}

	public static Player of(String userId, String guildId) {
		boolean local = db().getGuild(guildId).getData().getRpgLocalMode();
		return new Player(userId + ":" + (local ? guildId : "g"), 0L, 0L, 0L, "", new PlayerData());
	}

	@Getter
	private final String id;
	@Getter
	private long money = 0;
	@Getter @Setter
	private long reputation = 0;
	@Getter
	private long level = 0;
	@Getter
	private final PlayerData data;

	private transient Inventory inventory = new Inventory();

	@ConstructorProperties({"id", "level", "money", "reputation", "inventory", "data"})
	public Player(String id, Long level, Long money, Long reputation, String inventory, PlayerData data) {
		this.id = id;
		this.level = level;
		this.money = money;
		this.reputation = reputation;
		this.data = data;

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
		this.setReputation(reputation);
		return true;
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

	public Player setMoney(long money) {
		this.money = money < 0 ? 0 : money;
		return this;
	}

	public Player setLevel(long level){
		this.level = level;
		return this;
	}
}
