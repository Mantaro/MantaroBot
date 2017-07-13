package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
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
		registerRemoteSources(playerManager);
	}

	public synchronized GuildMusicManager getMusicManager(Guild guild) {
		GuildMusicManager musicManager = musicManagers
			.computeIfAbsent(guild.getId(), id -> new GuildMusicManager(playerManager, guild));
		if (guild.getAudioManager().getSendingHandler() == null){
			guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
		}

		return musicManager;
	}

	public Map<String, GuildMusicManager> getMusicManagers() {
		return musicManagers;
	}

	public AudioPlayerManager getPlayerManager() {
		return playerManager;
	}

	public long getTotalQueueSize() {
		return musicManagers.values().stream().map(m -> m.getTrackScheduler().getQueue().size()).mapToInt(Integer::intValue).sum();
	}

	public void loadAndPlay(GuildMessageReceivedEvent event, String trackUrl, boolean skipSelection) {
		GuildMusicManager musicManager = getMusicManager(event.getGuild());
		if (!AudioCmdUtils.connectToVoiceChannel(event)) return;
		musicManager.getTrackScheduler().getAudioPlayer().setPaused(false);
		if(musicManager.getTrackScheduler().getQueue().isEmpty()) musicManager.getTrackScheduler().setRepeat(null);
		playerManager.loadItemOrdered(musicManager, trackUrl, new AudioRequester(musicManager, event, trackUrl, skipSelection));
	}

	public static void registerRemoteSources(AudioPlayerManager playerManager) {
		playerManager.registerSourceManager(new YoutubeAudioSourceManager(true));
		playerManager.registerSourceManager(new SoundCloudAudioSourceManager(true));
		playerManager.registerSourceManager(new BandcampAudioSourceManager());
		playerManager.registerSourceManager(new VimeoAudioSourceManager());
		playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
		playerManager.registerSourceManager(new BeamAudioSourceManager());
	}

}