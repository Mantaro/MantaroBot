package net.kodehawa.mantarobot.data.data;

import net.kodehawa.mantarobot.commands.rpg.entity.player.EntityPlayer;

import java.util.*;

public class GuildData {
	public String autoRole = null;
	public String birthdayChannel = null;
	public String birthdayRole = null;
	public Map<String, List<String>> customCommands = new HashMap<>();
	public boolean customCommandsAdminOnly = false;
	public boolean devaluation = true; //TODO HOOK THIS FIELD IN OPTS
	public boolean localMode = false;
	public String logChannel = null;
	public String musicChannel = null;
	public String nsfwChannel = null;
	public String prefix = null;
	public Integer queueSizeLimit = null;
	public Integer songDurationLimit = null;
	public Set<String> unsafeChannels = new HashSet<>();
	public Map<String, EntityPlayer> users = new HashMap<>();
}
