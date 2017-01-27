package net.kodehawa.mantarobot.data;

import java.util.ArrayList;
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
		public String birthdayRole = null;
	}

	public String defaultPrefix = "~>";
	public Map<String, GuildData> guilds = new HashMap<>();
	public List<String> splashes = new ArrayList<>();
	public Map<String, UserData> users = new HashMap<>();
	public List<String> greet = new ArrayList<>();
	public List<String> pat = new ArrayList<>();
	public List<String> bleach = new ArrayList<>();
	public List<String> hugs = new ArrayList<>();
	public List<String> tsun = new ArrayList<>();
}
