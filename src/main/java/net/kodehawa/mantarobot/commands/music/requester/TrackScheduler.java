/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.music.requester;

import br.com.brjdevs.java.utils.async.Async;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.utils.AudioUtils;
import net.kodehawa.mantarobot.core.shard.MantaroShard;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TrackScheduler extends AudioEventAdapter {
    @Getter
    private final AudioPlayer audioPlayer;
    private final String guildId;
    @Getter
    private final ConcurrentLinkedQueue<AudioTrack> queue;
    @Getter
    private final List<String> voteSkips;
    @Getter
    private final List<String> voteStop;
    @Getter
    private AudioTrack previousTrack, currentTrack;
    @Getter
    @Setter
    private Repeat repeatMode;
    @Setter
    private long requestedChannel;

    public TrackScheduler(AudioPlayer player, String guildId) {
        this.audioPlayer = player;
        this.queue = new ConcurrentLinkedQueue<>();
        this.guildId = guildId;
        this.voteSkips = new ArrayList<>();
        this.voteStop = new ArrayList<>();
    }

    public void queue(AudioTrack track) {
        if(!audioPlayer.startTrack(track, true)) {
            queue.offer(track);
        } else {
            currentTrack = track;
        }
    }

    public void nextTrack(boolean force, boolean skip) {
        getVoteSkips().clear();
        if(repeatMode == Repeat.SONG && currentTrack != null && !force) {
            queue(currentTrack.makeClone());
        } else {
            if(currentTrack != null) previousTrack = currentTrack;
            currentTrack = queue.poll();
            audioPlayer.startTrack(currentTrack, !force);
            if(skip) onTrackStart();
            if(repeatMode == Repeat.QUEUE) queue(previousTrack.makeClone());
        }
    }

    private void onTrackStart() {
        if(currentTrack == null) {
            onStop();
            return;
        }

        if(MantaroData.db().getGuild(guildId).getData().isMusicAnnounce() && requestedChannel != 0 && getRequestedChannelParsed() != null) {
            VoiceChannel voiceChannel = getRequestedChannelParsed().getGuild().getSelfMember().getVoiceState().getChannel();

            //What kind of massive meme is this?
            //It's called mantaro
            if(voiceChannel == null) return;

            if(getRequestedChannelParsed().canTalk()) {
                AudioTrackInfo information = currentTrack.getInfo();
                String title = information.title;
                long trackLength = information.length;

                User user = null;
                if(getCurrentTrack().getUserData() != null) {
                    user = MantaroBot.getInstance().getUserById(String.valueOf(getCurrentTrack().getUserData()));
                }

                getRequestedChannelParsed().sendMessage(
                        new MessageBuilder().append(String.format("\uD83D\uDCE3 Now playing **%s** (%s) on **%s** | %s",
                        title, AudioUtils.getLength(trackLength), voiceChannel.getName(), user != null ?
                                        String.format("Requested by **%s#%s**", user.getName(), user.getDiscriminator()) : ""))
                                .stripMentions(getGuild(), MessageBuilder.MentionType.EVERYONE, MessageBuilder.MentionType.HERE)
                                .build()
                ).queue(message -> message.delete().queueAfter(90, TimeUnit.SECONDS));
            }
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if(endReason.mayStartNext) {
            nextTrack(false, false);
            onTrackStart();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if(getRequestedChannelParsed() != null && getRequestedChannelParsed().canTalk()) {
            getRequestedChannelParsed().sendMessage(EmoteReference.SAD +
                    "Something went wrong while playing this track! Sorry for the inconveniences, I'll try to play the next one available if there is one.").queue();
        }
    }

    public Guild getGuild() {
        return MantaroBot.getInstance().getGuildById(guildId);
    }

    public int getRequiredVotes() {
        int listeners = (int) getGuild().getAudioManager().getConnectedChannel().getMembers().stream().filter(m -> !m.getUser().isBot() && !m.getVoiceState().isDeafened())
                .count();

        return (int) Math.ceil(listeners * .55);
    }

    public void shuffle() {
        List<AudioTrack> tempList = new ArrayList<>(getQueue());
        Collections.shuffle(tempList);

        queue.clear();
        queue.addAll(tempList);
    }

    public MantaroShard getShard() {
        return MantaroBot.getInstance().getShard(getGuild().getJDA().getShardInfo().getShardId());
    }

    public TextChannel getRequestedChannelParsed() {
        if(requestedChannel == 0) return null;
        return MantaroBot.getInstance().getTextChannelById(requestedChannel);
    }

    public void stop() {
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
        if(g == null) return;
        AudioManager m = g.getAudioManager();
        if(m == null) return;

        boolean premium = MantaroData.db().getGuild(g).isPremium();

        try {
            TextChannel ch = getRequestedChannelParsed();
            if(ch != null && ch.canTalk()) {
                ch.sendMessage(EmoteReference.MEGA + "Finished playing current queue! I hope you enjoyed it.\n" +
                        (premium ? "" :
                                ":heart: Consider donating on patreon.com/mantaro if you like me, even a small donation will help towards keeping the bot alive"))
                        .queue(message -> message.delete().queueAfter(30, TimeUnit.SECONDS));
            }
        } catch(Exception ignored) {}

        requestedChannel = 0;
        Async.thread("Audio connection close", m::closeAudioConnection);
    }

    public enum Repeat {
        SONG, QUEUE
    }
}
