/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
import net.kodehawa.mantarobot.core.modules.commands.base.Category;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
	private int maxResultsSearch = 5;
	private Set<String> disabledRoles = new HashSet<>();
	private String premiumKey; //Placeholder here for rethonk plz
}
