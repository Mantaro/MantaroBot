package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.kodehawa.mantarobot.commands.moderation.WarnAction;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.data.annotations.ConfigName;
import net.kodehawa.mantarobot.data.annotations.HiddenConfig;
import net.kodehawa.mantarobot.db.ManagedObject;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuildDatabase implements ManagedObject {
    @BsonIgnore
    public static final String DB_TABLE = "guilds";
    @BsonId
    private String id;

    public GuildDatabase() {}

    public GuildDatabase(String id) {
        this.id = id;
    }

    @HiddenConfig
    private long premiumUntil;
    @ConfigName("Autoroles")
    private HashMap<String, String> autoroles = new HashMap<>();
    @ConfigName("Birthday Announcer Channel")
    private String birthdayChannel = null;
    @ConfigName("Birthday Announcer Role")
    private String birthdayRole = null;
    @ConfigName("Mod action counter")
    private long cases = 0L;
    @ConfigName("Categories disabled in channels")
    private HashMap<String, List<CommandCategory>> channelSpecificDisabledCategories = new HashMap<>();
    @ConfigName("Commands disabled in channels")
    private HashMap<String, List<String>> channelSpecificDisabledCommands = new HashMap<>();

    @ConfigName("Disabled Categories")
    private Set<CommandCategory> disabledCategories = new HashSet<>();
    @ConfigName("Disabled Channels")
    private Set<String> disabledChannels = new HashSet<>();
    @ConfigName("Disabled Commands")
    private Set<String> disabledCommands = new HashSet<>();
    @ConfigName("Disabled Roles")
    private Set<String> disabledRoles = new HashSet<>();
    @ConfigName("Disabled Members")
    private List<String> disabledUsers = new ArrayList<>();
    @ConfigName("Server auto-role id")
    private String guildAutoRole = null;
    @ConfigName("Server custom prefix")
    private String guildCustomPrefix = null;
    @ConfigName("Server modlog channel")
    private String guildLogChannel = null;

    @ConfigName("Server join message")
    private String joinMessage = null;
    @ConfigName("Server leave message")
    private String leaveMessage = null;

    @ConfigName("Channels (ids): don't log changes for modlog")
    private Set<String> logExcludedChannels = new HashSet<>();
    @ConfigName("Channel (id): log joins")
    private String logJoinLeaveChannel = null;
    @ConfigName("Fair Queue Limit")
    private int maxFairQueue = 4;

    @ConfigName("Modlog: Ignored people")
    private Set<String> modlogBlacklistedPeople = new HashSet<>();
    @ConfigName("Music Announce")
    private boolean musicAnnounce = true;
    @ConfigName("Channel (id): lock to specific music channel")
    private String musicChannel = null;

    @ConfigName("Role for the mute command")
    private String mutedRole = null;
    @ConfigName("Users awaiting for a mute expire")
    private ConcurrentHashMap<Long, Long> mutedTimelyUsers = new ConcurrentHashMap<>();
    @ConfigName("Action commands ping (for giver)")
    private boolean noMentionsAction = false;
    @HiddenConfig // We do not need to show this
    private String premiumKey;

    @ConfigName("Amount of polls ran")
    private long ranPolls = 0L;

    @ConfigName("Roles that can't use commands")
    private ArrayList<String> rolesBlockedFromCommands = new ArrayList<>();

    @ConfigName("Mute default timeout")
    private long setModTimeout = 0L;

    @ConfigName("How will Mantaro display time")
    private int timeDisplay = 0; //0 = 24h, 1 = 12h

    @HiddenConfig
    private Map<Long, WarnAction> warnActions = new HashMap<>();
    @HiddenConfig // not implemented, see above
    private Map<String, Long> warnCount = new HashMap<>();

    @HiddenConfig // we do not need to show this to the user
    private String gameTimeoutExpectedAt;
    @ConfigName("Ignore bots: welcome message")
    private boolean ignoreBotsWelcomeMessage = false;
    @ConfigName("Ignore bots: autorole")
    private boolean ignoreBotsAutoRole = false;
    @HiddenConfig //removed in 6.0.10
    private boolean enabledLevelUpMessages = false;
    @HiddenConfig //removed in 6.0.10
    private String levelUpChannel = null;
    @HiddenConfig //removed in 6.0.10
    private String levelUpMessage = null;
    @ConfigName("Blacklisted image tags")
    private Set<String> blackListedImageTags = new HashSet<>();
    @ConfigName("Channel (id): Join message channel")
    private String logJoinChannel = null;
    @ConfigName("Channel (id): Leave message channel")
    private String logLeaveChannel = null;

    @ConfigName("Link Protection ignore (users)")
    private Set<String> linkProtectionAllowedUsers = new HashSet<>();
    @ConfigName("Disabled Categories for Role (id)")
    private HashMap<String, List<CommandCategory>> roleSpecificDisabledCategories = new HashMap<>();
    @ConfigName("Disabled Commands for Role (id)")
    private HashMap<String, List<String>> roleSpecificDisabledCommands = new HashMap<>();
    @ConfigName("Server language")
    private String lang = "en_US";
    @ConfigName("Music vote toggle")
    private boolean musicVote = true;
    @ConfigName("Extra join messages")
    private List<String> extraJoinMessages = new ArrayList<>();
    @ConfigName("Extra leave messages")
    private List<String> extraLeaveMessages = new ArrayList<>();

    @ConfigName("Birthday message")
    private String birthdayMessage = null;
    @ConfigName("CCS lock: can it be used by only admins?")
    private boolean customAdminLockNew = true;
    @ConfigName("Who's this MP linked to")
    private String mpLinkedTo = null; //user id of the person who linked MP to a specific server (used for patreon checks)
    @ConfigName("Modlog: blacklisted words")
    private List<String> modLogBlacklistWords = new ArrayList<>();
    @ConfigName("Autoroles categories")
    private Map<String, List<String>> autoroleCategories = new HashMap<>();

    //mod logs customization
    @ConfigName("Edit message on mod logs")
    private String editMessageLog;
    @ConfigName("Delete message on mod logs")
    private String deleteMessageLog;
    @ConfigName("Ban message on mod logs")
    private String bannedMemberLog;
    @ConfigName("Unban message on mod logs")
    private String unbannedMemberLog;
    @ConfigName("Kick message on mod logs")
    private String kickedMemberLog;

    @ConfigName("Disabled command warning display")
    private boolean commandWarningDisplay = false;

    @ConfigName("Has received greet message")
    @JsonProperty("hasReceivedGreet")
    private boolean hasReceivedGreet = false;

    @ConfigName("People blocked from the birthday logging on this server.")
    private List<String> birthdayBlockedIds = new ArrayList<>();

    @ConfigName("Disabled game lobby/multiple")
    @JsonProperty("gameMultipleDisabled")
    private boolean gameMultipleDisabled = false;

    @ConfigName("Timezone of logs")
    private String logTimezone;
    @SuppressWarnings("CanBeFinal")
    @HiddenConfig // It's not unused, but this hides it from opts check data
    private List<String> allowedBirthdays = new ArrayList<>();

    @HiddenConfig // It's not unused, but this hides it from opts check data
    private boolean notifiedFromBirthdayChange = false;

    @ConfigName("Disable questionable/explicit imageboard search")
    private boolean disableExplicit = false;

    @ConfigName("The custom DJ role.")
    private String djRoleId;

    public String getId() {
        return id;
    }

    @NotNull
    @BsonIgnore
    @Override
    public String getTableName() {
        return DB_TABLE;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getPremiumUntil() {
        return premiumUntil;
    }

    public void setPremiumUntil(long premiumUntil) {
        this.premiumUntil = premiumUntil;
    }

    public HashMap<String, String> getAutoroles() {
        return autoroles;
    }

    public void setAutoroles(HashMap<String, String> autoroles) {
        this.autoroles = autoroles;
    }

    public String getBirthdayChannel() {
        return birthdayChannel;
    }

    public void setBirthdayChannel(String birthdayChannel) {
        this.birthdayChannel = birthdayChannel;
    }

    public String getBirthdayRole() {
        return birthdayRole;
    }

    public void setBirthdayRole(String birthdayRole) {
        this.birthdayRole = birthdayRole;
    }

    public long getCases() {
        return cases;
    }

    public void setCases(long cases) {
        this.cases = cases;
    }

    public HashMap<String, List<CommandCategory>> getChannelSpecificDisabledCategories() {
        return channelSpecificDisabledCategories;
    }

    public void setChannelSpecificDisabledCategories(HashMap<String, List<CommandCategory>> channelSpecificDisabledCategories) {
        this.channelSpecificDisabledCategories = channelSpecificDisabledCategories;
    }

    public HashMap<String, List<String>> getChannelSpecificDisabledCommands() {
        return channelSpecificDisabledCommands;
    }

    public void setChannelSpecificDisabledCommands(HashMap<String, List<String>> channelSpecificDisabledCommands) {
        this.channelSpecificDisabledCommands = channelSpecificDisabledCommands;
    }

    public Set<CommandCategory> getDisabledCategories() {
        return disabledCategories;
    }

    public void setDisabledCategories(Set<CommandCategory> disabledCategories) {
        this.disabledCategories = disabledCategories;
    }

    public Set<String> getDisabledChannels() {
        return disabledChannels;
    }

    public void setDisabledChannels(Set<String> disabledChannels) {
        this.disabledChannels = disabledChannels;
    }

    public Set<String> getDisabledCommands() {
        return disabledCommands;
    }

    public void setDisabledCommands(Set<String> disabledCommands) {
        this.disabledCommands = disabledCommands;
    }

    public Set<String> getDisabledRoles() {
        return disabledRoles;
    }

    public void setDisabledRoles(Set<String> disabledRoles) {
        this.disabledRoles = disabledRoles;
    }

    public List<String> getDisabledUsers() {
        return disabledUsers;
    }

    public void setDisabledUsers(List<String> disabledUsers) {
        this.disabledUsers = disabledUsers;
    }

    public String getGuildAutoRole() {
        return guildAutoRole;
    }

    public void setGuildAutoRole(String guildAutoRole) {
        this.guildAutoRole = guildAutoRole;
    }

    public String getGuildCustomPrefix() {
        return guildCustomPrefix;
    }

    public void setGuildCustomPrefix(String guildCustomPrefix) {
        this.guildCustomPrefix = guildCustomPrefix;
    }

    public String getGuildLogChannel() {
        return guildLogChannel;
    }

    public void setGuildLogChannel(String guildLogChannel) {
        this.guildLogChannel = guildLogChannel;
    }

    public String getJoinMessage() {
        return joinMessage;
    }

    public void setJoinMessage(String joinMessage) {
        this.joinMessage = joinMessage;
    }

    public String getLeaveMessage() {
        return leaveMessage;
    }

    public void setLeaveMessage(String leaveMessage) {
        this.leaveMessage = leaveMessage;
    }

    public Set<String> getLogExcludedChannels() {
        return logExcludedChannels;
    }

    public void setLogExcludedChannels(Set<String> logExcludedChannels) {
        this.logExcludedChannels = logExcludedChannels;
    }

    public String getLogJoinLeaveChannel() {
        return logJoinLeaveChannel;
    }

    public void setLogJoinLeaveChannel(String logJoinLeaveChannel) {
        this.logJoinLeaveChannel = logJoinLeaveChannel;
    }

    public int getMaxFairQueue() {
        return maxFairQueue;
    }

    public void setMaxFairQueue(int maxFairQueue) {
        this.maxFairQueue = maxFairQueue;
    }

    public Set<String> getModlogBlacklistedPeople() {
        return modlogBlacklistedPeople;
    }

    public void setModlogBlacklistedPeople(Set<String> modlogBlacklistedPeople) {
        this.modlogBlacklistedPeople = modlogBlacklistedPeople;
    }

    public boolean isMusicAnnounce() {
        return musicAnnounce;
    }

    public void setMusicAnnounce(boolean musicAnnounce) {
        this.musicAnnounce = musicAnnounce;
    }

    public String getMusicChannel() {
        return musicChannel;
    }

    public void setMusicChannel(String musicChannel) {
        this.musicChannel = musicChannel;
    }

    public String getMutedRole() {
        return mutedRole;
    }

    public void setMutedRole(String mutedRole) {
        this.mutedRole = mutedRole;
    }

    public ConcurrentHashMap<Long, Long> getMutedTimelyUsers() {
        return mutedTimelyUsers;
    }

    public void setMutedTimelyUsers(ConcurrentHashMap<Long, Long> mutedTimelyUsers) {
        this.mutedTimelyUsers = mutedTimelyUsers;
    }

    public boolean isNoMentionsAction() {
        return noMentionsAction;
    }

    public void setNoMentionsAction(boolean noMentionsAction) {
        this.noMentionsAction = noMentionsAction;
    }

    public String getPremiumKey() {
        return premiumKey;
    }

    public void setPremiumKey(String premiumKey) {
        this.premiumKey = premiumKey;
    }

    public long getRanPolls() {
        return ranPolls;
    }

    public void setRanPolls(long ranPolls) {
        this.ranPolls = ranPolls;
    }

    public ArrayList<String> getRolesBlockedFromCommands() {
        return rolesBlockedFromCommands;
    }

    public void setRolesBlockedFromCommands(ArrayList<String> rolesBlockedFromCommands) {
        this.rolesBlockedFromCommands = rolesBlockedFromCommands;
    }

    public long getSetModTimeout() {
        return setModTimeout;
    }

    public void setSetModTimeout(long setModTimeout) {
        this.setModTimeout = setModTimeout;
    }

    public int getTimeDisplay() {
        return timeDisplay;
    }

    public void setTimeDisplay(int timeDisplay) {
        this.timeDisplay = timeDisplay;
    }

    public Map<Long, WarnAction> getWarnActions() {
        return warnActions;
    }

    public void setWarnActions(Map<Long, WarnAction> warnActions) {
        this.warnActions = warnActions;
    }

    public Map<String, Long> getWarnCount() {
        return warnCount;
    }

    public void setWarnCount(Map<String, Long> warnCount) {
        this.warnCount = warnCount;
    }

    public String getGameTimeoutExpectedAt() {
        return gameTimeoutExpectedAt;
    }

    public void setGameTimeoutExpectedAt(String gameTimeoutExpectedAt) {
        this.gameTimeoutExpectedAt = gameTimeoutExpectedAt;
    }

    public boolean isIgnoreBotsWelcomeMessage() {
        return ignoreBotsWelcomeMessage;
    }

    public void setIgnoreBotsWelcomeMessage(boolean ignoreBotsWelcomeMessage) {
        this.ignoreBotsWelcomeMessage = ignoreBotsWelcomeMessage;
    }

    public boolean isIgnoreBotsAutoRole() {
        return ignoreBotsAutoRole;
    }

    public void setIgnoreBotsAutoRole(boolean ignoreBotsAutoRole) {
        this.ignoreBotsAutoRole = ignoreBotsAutoRole;
    }

    public boolean isEnabledLevelUpMessages() {
        return enabledLevelUpMessages;
    }

    public void setEnabledLevelUpMessages(boolean enabledLevelUpMessages) {
        this.enabledLevelUpMessages = enabledLevelUpMessages;
    }

    public String getLevelUpChannel() {
        return levelUpChannel;
    }

    public void setLevelUpChannel(String levelUpChannel) {
        this.levelUpChannel = levelUpChannel;
    }

    public String getLevelUpMessage() {
        return levelUpMessage;
    }

    public void setLevelUpMessage(String levelUpMessage) {
        this.levelUpMessage = levelUpMessage;
    }

    public Set<String> getBlackListedImageTags() {
        return blackListedImageTags;
    }

    public void setBlackListedImageTags(Set<String> blackListedImageTags) {
        this.blackListedImageTags = blackListedImageTags;
    }

    public String getLogJoinChannel() {
        return logJoinChannel;
    }

    public void setLogJoinChannel(String logJoinChannel) {
        this.logJoinChannel = logJoinChannel;
    }

    public String getLogLeaveChannel() {
        return logLeaveChannel;
    }

    public void setLogLeaveChannel(String logLeaveChannel) {
        this.logLeaveChannel = logLeaveChannel;
    }

    public Set<String> getLinkProtectionAllowedUsers() {
        return linkProtectionAllowedUsers;
    }

    public void setLinkProtectionAllowedUsers(Set<String> linkProtectionAllowedUsers) {
        this.linkProtectionAllowedUsers = linkProtectionAllowedUsers;
    }

    public HashMap<String, List<CommandCategory>> getRoleSpecificDisabledCategories() {
        return roleSpecificDisabledCategories;
    }

    public void setRoleSpecificDisabledCategories(HashMap<String, List<CommandCategory>> roleSpecificDisabledCategories) {
        this.roleSpecificDisabledCategories = roleSpecificDisabledCategories;
    }

    public HashMap<String, List<String>> getRoleSpecificDisabledCommands() {
        return roleSpecificDisabledCommands;
    }

    public void setRoleSpecificDisabledCommands(HashMap<String, List<String>> roleSpecificDisabledCommands) {
        this.roleSpecificDisabledCommands = roleSpecificDisabledCommands;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public boolean isMusicVote() {
        return musicVote;
    }

    public void setMusicVote(boolean musicVote) {
        this.musicVote = musicVote;
    }

    public List<String> getExtraJoinMessages() {
        return extraJoinMessages;
    }

    public void setExtraJoinMessages(List<String> extraJoinMessages) {
        this.extraJoinMessages = extraJoinMessages;
    }

    public List<String> getExtraLeaveMessages() {
        return extraLeaveMessages;
    }

    public void setExtraLeaveMessages(List<String> extraLeaveMessages) {
        this.extraLeaveMessages = extraLeaveMessages;
    }

    public String getBirthdayMessage() {
        return birthdayMessage;
    }

    public void setBirthdayMessage(String birthdayMessage) {
        this.birthdayMessage = birthdayMessage;
    }

    public boolean isCustomAdminLockNew() {
        return customAdminLockNew;
    }

    public void setCustomAdminLockNew(boolean customAdminLockNew) {
        this.customAdminLockNew = customAdminLockNew;
    }

    public String getMpLinkedTo() {
        return mpLinkedTo;
    }

    public void setMpLinkedTo(String mpLinkedTo) {
        this.mpLinkedTo = mpLinkedTo;
    }

    public List<String> getModLogBlacklistWords() {
        return modLogBlacklistWords;
    }

    public void setModLogBlacklistWords(List<String> modLogBlacklistWords) {
        this.modLogBlacklistWords = modLogBlacklistWords;
    }

    public Map<String, List<String>> getAutoroleCategories() {
        return autoroleCategories;
    }

    public void setAutoroleCategories(Map<String, List<String>> autoroleCategories) {
        this.autoroleCategories = autoroleCategories;
    }

    public String getEditMessageLog() {
        return editMessageLog;
    }

    public void setEditMessageLog(String editMessageLog) {
        this.editMessageLog = editMessageLog;
    }

    public String getDeleteMessageLog() {
        return deleteMessageLog;
    }

    public void setDeleteMessageLog(String deleteMessageLog) {
        this.deleteMessageLog = deleteMessageLog;
    }

    public String getBannedMemberLog() {
        return bannedMemberLog;
    }

    public void setBannedMemberLog(String bannedMemberLog) {
        this.bannedMemberLog = bannedMemberLog;
    }

    public String getUnbannedMemberLog() {
        return unbannedMemberLog;
    }

    public void setUnbannedMemberLog(String unbannedMemberLog) {
        this.unbannedMemberLog = unbannedMemberLog;
    }

    public String getKickedMemberLog() {
        return kickedMemberLog;
    }

    public void setKickedMemberLog(String kickedMemberLog) {
        this.kickedMemberLog = kickedMemberLog;
    }

    public boolean isCommandWarningDisplay() {
        return commandWarningDisplay;
    }

    public void setCommandWarningDisplay(boolean commandWarningDisplay) {
        this.commandWarningDisplay = commandWarningDisplay;
    }

    public boolean isHasReceivedGreet() {
        return hasReceivedGreet;
    }

    public void setHasReceivedGreet(boolean hasReceivedGreet) {
        this.hasReceivedGreet = hasReceivedGreet;
    }

    public List<String> getBirthdayBlockedIds() {
        return birthdayBlockedIds;
    }

    public void setBirthdayBlockedIds(List<String> birthdayBlockedIds) {
        this.birthdayBlockedIds = birthdayBlockedIds;
    }

    public boolean isGameMultipleDisabled() {
        return gameMultipleDisabled;
    }

    public void setGameMultipleDisabled(boolean gameMultipleDisabled) {
        this.gameMultipleDisabled = gameMultipleDisabled;
    }

    public String getLogTimezone() {
        return logTimezone;
    }

    public void setLogTimezone(String logTimezone) {
        this.logTimezone = logTimezone;
    }

    public List<String> getAllowedBirthdays() {
        return allowedBirthdays;
    }

    public void setAllowedBirthdays(List<String> allowedBirthdays) {
        this.allowedBirthdays = allowedBirthdays;
    }

    public boolean isNotifiedFromBirthdayChange() {
        return notifiedFromBirthdayChange;
    }

    public void setNotifiedFromBirthdayChange(boolean notifiedFromBirthdayChange) {
        this.notifiedFromBirthdayChange = notifiedFromBirthdayChange;
    }

    public String getDjRoleId() {
        return djRoleId;
    }

    public void setDjRoleId(String djRoleId) {
        this.djRoleId = djRoleId;
    }

    public boolean isDisableExplicit() {
        return disableExplicit;
    }

    public void setDisableExplicit(boolean disableExplicit) {
        this.disableExplicit = disableExplicit;
    }
}
