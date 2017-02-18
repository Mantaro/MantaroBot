package net.kodehawa.mantarobot.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class MusicManager {
	private final AudioPlayer player;
	private final Scheduler scheduler;

	/**
	 * Creates a player and a track scheduler.
	 *
	 * @param manager Audio player manager to use for creating the player.
	 * @param event   Guild event
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

	public boolean nextTrackAvailable() {
		return getScheduler().getQueueSize() > 0;
	}

	public void shuffle() {
		List<AudioTrack> tempList = new ArrayList<>();
		BlockingQueue<AudioTrack> queue = getScheduler().getQueue();
		if (!queue.isEmpty()) queue.drainTo(tempList);
		queue.clear();

		Random rand = new Random();
		Collections.shuffle(tempList, new Random(rand.nextInt(18975545)));
		queue.addAll(tempList);
		tempList.clear();
	}

	public void skipTrack(GuildMessageReceivedEvent event) {
		if (nextTrackAvailable()) {
			getScheduler().nextTrack();
			event.getChannel().sendMessage("\uD83D\uDCE3 Skipped to next track -> **" + getScheduler().getPlayer().getPlayingTrack().getInfo().title + "**").queue();
		} else {
			event.getChannel().sendMessage("No tracks next. Disconnecting...").queue();
		}
	}

}
