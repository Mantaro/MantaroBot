package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Collections;

import static net.kodehawa.mantarobot.commands.music.MantaroAudioManager.closeConnection;

public class MusicManager {
	private final AudioPlayer player;
	private final Scheduler scheduler;

	/**
	 * Creates a player and a track scheduler.
	 *
	 * @param manager Audio player manager to use for creating the player.
	 * @param channel   TextChannel
	 */
	public MusicManager(AudioPlayerManager manager, TextChannel channel) {
		player = manager.createPlayer();
		scheduler = new Scheduler(channel, player);
		player.addListener(scheduler);
	}

	public Scheduler getScheduler() {
		return scheduler;
	}

	public AudioPlayerSendHandler getSendHandler() {
		return new AudioPlayerSendHandler(player);
	}

	public boolean nextTrackAvailable() {
		return getScheduler().getQueueSize() > 0;
	}

	public void shuffle() {
		getScheduler().getQueueAsList(Collections::shuffle);
	}

	public void skipTrack(GuildMessageReceivedEvent event) {
		if (nextTrackAvailable()) {
			getScheduler().nextTrack();
			event.getChannel().sendMessage("\uD83D\uDCE3 Skipped to next track -> **" + getScheduler().getPlayer().getPlayingTrack().getInfo().title + "**").queue();
		} else {
			event.getChannel().sendMessage("No tracks next. Disconnecting...").queue();
			getScheduler().channel().getGuild().getAudioManager().closeAudioConnection();
		}
	}

}
