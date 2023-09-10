package net.kodehawa.mantarobot.db.entities;

import net.kodehawa.mantarobot.commands.utils.polls.Poll;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.annotations.ConfigName;
import net.kodehawa.mantarobot.data.annotations.HiddenConfig;
import net.kodehawa.mantarobot.db.ManagedMongoObject;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.Pair;
import net.kodehawa.mantarobot.utils.patreon.PatreonPledge;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

@SuppressWarnings("unused")
public class MongoGuild implements ManagedMongoObject {
    @BsonIgnore
    public static final String DB_TABLE = "guilds";
    @BsonIgnore
    private final Config config = MantaroData.config().get();
    @BsonIgnore
    public Map<String, Object> fieldTracker = new HashMap<>();

    @BsonId
    private String id;
    @HiddenConfig
    private long premiumUntil = 0L;
    @ConfigName("Autoroles")
    private Map<String, String> autoroles = new HashMap<>();
    @ConfigName("Birthday Announcer Channel")
    private String birthdayChannel = null;
    @ConfigName("Birthday Announcer Role")
    private String birthdayRole = null;
    @ConfigName("Mod action counter")
    private long cases = 0L;
    @ConfigName("Categories disabled in channels")
    private Map<String, List<CommandCategory>> channelSpecificDisabledCategories = new HashMap<>();
    @ConfigName("Commands disabled in channels")
    private Map<String, List<String>> channelSpecificDisabledCommands = new HashMap<>();
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

    // ------------------------- DATA CLASS START ------------------------- //
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
    @ConfigName("Action commands ping (for giver)")
    private boolean noMentionsAction = false;
    @HiddenConfig // We do not need to show this
    private String premiumKey;
    @ConfigName("Amount of polls ran")
    private long ranPolls = 0L;
    @ConfigName("Roles that can't use commands")
    private List<String> rolesBlockedFromCommands = new ArrayList<>();
    @ConfigName("Mute default timeout")
    private long setModTimeout = 0L;
    @ConfigName("How will Mantaro display time")
    private int timeDisplay = 0; //0 = 24h, 1 = 12h
    @HiddenConfig // we do not need to show this to the user
    private String gameTimeoutExpectedAt;
    @ConfigName("Ignore bots: welcome message")
    private boolean ignoreBotsWelcomeMessage = false;
    @ConfigName("Ignore bots: autorole")
    private boolean ignoreBotsAutoRole = false;
    @ConfigName("Blacklisted image tags")
    private Set<String> blackListedImageTags = new HashSet<>();
    @ConfigName("Channel (id): Join message channel")
    private String logJoinChannel = null;
    @ConfigName("Channel (id): Leave message channel")
    private String logLeaveChannel = null;
    @ConfigName("Disabled Categories for Role (id)")
    private Map<String, List<CommandCategory>> roleSpecificDisabledCategories = new HashMap<>();
    @ConfigName("Disabled Commands for Role (id)")
    private Map<String, List<String>> roleSpecificDisabledCommands = new HashMap<>();
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
    private boolean hasReceivedGreet = false;
    @ConfigName("People blocked from the birthday logging on this server.")
    private List<String> birthdayBlockedIds = new ArrayList<>();
    @ConfigName("Disabled game lobby/multiple")
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
    @ConfigName("Custom DJ role.")
    private String djRoleId;
    @ConfigName("Custom music queue size limit.")
    private Long musicQueueSizeLimit = null;
    @HiddenConfig // its a list of polls
    private Map<String, Poll.PollDatabaseObject> runningPolls = new HashMap<>();

    // Constructors needed for the Mongo Codec to deserialize/serialize this.
    public MongoGuild() {
    }

    public MongoGuild(String id) {
        this.id = id;
    }

    @BsonIgnore
    public static MongoGuild of(String guildId) {
        return new MongoGuild(guildId);
    }

    public @NotNull String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @BsonIgnore
    @Override
    public @NotNull String getTableName() {
        return DB_TABLE;
    }

    @Override
    @BsonIgnore
    public void save() {
        MantaroData.db().saveMongo(this, MongoGuild.class);
    }

    @Override
    @BsonIgnore
    public void delete() {
        MantaroData.db().deleteMongo(this, MongoGuild.class);
    }

    @BsonIgnore
    @Override
    public void updateAllChanged() {
        MantaroData.db().updateFieldValues(this, fieldTracker);
    }

    @BsonIgnore
    public long getPremiumLeft() {
        return isPremium() ? this.premiumUntil - currentTimeMillis() : 0;
    }

    @BsonIgnore
    public boolean isPremium() {
        PremiumKey key = MantaroData.db().getPremiumKey(getPremiumKey());
        //Key validation check (is it still active? delete otherwise)
        if (key != null) {
            boolean isKeyActive = currentTimeMillis() < key.getExpiration();
            if (!isKeyActive && LocalDate.now(ZoneId.of("America/Chicago")).getDayOfMonth() > 5) {
                MongoUser owner = MantaroData.db().getUser(key.getOwner());
                owner.removeKeyClaimed(getId());
                owner.updateAllChanged();

                removePremiumKey(key.getOwner(), key.getId());
                key.delete();
                return false;
            }

            //Link key to owner if key == owner and key holder is on patreon.
            //Sadly gotta skip of holder isn't patron here bc there are some bought keys (paypal) which I can't convert without invalidating
            Pair<Boolean, String> pledgeInfo = APIUtils.getPledgeInformation(key.getOwner());
            if (pledgeInfo != null && pledgeInfo.left()) {
                key.setLinkedTo(key.getOwner());
                key.save(); //doesn't matter if it doesn't save immediately, will do later anyway (key is usually immutable in db)
            }

            //If the receipt is not the owner, account them to the keys the owner has claimed.
            //This has usage later when seeing how many keys can they take. The second/third check is kind of redundant, but necessary anyway to see if it works.
            String keyLinkedTo = key.getLinkedTo();
            if (keyLinkedTo != null) {
                MongoUser owner = MantaroData.db().getUser(keyLinkedTo);
                if (!owner.getKeysClaimed().containsKey(getId())) {
                    owner.addKeyClaimed(getId(), key.getId());
                    owner.updateAllChanged();
                }
            }
        }

        //Patreon bot link check.
        String linkedTo = getMpLinkedTo();
        if (config.isPremiumBot() && linkedTo != null && key == null) { //Key should always be null in MP anyway.
            PatreonPledge pledgeInfo = APIUtils.getFullPledgeInformation(linkedTo);
            if (pledgeInfo != null && pledgeInfo.getReward().getKeyAmount() >= 3) {
                // Subscribed to MP properly.
                return true;
            }
        }

        //MP uses the old premium system for some guilds: keep it here.
        return currentTimeMillis() < premiumUntil || (key != null && currentTimeMillis() < key.getExpiration() && key.getParsedType().equals(PremiumKey.Type.GUILD));
    }

    @BsonIgnore
    public PremiumKey generateAndApplyPremiumKey(int days) {
        String premiumId = UUID.randomUUID().toString();
        PremiumKey newKey = new PremiumKey(premiumId, TimeUnit.DAYS.toMillis(days),
                currentTimeMillis() + TimeUnit.DAYS.toMillis(days), PremiumKey.Type.GUILD, true, id, null);
        premiumKey(premiumId);
        newKey.save();

        updateAllChanged();
        return newKey;
    }

    @BsonIgnore
    public void removePremiumKey(String keyOwner, String originalKey) {
        premiumKey(null);

        MongoUser dbUser = MantaroData.db().getUser(keyOwner);
        dbUser.removePremiumKey(dbUser.getUserIdFromKeyId(originalKey));
        dbUser.updateAllChanged();

        updateAllChanged();
    }

    @BsonIgnore
    public void incrementPremium(long milliseconds) {
        if (isPremium()) {
            this.premiumUntil += milliseconds;
        } else {
            this.premiumUntil = currentTimeMillis() + milliseconds;
        }

        fieldTracker.put("premiumUntil", this.premiumUntil);
    }

    public Map<String, String> getAutoroles() {
        return autoroles;
    }

    public Map<String, List<CommandCategory>> getChannelSpecificDisabledCategories() {
        return channelSpecificDisabledCategories;
    }

    public Map<String, List<String>> getChannelSpecificDisabledCommands() {
        return channelSpecificDisabledCommands;
    }

    public Set<CommandCategory> getDisabledCategories() {
        return disabledCategories;
    }

    public Set<String> getDisabledChannels() {
        return disabledChannels;
    }

    public Set<String> getDisabledCommands() {
        return disabledCommands;
    }

    public Set<String> getDisabledRoles() {
        return disabledRoles;
    }

    public List<String> getDisabledUsers() {
        return disabledUsers;
    }

    public Set<String> getLogExcludedChannels() {
        return logExcludedChannels;
    }

    public Set<String> getModlogBlacklistedPeople() {
        return modlogBlacklistedPeople;
    }

    public List<String> getRolesBlockedFromCommands() {
        return rolesBlockedFromCommands;
    }

    public Set<String> getBlackListedImageTags() {
        return blackListedImageTags;
    }

    public Map<String, List<CommandCategory>> getRoleSpecificDisabledCategories() {
        return roleSpecificDisabledCategories;
    }

    public Map<String, List<String>> getRoleSpecificDisabledCommands() {
        return roleSpecificDisabledCommands;
    }

    public List<String> getExtraJoinMessages() {
        return extraJoinMessages;
    }

    public List<String> getExtraLeaveMessages() {
        return extraLeaveMessages;
    }

    public List<String> getModLogBlacklistWords() {
        return modLogBlacklistWords;
    }

    public Map<String, List<String>> getAutoroleCategories() {
        return autoroleCategories;
    }

    public List<String> getBirthdayBlockedIds() {
        return birthdayBlockedIds;
    }

    public List<String> getAllowedBirthdays() {
        return allowedBirthdays;
    }

    public Map<String, Poll.PollDatabaseObject> getRunningPolls() {
        return runningPolls;
    }

    public long getPremiumUntil() {
        return premiumUntil;
    }

    public long getCases() {
        return cases;
    }

    public String getGuildAutoRole() {
        return guildAutoRole;
    }

    public String getGuildCustomPrefix() {
        return guildCustomPrefix;
    }

    public String getGuildLogChannel() {
        return guildLogChannel;
    }

    public String getJoinMessage() {
        return joinMessage;
    }

    public String getLeaveMessage() {
        return leaveMessage;
    }

    public String getLogJoinLeaveChannel() {
        return logJoinLeaveChannel;
    }

    public int getMaxFairQueue() {
        return maxFairQueue;
    }

    public boolean isMusicAnnounce() {
        return musicAnnounce;
    }

    public String getMusicChannel() {
        return musicChannel;
    }

    public String getMutedRole() {
        return mutedRole;
    }

    public boolean isNoMentionsAction() {
        return noMentionsAction;
    }

    public String getPremiumKey() {
        return premiumKey;
    }

    public long getRanPolls() {
        return ranPolls;
    }

    public long getSetModTimeout() {
        return setModTimeout;
    }

    public int getTimeDisplay() {
        return timeDisplay;
    }

    public String getGameTimeoutExpectedAt() {
        return gameTimeoutExpectedAt;
    }

    public boolean isIgnoreBotsWelcomeMessage() {
        return ignoreBotsWelcomeMessage;
    }

    public boolean isIgnoreBotsAutoRole() {
        return ignoreBotsAutoRole;
    }

    public String getLogJoinChannel() {
        return logJoinChannel;
    }

    public String getLogLeaveChannel() {
        return logLeaveChannel;
    }

    public String getLang() {
        return lang;
    }

    public boolean isMusicVote() {
        return musicVote;
    }

    public String getBirthdayMessage() {
        return birthdayMessage;
    }

    public boolean isCustomAdminLockNew() {
        return customAdminLockNew;
    }

    public String getMpLinkedTo() {
        return mpLinkedTo;
    }

    public String getEditMessageLog() {
        return editMessageLog;
    }

    public String getDeleteMessageLog() {
        return deleteMessageLog;
    }

    public String getBannedMemberLog() {
        return bannedMemberLog;
    }

    public String getUnbannedMemberLog() {
        return unbannedMemberLog;
    }

    public String getKickedMemberLog() {
        return kickedMemberLog;
    }

    public boolean isCommandWarningDisplay() {
        return commandWarningDisplay;
    }

    public boolean isHasReceivedGreet() {
        return hasReceivedGreet;
    }

    public boolean isGameMultipleDisabled() {
        return gameMultipleDisabled;
    }

    public String getLogTimezone() {
        return logTimezone;
    }

    public boolean isNotifiedFromBirthdayChange() {
        return notifiedFromBirthdayChange;
    }

    public String getDjRoleId() {
        return djRoleId;
    }

    public boolean isDisableExplicit() {
        return disableExplicit;
    }

    public Long getMusicQueueSizeLimit() {
        return musicQueueSizeLimit;
    }

    public String getBirthdayRole() {
        return birthdayRole;
    }

    public String getBirthdayChannel() {
        return birthdayChannel;
    }

    public boolean hasReceivedGreet() {
        return hasReceivedGreet;
    }

    protected void setPremiumUntil(long premiumUntil) {
        this.premiumUntil = premiumUntil;
    }

    protected void setAutoroles(Map<String, String> autoroles) {
        this.autoroles = autoroles;
    }

    protected void setBirthdayChannel(String birthdayChannel) {
        this.birthdayChannel = birthdayChannel;
    }

    protected void setBirthdayRole(String birthdayRole) {
        this.birthdayRole = birthdayRole;
    }

    protected void setCases(long cases) {
        this.cases = cases;
    }

    protected void setChannelSpecificDisabledCategories(Map<String, List<CommandCategory>> channelSpecificDisabledCategories) {
        this.channelSpecificDisabledCategories = channelSpecificDisabledCategories;
    }

    protected void setChannelSpecificDisabledCommands(Map<String, List<String>> channelSpecificDisabledCommands) {
        this.channelSpecificDisabledCommands = channelSpecificDisabledCommands;
    }

    protected void setDisabledCategories(Set<CommandCategory> disabledCategories) {
        this.disabledCategories = disabledCategories;
    }

    protected void setDisabledChannels(Set<String> disabledChannels) {
        this.disabledChannels = disabledChannels;
    }

    protected void setDisabledCommands(Set<String> disabledCommands) {
        this.disabledCommands = disabledCommands;
    }

    protected void setDisabledRoles(Set<String> disabledRoles) {
        this.disabledRoles = disabledRoles;
    }

    protected void setDisabledUsers(List<String> disabledUsers) {
        this.disabledUsers = disabledUsers;
    }

    protected void setGuildAutoRole(String guildAutoRole) {
        this.guildAutoRole = guildAutoRole;
    }

    protected void setGuildCustomPrefix(String guildCustomPrefix) {
        this.guildCustomPrefix = guildCustomPrefix;
    }

    protected void setGuildLogChannel(String guildLogChannel) {
        this.guildLogChannel = guildLogChannel;
    }

    protected void setJoinMessage(String joinMessage) {
        this.joinMessage = joinMessage;
    }

    protected void setLeaveMessage(String leaveMessage) {
        this.leaveMessage = leaveMessage;
    }

    protected void setLogExcludedChannels(Set<String> logExcludedChannels) {
        this.logExcludedChannels = logExcludedChannels;
    }

    protected void setLogJoinLeaveChannel(String logJoinLeaveChannel) {
        this.logJoinLeaveChannel = logJoinLeaveChannel;
    }

    protected void setMaxFairQueue(int maxFairQueue) {
        this.maxFairQueue = maxFairQueue;
    }

    protected void setModlogBlacklistedPeople(Set<String> modlogBlacklistedPeople) {
        this.modlogBlacklistedPeople = modlogBlacklistedPeople;
    }

    protected void setMusicAnnounce(boolean musicAnnounce) {
        this.musicAnnounce = musicAnnounce;
    }

    protected void setMusicChannel(String musicChannel) {
        this.musicChannel = musicChannel;
    }

    protected void setMutedRole(String mutedRole) {
        this.mutedRole = mutedRole;
    }

    protected void setNoMentionsAction(boolean noMentionsAction) {
        this.noMentionsAction = noMentionsAction;
    }

    protected void setPremiumKey(String premiumKey) {
        this.premiumKey = premiumKey;
    }

    protected void setRanPolls(long ranPolls) {
        this.ranPolls = ranPolls;
    }

    protected void setRolesBlockedFromCommands(List<String> rolesBlockedFromCommands) {
        this.rolesBlockedFromCommands = rolesBlockedFromCommands;
    }

    protected void setSetModTimeout(long setModTimeout) {
        this.setModTimeout = setModTimeout;
    }

    protected void setTimeDisplay(int timeDisplay) {
        this.timeDisplay = timeDisplay;
    }

    protected void setGameTimeoutExpectedAt(String gameTimeoutExpectedAt) {
        this.gameTimeoutExpectedAt = gameTimeoutExpectedAt;
    }

    protected void setBlackListedImageTags(Set<String> blackListedImageTags) {
        this.blackListedImageTags = blackListedImageTags;
    }

    protected void setIgnoreBotsAutoRole(boolean ignoreBotsAutoRole) {
        this.ignoreBotsAutoRole = ignoreBotsAutoRole;
    }

    protected void setLogJoinChannel(String logJoinChannel) {
        this.logJoinChannel = logJoinChannel;
    }

    protected void setIgnoreBotsWelcomeMessage(boolean ignoreBotsWelcomeMessage) {
        this.ignoreBotsWelcomeMessage = ignoreBotsWelcomeMessage;
    }

    protected void setRoleSpecificDisabledCategories(Map<String, List<CommandCategory>> roleSpecificDisabledCategories) {
        this.roleSpecificDisabledCategories = roleSpecificDisabledCategories;
    }

    protected void setRoleSpecificDisabledCommands(Map<String, List<String>> roleSpecificDisabledCommands) {
        this.roleSpecificDisabledCommands = roleSpecificDisabledCommands;
    }

    protected void setMusicVote(boolean musicVote) {
        this.musicVote = musicVote;
    }

    protected void setExtraJoinMessages(List<String> extraJoinMessages) {
        this.extraJoinMessages = extraJoinMessages;
    }

    protected void setExtraLeaveMessages(List<String> extraLeaveMessages) {
        this.extraLeaveMessages = extraLeaveMessages;
    }

    protected void setBirthdayMessage(String birthdayMessage) {
        this.birthdayMessage = birthdayMessage;
    }

    protected void setCustomAdminLockNew(boolean customAdminLockNew) {
        this.customAdminLockNew = customAdminLockNew;
    }

    protected void setMpLinkedTo(String mpLinkedTo) {
        this.mpLinkedTo = mpLinkedTo;
    }

    protected void setModLogBlacklistWords(List<String> modLogBlacklistWords) {
        this.modLogBlacklistWords = modLogBlacklistWords;
    }

    protected void setAutoroleCategories(Map<String, List<String>> autoroleCategories) {
        this.autoroleCategories = autoroleCategories;
    }

    protected void setDeleteMessageLog(String deleteMessageLog) {
        this.deleteMessageLog = deleteMessageLog;
    }

    protected void setEditMessageLog(String editMessageLog) {
        this.editMessageLog = editMessageLog;
    }

    protected void setBannedMemberLog(String bannedMemberLog) {
        this.bannedMemberLog = bannedMemberLog;
    }

    protected void setUnbannedMemberLog(String unbannedMemberLog) {
        this.unbannedMemberLog = unbannedMemberLog;
    }

    protected void setKickedMemberLog(String kickedMemberLog) {
        this.kickedMemberLog = kickedMemberLog;
    }

    protected void setCommandWarningDisplay(boolean commandWarningDisplay) {
        this.commandWarningDisplay = commandWarningDisplay;
    }

    protected void setGameMultipleDisabled(boolean gameMultipleDisabled) {
        this.gameMultipleDisabled = gameMultipleDisabled;
    }

    protected void setBirthdayBlockedIds(List<String> birthdayBlockedIds) {
        this.birthdayBlockedIds = birthdayBlockedIds;
    }

    protected void setHasReceivedGreet(boolean hasReceivedGreet) {
        this.hasReceivedGreet = hasReceivedGreet;
    }

    protected void setLogTimezone(String logTimezone) {
        this.logTimezone = logTimezone;
    }

    protected void setAllowedBirthdays(List<String> allowedBirthdays) {
        this.allowedBirthdays = allowedBirthdays;
    }

    protected void setNotifiedFromBirthdayChange(boolean notifiedFromBirthdayChange) {
        this.notifiedFromBirthdayChange = notifiedFromBirthdayChange;
    }

    protected void setDjRoleId(String djRoleId) {
        this.djRoleId = djRoleId;
    }

    protected void setDisableExplicit(boolean disableExplicit) {
        this.disableExplicit = disableExplicit;
    }

    protected void setMusicQueueSizeLimit(Long musicQueueSizeLimit) {
        this.musicQueueSizeLimit = musicQueueSizeLimit;
    }

    protected void setRunningPolls(Map<String, Poll.PollDatabaseObject> polls) {
        this.runningPolls = polls;
    }

    protected void setLogLeaveChannel(String logLeaveChannel) {
        this.logLeaveChannel = logLeaveChannel;
    }

    protected void setLang(String lang) {
        this.lang = lang;
    }

    // Database helpers to track changes -- the setX methods get called upon serialization so they can't hold the tracker.
    public void premiumUntil(long premiumUntil) {
        this.premiumUntil = premiumUntil;
        fieldTracker.put("premiumUntil", this.premiumUntil);
    }

    public void autoroles(Map<String, String> autoroles) {
        this.autoroles = autoroles;
        fieldTracker.put("autoroles", this.autoroles);
    }

    public void birthdayChannel(String birthdayChannel) {
        this.birthdayChannel = birthdayChannel;
        fieldTracker.put("birthdayChannel", this.birthdayChannel);
    }

    public void birthdayRole(String birthdayRole) {
        this.birthdayRole = birthdayRole;
        fieldTracker.put("birthdayRole", this.birthdayRole);
    }

    public void birthdayMessage(String birthdayMessage) {
        this.birthdayMessage = birthdayMessage;
        fieldTracker.put("birthdayMessage", this.birthdayMessage);
    }

    public void cases(long cases) {
        this.cases = cases;
        fieldTracker.put("cases", this.cases);
    }

    public void lang(String lang) {
        this.lang = lang;
        fieldTracker.put("lang", this.lang);
    }

    public void guildAutoRole(String guildAutoRole) {
        this.guildAutoRole = guildAutoRole;
        fieldTracker.put("guildAutoRole", this.guildAutoRole);
    }

    public void guildCustomPrefix(String guildCustomPrefix) {
        this.guildCustomPrefix = guildCustomPrefix;
        fieldTracker.put("guildCustomPrefix", this.guildCustomPrefix);
    }

    public void guildLogChannel(String guildLogChannel) {
        this.guildLogChannel = guildLogChannel;
        fieldTracker.put("guildLogChannel", this.guildLogChannel);
    }

    public void joinMessage(String joinMessage) {
        this.joinMessage = joinMessage;
        fieldTracker.put("joinMessage", this.joinMessage);
    }

    public void leaveMessage(String leaveMessage) {
        this.leaveMessage = leaveMessage;
        fieldTracker.put("leaveMessage", this.leaveMessage);
    }

    public void logJoinLeaveChannel(String logJoinLeaveChannel) {
        this.logJoinLeaveChannel = logJoinLeaveChannel;
        fieldTracker.put("logJoinLeaveChannel", this.logJoinLeaveChannel);
    }

    public void maxFairQueue(int maxFairQueue) {
        this.maxFairQueue = maxFairQueue;
        fieldTracker.put("maxFairQueue", this.maxFairQueue);
    }

    public void musicAnnounce(boolean musicAnnounce) {
        this.musicAnnounce = musicAnnounce;
        fieldTracker.put("musicAnnounce", this.musicAnnounce);
    }

    public void musicChannel(String musicChannel) {
        this.musicChannel = musicChannel;
        fieldTracker.put("musicChannel", this.musicChannel);
    }

    public void mutedRole(String mutedRole) {
        this.mutedRole = mutedRole;
        fieldTracker.put("mutedRole", this.mutedRole);
    }

    public void noMentionsAction(boolean noMentionsAction) {
        this.noMentionsAction = noMentionsAction;
        fieldTracker.put("noMentionsAction", this.noMentionsAction);
    }

    public void premiumKey(String premiumKey) {
        this.premiumKey = premiumKey;
        fieldTracker.put("premiumKey", this.premiumKey);
    }

    public void ranPolls(long ranPolls) {
        this.ranPolls = ranPolls;
        fieldTracker.put("ranPolls", this.ranPolls);
    }

    public void rolesBlockedFromCommands(List<String> rolesBlockedFromCommands) {
        this.rolesBlockedFromCommands = rolesBlockedFromCommands;
        fieldTracker.put("rolesBlockedFromCommands", this.rolesBlockedFromCommands);
    }

    public void modTimeout(long setModTimeout) {
        this.setModTimeout = setModTimeout;
        fieldTracker.put("setModTimeout", this.setModTimeout);
    }

    public void timeDisplay(int timeDisplay) {
        this.timeDisplay = timeDisplay;
        fieldTracker.put("timeDisplay", this.timeDisplay);
    }

    public void gameTimeoutExpectedAt(String gameTimeoutExpectedAt) {
        this.gameTimeoutExpectedAt = gameTimeoutExpectedAt;
        fieldTracker.put("gameTimeoutExpectedAt", this.gameTimeoutExpectedAt);
    }

    public void ignoreBotsAutoRole(boolean ignoreBotsAutoRole) {
        this.ignoreBotsAutoRole = ignoreBotsAutoRole;
        fieldTracker.put("ignoreBotsAutoRole", this.ignoreBotsAutoRole);
    }

    public void logJoinChannel(String logJoinChannel) {
        this.logJoinChannel = logJoinChannel;
        fieldTracker.put("logJoinChannel", this.logJoinChannel);
    }

    public void ignoreBotsWelcomeMessage(boolean ignoreBotsWelcomeMessage) {
        this.ignoreBotsWelcomeMessage = ignoreBotsWelcomeMessage;
        fieldTracker.put("ignoreBotsWelcomeMessage", this.ignoreBotsWelcomeMessage);
    }

    public void musicVote(boolean musicVote) {
        this.musicVote = musicVote;
        fieldTracker.put("musicVote", this.musicVote);
    }

    public void customAdminLockNew(boolean customAdminLockNew) {
        this.customAdminLockNew = customAdminLockNew;
        fieldTracker.put("customAdminLockNew", this.customAdminLockNew);
    }

    public void mpLinkedTo(String mpLinkedTo) {
        this.mpLinkedTo = mpLinkedTo;
        fieldTracker.put("mpLinkedTo", this.mpLinkedTo);
    }

    public void deleteMessageLog(String deleteMessageLog) {
        this.deleteMessageLog = deleteMessageLog;
        fieldTracker.put("deleteMessageLog", this.deleteMessageLog);
    }

    public void editMessageLog(String editMessageLog) {
        this.editMessageLog = editMessageLog;
        fieldTracker.put("editMessageLog", this.editMessageLog);
    }

    public void bannedMemberLog(String bannedMemberLog) {
        this.bannedMemberLog = bannedMemberLog;
        fieldTracker.put("bannedMemberLog", this.bannedMemberLog);
    }

    public void unbannedMemberLog(String unbannedMemberLog) {
        this.unbannedMemberLog = unbannedMemberLog;
        fieldTracker.put("unbannedMemberLog", this.unbannedMemberLog);
    }

    public void kickedMemberLog(String kickedMemberLog) {
        this.kickedMemberLog = kickedMemberLog;
        fieldTracker.put("kickedMemberLog", this.kickedMemberLog);
    }

    public void commandWarningDisplay(boolean commandWarningDisplay) {
        this.commandWarningDisplay = commandWarningDisplay;
        fieldTracker.put("commandWarningDisplay", this.commandWarningDisplay);
    }

    public void gameMultipleDisabled(boolean gameMultipleDisabled) {
        this.gameMultipleDisabled = gameMultipleDisabled;
        fieldTracker.put("gameMultipleDisabled", this.gameMultipleDisabled);
    }

    public void receivedGreet(boolean hasReceivedGreet) {
        this.hasReceivedGreet = hasReceivedGreet;
        fieldTracker.put("hasReceivedGreet", this.hasReceivedGreet);
    }

    public void logTimezone(String logTimezone) {
        this.logTimezone = logTimezone;
        fieldTracker.put("logTimezone", this.logTimezone);
    }

    public void notifiedFromBirthdayChange(boolean notifiedFromBirthdayChange) {
        this.notifiedFromBirthdayChange = notifiedFromBirthdayChange;
        fieldTracker.put("notifiedFromBirthdayChange", this.notifiedFromBirthdayChange);
    }

    public void djRoleId(String djRoleId) {
        this.djRoleId = djRoleId;
        fieldTracker.put("djRoleId", this.djRoleId);
    }

    public void disableExplicit(boolean disableExplicit) {
        this.disableExplicit = disableExplicit;
        fieldTracker.put("disableExplicit", this.disableExplicit);
    }

    public void musicQueueSizeLimit(Long musicQueueSizeLimit) {
        this.musicQueueSizeLimit = musicQueueSizeLimit;
        fieldTracker.put("musicQueueSizeLimit", this.musicQueueSizeLimit);
    }

    public void logLeaveChannel(String logLeaveChannel) {
        this.logLeaveChannel = logLeaveChannel;
        fieldTracker.put("logLeaveChannel", this.logLeaveChannel);
    }

    // --- List helpers
    public void addChannelSpecificDisabledCategory(String channel, CommandCategory category) {
        channelSpecificDisabledCategories.compute(channel, (k, list) -> {
            list = list != null ? list : new ArrayList<>();
            list.add(category);
            return list;
        });

        fieldTracker.put("channelSpecificDisabledCategories", this.channelSpecificDisabledCategories);
    }

    public void removeChannelSpecificDisabledCategory(String channel, CommandCategory category) {
        channelSpecificDisabledCategories.computeIfPresent(channel, (k, list) -> {
            list.remove(category);
            return list;
        });

        fieldTracker.put("channelSpecificDisabledCategories", this.channelSpecificDisabledCategories);
    }

    public void addChannelSpecificDisabledCommand(String channel, String command) {
        channelSpecificDisabledCommands.compute(channel, (k, list) -> {
            list = list != null ? list : new ArrayList<>();
            list.add(command);
            return list;
        });

        fieldTracker.put("channelSpecificDisabledCommands", this.channelSpecificDisabledCategories);
    }

    public void removeChannelSpecificDisabledCommand(String channel, String command) {
        channelSpecificDisabledCommands.computeIfPresent(channel, (k, list) -> {
            list.remove(command);
            return list;
        });

        fieldTracker.put("channelSpecificDisabledCommands", this.channelSpecificDisabledCategories);
    }

    public void addDisabledCategory(CommandCategory category) {
        disabledCategories.add(category);
        fieldTracker.put("disabledCategories", this.disabledCategories);
    }

    public void removeDisabledCategory(CommandCategory category) {
        disabledCategories.remove(category);
        fieldTracker.put("disabledCategories", this.disabledCategories);
    }

    public void addDisabledChannel(String channelId) {
        disabledChannels.add(channelId);
        fieldTracker.put("disabledChannels", this.disabledChannels);
    }

    public void removeDisabledChannel(String channelId) {
        disabledChannels.remove(channelId);
        fieldTracker.put("disabledChannels", this.disabledChannels);
    }

    public void addDisabledCommand(String command) {
        disabledCommands.add(command);
        fieldTracker.put("disabledCommands", this.disabledCommands);
    }

    public void removeDisabledCommand(String command) {
        disabledCommands.remove(command);
        fieldTracker.put("disabledCommands", this.disabledCommands);
    }

    public void addDisabledRole(String roleId) {
        disabledRoles.add(roleId);
        fieldTracker.put("disabledRoles", this.disabledRoles);
    }

    public void removeDisabledRole(String roleId) {
        disabledRoles.remove(roleId);
        fieldTracker.put("disabledRoles", this.disabledRoles);
    }

    public void addDisabledUser(String userId) {
        disabledUsers.add(userId);
        fieldTracker.put("disabledUsers", this.disabledUsers);
    }

    public void removeDisabledUser(String userId) {
        disabledUsers.remove(userId);
        fieldTracker.put("disabledUsers", this.disabledUsers);
    }

    public void addLogExcludedChannel(String channelId) {
        logExcludedChannels.add(channelId);
        fieldTracker.put("logExcludedChannels", this.logExcludedChannels);
    }

    public void removeLogExcludedChannel(String channelId) {
        logExcludedChannels.remove(channelId);
        fieldTracker.put("logExcludedChannels", this.logExcludedChannels);
    }

    public void clearLogExcludedChannels() {
        logExcludedChannels.clear();
        fieldTracker.put("logExcludedChannels", this.logExcludedChannels);
    }

    public void addModlogBlacklistedPeople(String userId) {
        modlogBlacklistedPeople.add(userId);
        fieldTracker.put("modlogBlacklistedPeople", this.modlogBlacklistedPeople);
    }

    public void removeModlogBlacklistedPeople(String userId) {
        modlogBlacklistedPeople.remove(userId);
        fieldTracker.put("modlogBlacklistedPeople", this.modlogBlacklistedPeople);
    }

    public void addRolesBlockedFromCommand(String roleId) {
        rolesBlockedFromCommands.add(roleId);
        fieldTracker.put("rolesBlockedFromCommands", this.rolesBlockedFromCommands);
    }

    public void addBlackListedImageTag(String tag) {
        blackListedImageTags.add(tag);
        fieldTracker.put("blackListedImageTags", this.blackListedImageTags);
    }

    public void removeBlackListedImageTag(String tag) {
        blackListedImageTags.remove(tag);
        fieldTracker.put("blackListedImageTags", this.blackListedImageTags);
    }

    public void addExtraJoinMessage(String message) {
        extraJoinMessages.add(message);
        fieldTracker.put("extraJoinMessages", this.extraJoinMessages);
    }

    public void removeExtraJoinMessage(String message) {
        extraJoinMessages.remove(message);
        fieldTracker.put("extraJoinMessages", this.extraJoinMessages);
    }

    public void clearExtraJoinMessages() {
        extraJoinMessages.clear();
        fieldTracker.put("extraJoinMessages", this.extraJoinMessages);
    }

    public void addExtraLeaveMessage(String message) {
        extraLeaveMessages.add(message);
        fieldTracker.put("extraLeaveMessages", this.extraLeaveMessages);
    }

    public void clearExtraLeaveMessages() {
        extraLeaveMessages.clear();
        fieldTracker.put("extraLeaveMessages", this.extraLeaveMessages);
    }

    public void removeExtraLeaveMessage(String message) {
        extraLeaveMessages.remove(message);
        fieldTracker.put("extraLeaveMessages", this.extraLeaveMessages);
    }

    public void addModLogBlacklistWord(String word) {
        modLogBlacklistWords.add(word);
        fieldTracker.put("modLogBlacklistWords", this.modLogBlacklistWords);
    }

    public void removeModLogBlacklistWord(String word) {
        modLogBlacklistWords.remove(word);
        fieldTracker.put("modLogBlacklistWords", this.modLogBlacklistWords);
    }

    public void addBirthdayBlockedId(String userId) {
        birthdayBlockedIds.add(userId);
        fieldTracker.put("birthdayBlockedIds", this.birthdayBlockedIds);
    }

    public void removeBirthdayBlockedId(String userId) {
        birthdayBlockedIds.remove(userId);
        fieldTracker.put("birthdayBlockedIds", this.birthdayBlockedIds);
    }

    public void addAllowedBirthdays(String userId) {
        allowedBirthdays.add(userId);
        fieldTracker.put("allowedBirthdays", this.allowedBirthdays);
    }

    public void removeAllowedBirthday(String userId) {
        allowedBirthdays.add(userId);
        fieldTracker.put("allowedBirthdays", this.allowedBirthdays);
    }

    public void addAutorole(String name, String roleId) {
        autoroles.put(name, roleId);
        fieldTracker.put("autoroles", this.autoroles);
    }

    public void removeAutorole(String name) {
        autoroles.remove(name);
        fieldTracker.put("autoroles", this.autoroles);
    }

    public void clearAutoroles() {
        autoroles.clear();
        fieldTracker.put("autoroles", this.autoroles);
    }

    public void addRoleSpecificDisabledCategory(String roleId, CommandCategory category) {
        roleSpecificDisabledCategories.compute(roleId, (k, list) -> {
            list = list != null ? list : new ArrayList<>();
            list.add(category);
            return list;
        });


        fieldTracker.put("roleSpecificDisabledCategories", this.roleSpecificDisabledCategories);
    }

    public void removeRoleSpecificDisabledCategory(String roleId, CommandCategory category) {
        roleSpecificDisabledCategories.computeIfPresent(roleId, (k, list) -> {
            list.remove(category);
            return list;
        });

        fieldTracker.put("roleSpecificDisabledCategories", this.roleSpecificDisabledCategories);
    }

    public void addRoleSpecificDisabledCommand(String roleId, String commandName) {
        roleSpecificDisabledCommands.compute(roleId, (k, list) -> {
            list = list != null ? list : new ArrayList<>();
            list.add(commandName);
            return list;
        });


        fieldTracker.put("roleSpecificDisabledCommands", this.roleSpecificDisabledCommands);
    }

    public void removeRoleSpecificDisabledCommand(String roleId, String commandName) {
        roleSpecificDisabledCommands.computeIfPresent(roleId, (k, list) -> {
            list.remove(commandName);
            return list;
        });

        fieldTracker.put("roleSpecificDisabledCommands", this.roleSpecificDisabledCommands);
    }

    public void addAutoroleCategory(String categoryName, String autoroleName) {
        autoroleCategories.compute(categoryName, (k, list) -> {
            list = list != null ? list : new ArrayList<>();
            list.add(autoroleName);
            return list;
        });


        fieldTracker.put("autoroleCategories", this.autoroleCategories);
    }

    public void removeAutoroleCategoryRole(String categoryName, String autoroleName) {
        autoroleCategories.computeIfPresent(categoryName, (k, list) -> {
            list.remove(autoroleName);
            return list;
        });

        fieldTracker.put("autoroleCategories", this.autoroleCategories);
    }

    public void removeAutoroleCategory(String categoryName) {
        autoroleCategories.remove(categoryName);
        fieldTracker.put("autoroleCategories", this.autoroleCategories);
    }

    public void addRunningPoll(String id, Poll.PollDatabaseObject poll) {
        runningPolls.put(id, poll);
        fieldTracker.put("runningPolls", this.runningPolls);
    }

    public void removeRunningPoll(String id) {
        runningPolls.remove(id);
        fieldTracker.put("runningPolls", this.runningPolls);
    }
}
