package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.HashMap;
import java.util.Map;

public class MantaroAudioManager {
	private final Map<String, GuildMusicManager> musicManagers;
	private AudioPlayerManager playerManager;

	public MantaroAudioManager() {
		this.musicManagers = new HashMap<>();
		this.playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager);
	}

	public synchronized GuildMusicManager getMusicManager(Guild guild) {
		GuildMusicManager musicManager = musicManagers
			.computeIfAbsent(guild.getId(), id -> new GuildMusicManager(playerManager, guild));

		if (guild.getAudioManager().getSendingHandler() == null)
			guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
		return musicManager;
	}

	public Map<String, GuildMusicManager> getMusicManagers() {
		return musicManagers;
	}

	public long getTotalQueueSize() {
		return musicManagers.values().stream().map(m -> m.getTrackScheduler().getQueue().size()).mapToInt(Integer::intValue).sum();
	}

	public void loadAndPlay(GuildMessageReceivedEvent event, String trackUrl) {
		GuildMusicManager musicManager = getMusicManager(event.getGuild());
		if (!AudioCmdUtils.connectToVoiceChannel(event)) return;
		musicManager.getTrackScheduler().getAudioPlayer().setPaused(false);
		playerManager.loadItemOrdered(musicManager, trackUrl, new AudioRequester(musicManager, event, trackUrl));

	}
}
