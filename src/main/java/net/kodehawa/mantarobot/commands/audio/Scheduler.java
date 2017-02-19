package net.kodehawa.mantarobot.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.commands.audio.MantaroAudioManager.closeConnection;
import static net.kodehawa.mantarobot.commands.audio.MantaroAudioManager.getGuildAudioPlayer;

public class Scheduler extends AudioEventAdapter {
	private final AudioPlayer player;
	private final BlockingQueue<AudioTrack> queue;
	private final JDA jda;
	private String channel, guild;
	private boolean repeat = false;

	Scheduler(TextChannel channel, AudioPlayer player) {
		this.jda = channel.getJDA();
		this.channel = channel.getId();
		this.guild = channel.getGuild().getId();
		this.player = player;
		this.queue = new LinkedBlockingQueue<>();
	}

	public TextChannel channel() {
		TextChannel channel = jda.getTextChannelById(this.channel);

		if (channel == null) {
			Guild g = jda.getGuildById(guild);

			if (g != null) channel = g.getPublicChannel();
		}
		return channel;
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if (!repeat) {
			if (endReason.mayStartNext) {
				nextTrack();
			}

			if (player.getPlayingTrack() == null) {
				MusicManager musicManager = getGuildAudioPlayer(channel().getGuild());
				channel().sendMessage(":zap: Finished playing queue, disconnecting...").queue();
				closeConnection(musicManager, channel().getGuild().getAudioManager(), channel());
				return;
			}

			return;
		}

		player.startTrack(track.makeClone(), false);
	}

	public AudioPlayer getPlayer() {
		return player;
	}

	public BlockingQueue<AudioTrack> getQueue() {
		return queue;
	}

	public String getQueueList() {
		StringBuilder sb = new StringBuilder();
		int n = 1;
		for (AudioTrack audioTrack : queue) {
			long aDuration = audioTrack.getDuration();
			String duration = String.format("%02d:%02d",
				TimeUnit.MILLISECONDS.toMinutes(aDuration),
				TimeUnit.MILLISECONDS.toSeconds(aDuration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(aDuration))
			);

			sb.append("[").append(n)
				.append("] ")
				.append(audioTrack.getInfo().title)
				.append(" **(")
				.append(duration)
				.append(")**")
				.append("\n"
				);
			n++;
		}
		return sb.toString();
	}

	public int getQueueSize() {
		return queue.size();
	}

	public boolean nextTrack() {
		return player.startTrack(queue.poll(), false);
	}

	public void queue(AudioTrack track) {
		if (!player.startTrack(track, true)) {
			queue.offer(track);
		}
	}

	public void setRepeat(boolean r) {
		repeat = r;
	}
}
