/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.music.requester;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.io.Link;
import lavalink.client.player.IPlayer;
import lavalink.client.player.event.PlayerEventListenerAdapter;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

public class TrackScheduler extends PlayerEventListenerAdapter {
    private final String guildId;
    private final ConcurrentLinkedDeque<AudioTrack> queue;
    private final List<String> voteSkips;
    private final List<String> voteStop;
    private final I18n language;
    private Link audioPlayer;
    private long lastMessageSentAt;
    private long lastErrorSentAt;
    private AudioTrack previousTrack, currentTrack;
    private Repeat repeatMode;
    private long requestedChannel;
    private long errorCount = 0;

    public TrackScheduler(Link player, String guildId) {
        this.audioPlayer = player;
        this.queue = new ConcurrentLinkedDeque<>();
        this.guildId = guildId;
        this.voteSkips = new ArrayList<>();
        this.voteStop = new ArrayList<>();

        //Only take guild language settings into consideration for announcement messages.
        this.language = I18n.of(guildId);
    }

    public void queue(AudioTrack track, boolean addFirst) {
        if (getMusicPlayer().getPlayingTrack() != null) {
            if (addFirst) {
                queue.addFirst(track);
            } else {
                queue.offer(track);
            }
        } else {
            getMusicPlayer().playTrack(track);
            currentTrack = track;
        }
    }

    public void queue(AudioTrack track) {
        queue(track, false);
    }

    public void nextTrack(boolean force, boolean skip) {
        getVoteSkips().clear();

        if (repeatMode == Repeat.SONG && currentTrack != null && !force) {
            queue(currentTrack.makeClone());
        } else {
            if (currentTrack != null) {
                previousTrack = currentTrack;
            }

            currentTrack = queue.poll();

            //This actually reads wrongly, but current = next in this context, since we switched it already.
            if (currentTrack != null) {
                getMusicPlayer().playTrack(currentTrack);
            }

            if (skip) {
                onTrackStart();
            }

            if (repeatMode == Repeat.QUEUE) {
                queue(previousTrack.makeClone());
            }
        }
    }

    private void onTrackStart() {
        if (currentTrack == null) {
            onStop();
            return;
        }

        final var guild = MantaroBot.getInstance().getShardManager().getGuildById(guildId);
        final var dbGuild = MantaroData.db().getGuild(guildId);

        if (dbGuild.getData().isMusicAnnounce() && requestedChannel != 0 && getRequestedTextChannel() != null) {
            var voiceState = getRequestedTextChannel().getGuild().getSelfMember().getVoiceState();

            //What kind of massive meme is this? part 2
            if (voiceState == null) {
                this.getAudioPlayer().destroy();
                return;
            }

            final var voiceChannel = voiceState.getChannel();

            //What kind of massive meme is this?
            //It's called mantaro
            if (voiceChannel == null) {
                this.getAudioPlayer().destroy();
                return;
            }

            //Force it in case it keeps going all the time?
            if (errorCount > 20) {
                getRequestedTextChannel().sendMessageFormat(
                        language.get("commands.music_general.too_many_errors"),
                        EmoteReference.ERROR
                ).queue();

                onStop();
                return;
            }

            if (getRequestedTextChannel().canTalk() && repeatMode != Repeat.SONG) {
                var information = currentTrack.getInfo();
                var title = information.title;
                var trackLength = information.length;

                Member user = null;
                if (getCurrentTrack().getUserData() != null && guild != null) {
                    // Retrieve member instead of user, so it gets cached.
                    try {
                        user = guild.retrieveMemberById(
                                String.valueOf(getCurrentTrack().getUserData()), false
                        ).complete();
                    } catch (Exception ignored) {}
                }

                //Avoid massive spam of "now playing..." when repeating songs.
                if (lastMessageSentAt == 0 || lastMessageSentAt + 10000 < System.currentTimeMillis()) {
                    getRequestedTextChannel().sendMessage(
                            new MessageBuilder()
                                    .append(String.format(
                                            language.get("commands.music_general.np_message"),
                                            "\uD83D\uDCE3", title,
                                            AudioCmdUtils.getDurationMinutes(trackLength),
                                            voiceChannel.getName(), user != null ?
                                                    String.format(language.get("general.requested_by"),
                                                            String.format("**%s**", user.getUser().getAsTag())) : "")
                                    )
                                    .build()
                    ).queue(message -> {
                        lastMessageSentAt = System.currentTimeMillis();
                        message.delete().queueAfter(90, TimeUnit.SECONDS);
                    });
                }
            }
        }
    }

    @Override
    public void onTrackEnd(IPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            nextTrack(false, false);
            onTrackStart();
        }
    }

    @Override
    public void onTrackException(IPlayer player, AudioTrack track, Exception exception) {
        if (getRequestedTextChannel() != null && getRequestedTextChannel().canTalk()) {
            //Avoid massive spam of when song error in mass.
            if ((lastErrorSentAt == 0 || lastErrorSentAt + 60000 < System.currentTimeMillis()) && errorCount < 10) {
                lastErrorSentAt = System.currentTimeMillis();
                getRequestedTextChannel().sendMessageFormat(
                        language.get("commands.music_general.track_error"), EmoteReference.SAD
                ).queue();
            }

            errorCount++;
        }
    }

    public Guild getGuild() {
        return MantaroBot.getInstance().getShardManager().getGuildById(guildId);
    }

    public int getRequiredVotes() {
        var listeners = (int) getGuild().getVoiceChannelById(getAudioPlayer().getChannel())
                .getMembers().stream()
                .filter(m -> !m.getUser().isBot() && !m.getVoiceState().isDeafened())
                .count();

        return (int) Math.ceil(listeners * .55);
    }

    public void shuffle() {
        List<AudioTrack> tempList = new ArrayList<>(getQueue());
        Collections.shuffle(tempList);

        queue.clear();
        queue.addAll(tempList);
    }

    public TextChannel getRequestedTextChannel() {
        if (requestedChannel == 0)
            return null;

        return MantaroBot.getInstance().getShardManager().getTextChannelById(requestedChannel);
    }

    public void stop() {
        queue.clear();
        onStop();
    }

    public List<AudioTrack> getQueueAsList() {
        return new LinkedList<>(getQueue());
    }

    public void acceptNewQueue(List<AudioTrack> newQueue) {
        queue.clear();
        queue.addAll(newQueue);
    }

    private void onStop() {
        final var managedDatabase = MantaroData.db();
        final var lavalinkPlayer = getAudioPlayer().getPlayer();
        // Stop the track.
        if (lavalinkPlayer.getPlayingTrack() != null) {
            lavalinkPlayer.stopTrack();
        }

        getVoteStop().clear();
        getVoteSkips().clear();

        var guild = getGuild();
        if (guild == null) {
            //Why?
            this.getAudioPlayer().destroy();
            return;
        }

        var premium = managedDatabase.getGuild(guild).isPremium();
        try {
            var ch = getRequestedTextChannel();
            if (ch != null && ch.canTalk()) {
                ch.sendMessageFormat(
                        language.get("commands.music_general.queue_finished"),
                        EmoteReference.MEGA, premium ? "" :
                                String.format(language.get("commands.music_general.premium_beg"),
                                        EmoteReference.HEART
                                )
                ).queue(message -> message.delete().queueAfter(30, TimeUnit.SECONDS));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // If not reset, this will come us to bite on next run.
        requestedChannel = 0;
        errorCount = 0;

        //If not set to null, those two objects will always be in scope and dangle around in the heap forever.
        //Some AudioTrack objects were of almost 500kb of size, I guess 100k of those can cause a meme.
        currentTrack = null;
        previousTrack = null;

        //Disconnect this audio player.
        MantaroBot.getInstance().getAudioManager().resetMusicManagerFor(guildId);
    }

    public ConcurrentLinkedDeque<AudioTrack> getQueue() {
        return this.queue;
    }

    public List<String> getVoteSkips() {
        return this.voteSkips;
    }

    public List<String> getVoteStop() {
        return this.voteStop;
    }

    public AudioTrack getPreviousTrack() {
        return this.previousTrack;
    }

    public AudioTrack getCurrentTrack() {
        return this.currentTrack;
    }

    public Repeat getRepeatMode() {
        return this.repeatMode;
    }

    public void setRepeatMode(Repeat repeatMode) {
        this.repeatMode = repeatMode;
    }

    public I18n getLanguage() {
        return this.language;
    }

    public void setRequestedChannel(long requestedChannel) {
        this.requestedChannel = requestedChannel;
    }

    public Link getAudioPlayer() {
        if (audioPlayer == null) {
            audioPlayer = MantaroBot.getInstance().getLavaLink().getLink(guildId);
        }

        return audioPlayer;
    }

    public IPlayer getMusicPlayer() {
        return getAudioPlayer().getPlayer();
    }

    public enum Repeat {
        SONG, QUEUE
    }
}
