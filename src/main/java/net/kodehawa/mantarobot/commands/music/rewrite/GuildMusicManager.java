package net.kodehawa.mantarobot.commands.music.rewrite;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.entities.Guild;
import net.kodehawa.mantarobot.commands.music.AudioPlayerSendHandler;

public class GuildMusicManager {
    private final AudioPlayer audioPlayer;
    private TrackScheduler trackScheduler;

    public GuildMusicManager(AudioPlayerManager playerManager, Guild guild) {
        this.audioPlayer = playerManager.createPlayer();
        this.trackScheduler = new TrackScheduler(audioPlayer, guild.getId());
        this.audioPlayer.addListener(trackScheduler);

    }

    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(audioPlayer);
    }

    public TrackScheduler getTrackScheduler() {
        return trackScheduler;
    }
}
