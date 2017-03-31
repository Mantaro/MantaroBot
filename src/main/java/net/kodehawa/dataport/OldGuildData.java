package net.kodehawa.dataport;

import java.util.*;

public class OldGuildData {
	public String autoRole = null;
	public String birthdayChannel = null;
	public String birthdayRole = null;
	public Map<String, List<String>> customCommands = new HashMap<>();
	public boolean customCommandsAdminOnly = false;
	public boolean devaluation = true;
	public boolean localMode = false;
	public String logChannel = null;
	public String musicChannel = null;
	public String prefix = null;
	public Integer queueSizeLimit = null;
	public Integer songDurationLimit = null;
	public Set<String> unsafeChannels = new HashSet<>();
	public Map<String, OldPlayerData> users = new HashMap<>();
}
