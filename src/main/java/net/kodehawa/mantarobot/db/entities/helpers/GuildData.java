/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.db.entities.helpers;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.kodehawa.mantarobot.commands.moderation.WarnAction;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.utils.annotations.ConfigName;
import net.kodehawa.mantarobot.utils.annotations.HiddenConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// The HiddenConfig annotation is used to not interfere with serialization of old configs: backwards compatibility.
// It just hides the config value from opts check data.
public class GuildData {
    @HiddenConfig //nobody used it, ended up getting removed in early 4.x
    private boolean antiSpam = false;

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

    @HiddenConfig //new lock = CCS are locked by default since 5.0
    private boolean customAdminLock = false;

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

    @HiddenConfig //see: discord added nsfw channels
    private Set<String> guildUnsafeChannels = new HashSet<>();

    @ConfigName("Server join message")
    private String joinMessage = null;
    @ConfigName("Server leave message")
    private String leaveMessage = null;
    @ConfigName("Server invite protection")
    private boolean linkProtection = false;
    @ConfigName("Channel (ids): don't run invite protection")
    private Set<String> linkProtectionAllowedChannels = new HashSet<>();
    @ConfigName("Channels (ids): don't log changes for modlog")
    private Set<String> logExcludedChannels = new HashSet<>();
    @ConfigName("Channel (id): log joins")
    private String logJoinLeaveChannel = null;
    @ConfigName("Fair Queue Limit")
    private int maxFairQueue = 4;

    @HiddenConfig // not implemented
    private int maxResultsSearch = 5;

    @ConfigName("Modlog: Ignored people")
    private Set<String> modlogBlacklistedPeople = new HashSet<>();
    @ConfigName("Music Announce")
    private boolean musicAnnounce = true;
    @ConfigName("Channel (id): lock to specific music channel")
    private String musicChannel = null;

    @HiddenConfig //I don't think we handle this anymore.
    private Long musicQueueSizeLimit = null;
    @HiddenConfig //I don't think we handle this anymore.
    private Long musicSongDurationLimit = null;

    @ConfigName("Role for the mute command")
    private String mutedRole = null;
    @ConfigName("Users awaiting for a mute expire")
    private ConcurrentHashMap<Long, Long> mutedTimelyUsers = new ConcurrentHashMap<>();
    @ConfigName("Action commands ping (for giver)")
    private boolean noMentionsAction = false;
    @ConfigName("Server Premium Key")
    private String premiumKey;

    @HiddenConfig //quotes got removed in early 4.x
    private long quoteLastId = 0L;

    @ConfigName("Amount of polls ran")
    private long ranPolls = 0L;

    @HiddenConfig //I don't think we handle this anymore.
    private boolean reactionMenus = true;

    @ConfigName("Roles that can't use commands")
    private ArrayList<String> rolesBlockedFromCommands = new ArrayList<>();

    @HiddenConfig //removed on first version of 3.x
    private boolean rpgDevaluation = true;
    @HiddenConfig //removed on first version of 3.x
    private boolean rpgLocalMode = false;

    @ConfigName("Mute default timeout")
    private long setModTimeout = 0L;

    @HiddenConfig //nobody used it, ended up getting removed in early 4.x
    private boolean slowMode = false;
    @HiddenConfig //nobody used it, ended up getting removed in early 4.x
    private Set<String> slowModeChannels = new HashSet<>();
    @HiddenConfig //nobody used it, ended up getting removed in early 4.x
    private Set<String> spamModeChannels = new HashSet<>();

    @ConfigName("How will Mantaro display time")
    private int timeDisplay = 0; //0 = 24h, 1 = 12h

    @HiddenConfig
    private Map<Long, WarnAction> warnActions = new HashMap<>();
    @HiddenConfig // not implemented, see above
    private Map<String, Long> warnCount = new HashMap<>();

    @ConfigName("Expected game timeout (epoch)")
    private String gameTimeoutExpectedAt;
    @ConfigName("Ignore bots: welcome message")
    private boolean ignoreBotsWelcomeMessage = false;
    @ConfigName("Ignore bots: autorole")
    private boolean ignoreBotsAutoRole = false;
    @ConfigName("Level-up messages toggle")
    private boolean enabledLevelUpMessages = false;
    @ConfigName("Channel (id): Level-ups")
    private String levelUpChannel = null;
    @ConfigName("Level-up message")
    private String levelUpMessage = null;
    @ConfigName("Blacklisted image tags")
    private Set<String> blackListedImageTags = new HashSet<>();
    @ConfigName("Channel (id): Join message channel")
    private String logJoinChannel = null;
    @ConfigName("Channel (id): Leave message channel")
    private String logLeaveChannel = null;

    @HiddenConfig //experiment, didn't work
    private List<LocalExperienceData> localPlayerExperience = new ArrayList<>();

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

    @HiddenConfig //we don't set this anywhere?
    private String whitelistedRole = null;

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
    @ConfigName("Edit message")
    private String editMessageLog;
    @ConfigName("Delete message")
    private String deleteMessageLog;
    @ConfigName("Ban message")
    private String bannedMemberLog;
    @ConfigName("Unban message")
    private String unbannedMemberLog;
    @ConfigName("Kick message")
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

    @SuppressWarnings("CanBeFinal")
    @HiddenConfig // It's not unused, but this hides it from opts check data lol
    private List<String> allowedBirthdays = new ArrayList<>();

    @HiddenConfig // It's not unused, but this hides it from opts check data lol
    private boolean notifiedFromBirthdayChange = false;

    @ConfigName("The custom DJ role.")
    private String djRoleId;

    public GuildData() { }

    public boolean isHasReceivedGreet() {
        return hasReceivedGreet;
    }

    public boolean isAntiSpam() {
        return this.antiSpam;
    }

    public void setAntiSpam(boolean antiSpam) {
        this.antiSpam = antiSpam;
    }

    public HashMap<String, String> getAutoroles() {
        return this.autoroles;
    }

    public void setAutoroles(HashMap<String, String> autoroles) {
        this.autoroles = autoroles;
    }

    public String getBirthdayChannel() {
        return this.birthdayChannel;
    }

    public void setBirthdayChannel(String birthdayChannel) {
        this.birthdayChannel = birthdayChannel;
    }

    public String getBirthdayRole() {
        return this.birthdayRole;
    }

    public void setBirthdayRole(String birthdayRole) {
        this.birthdayRole = birthdayRole;
    }

    public long getCases() {
        return this.cases;
    }

    public void setCases(long cases) {
        this.cases = cases;
    }

    public boolean hasReceivedGreet() {
        return hasReceivedGreet;
    }

    public void setHasReceivedGreet(boolean hasReceivedGreet) {
        this.hasReceivedGreet = hasReceivedGreet;
    }

    public HashMap<String, List<CommandCategory>> getChannelSpecificDisabledCategories() {
        return this.channelSpecificDisabledCategories;
    }

    public void setChannelSpecificDisabledCategories(HashMap<String, List<CommandCategory>> channelSpecificDisabledCategories) {
        this.channelSpecificDisabledCategories = channelSpecificDisabledCategories;
    }

    public HashMap<String, List<String>> getChannelSpecificDisabledCommands() {
        return this.channelSpecificDisabledCommands;
    }

    public void setChannelSpecificDisabledCommands(HashMap<String, List<String>> channelSpecificDisabledCommands) {
        this.channelSpecificDisabledCommands = channelSpecificDisabledCommands;
    }

    public boolean isCustomAdminLock() {
        return this.customAdminLock;
    }

    public void setCustomAdminLock(boolean customAdminLock) {
        this.customAdminLock = customAdminLock;
    }

    public Set<CommandCategory> getDisabledCategories() {
        return this.disabledCategories;
    }

    public void setDisabledCategories(Set<CommandCategory> disabledCategories) {
        this.disabledCategories = disabledCategories;
    }

    public Set<String> getDisabledChannels() {
        return this.disabledChannels;
    }

    public void setDisabledChannels(Set<String> disabledChannels) {
        this.disabledChannels = disabledChannels;
    }

    public Set<String> getDisabledCommands() {
        return this.disabledCommands;
    }

    public void setDisabledCommands(Set<String> disabledCommands) {
        this.disabledCommands = disabledCommands;
    }

    public Set<String> getDisabledRoles() {
        return this.disabledRoles;
    }

    public void setDisabledRoles(Set<String> disabledRoles) {
        this.disabledRoles = disabledRoles;
    }

    public List<String> getDisabledUsers() {
        return this.disabledUsers;
    }

    public void setDisabledUsers(List<String> disabledUsers) {
        this.disabledUsers = disabledUsers;
    }

    public String getGuildAutoRole() {
        return this.guildAutoRole;
    }

    public void setGuildAutoRole(String guildAutoRole) {
        this.guildAutoRole = guildAutoRole;
    }

    public String getGuildCustomPrefix() {
        return this.guildCustomPrefix;
    }

    public void setGuildCustomPrefix(String guildCustomPrefix) {
        this.guildCustomPrefix = guildCustomPrefix;
    }

    public String getGuildLogChannel() {
        return this.guildLogChannel;
    }

    public void setGuildLogChannel(String guildLogChannel) {
        this.guildLogChannel = guildLogChannel;
    }

    public Set<String> getGuildUnsafeChannels() {
        return this.guildUnsafeChannels;
    }

    public void setGuildUnsafeChannels(Set<String> guildUnsafeChannels) {
        this.guildUnsafeChannels = guildUnsafeChannels;
    }

    public String getJoinMessage() {
        return this.joinMessage;
    }

    public void setJoinMessage(String joinMessage) {
        this.joinMessage = joinMessage;
    }

    public String getLeaveMessage() {
        return this.leaveMessage;
    }

    public void setLeaveMessage(String leaveMessage) {
        this.leaveMessage = leaveMessage;
    }

    public boolean isLinkProtection() {
        return this.linkProtection;
    }

    public void setLinkProtection(boolean linkProtection) {
        this.linkProtection = linkProtection;
    }

    public Set<String> getLinkProtectionAllowedChannels() {
        return this.linkProtectionAllowedChannels;
    }

    public void setLinkProtectionAllowedChannels(Set<String> linkProtectionAllowedChannels) {
        this.linkProtectionAllowedChannels = linkProtectionAllowedChannels;
    }

    public Set<String> getLogExcludedChannels() {
        return this.logExcludedChannels;
    }

    public void setLogExcludedChannels(Set<String> logExcludedChannels) {
        this.logExcludedChannels = logExcludedChannels;
    }

    public String getLogJoinLeaveChannel() {
        return this.logJoinLeaveChannel;
    }

    public void setLogJoinLeaveChannel(String logJoinLeaveChannel) {
        this.logJoinLeaveChannel = logJoinLeaveChannel;
    }

    public int getMaxFairQueue() {
        return this.maxFairQueue;
    }

    public void setMaxFairQueue(int maxFairQueue) {
        this.maxFairQueue = maxFairQueue;
    }

    public int getMaxResultsSearch() {
        return this.maxResultsSearch;
    }

    public void setMaxResultsSearch(int maxResultsSearch) {
        this.maxResultsSearch = maxResultsSearch;
    }

    public Set<String> getModlogBlacklistedPeople() {
        return this.modlogBlacklistedPeople;
    }

    public void setModlogBlacklistedPeople(Set<String> modlogBlacklistedPeople) {
        this.modlogBlacklistedPeople = modlogBlacklistedPeople;
    }

    public boolean isMusicAnnounce() {
        return this.musicAnnounce;
    }

    public void setMusicAnnounce(boolean musicAnnounce) {
        this.musicAnnounce = musicAnnounce;
    }

    public String getMusicChannel() {
        return this.musicChannel;
    }

    public void setMusicChannel(String musicChannel) {
        this.musicChannel = musicChannel;
    }

    public Long getMusicQueueSizeLimit() {
        return this.musicQueueSizeLimit;
    }

    public void setMusicQueueSizeLimit(Long musicQueueSizeLimit) {
        this.musicQueueSizeLimit = musicQueueSizeLimit;
    }

    public Long getMusicSongDurationLimit() {
        return this.musicSongDurationLimit;
    }

    public void setMusicSongDurationLimit(Long musicSongDurationLimit) {
        this.musicSongDurationLimit = musicSongDurationLimit;
    }

    public String getMutedRole() {
        return this.mutedRole;
    }

    public void setMutedRole(String mutedRole) {
        this.mutedRole = mutedRole;
    }

    public ConcurrentHashMap<Long, Long> getMutedTimelyUsers() {
        return this.mutedTimelyUsers;
    }

    public void setMutedTimelyUsers(ConcurrentHashMap<Long, Long> mutedTimelyUsers) {
        this.mutedTimelyUsers = mutedTimelyUsers;
    }

    public boolean isNoMentionsAction() {
        return this.noMentionsAction;
    }

    public void setNoMentionsAction(boolean noMentionsAction) {
        this.noMentionsAction = noMentionsAction;
    }

    public String getPremiumKey() {
        return this.premiumKey;
    }

    public void setPremiumKey(String premiumKey) {
        this.premiumKey = premiumKey;
    }

    public long getQuoteLastId() {
        return this.quoteLastId;
    }

    public void setQuoteLastId(long quoteLastId) {
        this.quoteLastId = quoteLastId;
    }

    public long getRanPolls() {
        return this.ranPolls;
    }

    public void setRanPolls(long ranPolls) {
        this.ranPolls = ranPolls;
    }

    public boolean isReactionMenus() {
        return this.reactionMenus;
    }

    public void setReactionMenus(boolean reactionMenus) {
        this.reactionMenus = reactionMenus;
    }

    public ArrayList<String> getRolesBlockedFromCommands() {
        return this.rolesBlockedFromCommands;
    }

    public void setRolesBlockedFromCommands(ArrayList<String> rolesBlockedFromCommands) {
        this.rolesBlockedFromCommands = rolesBlockedFromCommands;
    }

    public boolean isRpgDevaluation() {
        return this.rpgDevaluation;
    }

    public void setRpgDevaluation(boolean rpgDevaluation) {
        this.rpgDevaluation = rpgDevaluation;
    }

    public boolean isRpgLocalMode() {
        return this.rpgLocalMode;
    }

    public void setRpgLocalMode(boolean rpgLocalMode) {
        this.rpgLocalMode = rpgLocalMode;
    }

    public long getSetModTimeout() {
        return this.setModTimeout;
    }

    public void setSetModTimeout(long setModTimeout) {
        this.setModTimeout = setModTimeout;
    }

    public boolean isSlowMode() {
        return this.slowMode;
    }

    public void setSlowMode(boolean slowMode) {
        this.slowMode = slowMode;
    }

    public Set<String> getSlowModeChannels() {
        return this.slowModeChannels;
    }

    public void setSlowModeChannels(Set<String> slowModeChannels) {
        this.slowModeChannels = slowModeChannels;
    }

    public Set<String> getSpamModeChannels() {
        return this.spamModeChannels;
    }

    public void setSpamModeChannels(Set<String> spamModeChannels) {
        this.spamModeChannels = spamModeChannels;
    }

    public int getTimeDisplay() {
        return this.timeDisplay;
    }

    public void setTimeDisplay(int timeDisplay) {
        this.timeDisplay = timeDisplay;
    }

    public Map<Long, WarnAction> getWarnActions() {
        return this.warnActions;
    }

    public void setWarnActions(Map<Long, WarnAction> warnActions) {
        this.warnActions = warnActions;
    }

    public Map<String, Long> getWarnCount() {
        return this.warnCount;
    }

    public void setWarnCount(Map<String, Long> warnCount) {
        this.warnCount = warnCount;
    }

    public String getGameTimeoutExpectedAt() {
        return this.gameTimeoutExpectedAt;
    }

    public void setGameTimeoutExpectedAt(String gameTimeoutExpectedAt) {
        this.gameTimeoutExpectedAt = gameTimeoutExpectedAt;
    }

    public boolean isIgnoreBotsWelcomeMessage() {
        return this.ignoreBotsWelcomeMessage;
    }

    public void setIgnoreBotsWelcomeMessage(boolean ignoreBotsWelcomeMessage) {
        this.ignoreBotsWelcomeMessage = ignoreBotsWelcomeMessage;
    }

    public boolean isIgnoreBotsAutoRole() {
        return this.ignoreBotsAutoRole;
    }

    public void setIgnoreBotsAutoRole(boolean ignoreBotsAutoRole) {
        this.ignoreBotsAutoRole = ignoreBotsAutoRole;
    }

    public boolean isEnabledLevelUpMessages() {
        return this.enabledLevelUpMessages;
    }

    public void setEnabledLevelUpMessages(boolean enabledLevelUpMessages) {
        this.enabledLevelUpMessages = enabledLevelUpMessages;
    }

    public String getLevelUpChannel() {
        return this.levelUpChannel;
    }

    public void setLevelUpChannel(String levelUpChannel) {
        this.levelUpChannel = levelUpChannel;
    }

    public String getLevelUpMessage() {
        return this.levelUpMessage;
    }

    public void setLevelUpMessage(String levelUpMessage) {
        this.levelUpMessage = levelUpMessage;
    }

    public Set<String> getBlackListedImageTags() {
        return this.blackListedImageTags;
    }

    public void setBlackListedImageTags(Set<String> blackListedImageTags) {
        this.blackListedImageTags = blackListedImageTags;
    }

    public String getLogJoinChannel() {
        return this.logJoinChannel;
    }

    public void setLogJoinChannel(String logJoinChannel) {
        this.logJoinChannel = logJoinChannel;
    }

    public String getLogLeaveChannel() {
        return this.logLeaveChannel;
    }

    public void setLogLeaveChannel(String logLeaveChannel) {
        this.logLeaveChannel = logLeaveChannel;
    }

    public List<LocalExperienceData> getLocalPlayerExperience() {
        return this.localPlayerExperience;
    }

    public void setLocalPlayerExperience(List<LocalExperienceData> localPlayerExperience) {
        this.localPlayerExperience = localPlayerExperience;
    }

    public Set<String> getLinkProtectionAllowedUsers() {
        return this.linkProtectionAllowedUsers;
    }

    public void setLinkProtectionAllowedUsers(Set<String> linkProtectionAllowedUsers) {
        this.linkProtectionAllowedUsers = linkProtectionAllowedUsers;
    }

    public HashMap<String, List<CommandCategory>> getRoleSpecificDisabledCategories() {
        return this.roleSpecificDisabledCategories;
    }

    public void setRoleSpecificDisabledCategories(HashMap<String, List<CommandCategory>> roleSpecificDisabledCategories) {
        this.roleSpecificDisabledCategories = roleSpecificDisabledCategories;
    }

    public HashMap<String, List<String>> getRoleSpecificDisabledCommands() {
        return this.roleSpecificDisabledCommands;
    }

    public void setRoleSpecificDisabledCommands(HashMap<String, List<String>> roleSpecificDisabledCommands) {
        this.roleSpecificDisabledCommands = roleSpecificDisabledCommands;
    }

    public String getLang() {
        return this.lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public boolean isMusicVote() {
        return this.musicVote;
    }

    public void setMusicVote(boolean musicVote) {
        this.musicVote = musicVote;
    }

    public List<String> getExtraJoinMessages() {
        return this.extraJoinMessages;
    }

    public void setExtraJoinMessages(List<String> extraJoinMessages) {
        this.extraJoinMessages = extraJoinMessages;
    }

    public List<String> getExtraLeaveMessages() {
        return this.extraLeaveMessages;
    }

    public void setExtraLeaveMessages(List<String> extraLeaveMessages) {
        this.extraLeaveMessages = extraLeaveMessages;
    }

    public String getWhitelistedRole() {
        return this.whitelistedRole;
    }

    public void setWhitelistedRole(String whitelistedRole) {
        this.whitelistedRole = whitelistedRole;
    }

    public String getBirthdayMessage() {
        return this.birthdayMessage;
    }

    public void setBirthdayMessage(String birthdayMessage) {
        this.birthdayMessage = birthdayMessage;
    }

    public boolean isCustomAdminLockNew() {
        return this.customAdminLockNew;
    }

    public void setCustomAdminLockNew(boolean customAdminLockNew) {
        this.customAdminLockNew = customAdminLockNew;
    }

    public String getMpLinkedTo() {
        return this.mpLinkedTo;
    }

    public void setMpLinkedTo(String mpLinkedTo) {
        this.mpLinkedTo = mpLinkedTo;
    }

    public List<String> getModLogBlacklistWords() {
        return this.modLogBlacklistWords;
    }

    public void setModLogBlacklistWords(List<String> modLogBlacklistWords) {
        this.modLogBlacklistWords = modLogBlacklistWords;
    }

    public Map<String, List<String>> getAutoroleCategories() {
        return this.autoroleCategories;
    }

    public void setAutoroleCategories(Map<String, List<String>> autoroleCategories) {
        this.autoroleCategories = autoroleCategories;
    }

    public String getEditMessageLog() {
        return this.editMessageLog;
    }

    public void setEditMessageLog(String editMessageLog) {
        this.editMessageLog = editMessageLog;
    }

    public String getDeleteMessageLog() {
        return this.deleteMessageLog;
    }

    public void setDeleteMessageLog(String deleteMessageLog) {
        this.deleteMessageLog = deleteMessageLog;
    }

    public String getBannedMemberLog() {
        return this.bannedMemberLog;
    }

    public void setBannedMemberLog(String bannedMemberLog) {
        this.bannedMemberLog = bannedMemberLog;
    }

    public String getUnbannedMemberLog() {
        return this.unbannedMemberLog;
    }

    public void setUnbannedMemberLog(String unbannedMemberLog) {
        this.unbannedMemberLog = unbannedMemberLog;
    }

    public String getKickedMemberLog() {
        return this.kickedMemberLog;
    }

    public void setKickedMemberLog(String kickedMemberLog) {
        this.kickedMemberLog = kickedMemberLog;
    }

    public boolean isCommandWarningDisplay() {
        return this.commandWarningDisplay;
    }

    public void setCommandWarningDisplay(boolean commandWarningDisplay) {
        this.commandWarningDisplay = commandWarningDisplay;
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

    public boolean isNotifiedFromBirthdayChange() {
        return notifiedFromBirthdayChange;
    }

    public void setNotifiedFromBirthdayChange(boolean notifiedFromBirthdayChange) {
        this.notifiedFromBirthdayChange = notifiedFromBirthdayChange;
    }

    public List<String> getAllowedBirthdays() {
        return allowedBirthdays;
    }

    public String getDjRoleId() {
        return djRoleId;
    }

    public void setDjRoleId(String djRoleId) {
        this.djRoleId = djRoleId;
    }
}
