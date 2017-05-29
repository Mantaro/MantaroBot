package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.Guild;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.debug.SpeedingTicketFactory;
import net.kodehawa.mantarobot.utils.sql.SQLAction;
import net.kodehawa.mantarobot.utils.sql.SQLDatabase;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class GuildMusicManager {
	private final AudioPlayer audioPlayer;
	private TrackScheduler trackScheduler;

	public GuildMusicManager(AudioPlayerManager playerManager, Guild guild) {
		this.audioPlayer = playerManager.createPlayer();
		this.trackScheduler = new TrackScheduler(audioPlayer, guild.getId(), MantaroBot.getInstance().getId(guild.getJDA()));
		this.audioPlayer.addListener(trackScheduler);
		this.audioPlayer.addListener(new AudioEventAdapter() {
			@Override
			public void onTrackStart(AudioPlayer player, AudioTrack track) {
				super.onTrackStart(player, track);
				try {
					SQLDatabase.getInstance().run((conn) -> {
						try {
							PreparedStatement statement = conn.prepareStatement("INSERT INTO PLAYED_SONGS " +
								"VALUES(" +
								"?, " +
								"1" +
								") ON DUPLICATE KEY UPDATE times_played = times_played + 1;");
							statement.setString(1, track.getInfo().identifier);
							statement.executeUpdate();
						} catch (SQLException e) {
							SQLAction.getLog().error(null, e);
						}
					}).queue();
				} catch (SQLException e) {
					SQLAction.getLog().error(null, e);
				}
			}
		});
	}

	public AudioPlayerSendHandler getSendHandler() {
		return new AudioPlayerSendHandler(audioPlayer);
	}

	public AudioPlayer getAudioPlayer() {
		return audioPlayer;
	}
	public TrackScheduler getTrackScheduler() {
		return trackScheduler;
	}
}
