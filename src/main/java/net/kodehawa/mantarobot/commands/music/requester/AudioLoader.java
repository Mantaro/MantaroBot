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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.MongoGuild;
import net.kodehawa.mantarobot.db.entities.MongoUser;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.exporters.Metrics;

import java.awt.Color;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

public class AudioLoader implements AudioLoadResultHandler {
    private static final int MAX_QUEUE_LENGTH = 350;
    private static final long MAX_SONG_LENGTH = TimeUnit.HOURS.toMillis(2);
    private static final ManagedDatabase db = MantaroData.db();
    private final SlashContext ctx;
    private final boolean insertFirst;
    private final GuildMusicManager musicManager;
    private final boolean skipSelection;
    private final I18n language;
    private int failureCount = 0;

    public AudioLoader(GuildMusicManager musicManager, SlashContext ctx, boolean skipSelection, boolean insertFirst) {
        this.musicManager = musicManager;
        this.ctx = ctx;
        this.skipSelection = skipSelection;
        this.insertFirst = insertFirst;
        this.language = I18n.of(ctx.getGuild());
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        loadSingle(track, false, db.getGuild(ctx.getGuild()), db.getUser(ctx.getMember()));
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        final var member = ctx.getMember();
        if (playlist.isSearchResult()) {
            if (!skipSelection) {
                onSearch(playlist);
            } else {
                loadSingle(playlist.getTracks().get(0), false, db.getGuild(ctx.getGuild()), db.getUser(member));
            }

            return;
        }

        try {
            var count = 0;
            var dbGuild = db.getGuild(ctx.getGuild());
            var user = db.getUser(member);
            var i18nContext = new I18nContext(language);

            for (var track : playlist.getTracks()) {
                if (dbGuild.getMusicQueueSizeLimit() != null) {
                    if (count <= dbGuild.getMusicQueueSizeLimit()) {
                        loadSingle(track, true, dbGuild, user);
                    } else {
                        ctx.edit("commands.music_general.loader.over_limit", EmoteReference.WARNING, dbGuild.getMusicQueueSizeLimit());
                        break;
                    }
                } else {
                    if (count >= MAX_QUEUE_LENGTH && (!dbGuild.isPremium() && !user.isPremium())) {
                        ctx.edit("commands.music_general.loader.over_limit", EmoteReference.WARNING, MAX_QUEUE_LENGTH);
                        break; //stop adding songs
                    } else {
                        loadSingle(track, true, dbGuild, user);
                    }
                }

                count++;
            }

            ctx.editStripped("commands.music_general.loader.loaded_playlist",
                    EmoteReference.SATELLITE, count, MarkdownSanitizer.sanitize(playlist.getName()),
                    Utils.formatDuration(i18nContext,
                            playlist.getTracks()
                                    .stream()
                                    .mapToLong(temp -> temp.getInfo().length).sum()
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void noMatches() {
        ctx.edit("commands.music_general.loader.no_matches", EmoteReference.ERROR);
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        if (failureCount == 0) {
            if (exception.getMessage() == null) {
                ctx.edit("commands.music_general.loader.unknown_error_loading", EmoteReference.ERROR);
            } else {
                ctx.edit("commands.music_general.loader.error_loading", EmoteReference.ERROR, exception.getMessage());
            }

            // Just in case.
            exception.printStackTrace();
        }

        Metrics.TRACK_EVENTS.labels("tracks_failed").inc();
        failureCount++;
    }

    private void loadSingle(AudioTrack audioTrack, boolean silent, MongoGuild dbGuild, MongoUser dbUser) {
        final var trackInfo = audioTrack.getInfo();
        final var trackScheduler = musicManager.getTrackScheduler();
        var i18nContext = new I18nContext(language);

        audioTrack.setUserData(ctx.getAuthor().getId());

        final var title = trackInfo.title;
        final var length = trackInfo.length;

        long queueLimit = MAX_QUEUE_LENGTH;
        if (dbGuild.getMusicQueueSizeLimit() != null && dbGuild.getMusicQueueSizeLimit() > 1) {
            queueLimit = dbGuild.getMusicQueueSizeLimit();
        }

        var fqSize = dbGuild.getMaxFairQueue();
        ConcurrentLinkedDeque<AudioTrack> queue = trackScheduler.getQueue();

        if (queue.size() > queueLimit && !dbUser.isPremium() && !dbGuild.isPremium()) {
            if (!silent) {
                ctx.edit("commands.music_general.loader.over_queue_limit", EmoteReference.WARNING, title, queueLimit);
            }
            return;
        }

        if (trackInfo.length > MAX_SONG_LENGTH && (!dbUser.isPremium() && !dbGuild.isPremium())) {
            ctx.edit("commands.music_general.loader.over_32_minutes",
                    EmoteReference.WARNING, title,
                    Utils.formatDuration(i18nContext, MAX_SONG_LENGTH),
                    Utils.formatDuration(i18nContext, length)
            );
            return;
        }

        // Comparing if the URLs are the same to be 100% sure they're just not spamming the same url over and over again.
        if (queue.stream().filter(track -> trackInfo.uri.equals(track.getInfo().uri)).count() > fqSize && !silent) {
            ctx.edit("commands.music_general.loader.fair_queue_limit_reached", EmoteReference.ERROR, fqSize + 1);
            return;
        }

        trackScheduler.queue(audioTrack, insertFirst);
        trackScheduler.setRequestedChannel(ctx.getChannel().getIdLong());

        if (!silent) {
            var player = db.getPlayer(ctx.getAuthor());
            var badge = APIUtils.getHushBadge(audioTrack.getIdentifier(), Utils.HushType.MUSIC);
            if (badge != null && player.addBadgeIfAbsent(badge)) {
                player.updateAllChanged();
            }

            var duration = Utils.formatDuration(i18nContext, length);
            if (length == Long.MAX_VALUE) {
                duration = "stream";
            }

            var displayTitle = MarkdownSanitizer.sanitize(title);
            ctx.editStripped("commands.music_general.loader.loaded_song", EmoteReference.CORRECT, displayTitle, duration);
        }

        Metrics.TRACK_EVENTS.labels("tracks_load").inc();
    }

    // Yes, this is repeated twice. I need the hook for the search stuff.
    @SuppressWarnings("SameParameterValue")
    private void loadSingle(InteractionHook hook, AudioTrack audioTrack, boolean silent, MongoGuild dbGuild, MongoUser dbUser) {
        final var trackInfo = audioTrack.getInfo();
        final var trackScheduler = musicManager.getTrackScheduler();
        var i18nContext = new I18nContext(language);

        audioTrack.setUserData(ctx.getAuthor().getId());

        final var title = trackInfo.title;
        final var length = trackInfo.length;

        long queueLimit = MAX_QUEUE_LENGTH;
        if (dbGuild.getMusicQueueSizeLimit() != null && dbGuild.getMusicQueueSizeLimit() > 1) {
            queueLimit = dbGuild.getMusicQueueSizeLimit();
        }

        var fqSize = dbGuild.getMaxFairQueue();
        ConcurrentLinkedDeque<AudioTrack> queue = trackScheduler.getQueue();

        if (queue.size() > queueLimit && !dbUser.isPremium() && !dbGuild.isPremium()) {
            if (!silent) {
                hook.editOriginal(i18nContext.get("commands.music_general.loader.over_queue_limit").formatted(EmoteReference.WARNING, title, queueLimit))
                        .setEmbeds()
                        .setComponents()
                        .queue();
            }
            return;
        }

        if (trackInfo.length > MAX_SONG_LENGTH && (!dbUser.isPremium() && !dbGuild.isPremium())) {
            hook.editOriginal(i18nContext.get("commands.music_general.loader.over_32_minutes").formatted(
                    EmoteReference.WARNING, title,
                    Utils.formatDuration(i18nContext, MAX_SONG_LENGTH),
                    Utils.formatDuration(i18nContext, length)
            )).setEmbeds().setComponents().queue();
            return;
        }

        // Comparing if the URLs are the same to be 100% sure they're just not spamming the same url over and over again.
        if (queue.stream().filter(track -> trackInfo.uri.equals(track.getInfo().uri)).count() > fqSize && !silent) {
            hook.editOriginal(i18nContext.get("commands.music_general.loader.fair_queue_limit_reached").formatted(EmoteReference.ERROR, fqSize + 1))
                    .setEmbeds()
                    .setComponents()
                    .queue();
            return;
        }

        trackScheduler.queue(audioTrack, insertFirst);
        trackScheduler.setRequestedChannel(ctx.getChannel().getIdLong());

        if (!silent) {
            var player = db.getPlayer(ctx.getAuthor());
            var badge = APIUtils.getHushBadge(audioTrack.getIdentifier(), Utils.HushType.MUSIC);
            if (badge != null && player.addBadgeIfAbsent(badge)) {
                player.updateAllChanged();
            }

            var duration = Utils.formatDuration(i18nContext, length);
            if (length == Long.MAX_VALUE) {
                duration = "stream";
            }

            var displayTitle = MarkdownSanitizer.sanitize(title);
            hook.editOriginal(i18nContext.get("commands.music_general.loader.loaded_song").formatted(EmoteReference.CORRECT, displayTitle, duration))
                    .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                    .setEmbeds()
                    .setComponents()
                    .queue();
        }

        Metrics.TRACK_EVENTS.labels("tracks_load").inc();
    }

    private void onSearch(AudioPlaylist playlist) {
        final var list = playlist.getTracks();

        if (!ctx.getGuild().getSelfMember().hasPermission(ctx.getChannel(), Permission.MESSAGE_EMBED_LINKS)) {
            ctx.edit("commands.music_general.missing_embed_permissions", EmoteReference.ERROR);

            // Destroy connection if there's nothing playing
            final var trackScheduler = musicManager.getTrackScheduler();
            if (trackScheduler.getQueue().isEmpty() && trackScheduler.getCurrentTrack() == null) {
                MantaroBot.getInstance().getAudioManager().resetMusicManagerFor(ctx.getGuild().getId());
            }

            return;
        }

        if (list.isEmpty()) {
            ctx.edit("commands.music_general.loader.no_matches", EmoteReference.ERROR);
            return;
        }

        DiscordUtils.selectListButtonSlash(ctx, list.subList(0, Math.min(5, list.size())),
                track -> String.format(
                        "%s**%s** (%s)",
                        EmoteReference.BLUE_SMALL_MARKER,
                        track.getInfo().title,
                        AudioCmdUtils.getDurationMinutes(track.getInfo().length)
                ), s -> new EmbedBuilder()
                        .setColor(Color.CYAN)
                        .setAuthor(language.get("commands.music_general.loader.selection_text"),
                                null,
                                ctx.getAuthor().getEffectiveAvatarUrl()
                        )
                        .setThumbnail("https://apiv2.mantaro.site/image/common/musical-note.png")
                        .setDescription(s)
                        .setFooter(language.get("commands.music_general.loader.timeout_text"),
                                ctx.getAuthor().getEffectiveAvatarUrl()
                        )
                        .build(),
                (selected, hook) -> loadSingle(hook, selected, false, db.getGuild(ctx.getGuild()), db.getUser(ctx.getMember()))
        );

        Metrics.TRACK_EVENTS.labels("tracks_search").inc();
    }
}
