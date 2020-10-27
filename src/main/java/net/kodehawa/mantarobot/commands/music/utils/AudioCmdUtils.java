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

import lavalink.client.io.Link;
import lavalink.client.io.jda.JdaLink;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.IntIntObjectFunction;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class AudioCmdUtils {
    private static final Logger log = LoggerFactory.getLogger(AudioCmdUtils.class);

    public static void embedForQueue(GuildMessageReceivedEvent event, GuildMusicManager musicManager, I18nContext lang) {
        final var trackScheduler = musicManager.getTrackScheduler();
        final var toSend = AudioUtils.getQueueList(trackScheduler.getQueue(), musicManager);
        final var guild = event.getGuild();
        final var musicPlayer = trackScheduler.getMusicPlayer();
        final var playingTrack = musicPlayer.getPlayingTrack();

        // This used to be a ternary, but it wasn't too readable, to say the least.
        var nowPlaying = "";
        if (playingTrack == null) {
            nowPlaying = lang.get("commands.music_general.queue.no_track_found_np");
        } else {
            nowPlaying = String.format("**[%s](%s)** (%s)",
                    playingTrack.getInfo().title,
                    playingTrack.getInfo().uri,
                    getDurationMinutes(playingTrack.getInfo().length)
            );
        }

        if (toSend.isEmpty()) {
            event.getChannel().sendMessage(new EmbedBuilder()
                    .setAuthor(String.format(lang.get("commands.music_general.queue.header"), guild.getName()), null, guild.getIconUrl())
                    .setColor(Color.CYAN).setDescription(lang.get("commands.music_general.queue.nothing_playing") + "\n\n"
                            + lang.get("commands.music_general.queue.nothing_playing_2"))
                    .addField(lang.get("commands.music_general.queue.np"), nowPlaying, false)
                    .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png").build()).queue();
            return;
        }

        var length = trackScheduler.getQueue().stream().mapToLong(value -> value.getInfo().length).sum();
        var voiceChannel = guild.getSelfMember().getVoiceState().getChannel();
        var builder = new EmbedBuilder()
                .setAuthor(String.format(lang.get("commands.music_general.queue.header"), guild.getName()), null, guild.getIconUrl())
                .setColor(Color.CYAN);

        // error: local variables referenced from a lambda expression must be final or effectively final
        // sob
        final var np = nowPlaying;
        IntIntObjectFunction<EmbedBuilder> supplier = (p, total) ->
                builder.addField(lang.get("commands.music_general.queue.header_field"),
                        lang.get("commands.music_general.queue.header_instructions"), false
                ).addField(
                        lang.get("commands.music_general.queue.np"), np, false
                ).addField(
                        lang.get("commands.music_general.queue.total_queue_time"),
                        String.format("`%s`", Utils.formatDuration(length)), false
                ).addField(
                        lang.get("commands.music_general.queue.total_size"),
                        String.format("`%d %s`", trackScheduler.getQueue().size(), lang.get("commands.music_general.queue.songs")),
                        true
                ).addField(
                        lang.get("commands.music_general.queue.togglers"),
                        String.format("`%s / %s`", trackScheduler.getRepeatMode() == null ? "false" :
                                trackScheduler.getRepeatMode(), musicPlayer.isPaused()),
                        true
                ).addField(lang.get("commands.music_general.queue.playing_in"),
                        voiceChannel == null ? lang.get("commands.music_general.queue.no_channel") : "`" + voiceChannel.getName() + "`",
                        true
                ).setFooter(String.format("Total Pages: %s | Current: %s", total, p), event.getAuthor().getEffectiveAvatarUrl());

        var split = DiscordUtils.divideString(MessageEmbed.TEXT_MAX_LENGTH, toSend);
        boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);
        if (hasReactionPerms) {
            DiscordUtils.list(event, 150, false, supplier, split);
        } else {
            DiscordUtils.listText(event, 150, false, supplier, split);
        }
    }

    public static CompletionStage<Void> openAudioConnection(GuildMessageReceivedEvent event, JdaLink link, VoiceChannel userChannel, I18nContext lang) {
        if (userChannel.getUserLimit() <= userChannel.getMembers().size() && userChannel.getUserLimit() > 0 &&
                !event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.full_channel"), EmoteReference.ERROR).queue();
            return completedFuture(null);
        }

        try {
            //This used to be a CompletableFuture that went through a listener which is now useless bc im 99% sure you can't listen to the connection status on LL.
            joinVoiceChannel(link, userChannel);
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.success"), EmoteReference.CORRECT, userChannel.getName()).queue();
            return completedFuture(null);
        } catch (NullPointerException e) {
            e.printStackTrace();
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.non_existent_channel"), EmoteReference.ERROR).queue();

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
        var voiceChannel = event.getMember().getVoiceState().getChannel();
        var guild = event.getGuild();
        var textChannel = event.getChannel();

        //I can't see you in any VC here?
        if (voiceChannel == null) {
            textChannel.sendMessageFormat(lang.get("commands.music_general.connect.user_no_vc"), EmoteReference.ERROR).queue();
            return completedFuture(false);
        }

        //Can't connect to this channel
        if (!guild.getSelfMember().hasPermission(voiceChannel, Permission.VOICE_CONNECT)) {
            textChannel.sendMessageFormat(lang.get("commands.music_general.connect.missing_permissions_connect"), EmoteReference.ERROR,
                    lang.get("discord_permissions.voice_connect")).queue();
            return completedFuture(false);
        }

        //Can't speak on this channel
        if (!guild.getSelfMember().hasPermission(voiceChannel, Permission.VOICE_SPEAK)) {
            textChannel.sendMessageFormat(lang.get("commands.music_general.connect.missing_permission_speak"), EmoteReference.ERROR,
                    lang.get("discord_permissions.voice_speak")).queue();
            return completedFuture(false);
        }

        //Set the custom guild music channel from the db value
        VoiceChannel guildMusicChannel = null;
        if (MantaroData.db().getGuild(guild).getData().getMusicChannel() != null) {
            guildMusicChannel = guild.getVoiceChannelById(MantaroData.db().getGuild(guild).getData().getMusicChannel());
        }

        //This is where we call LL.
        var link = MantaroBot.getInstance().getAudioManager().getMusicManager(guild).getLavaLink();

        //Cursed lavalink issues tracker.
        var cursed = false;
        if (guildMusicChannel != null) {
            //If the channel is not the set one, reject this connect.
            if (!voiceChannel.equals(guildMusicChannel)) {
                textChannel.sendMessageFormat(lang.get("commands.music_general.connect.channel_locked"), EmoteReference.ERROR, guildMusicChannel.getName()).queue();
                return completedFuture(false);
            }

            //If the link is not currently connected or connecting, accept connection and call openAudioConnection
            if (link.getState() != Link.State.CONNECTED && link.getState() != Link.State.CONNECTING) {
                log.debug("Connected to channel {}." +
                        " Reason: Link is not CONNECTED or CONNECTING and we requested a connection from connectToVoiceChannel (custom music channel)",
                        voiceChannel.getId()
                );

                return openAudioConnection(event, link, voiceChannel, lang).thenApply(__ -> true);
            }

            //Nothing to connect to, but pass true so we can load the song (for example, it's already connected)
            return completedFuture(true);
        }

        //Assume last channel it's the one it was attempting to connect to? (on the one below this too)
        //If the link is CONNECTED and the lastChannel is not the one it's already connected to, reject connection
        if (link.getState() == Link.State.CONNECTED && link.getLastChannel() != null && !link.getLastChannel().equals(voiceChannel.getId())) {
            VoiceChannel vc = guild.getVoiceChannelById(link.getLastChannel());

            //Workaround for a bug in lavalink that gives us Link.State.CONNECTED and a channel that doesn't exist anymore.
            //This is a little cursed.
            if (vc != null) {
                textChannel.sendMessageFormat(lang.get("commands.music_general.connect.already_connected"), EmoteReference.WARNING, vc.getName()).queue();
                return completedFuture(false);
            } else {
                cursed = true;
            }
        }

        //If the link is CONNECTING and the lastChannel is not the one it's already connected to, reject connection
        if (link.getState() == Link.State.CONNECTING && link.getLastChannel() != null && !link.getLastChannel().equals(voiceChannel.getId())) {
            VoiceChannel vc = guild.getVoiceChannelById(link.getLastChannel());

            //Workaround for a bug in lavalink that gives us Link.State.CONNECTING and a channel that doesn't exist anymore.
            //This is a little cursed.
            if (vc != null) {
                textChannel.sendMessageFormat(lang.get("commands.music_general.connect.attempting_to_connect"), EmoteReference.ERROR, vc.getName()).queue();
                return completedFuture(false);
            } else {
                cursed = true;
            }
        }

        //If the link is not currently connected or connecting, accept connection and call openAudioConnection
        if ((link.getState() != Link.State.CONNECTED && link.getState() != Link.State.CONNECTING) || cursed) {
            log.debug("Connected to voice channel {}. " +
                    "Reason: Link is not CONNECTED or CONNECTING and we requested a connection from connectToVoiceChannel",
                    voiceChannel.getId()
            );

            if (cursed)
                log.debug("We seemed to hit a Lavalink/JDA bug? Null voice channel, but {} state.", link.getState());

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
                TimeUnit.MILLISECONDS.toMinutes(length),
                TimeUnit.MILLISECONDS.toSeconds(length) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(length))
        );
    }
}
