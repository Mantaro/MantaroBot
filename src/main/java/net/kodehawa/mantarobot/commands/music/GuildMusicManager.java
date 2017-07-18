package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.handlers.AudioPlayerSendHandler;
import net.kodehawa.mantarobot.commands.music.requester.TrackScheduler;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GuildMusicManager {
    @Getter
    public final AudioPlayer audioPlayer;
    @Getter
    public final TrackScheduler trackScheduler;
    @Getter
    public final AudioPlayerSendHandler audioPlayerSendHandler;
    @Getter
    @Setter
    public boolean isAwaitingDeath;
    private ScheduledFuture<?> leaveTask = null;

    public GuildMusicManager(AudioPlayerManager manager, String guildId) {
        audioPlayer = manager.createPlayer();
        trackScheduler = new TrackScheduler(audioPlayer, guildId);
        audioPlayerSendHandler = new AudioPlayerSendHandler(audioPlayer);
        audioPlayer.addListener(trackScheduler);
    }

    public synchronized void leave() {
        Guild guild = trackScheduler.getGuild();

        if(guild == null) return;

        (trackScheduler.getCurrentTrack() == null ?
                guild.getTextChannels().stream().filter(TextChannel::canTalk).findFirst().orElseThrow(() -> new IllegalStateException("No channel to speak")) :
                trackScheduler.getRequestedChannelParsed()).sendMessage(EmoteReference.THINKING + "I decided to leave **" + guild.getSelfMember().getVoiceState().getChannel().getName() + "** " +
                "because I was left all alone :<").queue();
        trackScheduler.stop();
        trackScheduler.getGuild().getAudioManager().closeAudioConnection();
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