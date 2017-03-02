package net.kodehawa.mantarobot.data.data;

import java.util.*;

public class GuildData {
	public String birthdayChannel = null;
	public String birthdayRole = null;
	public Map<String, List<String>> customCommands = new HashMap<>();
	public Map<String, UserData> users = new HashMap<>();
	public boolean localMode = false;
	public boolean customCommandsAdminOnly = false;
	public String logChannel = null;
	public String musicChannel = null;
	public Set<String> unsafeChannels = new HashSet<>();
	public String nsfwChannel = null;
	public String prefix = null;
	public Integer songDurationLimit = null;
	public Integer queueSizeLimit = null;
	public String autoRole = null;
}
