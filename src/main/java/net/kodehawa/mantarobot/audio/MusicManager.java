package net.kodehawa.mantarobot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class MusicManager {

	final AudioPlayer player;
	final Scheduler scheduler;

	/**
	 * Creates a player and a track scheduler.
	 *
	 * @param manager Audio player manager to use for creating the player.
	 * @param event
	 */
	public MusicManager(AudioPlayerManager manager, GuildMessageReceivedEvent event) {
		player = manager.createPlayer();
		scheduler = new Scheduler(event, player);
		player.addListener(scheduler);
	}

	public Scheduler getScheduler() {
		return scheduler;
	}

	public AudioPlayerSendHandler getSendHandler() {
		return new AudioPlayerSendHandler(player);
	}
}
