/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
 */

package net.kodehawa.mantarobot.db.entities.helpers;

import lombok.Data;
import net.kodehawa.mantarobot.commands.moderation.WarnAction;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.annotations.ConfigName;
import net.kodehawa.mantarobot.utils.annotations.UnusedConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
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
    @ConfigName("Server lenguage")
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
    private String editMessageLog;
    private String deleteMessageLog;
    private String bannedMemberLog;
    private String kickedMemberLog;
}
