/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.db.entities.helpers;

import net.kodehawa.mantarobot.commands.moderation.WarnAction;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.annotations.ConfigName;
import net.kodehawa.mantarobot.utils.annotations.UnusedConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//UnusedConfig annotation is used to not interfere with serialization of old configs: backwards compatibility. The annotation only takes effect on check data.
public class GuildData {
    @UnusedConfig //nobody used it, ended up getting removed in early 4.x
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
    private HashMap<String, List<Category>> channelSpecificDisabledCategories = new HashMap<>();
    @ConfigName("Commands disabled in channels")
    private HashMap<String, List<String>> channelSpecificDisabledCommands = new HashMap<>();
    
    @UnusedConfig //new lock = CCS are locked by default since 5.0
    private boolean customAdminLock = false;
    
    @ConfigName("Disabled Categories")
    private Set<Category> disabledCategories = new HashSet<>();
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
    
    @UnusedConfig //see: discord added nsfw channels
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
    
    @UnusedConfig // not implemented
    private int maxResultsSearch = 5;
    
    @ConfigName("Modlog: Ignored people")
    private Set<String> modlogBlacklistedPeople = new HashSet<>();
    @ConfigName("Music Announce")
    private boolean musicAnnounce = true;
    @ConfigName("Channel (id): lock to specific music channel")
    private String musicChannel = null;
    
    @UnusedConfig //I don't think we handle this anymore.
    private Long musicQueueSizeLimit = null;
    @UnusedConfig //I don't think we handle this anymore.
    private Long musicSongDurationLimit = null;
    
    @ConfigName("Role for the mute command")
    private String mutedRole = null;
    @ConfigName("Users awaiting for a mute expire")
    private ConcurrentHashMap<Long, Long> mutedTimelyUsers = new ConcurrentHashMap<>();
    @ConfigName("Action commands ping (for giver)")
    private boolean noMentionsAction = false;
    @ConfigName("Server Premium Key")
    private String premiumKey;
    
    @UnusedConfig //quotes got removed in early 4.x
    private long quoteLastId = 0L;
    
    @ConfigName("Amount of polls ran")
    private long ranPolls = 0L;
    
    @UnusedConfig //I don't think we handle this anymore.
    private boolean reactionMenus = true;
    
    @ConfigName("Roles that can't use commands")
    private ArrayList<String> rolesBlockedFromCommands = new ArrayList<>();
    
    @UnusedConfig //removed on first version of 3.x
    private boolean rpgDevaluation = true;
    @UnusedConfig //removed on first version of 3.x
    private boolean rpgLocalMode = false;
    
    @ConfigName("Mute default timeout")
    private long setModTimeout = 0L;
    
    @UnusedConfig //nobody used it, ended up getting removed in early 4.x
    private boolean slowMode = false;
    @UnusedConfig //nobody used it, ended up getting removed in early 4.x
    private Set<String> slowModeChannels = new HashSet<>();
    @UnusedConfig //nobody used it, ended up getting removed in early 4.x
    private Set<String> spamModeChannels = new HashSet<>();
    
    @ConfigName("How will Mantaro display time")
    private int timeDisplay = 0; //0 = 24h, 1 = 12h
    
    @UnusedConfig //TODO: not implemented, planned somewhen
    private Map<Long, WarnAction> warnActions = new HashMap<>();
    @UnusedConfig // not implemented, see above
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
    
    @UnusedConfig //experiment, didn't work
    private List<LocalExperienceData> localPlayerExperience = new ArrayList<>();
    
    @ConfigName("Link Protection ignore (users)")
    private Set<String> linkProtectionAllowedUsers = new HashSet<>();
    @ConfigName("Disabled Categories for Role (id)")
    private HashMap<String, List<Category>> roleSpecificDisabledCategories = new HashMap<>();
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
    
    @UnusedConfig //we don't set this anywhere?
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
    
    public GuildData() {
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
    
    public HashMap<String, List<Category>> getChannelSpecificDisabledCategories() {
        return this.channelSpecificDisabledCategories;
    }
    
    public void setChannelSpecificDisabledCategories(HashMap<String, List<Category>> channelSpecificDisabledCategories) {
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
    
    public Set<Category> getDisabledCategories() {
        return this.disabledCategories;
    }
    
    public void setDisabledCategories(Set<Category> disabledCategories) {
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
    
    public HashMap<String, List<Category>> getRoleSpecificDisabledCategories() {
        return this.roleSpecificDisabledCategories;
    }
    
    public void setRoleSpecificDisabledCategories(HashMap<String, List<Category>> roleSpecificDisabledCategories) {
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
    
    protected boolean canEqual(final Object other) {
        return other instanceof GuildData;
    }
    
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isAntiSpam() ? 79 : 97);
        final Object $autoroles = this.getAutoroles();
        result = result * PRIME + ($autoroles == null ? 43 : $autoroles.hashCode());
        final Object $birthdayChannel = this.getBirthdayChannel();
        result = result * PRIME + ($birthdayChannel == null ? 43 : $birthdayChannel.hashCode());
        final Object $birthdayRole = this.getBirthdayRole();
        result = result * PRIME + ($birthdayRole == null ? 43 : $birthdayRole.hashCode());
        final long $cases = this.getCases();
        result = result * PRIME + (int) ($cases >>> 32 ^ $cases);
        final Object $channelSpecificDisabledCategories = this.getChannelSpecificDisabledCategories();
        result = result * PRIME + ($channelSpecificDisabledCategories == null ? 43 : $channelSpecificDisabledCategories.hashCode());
        final Object $channelSpecificDisabledCommands = this.getChannelSpecificDisabledCommands();
        result = result * PRIME + ($channelSpecificDisabledCommands == null ? 43 : $channelSpecificDisabledCommands.hashCode());
        result = result * PRIME + (this.isCustomAdminLock() ? 79 : 97);
        final Object $disabledCategories = this.getDisabledCategories();
        result = result * PRIME + ($disabledCategories == null ? 43 : $disabledCategories.hashCode());
        final Object $disabledChannels = this.getDisabledChannels();
        result = result * PRIME + ($disabledChannels == null ? 43 : $disabledChannels.hashCode());
        final Object $disabledCommands = this.getDisabledCommands();
        result = result * PRIME + ($disabledCommands == null ? 43 : $disabledCommands.hashCode());
        final Object $disabledRoles = this.getDisabledRoles();
        result = result * PRIME + ($disabledRoles == null ? 43 : $disabledRoles.hashCode());
        final Object $disabledUsers = this.getDisabledUsers();
        result = result * PRIME + ($disabledUsers == null ? 43 : $disabledUsers.hashCode());
        final Object $guildAutoRole = this.getGuildAutoRole();
        result = result * PRIME + ($guildAutoRole == null ? 43 : $guildAutoRole.hashCode());
        final Object $guildCustomPrefix = this.getGuildCustomPrefix();
        result = result * PRIME + ($guildCustomPrefix == null ? 43 : $guildCustomPrefix.hashCode());
        final Object $guildLogChannel = this.getGuildLogChannel();
        result = result * PRIME + ($guildLogChannel == null ? 43 : $guildLogChannel.hashCode());
        final Object $guildUnsafeChannels = this.getGuildUnsafeChannels();
        result = result * PRIME + ($guildUnsafeChannels == null ? 43 : $guildUnsafeChannels.hashCode());
        final Object $joinMessage = this.getJoinMessage();
        result = result * PRIME + ($joinMessage == null ? 43 : $joinMessage.hashCode());
        final Object $leaveMessage = this.getLeaveMessage();
        result = result * PRIME + ($leaveMessage == null ? 43 : $leaveMessage.hashCode());
        result = result * PRIME + (this.isLinkProtection() ? 79 : 97);
        final Object $linkProtectionAllowedChannels = this.getLinkProtectionAllowedChannels();
        result = result * PRIME + ($linkProtectionAllowedChannels == null ? 43 : $linkProtectionAllowedChannels.hashCode());
        final Object $logExcludedChannels = this.getLogExcludedChannels();
        result = result * PRIME + ($logExcludedChannels == null ? 43 : $logExcludedChannels.hashCode());
        final Object $logJoinLeaveChannel = this.getLogJoinLeaveChannel();
        result = result * PRIME + ($logJoinLeaveChannel == null ? 43 : $logJoinLeaveChannel.hashCode());
        result = result * PRIME + this.getMaxFairQueue();
        result = result * PRIME + this.getMaxResultsSearch();
        final Object $modlogBlacklistedPeople = this.getModlogBlacklistedPeople();
        result = result * PRIME + ($modlogBlacklistedPeople == null ? 43 : $modlogBlacklistedPeople.hashCode());
        result = result * PRIME + (this.isMusicAnnounce() ? 79 : 97);
        final Object $musicChannel = this.getMusicChannel();
        result = result * PRIME + ($musicChannel == null ? 43 : $musicChannel.hashCode());
        final Object $musicQueueSizeLimit = this.getMusicQueueSizeLimit();
        result = result * PRIME + ($musicQueueSizeLimit == null ? 43 : $musicQueueSizeLimit.hashCode());
        final Object $musicSongDurationLimit = this.getMusicSongDurationLimit();
        result = result * PRIME + ($musicSongDurationLimit == null ? 43 : $musicSongDurationLimit.hashCode());
        final Object $mutedRole = this.getMutedRole();
        result = result * PRIME + ($mutedRole == null ? 43 : $mutedRole.hashCode());
        final Object $mutedTimelyUsers = this.getMutedTimelyUsers();
        result = result * PRIME + ($mutedTimelyUsers == null ? 43 : $mutedTimelyUsers.hashCode());
        result = result * PRIME + (this.isNoMentionsAction() ? 79 : 97);
        final Object $premiumKey = this.getPremiumKey();
        result = result * PRIME + ($premiumKey == null ? 43 : $premiumKey.hashCode());
        final long $quoteLastId = this.getQuoteLastId();
        result = result * PRIME + (int) ($quoteLastId >>> 32 ^ $quoteLastId);
        final long $ranPolls = this.getRanPolls();
        result = result * PRIME + (int) ($ranPolls >>> 32 ^ $ranPolls);
        result = result * PRIME + (this.isReactionMenus() ? 79 : 97);
        final Object $rolesBlockedFromCommands = this.getRolesBlockedFromCommands();
        result = result * PRIME + ($rolesBlockedFromCommands == null ? 43 : $rolesBlockedFromCommands.hashCode());
        result = result * PRIME + (this.isRpgDevaluation() ? 79 : 97);
        result = result * PRIME + (this.isRpgLocalMode() ? 79 : 97);
        final long $setModTimeout = this.getSetModTimeout();
        result = result * PRIME + (int) ($setModTimeout >>> 32 ^ $setModTimeout);
        result = result * PRIME + (this.isSlowMode() ? 79 : 97);
        final Object $slowModeChannels = this.getSlowModeChannels();
        result = result * PRIME + ($slowModeChannels == null ? 43 : $slowModeChannels.hashCode());
        final Object $spamModeChannels = this.getSpamModeChannels();
        result = result * PRIME + ($spamModeChannels == null ? 43 : $spamModeChannels.hashCode());
        result = result * PRIME + this.getTimeDisplay();
        final Object $warnActions = this.getWarnActions();
        result = result * PRIME + ($warnActions == null ? 43 : $warnActions.hashCode());
        final Object $warnCount = this.getWarnCount();
        result = result * PRIME + ($warnCount == null ? 43 : $warnCount.hashCode());
        final Object $gameTimeoutExpectedAt = this.getGameTimeoutExpectedAt();
        result = result * PRIME + ($gameTimeoutExpectedAt == null ? 43 : $gameTimeoutExpectedAt.hashCode());
        result = result * PRIME + (this.isIgnoreBotsWelcomeMessage() ? 79 : 97);
        result = result * PRIME + (this.isIgnoreBotsAutoRole() ? 79 : 97);
        result = result * PRIME + (this.isEnabledLevelUpMessages() ? 79 : 97);
        final Object $levelUpChannel = this.getLevelUpChannel();
        result = result * PRIME + ($levelUpChannel == null ? 43 : $levelUpChannel.hashCode());
        final Object $levelUpMessage = this.getLevelUpMessage();
        result = result * PRIME + ($levelUpMessage == null ? 43 : $levelUpMessage.hashCode());
        final Object $blackListedImageTags = this.getBlackListedImageTags();
        result = result * PRIME + ($blackListedImageTags == null ? 43 : $blackListedImageTags.hashCode());
        final Object $logJoinChannel = this.getLogJoinChannel();
        result = result * PRIME + ($logJoinChannel == null ? 43 : $logJoinChannel.hashCode());
        final Object $logLeaveChannel = this.getLogLeaveChannel();
        result = result * PRIME + ($logLeaveChannel == null ? 43 : $logLeaveChannel.hashCode());
        final Object $localPlayerExperience = this.getLocalPlayerExperience();
        result = result * PRIME + ($localPlayerExperience == null ? 43 : $localPlayerExperience.hashCode());
        final Object $linkProtectionAllowedUsers = this.getLinkProtectionAllowedUsers();
        result = result * PRIME + ($linkProtectionAllowedUsers == null ? 43 : $linkProtectionAllowedUsers.hashCode());
        final Object $roleSpecificDisabledCategories = this.getRoleSpecificDisabledCategories();
        result = result * PRIME + ($roleSpecificDisabledCategories == null ? 43 : $roleSpecificDisabledCategories.hashCode());
        final Object $roleSpecificDisabledCommands = this.getRoleSpecificDisabledCommands();
        result = result * PRIME + ($roleSpecificDisabledCommands == null ? 43 : $roleSpecificDisabledCommands.hashCode());
        final Object $lang = this.getLang();
        result = result * PRIME + ($lang == null ? 43 : $lang.hashCode());
        result = result * PRIME + (this.isMusicVote() ? 79 : 97);
        final Object $extraJoinMessages = this.getExtraJoinMessages();
        result = result * PRIME + ($extraJoinMessages == null ? 43 : $extraJoinMessages.hashCode());
        final Object $extraLeaveMessages = this.getExtraLeaveMessages();
        result = result * PRIME + ($extraLeaveMessages == null ? 43 : $extraLeaveMessages.hashCode());
        final Object $whitelistedRole = this.getWhitelistedRole();
        result = result * PRIME + ($whitelistedRole == null ? 43 : $whitelistedRole.hashCode());
        final Object $birthdayMessage = this.getBirthdayMessage();
        result = result * PRIME + ($birthdayMessage == null ? 43 : $birthdayMessage.hashCode());
        result = result * PRIME + (this.isCustomAdminLockNew() ? 79 : 97);
        final Object $mpLinkedTo = this.getMpLinkedTo();
        result = result * PRIME + ($mpLinkedTo == null ? 43 : $mpLinkedTo.hashCode());
        final Object $modLogBlacklistWords = this.getModLogBlacklistWords();
        result = result * PRIME + ($modLogBlacklistWords == null ? 43 : $modLogBlacklistWords.hashCode());
        final Object $autoroleCategories = this.getAutoroleCategories();
        result = result * PRIME + ($autoroleCategories == null ? 43 : $autoroleCategories.hashCode());
        final Object $editMessageLog = this.getEditMessageLog();
        result = result * PRIME + ($editMessageLog == null ? 43 : $editMessageLog.hashCode());
        final Object $deleteMessageLog = this.getDeleteMessageLog();
        result = result * PRIME + ($deleteMessageLog == null ? 43 : $deleteMessageLog.hashCode());
        final Object $bannedMemberLog = this.getBannedMemberLog();
        result = result * PRIME + ($bannedMemberLog == null ? 43 : $bannedMemberLog.hashCode());
        final Object $unbannedMemberLog = this.getUnbannedMemberLog();
        result = result * PRIME + ($unbannedMemberLog == null ? 43 : $unbannedMemberLog.hashCode());
        final Object $kickedMemberLog = this.getKickedMemberLog();
        result = result * PRIME + ($kickedMemberLog == null ? 43 : $kickedMemberLog.hashCode());
        result = result * PRIME + (this.isCommandWarningDisplay() ? 79 : 97);
        return result;
    }
    
    public boolean equals(final Object o) {
        if(o == this) return true;
        if(!(o instanceof GuildData)) return false;
        final GuildData other = (GuildData) o;
        if(!other.canEqual(this)) return false;
        if(this.isAntiSpam() != other.isAntiSpam()) return false;
        final Object this$autoroles = this.getAutoroles();
        final Object other$autoroles = other.getAutoroles();
        if(this$autoroles == null ? other$autoroles != null : !this$autoroles.equals(other$autoroles)) return false;
        final Object this$birthdayChannel = this.getBirthdayChannel();
        final Object other$birthdayChannel = other.getBirthdayChannel();
        if(this$birthdayChannel == null ? other$birthdayChannel != null : !this$birthdayChannel.equals(other$birthdayChannel))
            return false;
        final Object this$birthdayRole = this.getBirthdayRole();
        final Object other$birthdayRole = other.getBirthdayRole();
        if(this$birthdayRole == null ? other$birthdayRole != null : !this$birthdayRole.equals(other$birthdayRole))
            return false;
        if(this.getCases() != other.getCases()) return false;
        final Object this$channelSpecificDisabledCategories = this.getChannelSpecificDisabledCategories();
        final Object other$channelSpecificDisabledCategories = other.getChannelSpecificDisabledCategories();
        if(this$channelSpecificDisabledCategories == null ? other$channelSpecificDisabledCategories != null : !this$channelSpecificDisabledCategories.equals(other$channelSpecificDisabledCategories))
            return false;
        final Object this$channelSpecificDisabledCommands = this.getChannelSpecificDisabledCommands();
        final Object other$channelSpecificDisabledCommands = other.getChannelSpecificDisabledCommands();
        if(this$channelSpecificDisabledCommands == null ? other$channelSpecificDisabledCommands != null : !this$channelSpecificDisabledCommands.equals(other$channelSpecificDisabledCommands))
            return false;
        if(this.isCustomAdminLock() != other.isCustomAdminLock()) return false;
        final Object this$disabledCategories = this.getDisabledCategories();
        final Object other$disabledCategories = other.getDisabledCategories();
        if(this$disabledCategories == null ? other$disabledCategories != null : !this$disabledCategories.equals(other$disabledCategories))
            return false;
        final Object this$disabledChannels = this.getDisabledChannels();
        final Object other$disabledChannels = other.getDisabledChannels();
        if(this$disabledChannels == null ? other$disabledChannels != null : !this$disabledChannels.equals(other$disabledChannels))
            return false;
        final Object this$disabledCommands = this.getDisabledCommands();
        final Object other$disabledCommands = other.getDisabledCommands();
        if(this$disabledCommands == null ? other$disabledCommands != null : !this$disabledCommands.equals(other$disabledCommands))
            return false;
        final Object this$disabledRoles = this.getDisabledRoles();
        final Object other$disabledRoles = other.getDisabledRoles();
        if(this$disabledRoles == null ? other$disabledRoles != null : !this$disabledRoles.equals(other$disabledRoles))
            return false;
        final Object this$disabledUsers = this.getDisabledUsers();
        final Object other$disabledUsers = other.getDisabledUsers();
        if(this$disabledUsers == null ? other$disabledUsers != null : !this$disabledUsers.equals(other$disabledUsers))
            return false;
        final Object this$guildAutoRole = this.getGuildAutoRole();
        final Object other$guildAutoRole = other.getGuildAutoRole();
        if(this$guildAutoRole == null ? other$guildAutoRole != null : !this$guildAutoRole.equals(other$guildAutoRole))
            return false;
        final Object this$guildCustomPrefix = this.getGuildCustomPrefix();
        final Object other$guildCustomPrefix = other.getGuildCustomPrefix();
        if(this$guildCustomPrefix == null ? other$guildCustomPrefix != null : !this$guildCustomPrefix.equals(other$guildCustomPrefix))
            return false;
        final Object this$guildLogChannel = this.getGuildLogChannel();
        final Object other$guildLogChannel = other.getGuildLogChannel();
        if(this$guildLogChannel == null ? other$guildLogChannel != null : !this$guildLogChannel.equals(other$guildLogChannel))
            return false;
        final Object this$guildUnsafeChannels = this.getGuildUnsafeChannels();
        final Object other$guildUnsafeChannels = other.getGuildUnsafeChannels();
        if(this$guildUnsafeChannels == null ? other$guildUnsafeChannels != null : !this$guildUnsafeChannels.equals(other$guildUnsafeChannels))
            return false;
        final Object this$joinMessage = this.getJoinMessage();
        final Object other$joinMessage = other.getJoinMessage();
        if(this$joinMessage == null ? other$joinMessage != null : !this$joinMessage.equals(other$joinMessage))
            return false;
        final Object this$leaveMessage = this.getLeaveMessage();
        final Object other$leaveMessage = other.getLeaveMessage();
        if(this$leaveMessage == null ? other$leaveMessage != null : !this$leaveMessage.equals(other$leaveMessage))
            return false;
        if(this.isLinkProtection() != other.isLinkProtection()) return false;
        final Object this$linkProtectionAllowedChannels = this.getLinkProtectionAllowedChannels();
        final Object other$linkProtectionAllowedChannels = other.getLinkProtectionAllowedChannels();
        if(this$linkProtectionAllowedChannels == null ? other$linkProtectionAllowedChannels != null : !this$linkProtectionAllowedChannels.equals(other$linkProtectionAllowedChannels))
            return false;
        final Object this$logExcludedChannels = this.getLogExcludedChannels();
        final Object other$logExcludedChannels = other.getLogExcludedChannels();
        if(this$logExcludedChannels == null ? other$logExcludedChannels != null : !this$logExcludedChannels.equals(other$logExcludedChannels))
            return false;
        final Object this$logJoinLeaveChannel = this.getLogJoinLeaveChannel();
        final Object other$logJoinLeaveChannel = other.getLogJoinLeaveChannel();
        if(this$logJoinLeaveChannel == null ? other$logJoinLeaveChannel != null : !this$logJoinLeaveChannel.equals(other$logJoinLeaveChannel))
            return false;
        if(this.getMaxFairQueue() != other.getMaxFairQueue()) return false;
        if(this.getMaxResultsSearch() != other.getMaxResultsSearch()) return false;
        final Object this$modlogBlacklistedPeople = this.getModlogBlacklistedPeople();
        final Object other$modlogBlacklistedPeople = other.getModlogBlacklistedPeople();
        if(this$modlogBlacklistedPeople == null ? other$modlogBlacklistedPeople != null : !this$modlogBlacklistedPeople.equals(other$modlogBlacklistedPeople))
            return false;
        if(this.isMusicAnnounce() != other.isMusicAnnounce()) return false;
        final Object this$musicChannel = this.getMusicChannel();
        final Object other$musicChannel = other.getMusicChannel();
        if(this$musicChannel == null ? other$musicChannel != null : !this$musicChannel.equals(other$musicChannel))
            return false;
        final Object this$musicQueueSizeLimit = this.getMusicQueueSizeLimit();
        final Object other$musicQueueSizeLimit = other.getMusicQueueSizeLimit();
        if(this$musicQueueSizeLimit == null ? other$musicQueueSizeLimit != null : !this$musicQueueSizeLimit.equals(other$musicQueueSizeLimit))
            return false;
        final Object this$musicSongDurationLimit = this.getMusicSongDurationLimit();
        final Object other$musicSongDurationLimit = other.getMusicSongDurationLimit();
        if(this$musicSongDurationLimit == null ? other$musicSongDurationLimit != null : !this$musicSongDurationLimit.equals(other$musicSongDurationLimit))
            return false;
        final Object this$mutedRole = this.getMutedRole();
        final Object other$mutedRole = other.getMutedRole();
        if(this$mutedRole == null ? other$mutedRole != null : !this$mutedRole.equals(other$mutedRole)) return false;
        final Object this$mutedTimelyUsers = this.getMutedTimelyUsers();
        final Object other$mutedTimelyUsers = other.getMutedTimelyUsers();
        if(this$mutedTimelyUsers == null ? other$mutedTimelyUsers != null : !this$mutedTimelyUsers.equals(other$mutedTimelyUsers))
            return false;
        if(this.isNoMentionsAction() != other.isNoMentionsAction()) return false;
        final Object this$premiumKey = this.getPremiumKey();
        final Object other$premiumKey = other.getPremiumKey();
        if(this$premiumKey == null ? other$premiumKey != null : !this$premiumKey.equals(other$premiumKey)) return false;
        if(this.getQuoteLastId() != other.getQuoteLastId()) return false;
        if(this.getRanPolls() != other.getRanPolls()) return false;
        if(this.isReactionMenus() != other.isReactionMenus()) return false;
        final Object this$rolesBlockedFromCommands = this.getRolesBlockedFromCommands();
        final Object other$rolesBlockedFromCommands = other.getRolesBlockedFromCommands();
        if(this$rolesBlockedFromCommands == null ? other$rolesBlockedFromCommands != null : !this$rolesBlockedFromCommands.equals(other$rolesBlockedFromCommands))
            return false;
        if(this.isRpgDevaluation() != other.isRpgDevaluation()) return false;
        if(this.isRpgLocalMode() != other.isRpgLocalMode()) return false;
        if(this.getSetModTimeout() != other.getSetModTimeout()) return false;
        if(this.isSlowMode() != other.isSlowMode()) return false;
        final Object this$slowModeChannels = this.getSlowModeChannels();
        final Object other$slowModeChannels = other.getSlowModeChannels();
        if(this$slowModeChannels == null ? other$slowModeChannels != null : !this$slowModeChannels.equals(other$slowModeChannels))
            return false;
        final Object this$spamModeChannels = this.getSpamModeChannels();
        final Object other$spamModeChannels = other.getSpamModeChannels();
        if(this$spamModeChannels == null ? other$spamModeChannels != null : !this$spamModeChannels.equals(other$spamModeChannels))
            return false;
        if(this.getTimeDisplay() != other.getTimeDisplay()) return false;
        final Object this$warnActions = this.getWarnActions();
        final Object other$warnActions = other.getWarnActions();
        if(this$warnActions == null ? other$warnActions != null : !this$warnActions.equals(other$warnActions))
            return false;
        final Object this$warnCount = this.getWarnCount();
        final Object other$warnCount = other.getWarnCount();
        if(this$warnCount == null ? other$warnCount != null : !this$warnCount.equals(other$warnCount)) return false;
        final Object this$gameTimeoutExpectedAt = this.getGameTimeoutExpectedAt();
        final Object other$gameTimeoutExpectedAt = other.getGameTimeoutExpectedAt();
        if(this$gameTimeoutExpectedAt == null ? other$gameTimeoutExpectedAt != null : !this$gameTimeoutExpectedAt.equals(other$gameTimeoutExpectedAt))
            return false;
        if(this.isIgnoreBotsWelcomeMessage() != other.isIgnoreBotsWelcomeMessage()) return false;
        if(this.isIgnoreBotsAutoRole() != other.isIgnoreBotsAutoRole()) return false;
        if(this.isEnabledLevelUpMessages() != other.isEnabledLevelUpMessages()) return false;
        final Object this$levelUpChannel = this.getLevelUpChannel();
        final Object other$levelUpChannel = other.getLevelUpChannel();
        if(this$levelUpChannel == null ? other$levelUpChannel != null : !this$levelUpChannel.equals(other$levelUpChannel))
            return false;
        final Object this$levelUpMessage = this.getLevelUpMessage();
        final Object other$levelUpMessage = other.getLevelUpMessage();
        if(this$levelUpMessage == null ? other$levelUpMessage != null : !this$levelUpMessage.equals(other$levelUpMessage))
            return false;
        final Object this$blackListedImageTags = this.getBlackListedImageTags();
        final Object other$blackListedImageTags = other.getBlackListedImageTags();
        if(this$blackListedImageTags == null ? other$blackListedImageTags != null : !this$blackListedImageTags.equals(other$blackListedImageTags))
            return false;
        final Object this$logJoinChannel = this.getLogJoinChannel();
        final Object other$logJoinChannel = other.getLogJoinChannel();
        if(this$logJoinChannel == null ? other$logJoinChannel != null : !this$logJoinChannel.equals(other$logJoinChannel))
            return false;
        final Object this$logLeaveChannel = this.getLogLeaveChannel();
        final Object other$logLeaveChannel = other.getLogLeaveChannel();
        if(this$logLeaveChannel == null ? other$logLeaveChannel != null : !this$logLeaveChannel.equals(other$logLeaveChannel))
            return false;
        final Object this$localPlayerExperience = this.getLocalPlayerExperience();
        final Object other$localPlayerExperience = other.getLocalPlayerExperience();
        if(this$localPlayerExperience == null ? other$localPlayerExperience != null : !this$localPlayerExperience.equals(other$localPlayerExperience))
            return false;
        final Object this$linkProtectionAllowedUsers = this.getLinkProtectionAllowedUsers();
        final Object other$linkProtectionAllowedUsers = other.getLinkProtectionAllowedUsers();
        if(this$linkProtectionAllowedUsers == null ? other$linkProtectionAllowedUsers != null : !this$linkProtectionAllowedUsers.equals(other$linkProtectionAllowedUsers))
            return false;
        final Object this$roleSpecificDisabledCategories = this.getRoleSpecificDisabledCategories();
        final Object other$roleSpecificDisabledCategories = other.getRoleSpecificDisabledCategories();
        if(this$roleSpecificDisabledCategories == null ? other$roleSpecificDisabledCategories != null : !this$roleSpecificDisabledCategories.equals(other$roleSpecificDisabledCategories))
            return false;
        final Object this$roleSpecificDisabledCommands = this.getRoleSpecificDisabledCommands();
        final Object other$roleSpecificDisabledCommands = other.getRoleSpecificDisabledCommands();
        if(this$roleSpecificDisabledCommands == null ? other$roleSpecificDisabledCommands != null : !this$roleSpecificDisabledCommands.equals(other$roleSpecificDisabledCommands))
            return false;
        final Object this$lang = this.getLang();
        final Object other$lang = other.getLang();
        if(this$lang == null ? other$lang != null : !this$lang.equals(other$lang)) return false;
        if(this.isMusicVote() != other.isMusicVote()) return false;
        final Object this$extraJoinMessages = this.getExtraJoinMessages();
        final Object other$extraJoinMessages = other.getExtraJoinMessages();
        if(this$extraJoinMessages == null ? other$extraJoinMessages != null : !this$extraJoinMessages.equals(other$extraJoinMessages))
            return false;
        final Object this$extraLeaveMessages = this.getExtraLeaveMessages();
        final Object other$extraLeaveMessages = other.getExtraLeaveMessages();
        if(this$extraLeaveMessages == null ? other$extraLeaveMessages != null : !this$extraLeaveMessages.equals(other$extraLeaveMessages))
            return false;
        final Object this$whitelistedRole = this.getWhitelistedRole();
        final Object other$whitelistedRole = other.getWhitelistedRole();
        if(this$whitelistedRole == null ? other$whitelistedRole != null : !this$whitelistedRole.equals(other$whitelistedRole))
            return false;
        final Object this$birthdayMessage = this.getBirthdayMessage();
        final Object other$birthdayMessage = other.getBirthdayMessage();
        if(this$birthdayMessage == null ? other$birthdayMessage != null : !this$birthdayMessage.equals(other$birthdayMessage))
            return false;
        if(this.isCustomAdminLockNew() != other.isCustomAdminLockNew()) return false;
        final Object this$mpLinkedTo = this.getMpLinkedTo();
        final Object other$mpLinkedTo = other.getMpLinkedTo();
        if(this$mpLinkedTo == null ? other$mpLinkedTo != null : !this$mpLinkedTo.equals(other$mpLinkedTo)) return false;
        final Object this$modLogBlacklistWords = this.getModLogBlacklistWords();
        final Object other$modLogBlacklistWords = other.getModLogBlacklistWords();
        if(this$modLogBlacklistWords == null ? other$modLogBlacklistWords != null : !this$modLogBlacklistWords.equals(other$modLogBlacklistWords))
            return false;
        final Object this$autoroleCategories = this.getAutoroleCategories();
        final Object other$autoroleCategories = other.getAutoroleCategories();
        if(this$autoroleCategories == null ? other$autoroleCategories != null : !this$autoroleCategories.equals(other$autoroleCategories))
            return false;
        final Object this$editMessageLog = this.getEditMessageLog();
        final Object other$editMessageLog = other.getEditMessageLog();
        if(this$editMessageLog == null ? other$editMessageLog != null : !this$editMessageLog.equals(other$editMessageLog))
            return false;
        final Object this$deleteMessageLog = this.getDeleteMessageLog();
        final Object other$deleteMessageLog = other.getDeleteMessageLog();
        if(this$deleteMessageLog == null ? other$deleteMessageLog != null : !this$deleteMessageLog.equals(other$deleteMessageLog))
            return false;
        final Object this$bannedMemberLog = this.getBannedMemberLog();
        final Object other$bannedMemberLog = other.getBannedMemberLog();
        if(this$bannedMemberLog == null ? other$bannedMemberLog != null : !this$bannedMemberLog.equals(other$bannedMemberLog))
            return false;
        final Object this$unbannedMemberLog = this.getUnbannedMemberLog();
        final Object other$unbannedMemberLog = other.getUnbannedMemberLog();
        if(this$unbannedMemberLog == null ? other$unbannedMemberLog != null : !this$unbannedMemberLog.equals(other$unbannedMemberLog))
            return false;
        final Object this$kickedMemberLog = this.getKickedMemberLog();
        final Object other$kickedMemberLog = other.getKickedMemberLog();
        if(this$kickedMemberLog == null ? other$kickedMemberLog != null : !this$kickedMemberLog.equals(other$kickedMemberLog))
            return false;
        return this.isCommandWarningDisplay() == other.isCommandWarningDisplay();
    }
    
    public String toString() {
        return "GuildData(antiSpam=" + this.isAntiSpam() + ", autoroles=" + this.getAutoroles() + ", birthdayChannel=" + this.getBirthdayChannel() + ", birthdayRole=" + this.getBirthdayRole() + ", cases=" + this.getCases() + ", channelSpecificDisabledCategories=" + this.getChannelSpecificDisabledCategories() + ", channelSpecificDisabledCommands=" + this.getChannelSpecificDisabledCommands() + ", customAdminLock=" + this.isCustomAdminLock() + ", disabledCategories=" + this.getDisabledCategories() + ", disabledChannels=" + this.getDisabledChannels() + ", disabledCommands=" + this.getDisabledCommands() + ", disabledRoles=" + this.getDisabledRoles() + ", disabledUsers=" + this.getDisabledUsers() + ", guildAutoRole=" + this.getGuildAutoRole() + ", guildCustomPrefix=" + this.getGuildCustomPrefix() + ", guildLogChannel=" + this.getGuildLogChannel() + ", guildUnsafeChannels=" + this.getGuildUnsafeChannels() + ", joinMessage=" + this.getJoinMessage() + ", leaveMessage=" + this.getLeaveMessage() + ", linkProtection=" + this.isLinkProtection() + ", linkProtectionAllowedChannels=" + this.getLinkProtectionAllowedChannels() + ", logExcludedChannels=" + this.getLogExcludedChannels() + ", logJoinLeaveChannel=" + this.getLogJoinLeaveChannel() + ", maxFairQueue=" + this.getMaxFairQueue() + ", maxResultsSearch=" + this.getMaxResultsSearch() + ", modlogBlacklistedPeople=" + this.getModlogBlacklistedPeople() + ", musicAnnounce=" + this.isMusicAnnounce() + ", musicChannel=" + this.getMusicChannel() + ", musicQueueSizeLimit=" + this.getMusicQueueSizeLimit() + ", musicSongDurationLimit=" + this.getMusicSongDurationLimit() + ", mutedRole=" + this.getMutedRole() + ", mutedTimelyUsers=" + this.getMutedTimelyUsers() + ", noMentionsAction=" + this.isNoMentionsAction() + ", premiumKey=" + this.getPremiumKey() + ", quoteLastId=" + this.getQuoteLastId() + ", ranPolls=" + this.getRanPolls() + ", reactionMenus=" + this.isReactionMenus() + ", rolesBlockedFromCommands=" + this.getRolesBlockedFromCommands() + ", rpgDevaluation=" + this.isRpgDevaluation() + ", rpgLocalMode=" + this.isRpgLocalMode() + ", setModTimeout=" + this.getSetModTimeout() + ", slowMode=" + this.isSlowMode() + ", slowModeChannels=" + this.getSlowModeChannels() + ", spamModeChannels=" + this.getSpamModeChannels() + ", timeDisplay=" + this.getTimeDisplay() + ", warnActions=" + this.getWarnActions() + ", warnCount=" + this.getWarnCount() + ", gameTimeoutExpectedAt=" + this.getGameTimeoutExpectedAt() + ", ignoreBotsWelcomeMessage=" + this.isIgnoreBotsWelcomeMessage() + ", ignoreBotsAutoRole=" + this.isIgnoreBotsAutoRole() + ", enabledLevelUpMessages=" + this.isEnabledLevelUpMessages() + ", levelUpChannel=" + this.getLevelUpChannel() + ", levelUpMessage=" + this.getLevelUpMessage() + ", blackListedImageTags=" + this.getBlackListedImageTags() + ", logJoinChannel=" + this.getLogJoinChannel() + ", logLeaveChannel=" + this.getLogLeaveChannel() + ", localPlayerExperience=" + this.getLocalPlayerExperience() + ", linkProtectionAllowedUsers=" + this.getLinkProtectionAllowedUsers() + ", roleSpecificDisabledCategories=" + this.getRoleSpecificDisabledCategories() + ", roleSpecificDisabledCommands=" + this.getRoleSpecificDisabledCommands() + ", lang=" + this.getLang() + ", musicVote=" + this.isMusicVote() + ", extraJoinMessages=" + this.getExtraJoinMessages() + ", extraLeaveMessages=" + this.getExtraLeaveMessages() + ", whitelistedRole=" + this.getWhitelistedRole() + ", birthdayMessage=" + this.getBirthdayMessage() + ", customAdminLockNew=" + this.isCustomAdminLockNew() + ", mpLinkedTo=" + this.getMpLinkedTo() + ", modLogBlacklistWords=" + this.getModLogBlacklistWords() + ", autoroleCategories=" + this.getAutoroleCategories() + ", editMessageLog=" + this.getEditMessageLog() + ", deleteMessageLog=" + this.getDeleteMessageLog() + ", bannedMemberLog=" + this.getBannedMemberLog() + ", unbannedMemberLog=" + this.getUnbannedMemberLog() + ", kickedMemberLog=" + this.getKickedMemberLog() + ", commandWarningDisplay=" + this.isCommandWarningDisplay() + ")";
    }
}
