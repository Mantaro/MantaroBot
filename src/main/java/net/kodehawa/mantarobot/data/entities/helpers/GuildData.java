package net.kodehawa.mantarobot.data.entities.helpers;

import lombok.Data;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class GuildData {
	private String birthdayChannel = null;
	private String birthdayRole = null;
	private boolean customAdminLock = false;
	private String guildAutoRole = null;
	private String guildCustomPrefix = null;
	private String guildLogChannel = null;
	private transient Set<String> guildUnsafeChannels = new HashSet<>();
	private String musicChannel = null;
	private Long musicQueueSizeLimit = null;
	private Long musicSongDurationLimit = null;
	private long quoteLastId = 0;
	private boolean rpgDevaluation = true; //TODO HOOK THIS FIELD IN OPTS
	private boolean rpgLocalMode = false;

	public List<String> getGuildUnsafeChannels() {
		return new ArrayList<>(guildUnsafeChannels);
	}

	public GuildData setGuildUnsafeChannels(List<String> guildUnsafeChannels) {
		this.guildUnsafeChannels = new HashSet<>(guildUnsafeChannels);
		return this;
	}

	@Transient
	public Set<String> rawgetGuildUnsafeChannels() {
		return this.guildUnsafeChannels;
	}
}
