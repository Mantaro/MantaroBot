package net.kodehawa.mantarobot.commands.music.rewrite;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;
import java.util.Map;

public class MantaroAudioManager {
    private final Map<String, GuildMusicManager> musicManagers;
    private AudioPlayerManager playerManager;
    private AudioRequester audioRequester;

    public MantaroAudioManager() {
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    public AudioRequester getAudioRequester() {
        return audioRequester;
    }
    public synchronized GuildMusicManager getMusicManager(Guild guild) {
        GuildMusicManager musicManager = musicManagers
                .computeIfAbsent(guild.getId(), id -> new GuildMusicManager(playerManager));

        if (guild.getAudioManager().getSendingHandler() == null)
            guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
        return musicManager;
    }
}
