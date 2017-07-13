package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.sql.SQLAction;
import net.kodehawa.mantarobot.utils.sql.SQLDatabase;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GuildMusicManager {
	private final AudioPlayer audioPlayer;
	private TrackScheduler trackScheduler;
	private AudioPlayerSendHandler audioPlayerSendHandler;
	private ScheduledFuture<?> leaveTask;
	@Getter @Setter private boolean isAwaitingDeath;


	public GuildMusicManager(AudioPlayerManager playerManager, Guild guild) {
		this.audioPlayer = playerManager.createPlayer();
		this.trackScheduler = new TrackScheduler(audioPlayer, guild.getId(), MantaroBot.getInstance().getId(guild.getJDA()));
		this.audioPlayer.addListener(trackScheduler);
		this.audioPlayerSendHandler = new AudioPlayerSendHandler(audioPlayer);
	}

	public AudioPlayerSendHandler getSendHandler() {
		return audioPlayerSendHandler;
	}
	public AudioPlayer getAudioPlayer() {
		return audioPlayer;
	}
	public TrackScheduler getTrackScheduler() {
		return trackScheduler;
	}

	public synchronized void leave() {
	    Guild guild = trackScheduler.getGuild();
        (trackScheduler.getCurrentTrack() == null ?
				guild.getTextChannels().stream().filter(TextChannel::canTalk).
						findFirst().orElseThrow(()-> new IllegalStateException("No channel to speak"))
				: trackScheduler.getCurrentTrack().getRequestedChannel())
				.sendMessage(EmoteReference.THINKING + "I decided to leave **" + guild.getSelfMember().getVoiceState().getChannel().getName() + "** " +
                "because I was left all " +
                "alone :<\n" +
						(MantaroData.config().get().isPremiumBot ? ""
								: "Consider donating on patreon.com/mantaro if you like me, even a small donation will help towards keeping the bot alive :heart:")).queue();
        trackScheduler.getQueue().clear();
		trackScheduler.stop();
        audioPlayer.stopTrack();
        audioPlayer.destroy();
        trackScheduler.getAudioManager().closeAudioConnection();
        MantaroBot.getInstance().getAudioManager().getMusicManagers().remove(trackScheduler.getGuild().getId());
    }

    public synchronized void scheduleLeave() {
	    if(leaveTask != null) return;
	    leaveTask = MantaroBot.getInstance().getExecutorService().schedule(this::leave, 2, TimeUnit.MINUTES);
    }

    public synchronized void cancelLeave() {
	    if(leaveTask == null) return;
        leaveTask.cancel(true);
	    leaveTask = null;
    }
}
