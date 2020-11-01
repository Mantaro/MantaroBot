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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.exporters.Metrics;

import java.awt.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

public class AudioLoader implements AudioLoadResultHandler {

    private static final int MAX_QUEUE_LENGTH = 350;
    private static final long MAX_SONG_LENGTH = TimeUnit.HOURS.toMillis(2);
    private final GuildMessageReceivedEvent event;
    private final boolean insertFirst;
    private final GuildMusicManager musicManager;
    private final boolean skipSelection;
    private final I18n language;

    public AudioLoader(GuildMusicManager musicManager, GuildMessageReceivedEvent event, boolean skipSelection, boolean insertFirst) {
        this.musicManager = musicManager;
        this.event = event;
        this.skipSelection = skipSelection;
        this.insertFirst = insertFirst;
        this.language = I18n.of(event.getGuild());
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        loadSingle(track, false);
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (playlist.isSearchResult()) {
            if (!skipSelection) {
                onSearch(playlist);
            } else {
                loadSingle(playlist.getTracks().get(0), false);
            }

            return;
        }

        try {
            var i = 0;
            for (var track : playlist.getTracks()) {
                var dbGuild = MantaroData.db().getGuild(event.getGuild());
                var user = MantaroData.db().getUser(event.getMember());
                var guildData = dbGuild.getData();

                if (guildData.getMusicQueueSizeLimit() != null) {
                    if (i < guildData.getMusicQueueSizeLimit()) {
                        loadSingle(track, true);
                    } else {
                        event.getChannel().sendMessageFormat(
                                language.get("commands.music_general.loader.over_limit"),
                                EmoteReference.WARNING, guildData.getMusicQueueSizeLimit()
                        ).queue();
                        break;
                    }
                } else {
                    if (i > MAX_QUEUE_LENGTH && (!dbGuild.isPremium() && !user.isPremium())) {
                        event.getChannel().sendMessageFormat(
                                language.get("commands.music_general.loader.over_limit"),
                                EmoteReference.WARNING, MAX_QUEUE_LENGTH
                        ).queue();
                        break; //stop adding songs
                    } else {
                        loadSingle(track, true);
                    }
                }

                i++;
            }

            event.getChannel().sendMessageFormat(language.get("commands.music_general.loader.loaded_playlist"),
                    EmoteReference.CORRECT, i, playlist.getName(),
                    AudioCmdUtils.getDurationMinutes(
                            playlist.getTracks()
                                    .stream()
                                    .mapToLong(temp -> temp.getInfo().length).sum()
                    )
            ).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void noMatches() {
        event.getChannel().sendMessageFormat(language.get("commands.music_general.loader.no_matches"), EmoteReference.ERROR).queue();
    }


    @Override
    public void loadFailed(FriendlyException exception) {
        if (!exception.severity.equals(FriendlyException.Severity.FAULT)) {
            event.getChannel().sendMessage(
                    String.format(language.get("commands.music_general.loader.error_fetching"),
                            EmoteReference.ERROR, exception.getMessage()
                    )
            ).queue();
        } else {
            Metrics.TRACK_EVENTS.labels("tracks_failed").inc();
        }
    }

    private void loadSingle(AudioTrack audioTrack, boolean silent) {
        final var trackInfo = audioTrack.getInfo();
        final var managedDatabase = MantaroData.db();

        audioTrack.setUserData(event.getAuthor().getId());

        final var trackScheduler = musicManager.getTrackScheduler();
        final var dbGuild = managedDatabase.getGuild(event.getGuild());
        final var dbUser = managedDatabase.getUser(event.getMember());
        final var guildData = dbGuild.getData();

        final var title = trackInfo.title;
        final var length = trackInfo.length;

        var queueLimit = guildData.getMusicQueueSizeLimit() == null || guildData.getMusicQueueSizeLimit() < 1 ?
                MAX_QUEUE_LENGTH : guildData.getMusicQueueSizeLimit();

        var fqSize = guildData.getMaxFairQueue();
        ConcurrentLinkedDeque<AudioTrack> queue = trackScheduler.getQueue();

        if (queue.size() > queueLimit && !dbUser.isPremium() && !dbGuild.isPremium()) {
            if (!silent) {
                event.getChannel().sendMessageFormat(
                        language.get("commands.music_general.loader.over_queue_limit"),
                        EmoteReference.WARNING, title, queueLimit
                ).queue(message -> message.delete().queueAfter(30, TimeUnit.SECONDS));
            }
            return;
        }

        if (trackInfo.length > MAX_SONG_LENGTH && (!dbUser.isPremium() && !dbGuild.isPremium())) {
            event.getChannel().sendMessageFormat(language.get("commands.music_general.loader.over_32_minutes"),
                    EmoteReference.WARNING, title,
                    Utils.formatDuration(MAX_SONG_LENGTH),
                    Utils.formatDuration(length)
            ).queue();
            return;
        }

        //Comparing if the URLs are the same to be 100% sure they're just not spamming the same url over and over again.
        if (queue.stream()
                .filter(track -> trackInfo.uri.equals(trackInfo.uri)).count() > fqSize && !silent) {
            event.getChannel().sendMessageFormat(
                    language.get("commands.music_general.loader.fair_queue_limit_reached"),
                    EmoteReference.ERROR, fqSize + 1
            ).queue();

            return;
        }

        trackScheduler.queue(audioTrack, insertFirst);
        trackScheduler.setRequestedChannel(event.getChannel().getIdLong());

        if (!silent) {
            var player = managedDatabase.getPlayer(event.getAuthor());
            var badge = APIUtils.getHushBadge(audioTrack.getIdentifier(), Utils.HushType.MUSIC);
            if (badge != null) {
                player.getData().addBadgeIfAbsent(badge);
                player.save();
            }

            event.getChannel().sendMessageFormat(
                    language.get("commands.music_general.loader.loaded_song"), EmoteReference.CORRECT, title, Utils.formatDuration(length)
            ).queue();
        }

        Metrics.TRACK_EVENTS.labels("tracks_load").inc();
    }

    private void onSearch(AudioPlaylist playlist) {
        var list = playlist.getTracks();
        DiscordUtils.selectList(event, list.subList(0, Math.min(5, list.size())),
                track -> String.format(
                        "%s**[%s](%s)** (%s)",
                        EmoteReference.BLUE_SMALL_MARKER,
                        track.getInfo().title,
                        track.getInfo().uri,
                        AudioCmdUtils.getDurationMinutes(track.getInfo().length)
                ), s -> new EmbedBuilder()
                        .setColor(Color.CYAN)
                        .setAuthor(language.get("commands.music_general.loader.selection_text"),
                                "https://i.imgur.com/sFDpUZy.png",
                                event.getAuthor().getEffectiveAvatarUrl()
                        )
                        .setThumbnail("https://i.imgur.com/FWKIR7N.png")
                        .setDescription(s)
                        .setFooter(language.get("commands.music_general.loader.timeout_text"),
                                event.getAuthor().getEffectiveAvatarUrl()
                        )
                        .build(),
                selected -> loadSingle(selected, false)
        );

        Metrics.TRACK_EVENTS.labels("tracks_search").inc();
    }
}
