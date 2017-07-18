package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.ExtraUserData;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import org.apache.http.util.Args;

import javax.annotation.Nonnegative;
import java.util.HashMap;
import java.util.Map;

import static com.rethinkdb.RethinkDB.r;
import static java.lang.System.currentTimeMillis;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class UserData implements ManagedObject {
	public static final String DB_TABLE = "users";
	private final ExtraUserData data;
	private final String id;
	private final Map<Integer, Integer> inventory;
	private String birthday;
	private String description;
	private long level, money, reputation, xp;
	private long premiumUntil;
	private String timezone;

	public UserData(User user) {
		this(user.getId());
	}

	public UserData(String userId) {
		this.id = userId;
		this.inventory = new HashMap<>();
		this.data = new ExtraUserData();
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

	public boolean addMoney(@Nonnegative long money) {
		if (money == 0) return false;
		try {
			this.money = Math.addExact(this.money, Args.positive(money, "money"));
			return true;
		} catch (ArithmeticException ignored) {
			this.money = 0;
			this.inventory().process(new ItemStack(Items.STAR, 1));
			return false;
		}
	}

	public void addReputation(long reputation) {
		if (reputation == 0) return;
		this.reputation += Args.positive(reputation, "reputation");
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
	public Inventory inventory() {
		return new Inventory(inventory);
	}

	@JsonIgnore
	public boolean isPremium() {
		return currentTimeMillis() < premiumUntil;
	}

	public void setMoney(long money) {
		this.money = Math.max(0, money);
	}
}
