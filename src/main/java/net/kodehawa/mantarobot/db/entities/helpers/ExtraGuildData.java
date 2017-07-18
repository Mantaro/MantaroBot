package net.kodehawa.mantarobot.db.entities.helpers;

import lombok.Data;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.options.ConfigName;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ExtraGuildData {
    @ConfigName("autoroles")
    private HashMap<String, String> autoroles = new HashMap<>();
    private HashMap<String, List<String>> channelSpecificDisabledCommands = new HashMap<>();
    private String birthdayChannel = null;
    private String birthdayRole = null;
    private Long cases = 0L;
    @ConfigName("admincustom")
    private boolean customAdminLock = false;
    private Set<String> disabledChannels = new HashSet<>();
    private Set<String> disabledCommands = new HashSet<>();
    private String guildAutoRole = null;
    @ConfigName("customPrefix")
    private String guildCustomPrefix = null;
    @ConfigName("logChannel")
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
    private boolean reactionMenus = true;
    private int maxFairQueue = 4;
    private Set<String> linkProtectionAllowedChannels = new HashSet<>();
    private boolean antiSpam = false;
    private Set<String> spamModeChannels = new HashSet<>();
    private Set<String> slowModeChannels = new HashSet<>();
    private ConcurrentHashMap<Long, Long> mutedTimelyUsers = new ConcurrentHashMap<>();
    private Set<String> modlogBlacklistedPeople = new HashSet<>();
    private long ranPolls = 0L;
    private HashMap<String, List<Category>> channelSpecificDisabledCategories = new HashMap<>();
    private Set<Category> disabledCategories = new HashSet<>();
    private long setModTimeout = 0L;
    //TODO implement
    private int maxResultsSearch = 5;
}
