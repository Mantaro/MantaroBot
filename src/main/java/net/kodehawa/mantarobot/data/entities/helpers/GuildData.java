package net.kodehawa.mantarobot.data.entities.helpers;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class GuildData {
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
	private String logJoinLeaveChannel = null;
	private String musicChannel = null;
	private Long musicQueueSizeLimit = null;
	private Long musicSongDurationLimit = null;
	private Long quoteLastId = 0L;
	private boolean rpgDevaluation = true;
	private boolean rpgLocalMode = false;
}
