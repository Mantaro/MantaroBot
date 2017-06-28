package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.sql.SQLAction;
import net.kodehawa.mantarobot.utils.sql.SQLDatabase;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class GuildMusicManager {
	private final AudioPlayer audioPlayer;
	private TrackScheduler trackScheduler;
	private AudioPlayerSendHandler audioPlayerSendHandler;
	private Future<Void> leaveTask;

	public GuildMusicManager(AudioPlayerManager playerManager, Guild guild) {
		this.audioPlayer = playerManager.createPlayer();
		this.trackScheduler = new TrackScheduler(audioPlayer, guild.getId(), MantaroBot.getInstance().getId(guild.getJDA()));
		this.audioPlayer.addListener(trackScheduler);
		this.audioPlayerSendHandler = new AudioPlayerSendHandler(audioPlayer);
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
        (trackScheduler.getCurrentTrack() == null ? guild.getTextChannels().stream().filter(TextChannel::canTalk).findFirst().orElseThrow(()->{
            return new IllegalStateException("No channel to speak");
        }) : trackScheduler.getCurrentTrack().getRequestedChannel()).sendMessage(EmoteReference.THINKING + "I decided to leave **" + guild.getSelfMember().getVoiceState().getChannel().getName() + "** " +
                "because I was left all " +
                "alone :<").queue();
        trackScheduler.getQueue().clear();
        trackScheduler.stop();
        audioPlayer.stopTrack();
        audioPlayer.destroy();
        trackScheduler.getAudioManager().closeAudioConnection();
        MantaroBot.getInstance().getAudioManager().getMusicManagers().remove(trackScheduler.getGuild().getId());
    }

    public synchronized void scheduleLeave() {
	    if(leaveTask != null) return;
	    MantaroBot.getInstance().getExecutorService().schedule(this::leave, 2, TimeUnit.MINUTES);
    }

    public synchronized void cancelLeave() {
	    if(leaveTask == null) return;
        leaveTask.cancel(true);
	    leaveTask = null;
    }
}
