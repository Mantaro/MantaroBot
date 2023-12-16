/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.music.requester;

import dev.arbjerg.lavalink.client.LavalinkPlayer;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.protocol.Track;
import dev.arbjerg.lavalink.protocol.v4.Message;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.commands.music.utils.TrackData;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TrackScheduler {
    private static final Random random = new Random();
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final String guildId;
    private final ConcurrentLinkedDeque<Track> queue;
    private final List<String> voteSkips;
    private final List<String> voteStop;
    private final I18n language;
    private Link audioPlayer;
    private long lastMessageSentAt;
    private long lastErrorSentAt;
    private Track previousTrack;
    private Track currentTrack;
    private Repeat repeatMode;
    private long requestedChannel;
    private long errorCount = 0;
    private boolean pausedManually = false;

    public TrackScheduler(Link player, String guildId) {
        this.audioPlayer = player;
        this.queue = new ConcurrentLinkedDeque<>();
        this.guildId = guildId;
        this.voteSkips = new ArrayList<>();
        this.voteStop = new ArrayList<>();

        //Only take guild language settings into consideration for announcement messages.
        this.language = I18n.of(guildId);
    }

    public void queue(Track track, boolean addFirst) {
        if (getMusicPlayer().block(Duration.ofMillis(300)).getTrack() != null) {
            if (addFirst) {
                queue.addFirst(track);
            } else {
                queue.offer(track);
            }
        } else {
            getLink().createOrUpdatePlayer()
                    .setTrack(track)
                    .asMono()
                    .subscribe();
            currentTrack = track;
        }
    }

    public void queue(Track track) {
        queue(track, false);
    }

    public void nextTrack(boolean force, boolean skip) {
        getVoteSkips().clear();
        if (repeatMode == Repeat.SONG && currentTrack != null && !force) {
            queue(currentTrack.makeClone());
        } else {
            if (currentTrack != null) {
                previousTrack = currentTrack.makeClone();
            }

            currentTrack = queue.poll();
            //This actually reads wrongly, but current = next in this context, since we switched it already.
            if (currentTrack != null) {
                getLink().createOrUpdatePlayer()
                        .setTrack(currentTrack)
                        .asMono()
                        .subscribe();
            }

            if (skip) {
                onTrackStart();
            }

            if (repeatMode == Repeat.QUEUE) {
                if (previousTrack == null) {
                    currentTrack = null;
                    onTrackStart();
                    return;
                }

                queue(previousTrack);
            }
        }
    }

    public void onTrackStart() {
        if (currentTrack == null) {
            onStop();
            return;
        }

        final var guild = MantaroBot.getInstance().getShardManager().getGuildById(guildId);
        if (guild == null) { // I mean, sure...
            onStop();
            return;
        }

        final var dbGuild = MantaroData.db().getGuild(guildId);

        if (dbGuild.isMusicAnnounce() && requestedChannel != 0 && getRequestedTextChannel() != null) {
            var voiceState = getRequestedTextChannel().getGuild().getSelfMember().getVoiceState();
            //What kind of massive meme is this? part 2
            if (voiceState == null) {
                this.getLink().destroyPlayer();
                return;
            }

            final var voiceChannel = voiceState.getChannel();

            //What kind of massive meme is this?
            //It's called mantaro
            if (voiceChannel == null) {
                this.getLink().destroyPlayer();
                return;
            }

            if (getRequestedTextChannel().canTalk() && repeatMode != Repeat.SONG) {
                var information = currentTrack.getInfo();
                var title = information.getTitle();
                var trackLength = information.getLength();

                Member user = null;
                var userData = getCurrentTrack().getUserData(TrackData.class);
                if (userData != null && userData.userId() != null) {
                    // Retrieve member instead of user, so it gets cached.
                    try {
                        user = guild.retrieveMemberById(userData.userId()).useCache(true).complete();
                    } catch (IllegalStateException | NumberFormatException ignored) {}
                }

                //Avoid massive spam of "now playing..." when repeating songs.
                if (lastMessageSentAt == 0 || lastMessageSentAt + 10000 < System.currentTimeMillis()) {
                    getRequestedTextChannel().sendMessage(
                            String.format(
                                language.get("commands.music_general.np_message"),
                                "\uD83D\uDCE3", title,
                                AudioCmdUtils.getDurationMinutes(trackLength),
                                voiceChannel.getName(), user != null ?
                                        String.format(language.get("general.requested_by"),
                                                String.format("**%s**", user.getUser().getName())) : "")
                    ).queue(message -> {
                        if (getRequestedTextChannel() != null) {
                            lastMessageSentAt = System.currentTimeMillis();
                            message.delete().queueAfter(90, TimeUnit.SECONDS, scheduledExecutor);
                        }
                    });
                }
            }
        }
    }

    public void onTrackEnd(Message.EmittedEvent.TrackEndEvent.AudioTrackEndReason reason) {
        if(reason.getMayStartNext()) {
            nextTrack(false, false);
            onTrackStart();
        }
    }

    public void onTrackException() {
        if (getRequestedTextChannel() != null && getRequestedTextChannel().canTalk()) {
            //Avoid massive spam of when song error in mass.
            if ((lastErrorSentAt == 0 || lastErrorSentAt + 60000 < System.currentTimeMillis()) && errorCount < 10) {
                lastErrorSentAt = System.currentTimeMillis();
                getRequestedTextChannel().sendMessageFormat(language.get("commands.music_general.track_error"), EmoteReference.SAD).queue();
            }

            errorCount++;
        }
    }

    public Guild getGuild() {
        return MantaroBot.getInstance().getShardManager().getGuildById(guildId);
    }

    public int getRequiredVotes() {
        //noinspection DataFlowIssue
        var listeners = (int) getGuild().getChannelById(AudioChannel.class, getGuild().getSelfMember().getVoiceState().getChannel().getId())
                .getMembers().stream()
                .filter(m -> m.getVoiceState() != null) // Shouldn't happen?
                .filter(m -> !m.getUser().isBot() && !m.getVoiceState().isDeafened())
                .count();

        return (int) Math.ceil(listeners * .55);
    }

    public void shuffle() {
        List<Track> tempList = new ArrayList<>(getQueue());
        Collections.shuffle(tempList);

        queue.clear();
        queue.addAll(tempList);
    }

    public GuildMessageChannel getRequestedTextChannel() {
        if (requestedChannel == 0)
            return null;

        return MantaroBot.getInstance().getShardManager().getChannelById(GuildMessageChannel.class, requestedChannel);
    }

    public void stop() {
        queue.clear();
        onStop();
    }

    public List<Track> getQueueAsList() {
        return new LinkedList<>(getQueue());
    }

    public void acceptNewQueue(List<Track> newQueue) {
        queue.clear();
        queue.addAll(newQueue);
    }

    private void onStop() {
        getVoteStop().clear();
        getVoteSkips().clear();

        var guild = getGuild();
        if (guild == null) {
            // Why?
            this.getLink().destroyPlayer();
            return;
        }

        final var managedDatabase = MantaroData.db();
        final var lavalinkPlayer = getLink().getPlayer();
        var premium = managedDatabase.getGuild(guild).isPremium();
        try {
            final var ch = getRequestedTextChannel();
            if (ch != null && ch.canTalk()) {
                String beg = "";
                if (!premium && random.nextBoolean()) {
                    beg = String.format(language.get("commands.music_general.premium_beg"), EmoteReference.HEART);
                }

                ch.sendMessageFormat(language.get("commands.music_general.queue_finished"), EmoteReference.MEGA, beg).queue();
            }
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }

        // If not reset, this will come us to bite on next run.
        requestedChannel = 0;
        errorCount = 0;
        pausedManually = false;

        // If not set to null, those two objects will always be in scope and dangle around in the heap forever.
        // Some AudioTrack objects were of almost 500kb of size, I guess 100k of those can cause a meme.
        currentTrack = null;
        previousTrack = null;

        // Stop the track and disconnect
        if (lavalinkPlayer.block(Duration.ofMillis(300)).getTrack() != null) {
            stopCurrentTrack();
        }

        guild.getJDA().getDirectAudioController().disconnect(guild);
        MantaroBot.getInstance().getAudioManager().resetMusicManagerFor(guildId);
    }

    public ConcurrentLinkedDeque<Track> getQueue() {
        return this.queue;
    }

    public List<String> getVoteSkips() {
        return this.voteSkips;
    }

    public List<String> getVoteStop() {
        return this.voteStop;
    }

    @SuppressWarnings("unused")
    public Track getPreviousTrack() {
        return this.previousTrack;
    }

    public Track getCurrentTrack() {
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

    public boolean isPausedManually() {
        return pausedManually;
    }

    public void setPausedManually(boolean pausedManually) {
        this.pausedManually = pausedManually;
    }

    public void setRequestedChannel(long requestedChannel) {
        this.requestedChannel = requestedChannel;
    }

    public void stopCurrentTrack() {
        getLink().createOrUpdatePlayer()
                .stopTrack()
                .asMono()
                .subscribe();
    }

    public Link getLink() {
        if (audioPlayer == null) {
            audioPlayer = MantaroBot.getInstance().getLavaLink().getLink(Long.parseLong(guildId));
        }

        return audioPlayer;
    }

    public Mono<LavalinkPlayer> getMusicPlayer() {
        return getLink().getPlayer();
    }

    public enum Repeat {
        SONG, QUEUE
    }
}
