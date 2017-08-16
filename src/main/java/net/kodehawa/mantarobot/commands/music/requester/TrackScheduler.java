package net.kodehawa.mantarobot.commands.music.requester;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.utils.AudioUtils;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.core.shard.MantaroShard;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TrackScheduler extends AudioEventAdapter {
    @Getter
    private final AudioPlayer audioPlayer;
    @Getter
    private final BlockingQueue<AudioTrack> queue;
    private final String guildId;
    @Getter @Setter
    private Repeat repeatMode;
    @Getter
    private final List<String> voteSkips;
    @Getter
    private final List<String> voteStop;
    @Setter
    private long requestedChannel;
    @Getter
    private AudioTrack previousTrack, currentTrack;

    public TrackScheduler(AudioPlayer player, String guildId){
        this.audioPlayer = player;
        this.queue = new LinkedBlockingQueue<>();
        this.guildId = guildId;
        this.voteSkips = new ArrayList<>();
        this.voteStop = new ArrayList<>();
    }

    public void queue(AudioTrack track) {
        if (!audioPlayer.startTrack(track, true)) {
            queue.offer(track);
        } else {
            currentTrack = track;
        }
    }

    public void nextTrack(boolean force, boolean skip) {
        getVoteSkips().clear();
        if(repeatMode == Repeat.SONG && currentTrack != null && !force){
            queue(currentTrack.makeClone());
        } else {
            if(currentTrack != null) previousTrack = currentTrack;
            currentTrack = queue.poll();
            audioPlayer.startTrack(currentTrack, !force);
            if(skip) onTrackStart();
            if(repeatMode == Repeat.QUEUE) queue(previousTrack.makeClone());
        }
    }

    private void onTrackStart(){
        if(currentTrack == null){
            onStop();
            return;
        }

        if(MantaroData.db().getGuild(guildId).getData().isMusicAnnounce() && getRequestedChannelParsed() != null){
            VoiceChannel voiceChannel = getRequestedChannelParsed().getGuild().getSelfMember().getVoiceState().getChannel();

            //What kind of massive meme is this?
            //It's called mantaro
            if(voiceChannel == null) return;

            if (getRequestedChannelParsed().canTalk()){
                AudioTrackInfo information = currentTrack.getInfo();
                String title = information.title;
                long trackLength = information.length;

                User user = null;
                if(getCurrentTrack().getUserData() != null){
                    user = MantaroBot.getInstance().getUserById(String.valueOf(getCurrentTrack().getUserData()));
                }

                getRequestedChannelParsed().sendMessage(String.format("\uD83D\uDCE3 Now playing **%s** (%s) on **%s** | %s",
                        title, AudioUtils.getLength(trackLength), voiceChannel.getName(), user != null ?
                                String.format("Requested by **%s#%s**", user.getName(), user.getDiscriminator()) : "")).queue(message -> message.delete().queueAfter(
                                        90, TimeUnit.SECONDS)
                );
            }
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            nextTrack(false, false);
            onTrackStart();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if(getRequestedChannelParsed() != null && getRequestedChannelParsed().canTalk()){
            getRequestedChannelParsed().sendMessage(EmoteReference.SAD +
                    "Something went wrong while playing this track! Sorry for the inconveniences, I'll try to play the next one avaliable if there is one.").queue();
        }
    }

    public Guild getGuild(){
        return MantaroBot.getInstance().getGuildById(guildId);
    }

    public int getRequiredVotes() {
        int listeners = (int) getGuild().getAudioManager().getConnectedChannel().getMembers().stream()
                .filter(m -> !m.getUser().isBot() && !m.getVoiceState().isDeafened()).count();
        return (int) Math.ceil(listeners * .55);
    }

    public boolean isStopped(){
        return queue.isEmpty() && currentTrack == null;
    }

    public void shuffle(){
        List<AudioTrack> tempList = new ArrayList<>(getQueue());
        Collections.shuffle(tempList);

        queue.clear();
        queue.addAll(tempList);
    }

    public MantaroShard getShard() {
        return MantaroBot.getInstance().getShard(getGuild().getJDA().getShardInfo().getShardId());
    }

    public TextChannel getRequestedChannelParsed(){
        if(requestedChannel == 0) return null;
        return MantaroBot.getInstance().getTextChannelById(requestedChannel);
    }

    public void stop(){
        queue.clear();
        onStop();
    }

    public void getQueueAsList(Consumer<List<AudioTrack>> list) {
        List<AudioTrack> tempList = new ArrayList<>(getQueue());
        list.accept(tempList);
        queue.clear();
        queue.addAll(tempList);
    }

    private void onStop() {
        getVoteStop().clear();
        getVoteSkips().clear();

        Guild g = getGuild();
        if (g == null) return;
        AudioManager m = g.getAudioManager();
        if (m == null) return;

        boolean premium = MantaroData.db().getGuild(g).isPremium();

        try{
            TextChannel ch = getRequestedChannelParsed();
            if (ch != null && ch.canTalk()) {
                ch.sendMessage(EmoteReference.MEGA + "Finished playing current queue! I hope you enjoyed it.\n" +
                        (premium ? "" :
                                ":heart: Consider donating on patreon.com/mantaro if you like me, even a small donation will help towards keeping the bot alive"))
                        .queue(message -> message.delete().queueAfter(30, TimeUnit.SECONDS));
            }

        } catch (Exception ignored){}

        requestedChannel = 0;
        m.closeAudioConnection();
    }

    public enum Repeat {
        SONG, QUEUE
    }
}
