package net.kodehawa.mantarobot.commands.music.rewrite;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.function.IntConsumer;

public class AudioRequester implements AudioLoadResultHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("AudioRequester");
    public static final long MAX_SONG_LENGTH = 600000;
    public static final int MAX_QUEUE_LENGTH = 200;

    private GuildMusicManager musicManager;
    private GuildMessageReceivedEvent event;
    private String trackUrl;

    public AudioRequester(GuildMusicManager musicManager, GuildMessageReceivedEvent event, String trackUrl) {
        this.musicManager = musicManager;
        this.trackUrl = trackUrl;
        this.event = event;
    }

    public GuildMusicManager getMusicManager() {
        return musicManager;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        loadSingle(track, false);
    }
    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (playlist.isSearchResult()) {
            onSearchResult(playlist);
            return;
        }

        playlist.getTracks().forEach(track -> loadSingle(track, true));

        event.getChannel().sendMessage(String.format(
                "Added **%d songs** to queue on playlist: **%s** *(%s)*",
                playlist.getTracks().size(),
                playlist.getName(),
                Utils.getDurationMinutes(playlist.getTracks().stream().mapToLong(temp -> temp.getInfo().length).sum())
        )).queue();

    }
    @Override
    public void noMatches() {
        event.getChannel().sendMessage("Nothing found by " + (trackUrl.startsWith("ytsearch:") ? trackUrl.substring(9) : trackUrl) + ".").queue();
        if (musicManager.getTrackScheduler().isStopped())
            event.getGuild().getAudioManager().closeAudioConnection();
    }
    @Override
    public void loadFailed(FriendlyException exception) {
        if (!exception.severity.equals(FriendlyException.Severity.FAULT)) {
            event.getChannel().sendMessage("\u274C Error while fetching music: " + exception.getMessage()).queue();
        } else {
            LOGGER.warn("Error caught while playing audio, the bot might be able to continue playing music.", exception);
        }
        if (musicManager.getTrackScheduler().isStopped())
            event.getGuild().getAudioManager().closeAudioConnection();
    }

    private void loadSingle(AudioTrack audioTrack, boolean silent) {
        if (audioTrack.getInfo().length > MAX_SONG_LENGTH) {
            event.getChannel().sendMessage("Could not queue " + audioTrack.getInfo().title + ": Track is longer than 10 minutes! (" + AudioUtils.getLength(audioTrack.getInfo().length) + ")").queue();
            if (musicManager.getTrackScheduler().isStopped())
                event.getGuild().getAudioManager().closeAudioConnection();
            return;
        }
        musicManager.getTrackScheduler().queue(new AudioTrackContext(event.getAuthor(), event.getChannel(), audioTrack.getSourceManager() instanceof YoutubeAudioSourceManager ? "https://www.youtube.com/watch?v="+  audioTrack.getIdentifier()  : trackUrl, audioTrack));
        if (!silent) {
            event.getChannel().sendMessage(
                    "\uD83D\uDCE3 Added to queue -> **" + audioTrack.getInfo().title + "**"
                            + " **!(" + AudioUtils.getLength(audioTrack.getInfo().length) + ")**"
            ).queue();
        }
    }

    private void onSearchResult(AudioPlaylist playlist) {
        EmbedBuilder builder = new EmbedBuilder().setColor(Color.CYAN).setTitle("Song selection. Type the song number to continue.", null).setFooter("This timeouts in 10 seconds.", null);
        java.util.List<AudioTrack> tracks = playlist.getTracks();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 4 && i < tracks.size(); i++) {
            AudioTrack at = tracks.get(i);
            b.append('[').append(i + 1).append("] ").append(at.getInfo().title).append(" **(")
                    .append(Utils.getDurationMinutes(at.getInfo().length)).append(")**").append("\n");
        }

        event.getChannel().sendMessage(builder.setDescription(b.toString()).build()).queue();
        IntConsumer consumer = (c) -> loadSingle(playlist.getTracks().get(c - 1), true);
        DiscordUtils.selectInt(event, 5, consumer);
    }
}
