package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.utils.sql.SQLAction;
import net.kodehawa.mantarobot.utils.sql.SQLDatabase;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MantaroAudioManager {
	private final Map<String, GuildMusicManager> musicManagers;
	private AudioPlayerManager playerManager;

	public MantaroAudioManager() {
		this.musicManagers = new HashMap<>();
		this.playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager);

		try {
			SQLDatabase.getInstance().run((conn) -> {
				try {
					conn.prepareStatement("CREATE TABLE IF NOT EXISTS PLAYED_SONGS (" +
						"id varchar(15)," +
						"times_played int," +
						"PRIMARY KEY(id)" +
						");").executeUpdate();
				} catch (SQLException e) {
					SQLAction.getLog().error(null, e);
				}
			}).queue();
		} catch (SQLException e) {
			SQLAction.getLog().error(null, e);
		}
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
}
