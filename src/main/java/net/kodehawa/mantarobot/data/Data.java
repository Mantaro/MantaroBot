package net.kodehawa.mantarobot.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Data {
	public static class GuildData {
		public String birthdayRole = null;
		public Map<String, List<String>> customCommands = new HashMap<>();
		public String musicChannel = null;
		public String prefix = null;
	}

	public static class UserData {
		public String birthdayDate = null;
	}

	public String defaultPrefix = "~>";
	public Map<String, GuildData> guilds = new HashMap<>();
	public Map<String, UserData> users = new HashMap<>();
}
