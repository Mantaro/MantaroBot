package net.kodehawa.mantarobot.data;

import net.dv8tion.jda.core.entities.Guild;

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
}
