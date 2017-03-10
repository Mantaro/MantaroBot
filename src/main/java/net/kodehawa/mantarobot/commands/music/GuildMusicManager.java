package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.entities.Guild;
import net.kodehawa.mantarobot.MantaroBot;

public class GuildMusicManager {
	private final AudioPlayer audioPlayer;
	private TrackScheduler trackScheduler;

	public GuildMusicManager(AudioPlayerManager playerManager, Guild guild) {
		this.audioPlayer = playerManager.createPlayer();
		this.trackScheduler = new TrackScheduler(audioPlayer, guild.getId(), MantaroBot.getInstance().getId(guild.getJDA()));
		this.audioPlayer.addListener(trackScheduler);

	}

	public AudioPlayerSendHandler getSendHandler() {
		return new AudioPlayerSendHandler(audioPlayer);
	}

	public TrackScheduler getTrackScheduler() {
		return trackScheduler;
	}
}
