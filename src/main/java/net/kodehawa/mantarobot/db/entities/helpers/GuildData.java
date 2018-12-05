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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class GuildData {
    private boolean antiSpam = false;
    private HashMap<String, String> autoroles = new HashMap<>();
    private String birthdayChannel = null;
    private String birthdayRole = null;
    private long cases = 0L;
    private HashMap<String, List<Category>> channelSpecificDisabledCategories = new HashMap<>();
    private HashMap<String, List<String>> channelSpecificDisabledCommands = new HashMap<>();
    private boolean customAdminLock = false;
    private Set<Category> disabledCategories = new HashSet<>();
    private Set<String> disabledChannels = new HashSet<>();
    private Set<String> disabledCommands = new HashSet<>();
    private Set<String> disabledRoles = new HashSet<>();
    private List<String> disabledUsers = new ArrayList<>();
    private String guildAutoRole = null;
    private String guildCustomPrefix = null;
    private String guildLogChannel = null;
    private Set<String> guildUnsafeChannels = new HashSet<>();
    private String joinMessage = null;
    private String leaveMessage = null;
    private boolean linkProtection = false;
    private Set<String> linkProtectionAllowedChannels = new HashSet<>();
    private Set<String> logExcludedChannels = new HashSet<>();
    private String logJoinLeaveChannel = null;
    private int maxFairQueue = 4;
    private int maxResultsSearch = 5;
    private Set<String> modlogBlacklistedPeople = new HashSet<>();
    private boolean musicAnnounce = true;
    private String musicChannel = null;
    private Long musicQueueSizeLimit = null;
    private Long musicSongDurationLimit = null;
    private String mutedRole = null;
    private ConcurrentHashMap<Long, Long> mutedTimelyUsers = new ConcurrentHashMap<>();
    private boolean noMentionsAction = false;
    private String premiumKey;
    private long quoteLastId = 0L;
    private long ranPolls = 0L;
    private boolean reactionMenus = true;
    private ArrayList<String> rolesBlockedFromCommands = new ArrayList<>();
    private boolean rpgDevaluation = true;
    private boolean rpgLocalMode = false;
    private long setModTimeout = 0L;
    private boolean slowMode = false;
    private Set<String> slowModeChannels = new HashSet<>();
    private Set<String> spamModeChannels = new HashSet<>();
    private int timeDisplay = 0; //0 = 24h, 1 = 12h
    private Map<Long, WarnAction> warnActions = new HashMap<>();
    private Map<String, Long> warnCount = new HashMap<>();
    private String gameTimeoutExpectedAt;
    private boolean ignoreBotsWelcomeMessage = false;
    private boolean ignoreBotsAutoRole = false;
    private boolean enabledLevelUpMessages = false;
    private String levelUpChannel = null;
    private String levelUpMessage = null;
    private Set<String> blackListedImageTags = new HashSet<>();
    private String logJoinChannel = null;
    private String logLeaveChannel = null;
    private List<LocalExperienceData> localPlayerExperience = new ArrayList<>();
    private Set<String> linkProtectionAllowedUsers = new HashSet<>();
    private HashMap<String, List<Category>> roleSpecificDisabledCategories = new HashMap<>();
    private HashMap<String, List<String>> roleSpecificDisabledCommands = new HashMap<>();
    private String lang = "en_US";
    private boolean musicVote = true;
    private List<String> extraJoinMessages = new ArrayList<>();
    private List<String> extraLeaveMessages = new ArrayList<>();
    private String whitelistedRole = null;
    private String birthdayMessage = null;
    private boolean customAdminLockNew = true;
    private String mpLinkedTo = null; //user id of the person who linked MP to a specific server (used for patreon checks)
    private List<String> modLogBlacklistWords = new ArrayList<>();
}
