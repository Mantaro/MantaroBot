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

package net.kodehawa.mantarobot.commands.music.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.io.Link;
import lavalink.client.io.jda.JdaLink;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.IntIntObjectFunction;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedDeque;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class AudioCmdUtils {
    private static final Logger log = LoggerFactory.getLogger(AudioCmdUtils.class);
    private static final String icon = "https://i.imgur.com/FWKIR7N.png";

    public static void embedForQueue(GuildMessageReceivedEvent event, GuildMusicManager musicManager, I18nContext lang) {
        final var trackScheduler = musicManager.getTrackScheduler();
        final var toSend = getQueueList(trackScheduler.getQueue());
        final var guild = event.getGuild();
        final var musicPlayer = trackScheduler.getMusicPlayer();
        final var playingTrack = musicPlayer.getPlayingTrack();
        final var selfMember = guild.getSelfMember();
        final var channel = event.getChannel();

        // This used to be a ternary, but it wasn't too readable, to say the least.
        var nowPlaying = "";
        if (playingTrack == null) {
            nowPlaying = lang.get("commands.music_general.queue.no_track_found_np");
        } else {
            Member dj = null;
            if (playingTrack.getUserData() != null) {
                try {
                    dj = guild.retrieveMemberById(String.valueOf(playingTrack.getUserData()), false).complete();
                } catch (Exception ignored) { }
            }

            nowPlaying = String.format("**[%s](%s)** (%s)\n%s",
                    playingTrack.getInfo().title,
                    playingTrack.getInfo().uri,
                    getDurationMinutes(playingTrack.getInfo().length),
                    dj != null ? lang.get("commands.music_general.queue.dj_np") + dj.getUser().getAsTag() : ""
            );
        }

        if (toSend.isEmpty()) {
            channel.sendMessage(new EmbedBuilder()
                    .setAuthor(String.format(lang.get("commands.music_general.queue.header"), guild.getName()),
                            null, guild.getIconUrl())
                    .setColor(Color.CYAN)
                    .setDescription(lang.get("commands.music_general.queue.nothing_playing") +
                            "\n\n" + lang.get("commands.music_general.queue.nothing_playing_2"))
                    .addField(lang.get("commands.music_general.queue.np"), nowPlaying, false)
                    .setThumbnail(icon).build()
            ).queue();

            return;
        }

        var length = trackScheduler.getQueue().stream().mapToLong(value -> value.getInfo().length).sum();
        var voiceChannel = selfMember.getVoiceState().getChannel();
        var builder = new EmbedBuilder()
                .setAuthor(String.format(lang.get("commands.music_general.queue.header"),
                        guild.getName()), null, guild.getIconUrl())
                .setColor(Color.CYAN);

        // error: local variables referenced from a lambda expression must be final or effectively final
        // sob
        final var np = nowPlaying;
        final var hasReactionPerms = selfMember.hasPermission(channel, Permission.MESSAGE_ADD_REACTION);

        IntIntObjectFunction<EmbedBuilder> supplier = (p, total) ->{
            // Cursed, but should work?
            // Fields were getting duplicated since the supplier was called everytime
            // obviously, but we need a clean field state here.
            // So just reset it.
            builder.clearFields();

            // Instructions in case there's no reaction perms.
            if (!hasReactionPerms) {
                builder.addField(lang.get("commands.music_general.queue.header_field"),
                        lang.get("commands.music_general.queue.header_noreact"),
                        false
                );
            }

            // Build the queue embed.
            // Description is then added on DiscordUtils.list/listText, as we have to
            // split it.
            return builder.setThumbnail(icon)
                    .addField(lang.get("commands.music_general.queue.np"), np, false)
                    .addField(lang.get("commands.music_general.queue.total_queue_time"),
                            Utils.formatDuration(length),
                            false
                    )
                    .addField(lang.get("commands.music_general.queue.total_size"),
                            String.format("%d %s",
                                    trackScheduler.getQueue().size(), lang.get("commands.music_general.queue.songs")
                            ),
                            true
                    )
                    .addField(lang.get("commands.music_general.queue.togglers"),
                        String.format("`%s / %s`", trackScheduler.getRepeatMode() == null ? "false" :
                             trackScheduler.getRepeatMode(), musicPlayer.isPaused()),
                            true
                    )
                    .addField(lang.get("commands.music_general.queue.playing_in"),
                            voiceChannel == null ? lang.get("commands.music_general.queue.no_channel") : voiceChannel.getName(),
                            true
                    )
                    .setFooter(String.format("Total Pages: %s | Current: %s", total, p),
                            event.getAuthor().getEffectiveAvatarUrl());
        };

        // Too long otherwise, so substract 800 from TEXT_MAX_LENGTH
        var split = DiscordUtils.divideString(MessageEmbed.TEXT_MAX_LENGTH - 800, toSend);
        if (hasReactionPerms) {
            DiscordUtils.list(event, 150, false, supplier, split);
        } else {
            DiscordUtils.listText(event, 150, false, supplier, split);
        }
    }

    public static CompletionStage<Void> openAudioConnection(GuildMessageReceivedEvent event, JdaLink link,
                                                            VoiceChannel userChannel, I18nContext lang) {
        final var textChannel = event.getChannel();
        final var userChannelMembers = userChannel.getMembers();
        Member selfMember = event.getGuild().getSelfMember();

        if (userChannel.getUserLimit() <= userChannelMembers.size()
                && userChannel.getUserLimit() > 0 && !selfMember.hasPermission(Permission.MANAGE_CHANNEL)) {
            textChannel.sendMessageFormat(
                    lang.get("commands.music_general.connect.full_channel"),
                    EmoteReference.ERROR
            ).queue();

            return completedFuture(null);
        }

        try {
            // This used to be a CompletableFuture that went through a listener
            // which is now useless bc im 99% sure you can't listen to the connection status on LL.
            joinVoiceChannel(link, userChannel);
            textChannel.sendMessageFormat(
                    lang.get("commands.music_general.connect.success"),
                    EmoteReference.CORRECT, userChannel.getName()
            ).queue();

            return completedFuture(null);
        } catch (NullPointerException e) {
            e.printStackTrace();
            textChannel.sendMessageFormat(
                    lang.get("commands.music_general.connect.non_existent_channel"),
                    EmoteReference.ERROR
            ).queue();

            //Reset custom channel.
            var dbGuild = MantaroData.db().getGuild(event.getGuild());
            dbGuild.getData().setMusicChannel(null);
            dbGuild.saveAsync();

            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    public static CompletionStage<Boolean> connectToVoiceChannel(GuildMessageReceivedEvent event, I18nContext lang) {
        final var voiceChannel = event.getMember().getVoiceState().getChannel();
        final var guild = event.getGuild();
        final var textChannel = event.getChannel();
        final var selfMember = guild.getSelfMember();
        final var guildData = MantaroData.db().getGuild(guild).getData();

        //I can't see you in any VC here?
        if (voiceChannel == null) {
            textChannel.sendMessageFormat(
                    lang.get("commands.music_general.connect.user_no_vc"),
                    EmoteReference.ERROR
            ).queue();

            return completedFuture(false);
        }

        //Can't connect to this channel
        if (!selfMember.hasPermission(voiceChannel, Permission.VOICE_CONNECT)) {
            textChannel.sendMessageFormat(
                    lang.get("commands.music_general.connect.missing_permissions_connect"),
                    EmoteReference.ERROR, lang.get("discord_permissions.voice_connect")
            ).queue();

            return completedFuture(false);
        }

        //Can't speak on this channel
        if (!selfMember.hasPermission(voiceChannel, Permission.VOICE_SPEAK)) {
            textChannel.sendMessageFormat(
                    lang.get("commands.music_general.connect.missing_permission_speak"),
                    EmoteReference.ERROR, lang.get("discord_permissions.voice_speak")
            ).queue();

            return completedFuture(false);
        }

        //Set the custom guild music channel from the db value
        VoiceChannel guildMusicChannel = null;
        if (guildData.getMusicChannel() != null) {
            guildMusicChannel = guild.getVoiceChannelById(guildData.getMusicChannel());
        }

        //This is where we call LL.
        final var link = MantaroBot.getInstance().getAudioManager().getMusicManager(guild).getLavaLink();
        final var lastChannel = link.getLastChannel();
        final var linkState = link.getState();

        //Cursed lavalink issues tracker.
        var cursed = false;
        if (guildMusicChannel != null) {
            //If the channel is not the set one, reject this connect.
            if (!voiceChannel.equals(guildMusicChannel)) {
                textChannel.sendMessageFormat(
                        lang.get("commands.music_general.connect.channel_locked"),
                        EmoteReference.ERROR, guildMusicChannel.getName()
                ).queue();

                return completedFuture(false);
            }

            //If the link is not currently connected or connecting, accept connection and call openAudioConnection
            if (linkState != Link.State.CONNECTED && linkState != Link.State.CONNECTING) {
                log.debug(
                        "Connected to channel {}. Reason: Link is not CONNECTED or CONNECTING " +
                        "and we requested a connection from connectToVoiceChannel (custom music channel)",
                        voiceChannel.getId()
                );

                return openAudioConnection(event, link, voiceChannel, lang)
                        .thenApply(__ -> true);
            }

            //Nothing to connect to, but pass true so we can load the song (for example, it's already connected)
            return completedFuture(true);
        }

        //Assume last channel it's the one it was attempting to connect to? (on the one below this too)
        //If the link is CONNECTED and the lastChannel is not the one it's already connected to, reject connection
        if (linkState == Link.State.CONNECTED && lastChannel != null && !lastChannel.equals(voiceChannel.getId())) {
            var vc = guild.getVoiceChannelById(lastChannel);

            //Workaround for a bug in lavalink that gives us Link.State.CONNECTED and a channel that doesn't exist anymore.
            //This is a little cursed.
            if (vc != null) {
                textChannel.sendMessageFormat(
                        lang.get("commands.music_general.connect.already_connected"),
                        EmoteReference.WARNING, vc.getName()
                ).queue();

                return completedFuture(false);
            } else {
                cursed = true;
            }
        }

        //If the link is CONNECTING and the lastChannel is not the one it's already connected to, reject connection
        if (linkState == Link.State.CONNECTING && lastChannel != null && !lastChannel.equals(voiceChannel.getId())) {
            var vc = guild.getVoiceChannelById(lastChannel);

            //Workaround for a bug in lavalink that gives us Link.State.CONNECTING and a channel that doesn't exist anymore.
            //This is a little cursed.
            if (vc != null) {
                textChannel.sendMessageFormat(
                        lang.get("commands.music_general.connect.attempting_to_connect"),
                        EmoteReference.ERROR, vc.getName()
                ).queue();

                return completedFuture(false);
            } else {
                cursed = true;
            }
        }

        //If the link is not currently connected or connecting, accept connection and call openAudioConnection
        if ((linkState != Link.State.CONNECTED && linkState != Link.State.CONNECTING) || cursed) {
            log.debug("Connected to voice channel {}. " +
                    "Reason: Link is not CONNECTED or CONNECTING and we requested a connection from connectToVoiceChannel",
                    voiceChannel.getId()
            );

            if (cursed) {
                log.debug("We seemed to hit a Lavalink/JDA bug? Null voice channel, but {} state.", linkState);
            }

            return openAudioConnection(event, link, voiceChannel, lang).thenApply(__ -> true);
        }

        //Nothing to connect to, but pass true so we can load the song (for example, it's already connected)
        return completedFuture(true);
    }

    private static void joinVoiceChannel(JdaLink manager, VoiceChannel channel) {
        manager.connect(channel);
    }

    public static String getDurationMinutes(long length) {
        return String.format("%d:%02d",
                MILLISECONDS.toMinutes(length),
                MILLISECONDS.toSeconds(length) - MINUTES.toSeconds(MILLISECONDS.toMinutes(length))
        );
    }

    public static String getQueueList(ConcurrentLinkedDeque<AudioTrack> queue) {
        var sb = new StringBuilder();
        var num = 1;

        for (var audioTrack : queue) {
            var aDuration = audioTrack.getDuration();

            var duration = String.format("%02d:%02d",
                    MILLISECONDS.toMinutes(aDuration),
                    MILLISECONDS.toSeconds(aDuration) - MINUTES.toSeconds(MILLISECONDS.toMinutes(aDuration))
            );

            sb.append("""
                    %s**%,d.** [%s] **[%s](%s)**
                    """.formatted(EmoteReference.BLUE_SMALL_MARKER,
                    num, duration,
                    formatTitle(audioTrack.getInfo().title),
                    audioTrack.getInfo().uri)
            );

            num++;
        }
        return sb.toString();
    }

    private static String formatTitle(String title) {
        // Sanitizing markdown doesn't remove [ and ], and that breaks queue.
        return MarkdownSanitizer.escape(StringUtils.limit(title, 33))
                .replace("[", "")
                .replace("]", "")
                .strip();
    }
}
