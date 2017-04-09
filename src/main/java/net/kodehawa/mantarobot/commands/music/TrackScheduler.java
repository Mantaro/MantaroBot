package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroShard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class TrackScheduler extends AudioEventAdapter {
	private AudioPlayer audioPlayer;
	private AudioTrackContext currentTrack;
	private String guildId;
	private String lastAnnounce;
	private AudioTrackContext previousTrack;
	private BlockingQueue<AudioTrackContext> queue;
	private Repeat repeat;
	private int shardId;
	private List<String> voteSkips;
	@Getter
	private List<String> voteStop;

	TrackScheduler(AudioPlayer audioPlayer, String guildId, int shardId) {
		this.queue = new LinkedBlockingQueue<>();
		this.voteSkips = new ArrayList<>();
		this.voteStop = new ArrayList<>();
		this.previousTrack = null;
		this.currentTrack = null;
		this.lastAnnounce = null;
		this.repeat = null;
		this.audioPlayer = audioPlayer;
		this.guildId = guildId;
		this.shardId = shardId;
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		getVoteSkips().clear();
		if (endReason.mayStartNext) {
			next(false);
		}
		announce();
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
		if (getCurrentTrack() != null && getCurrentTrack().getRequestedChannel() != null && getCurrentTrack().getRequestedChannel().canTalk()) {
			if (!exception.severity.equals(FriendlyException.Severity.FAULT))
				getCurrentTrack().getRequestedChannel().sendMessage("Something happened while attempting to play " + track.getInfo().title + ": " + exception.getMessage()).queue();
		}
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		if (getCurrentTrack().getRequestedChannel() != null && getCurrentTrack().getRequestedChannel().canTalk()) {
			getCurrentTrack().getRequestedChannel().sendMessage("Track got stuck, If it doesn't skip automatically, please do ~>skip.").queue();
		}
	}

	private void announce() {
		try {
			if (getPreviousTrack() != null && getPreviousTrack().getRequestedChannel() != null && getPreviousTrack().getRequestedChannel().canTalk())
				getPreviousTrack().getRequestedChannel().getMessageById(lastAnnounce).complete().delete().queue();
		} catch (Exception ignored) {
		}
		try {
			if (getCurrentTrack() != null && getCurrentTrack().getRequestedChannel() != null && getCurrentTrack().getRequestedChannel().canTalk())
				getCurrentTrack().getRequestedChannel()
					.sendMessage("\uD83D\uDCE3 Now playing in " + getAudioManager().getConnectedChannel().getName()
						+ ": " + getCurrentTrack().getInfo().title + " (" + AudioUtils.getLength(getCurrentTrack().getInfo().length) + ")"
						+ (getCurrentTrack().getDJ() != null ? " requested by " + getCurrentTrack().getDJ().getName() : "")).queue(this::setLastAnnounce);
		} catch (Exception ignored) {
		}
	}

	public AudioManager getAudioManager() {
		return getGuild().getAudioManager();
	}

	public AudioPlayer getAudioPlayer() {
		return audioPlayer;
	}

	public AudioTrackContext getCurrentTrack() {
		return currentTrack;
	}

	public Guild getGuild() {
		return getShard().getJDA().getGuildById(guildId);
	}

	public AudioTrackContext getPreviousTrack() {
		return previousTrack;
	}

	public BlockingQueue<AudioTrackContext> getQueue() {
		return queue;
	}

	public void getQueueAsList(Consumer<List<AudioTrackContext>> list) {
		List<AudioTrackContext> tempList = new ArrayList<>(getQueue());

		list.accept(tempList);

		queue.clear();
		queue.addAll(tempList);
	}

	public Repeat getRepeat() {
		return repeat;
	}

	public void setRepeat(Repeat repeat) {
		this.repeat = repeat;
	}

	public int getRequiredSkipVotes() {
		int listeners = (int) getGuild().getAudioManager().getConnectedChannel().getMembers().stream()
			.filter(m -> !m.getUser().isBot() && !m.getVoiceState().isDeafened()).count();
		return (int) Math.ceil(listeners * .55);
	}

	public MantaroShard getShard() {
		return MantaroBot.getInstance().getShard(shardId);
	}

	public List<String> getVoteSkips() {
		return voteSkips;
	}

	public boolean isStopped() {
		return getCurrentTrack() == null && getQueue().isEmpty();
	}

	public synchronized void next(boolean skip) {
		if (repeat == Repeat.SONG && !skip && getCurrentTrack() != null) {
			getAudioPlayer().startTrack(getCurrentTrack().makeClone().getAudioTrack(), false);
		} else {
			if (currentTrack != null) previousTrack = currentTrack;
			currentTrack = queue.poll();
			if (getCurrentTrack() != null && getCurrentTrack().getAudioTrack().getState() != AudioTrackState.INACTIVE)
				currentTrack = currentTrack.makeClone();
			getAudioPlayer().startTrack(getCurrentTrack() == null ? null : getCurrentTrack().getAudioTrack(), false);
			if (repeat == Repeat.QUEUE) queue.offer(previousTrack.makeClone());
		}

		if (currentTrack == null) onTrackSchedulerStop();
	}

	private void onTrackSchedulerStop() {
		Guild g = getGuild();
		if (g == null) return;

		AudioManager m = g.getAudioManager();
		if (m == null) return;

		m.closeAudioConnection();

		AudioTrackContext previousTrack;

		try {
			previousTrack = getPreviousTrack();
			if (previousTrack != null && previousTrack.getRequestedChannel() != null && previousTrack.getRequestedChannel().canTalk())
				previousTrack.getRequestedChannel().sendMessage(":mega: Finished playing queue.").queue();
		} catch (Exception ignored) {
		} //fuck
	}

	public void queue(AudioTrackContext audioTrackContext) {
		getQueue().offer(audioTrackContext);
		if (getAudioPlayer().getPlayingTrack() == null) next(true);
	}

	private void setLastAnnounce(Message m) {
		this.lastAnnounce = m == null ? null : m.getId();
	}

	public void shuffle() {
		getQueueAsList(Collections::shuffle);
	}

	public void stop() {
		getQueue().clear();
		next(true);
	}
}
