package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroShard;

public class AudioTrackContext {
	private AudioTrack audioTrack;
	private String channel;
	private String dj;
	private int shardId;
	private String url;

	public AudioTrackContext(User dj, TextChannel textChannel, String url, AudioTrack audioTrack) {
		this.dj = dj.getId();
		this.url = url;
		this.channel = textChannel.getId();
		this.audioTrack = audioTrack;
		this.shardId = MantaroBot.getInstance().getId(textChannel.getJDA());
	}

	public AudioTrack getAudioTrack() {
		return audioTrack;
	}

	public User getDJ() {
		return getShard().getJDA().getUserById(dj);
	}

	public long getDuration() {
		return getAudioTrack().getDuration();
	}

	public AudioTrackInfo getInfo() {
		return getAudioTrack().getInfo();
	}

	public long getPosition() {
		return getAudioTrack().getPosition();
	}

	public TextChannel getRequestedChannel() {
		return getShard().getJDA().getTextChannelById(channel);
	}

	public MantaroShard getShard() {
		return MantaroBot.getInstance().getShard(shardId);
	}

	public String getUrl() {
		return url;
	}

	public AudioTrackContext makeClone() {
		audioTrack = audioTrack.makeClone();
		return this;
	}
}
