package net.kodehawa.mantarobot.data;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.commands.currency.inventory.Inventory;
import net.kodehawa.mantarobot.commands.currency.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Data {
	public static class GuildData {
		public String birthdayChannel = null;
		public String birthdayRole = null;
		public Map<String, List<String>> customCommands = new HashMap<>();
		public boolean customCommandsAdminOnly = false;
		public String logChannel = null;
		public String musicChannel = null;
		public String nsfwChannel = null;
		public String prefix = null;
		public Integer songDurationLimit = null;
	}

	public static class UserData {

		public String birthdayDate = null;
		public Map<Integer, Integer> inventory = new HashMap<>();
		public int money = 0;

		public Inventory getInventory() {
			return new Inventory(this);
		}

		public boolean addMoney(int money) {
			try {
				this.money = Math.addExact(this.money, money);
				return true;
			} catch (ArithmeticException ignored) {
				this.money = 0;
				this.getInventory().add(new ItemStack(9,1));
				return false;
			}
		}
	}

	public String defaultPrefix = "~>";
	public Map<String, GuildData> guilds = new HashMap<>();
	public Map<String, UserData> users = new HashMap<>();

	public GuildData getGuild(Guild guild, boolean isRewritable) {
		if (isRewritable) return guilds.computeIfAbsent(guild.getId(), s -> new GuildData());
		return guilds.getOrDefault(guild.getId(), new GuildData());
	}

	public String getPrefix(Guild guild) {
		return Optional.ofNullable(getGuild(guild, false).prefix).orElse(defaultPrefix);
	}

	public UserData getUser(User user, boolean isRewritable) {
		if (isRewritable) return users.computeIfAbsent(user.getId(), s -> new UserData());
		return users.getOrDefault(user.getId(), new UserData());
	}

	public UserData getUser(Member member, boolean isRewritable) {
		return getUser(member.getUser(), isRewritable);
	}
}
