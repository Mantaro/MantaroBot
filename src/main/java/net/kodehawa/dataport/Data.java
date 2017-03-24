package net.kodehawa.dataport;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Data {
	public List<String> blacklistedGuilds = new CopyOnWriteArrayList<>();
	public List<String> blacklistedUsers = new CopyOnWriteArrayList<>();
	public String defaultPrefix = "~>";
	public Map<String, GuildData> guilds = new ConcurrentHashMap<>();
	public Map<String, OldGlobalPlayerData> users = new ConcurrentHashMap<>();
}
