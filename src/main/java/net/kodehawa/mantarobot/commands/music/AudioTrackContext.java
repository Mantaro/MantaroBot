package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.MantaroBot;

public class AudioTrackContext {
    private String dj;
    private String channel;
    private AudioTrack audioTrack;
    private String url;

    public AudioTrackContext(User dj, TextChannel textChannel, String url, AudioTrack audioTrack) {
        this.dj = dj.getId();
        this.url = url;
        this.channel = textChannel.getId();
        this.audioTrack = audioTrack;
    }

    public AudioTrack getAudioTrack() {
        return audioTrack;
    }

    public String getUrl() {
        return url;
    }

    public User getDJ() {
        return MantaroBot.getJDA().getUserById(dj);
    }

    public TextChannel getRequestedChannel() {
        return MantaroBot.getJDA().getTextChannelById(channel);
    }

    public AudioTrackContext makeClone() {
        audioTrack = audioTrack.makeClone();
        return this;
    }

    public long getDuration() {
        return getAudioTrack().getDuration();
    }

    public long getPosition() {
        return getAudioTrack().getPosition();
    }

    public AudioTrackInfo getInfo() {
        return getAudioTrack().getInfo();
    }
}
