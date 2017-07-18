package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.music.requester.AudioLoader;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;

import java.util.HashMap;
import java.util.Map;

public class MantaroAudioManager {
    @Getter
    private final Map<String, GuildMusicManager> musicManagers;
    @Getter
    private AudioPlayerManager playerManager;

    public MantaroAudioManager() {
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new YoutubeAudioSourceManager(true));
        playerManager.registerSourceManager(new SoundCloudAudioSourceManager(true));
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());

    }

    public synchronized GuildMusicManager getMusicManager(Guild guild) {
        GuildMusicManager musicManager = musicManagers.computeIfAbsent(guild.getId(), id -> new GuildMusicManager(playerManager, guild.getId()));
        if(guild.getAudioManager().getSendingHandler() == null)
            guild.getAudioManager().setSendingHandler(musicManager.getAudioPlayerSendHandler());
        return musicManager;
    }

    public synchronized long getTotalQueueSize() {
        return musicManagers.values().stream().map(m -> m.getTrackScheduler().getQueue().size()).mapToInt(Integer::intValue).sum();
    }

    public synchronized void loadAndPlay(GuildMessageReceivedEvent event, String trackUrl, boolean skipSelection) {
        GuildMusicManager musicManager = getMusicManager(event.getGuild());
        if(!AudioCmdUtils.connectToVoiceChannel(event)) return;
        musicManager.getTrackScheduler().getAudioPlayer().setPaused(false);
        if(musicManager.getTrackScheduler().getQueue().isEmpty()) musicManager.getTrackScheduler().setRepeatMode(null);
        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoader(musicManager, event, trackUrl, skipSelection));
    }
}