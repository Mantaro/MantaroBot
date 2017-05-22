package net.kodehawa.mantarobot.data.entities.helpers;

import lombok.Data;

import java.util.*;

@Data
public class GuildData {
	private HashMap<String, String> autoroles = new HashMap<>();
	private HashMap<String, List<String>> channelSpecificDisabledCommands = new HashMap<>();
	private String birthdayChannel = null;
	private String birthdayRole = null;
	private Long cases = 0L;
	private boolean customAdminLock = false;
	private Set<String> disabledChannels = new HashSet<>();
	private Set<String> disabledCommands = new HashSet<>();
	private String guildAutoRole = null;
	private String guildCustomPrefix = null;
	private String guildLogChannel = null;
	private Set<String> guildUnsafeChannels = new HashSet<>();
	private String joinMessage = null;
	private String leaveMessage = null;
	private Set<String> logExcludedChannels = new HashSet<>();
	private String logJoinLeaveChannel = null;
	private String musicChannel = null;
	private Long musicQueueSizeLimit = null;
	private Long musicSongDurationLimit = null;
	private String mutedRole = null;
	private Long quoteLastId = 0L;
	private boolean rpgLocalMode = false;
	private boolean rpgDevaluation = true;
	private boolean linkProtection = false;
	private boolean slowMode = false;
	private List<String> disabledUsers = new ArrayList<>();
	private boolean noMentionsAction = false;
	private boolean musicAnnounce = true;
	private int timeDisplay = 0; //0 = 24h, 1 = 12h
	private ArrayList<String> rolesBlockedFromCommands = new ArrayList<>();

	//TODO implement
	private int maxFairQueue = 4;
	private HashMap<String, List<String>> channelSpecificDisabledCategories = new HashMap<>();
	private Set<String> disabledCategories = new HashSet<>();
}
