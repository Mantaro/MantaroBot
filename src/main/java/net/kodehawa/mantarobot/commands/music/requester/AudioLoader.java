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
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import io.prometheus.client.Counter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.commands.music.utils.AudioUtils;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AudioLoader implements AudioLoadResultHandler {
    private static final Counter trackEvents = Counter.build()
            .name("track_event").help("Music Track Events (failed/loaded/searched)")
            .labelNames("type")
            .register();

    private static final int MAX_QUEUE_LENGTH = 350;
    private static final long MAX_SONG_LENGTH = 1920000; //32 minutes
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
            int i = 0;
            for (AudioTrack track : playlist.getTracks()) {
                DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                DBUser user = MantaroData.db().getUser(event.getMember());
                GuildData guildData = dbGuild.getData();

                if (guildData.getMusicQueueSizeLimit() != null) {
                    if (i < guildData.getMusicQueueSizeLimit()) {
                        loadSingle(track, true);
                    } else {
                        event.getChannel().sendMessageFormat(language.get("commands.music_general.loader.over_limit"), EmoteReference.WARNING, guildData.getMusicQueueSizeLimit()).queue();
                        break;
                    }
                } else {
                    if (i > MAX_QUEUE_LENGTH && (!dbGuild.isPremium() && !user.isPremium())) {
                        event.getChannel().sendMessageFormat(language.get("commands.music_general.loader.over_limit"), EmoteReference.WARNING, MAX_QUEUE_LENGTH).queue();
                        break; //stop adding songs
                    } else {
                        loadSingle(track, true);
                    }
                }

                i++;
            }

            event.getChannel().sendMessageFormat(language.get("commands.music_general.loader.loaded_playlist"),
                    EmoteReference.CORRECT, i, playlist.getName(), Utils.getDurationMinutes(playlist.getTracks().stream().mapToLong(temp -> temp.getInfo().length).sum())
            ).queue();
        } catch (Exception e) {
            SentryHelper.captureExceptionContext("Cannot load playlist. I guess something broke pretty hard. Please check", e, this.getClass(), "Music Loader");
        }
    }

    @Override
    public void noMatches() {
        event.getChannel().sendMessageFormat(language.get("commands.music_general.loader.no_matches"), EmoteReference.ERROR).queue();
    }


    @Override
    public void loadFailed(FriendlyException exception) {
        if (!exception.severity.equals(FriendlyException.Severity.FAULT)) {
            event.getChannel().sendMessage(String.format(language.get("commands.music_general.loader.error_fetching"), EmoteReference.ERROR, exception.getMessage())).queue();
        } else {
            trackEvents.labels("tracks_failed").inc();
        }
    }

    private void loadSingle(AudioTrack audioTrack, boolean silent) {
        AudioTrackInfo trackInfo = audioTrack.getInfo();
        audioTrack.setUserData(event.getAuthor().getId());
        DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
        DBUser dbUser = MantaroData.db().getUser(event.getMember());
        GuildData guildData = dbGuild.getData();

        String title = trackInfo.title;
        long length = trackInfo.length;

        long queueLimit = !Optional.ofNullable(dbGuild.getData().getMusicQueueSizeLimit()).isPresent() ? MAX_QUEUE_LENGTH :
                dbGuild.getData().getMusicQueueSizeLimit();
        int fqSize = guildData.getMaxFairQueue();

        if (musicManager.getTrackScheduler().getQueue().size() > queueLimit && !dbUser.isPremium() && !dbGuild.isPremium()) {
            if (!silent)
                event.getChannel().sendMessageFormat(language.get("commands.music_general.loader.over_queue_limit"),
                        EmoteReference.WARNING, title, queueLimit
                ).queue(message -> message.delete().queueAfter(30, TimeUnit.SECONDS));
            return;
        }

        if (audioTrack.getInfo().length > MAX_SONG_LENGTH && (!dbUser.isPremium() && !dbGuild.isPremium())) {
            event.getChannel().sendMessageFormat(language.get("commands.music_general.loader.over_32_minutes"),
                    EmoteReference.WARNING, title, AudioUtils.getLength(length)
            ).queue();
            return;
        }

        //Comparing if the URLs are the same to be 100% sure they're just not spamming the same url over and over again.
        if (musicManager.getTrackScheduler().getQueue().stream().filter(track -> track.getInfo().uri.equals(audioTrack.getInfo().uri)).count() > fqSize && !silent) {
            event.getChannel().sendMessageFormat(language.get("commands.music_general.loader.fair_queue_limit_reached"), EmoteReference.ERROR, fqSize + 1).queue();
            return;
        }

        musicManager.getTrackScheduler().queue(audioTrack, insertFirst);
        musicManager.getTrackScheduler().setRequestedChannel(event.getChannel().getIdLong());

        if (!silent) {
            //Hush from here babe, hehe.
            Player player = MantaroData.db().getPlayer(event.getAuthor());
            Badge badge = APIUtils.getHushBadge(audioTrack.getIdentifier(), Utils.HushType.MUSIC);
            if (badge != null) {
                player.getData().addBadgeIfAbsent(badge);
                player.save();
            }

            new MessageBuilder().append(
                    String.format(language.get("commands.music_general.loader.loaded_song"), EmoteReference.CORRECT, title, AudioUtils.getLength(length)))
                    .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE)
                    .sendTo(event.getChannel()).queue();
        }

        trackEvents.labels("tracks_load").inc();
    }

    private void onSearch(AudioPlaylist playlist) {
        List<AudioTrack> list = playlist.getTracks();
        DiscordUtils.selectList(event, list.subList(0, Math.min(5, list.size())),
                track -> String.format("**[%s](%s)** (%s)", track.getInfo().title, track.getInfo().uri, Utils.getDurationMinutes(track.getInfo().length)),
                s -> new EmbedBuilder().setColor(Color.CYAN).setAuthor(language.get("commands.music_general.loader.selection_text"), "https://i.imgur.com/sFDpUZy.png")
                        .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png")
                        .setDescription(s)
                        .setFooter(language.get("commands.music_general.loader.timeout_text"), null).build(),
                selected -> loadSingle(selected, false)
        );

        trackEvents.labels("tracks_search").inc();
    }
}
