/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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

package net.kodehawa.mantarobot.commands.music.utils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.audio.hooks.ConnectionListener;
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.commands.music.requester.TrackScheduler;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static net.kodehawa.mantarobot.utils.data.SimpleFileDataManager.NEWLINE_PATTERN;

public class AudioCmdUtils {
    private static final int MAX_TIMEOUTS = 3;
    private static final Throwable ERROR_TIMEOUT = new Throwable(null, null, false, false){};
    private static final Throwable ERROR_UNKNOWN = new Throwable(null, null, false, false){};

    private final static String BLOCK_INACTIVE = "\u25AC";
    private final static String BLOCK_ACTIVE = "\uD83D\uDD18";
    private static final int TOTAL_BLOCKS = 10;

    public static void closeAudioConnection(GuildMessageReceivedEvent event, AudioManager audioManager, I18nContext lang) {
        audioManager.closeAudioConnection();
        event.getChannel().sendMessageFormat(lang.get("commands.music_general.closed_connection"), EmoteReference.CORRECT).queue();
    }

    public static void embedForQueue(int page, GuildMessageReceivedEvent event, GuildMusicManager musicManager, I18nContext lang) {
        final TrackScheduler trackScheduler = musicManager.getTrackScheduler();
        final String toSend = AudioUtils.getQueueList(trackScheduler.getQueue());
        final Guild guild = event.getGuild();
        String nowPlaying = trackScheduler.getAudioPlayer().getPlayingTrack() != null ?
                "**[" + trackScheduler.getAudioPlayer().getPlayingTrack().getInfo().title
                        + "](" + trackScheduler.getAudioPlayer().getPlayingTrack().getInfo().uri +
                        ")** (" + Utils.getDurationMinutes(trackScheduler.getAudioPlayer().getPlayingTrack().getInfo().length) + ")" :
                lang.get("commands.music_general.queue.no_track_found_np");

        if(toSend.isEmpty()) {
            event.getChannel().sendMessage(new EmbedBuilder()
                    .setAuthor(String.format(lang.get("commands.music_general.queue.header"), guild.getName()), null, guild.getIconUrl())
                    .setColor(Color.CYAN).setDescription(lang.get("commands.music_general.queue.nothing_playing") + "\n\n" + lang.get("commands.music_general.queue.nothing_playing_2"))
                    .addField(lang.get("commands.music_general.queue.np"), nowPlaying, false)
                    .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png").build()).queue();
            return;
        }

        String[] lines = NEWLINE_PATTERN.split(toSend);

        if(!guild.getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION)) {
            String line = null;
            StringBuilder sb = new StringBuilder();
            int total;
            {
                int t = 0;
                int c = 0;
                for(String s : lines) {
                    if(s.length() + c + 1 > MessageEmbed.TEXT_MAX_LENGTH) {
                        t++;
                        c = 0;
                    }
                    c += s.length() + 1;
                }
                if(c > 0) t++;
                total = t;
            }
            int current = 0;
            for(String s : lines) {
                int l = s.length() + 1;
                if(l > MessageEmbed.TEXT_MAX_LENGTH)
                    throw new IllegalArgumentException("Length for one of the pages is greater than the maximum");
                if(sb.length() + l > MessageEmbed.TEXT_MAX_LENGTH) {
                    current++;
                    if(current == page) {
                        line = sb.toString();
                        break;
                    }
                    sb = new StringBuilder();
                }
                sb.append(s).append('\n');
            }
            if(sb.length() > 0 && current + 1 == page) {
                line = sb.toString();
            }
            if(line == null || page > total) {
                event.getChannel().sendMessage(new EmbedBuilder()
                        .setAuthor(String.format(lang.get("commands.music_general.queue.header"), guild.getName()), null, guild.getIconUrl())
                        .setColor(Color.CYAN).setDescription(lang.get("commands.music_general.queue.page_overflow") + "\n" + lang.get("commands.music_general.queue.page_overflow_2"))
                        .addField(lang.get("commands.music_general.queue.np"), nowPlaying, false)
                        .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png").build()).queue();
            } else {
                long length = trackScheduler.getQueue().stream().mapToLong(value -> value.getInfo().length).sum();
                EmbedBuilder builder = new EmbedBuilder()
                        .setAuthor(String.format(lang.get("commands.music_general.queue.header"), guild.getName()), null, guild.getIconUrl())
                        .setColor(Color.CYAN);

                VoiceChannel vch = guild.getSelfMember().getVoiceState().getChannel();
                builder.addField(lang.get("commands.music_general.queue.np"), nowPlaying, false)
                        .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png")
                        .addField(lang.get("commands.music_general.queue.total_queue_time"),
                                String.format("`%s`", Utils.getReadableTime(length)), true)
                        .addField(lang.get("commands.music_general.queue.total_size"),
                                String.format("`%d %s`", trackScheduler.getQueue().size(), lang.get("commands.music_general.queue.songs")), true)
                        .addField(lang.get("commands.music_general.queue.togglers"),
                                String.format("`%s / %s`", trackScheduler.getRepeatMode() == null ? "false" : trackScheduler.getRepeatMode(), String.valueOf(trackScheduler.getAudioPlayer().isPaused())), true)
                        .addField(lang.get("commands.music_general.queue.playing_in"),
                                vch == null ? lang.get("commands.music_general.queue.no_channel") : "`" + vch.getName() + "`", true)
                        .setFooter(String.format(lang.get("commands.music_general.queue.footer"), total, total == 1 ? "" : lang.get("commands.music_general.queue.multiple_pages"), page), guild.getIconUrl());
                event.getChannel().sendMessage(builder.setDescription(line).build()).queue();
            }
            return;
        }

        DiscordUtils.list(event, 30, false, MessageEmbed.TEXT_MAX_LENGTH, (p, total) -> {
            long length = trackScheduler.getQueue().stream().mapToLong(value -> value.getInfo().length).sum();
            EmbedBuilder builder = new EmbedBuilder()
                    .setAuthor(String.format(lang.get("commands.music_general.queue.header"), guild.getName()), null, guild.getIconUrl())
                    .setColor(Color.CYAN);

            VoiceChannel vch = guild.getSelfMember().getVoiceState().getChannel();
            builder.addField(lang.get("commands.music_general.queue.np"), nowPlaying, false)
                    .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png")
                    .addField(lang.get("commands.music_general.queue.total_queue_time"),
                            String.format("`%s`", Utils.getReadableTime(length)), true)
                    .addField(lang.get("commands.music_general.queue.total_size"),
                            String.format("`%d %s`", trackScheduler.getQueue().size(), lang.get("commands.music_general.queue.songs")), true)
                    .addField(lang.get("commands.music_general.queue.togglers"),
                            String.format("`%s / %s`", trackScheduler.getRepeatMode() == null ? "false" :
                                    trackScheduler.getRepeatMode(), String.valueOf(trackScheduler.getAudioPlayer().isPaused())), true)
                    .addField(lang.get("commands.music_general.queue.playing_in"),
                            vch == null ? lang.get("commands.music_general.queue.no_channel") : "`" + vch.getName() + "`", true)
                    .setFooter(String.format(lang.get("commands.music_general.queue.footer"), total, total == 1 ? "" : lang.get("commands.music_general.queue.page_react"), p), guild.getIconUrl());
            return builder;
        }, lines);
    }

    public static CompletionStage<Void> openAudioConnection(GuildMessageReceivedEvent event, AudioManager audioManager, VoiceChannel userChannel, I18nContext lang) {
        if(userChannel.getUserLimit() <= userChannel.getMembers().size() && userChannel.getUserLimit() > 0 && !event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.full_channel"), EmoteReference.ERROR).queue();
            return completedFuture(null);
        }

        try {
            return joinVoiceChannel(audioManager, userChannel)
                    .thenRun(() -> {
                        event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.success"), EmoteReference.CORRECT, userChannel.getName()).queue();
                    })
                    .exceptionally(e -> {
                        event.getChannel().sendMessageFormat(getErrorMessage(lang, e), EmoteReference.ERROR, userChannel.getName()).queue();
                        return null;
                    });
        } catch(NullPointerException e) {
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.non_existing_channel"), EmoteReference.ERROR).queue();
            //Reset custom channel.
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            dbGuild.getData().setMusicChannel(null);
            dbGuild.saveAsync();
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    public static CompletionStage<Boolean> connectToVoiceChannel(GuildMessageReceivedEvent event, I18nContext lang) {
        VoiceChannel userChannel = event.getMember().getVoiceState().getChannel();

        if(userChannel == null) {
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.user_no_vc"), EmoteReference.ERROR).queue();
            return completedFuture(false);
        }

        if(!event.getGuild().getSelfMember().hasPermission(userChannel, Permission.VOICE_CONNECT)) {
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.missing_permissions_connect"), EmoteReference.ERROR, lang.get("discord_permissions.voice_connect")).queue();
            return completedFuture(false);
        }

        if(!event.getGuild().getSelfMember().hasPermission(userChannel, Permission.VOICE_SPEAK)) {
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.missing_permission_speak"), EmoteReference.ERROR, lang.get("discord_permissions.voice_speak")).queue();
            return completedFuture(false);
        }

        VoiceChannel guildMusicChannel = null;
        if(MantaroData.db().getGuild(event.getGuild()).getData().getMusicChannel() != null) {
            guildMusicChannel = event.getGuild().getVoiceChannelById(MantaroData.db().getGuild(event.getGuild()).getData().getMusicChannel());
        }

        AudioManager audioManager = event.getGuild().getAudioManager();

        if(guildMusicChannel != null) {
            if(!userChannel.equals(guildMusicChannel)) {
                event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.channel_locked"), EmoteReference.ERROR, guildMusicChannel.getName()).queue();
                return completedFuture(false);
            }

            if(!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
                return joinVoiceChannel(audioManager, userChannel)
                        .thenRun(() -> {
                            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.success"), EmoteReference.CORRECT, userChannel.getName()).queue();
                        })
                        .thenApply(__ -> true)
                        .exceptionally(e -> {
                            event.getChannel().sendMessageFormat(getErrorMessage(lang, e), EmoteReference.ERROR, userChannel.getName()).queue();
                            return false;
                        });
            }

            return completedFuture(true);
        }

        if(audioManager.isConnected() && !audioManager.getConnectedChannel().equals(userChannel)) {
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.already_connected"), EmoteReference.WARNING, audioManager.getConnectedChannel().getName()).queue();
            return completedFuture(false);
        }

        if(audioManager.isAttemptingToConnect() && !audioManager.getQueuedAudioConnection().equals(userChannel)) {
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.attempting_to_connect"), EmoteReference.ERROR, audioManager.getQueuedAudioConnection().getName()).queue();
            return completedFuture(false);
        }

        if(!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            return openAudioConnection(event, audioManager, userChannel, lang)
                    .thenApply(__ -> true);
        }

        return completedFuture(true);
    }

    public static String getProgressBar(long now, long total) {
        int activeBlocks = (int) ((float) now / total * TOTAL_BLOCKS);
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < TOTAL_BLOCKS; i++) builder.append(activeBlocks == i ? BLOCK_ACTIVE : BLOCK_INACTIVE);
        return builder.append(BLOCK_INACTIVE).toString();
    }

    private static CompletionStage<Void> joinVoiceChannel(AudioManager manager, VoiceChannel channel) {
        JoinListener listener = new JoinListener(manager.getGuild().getIdLong());
        manager.setConnectionListener(listener);
        manager.openAudioConnection(channel);
        return listener.thenRun(() -> manager.setConnectionListener(null));
    }

    public static String getErrorMessage(I18nContext context, Throwable cause) {
        if(cause == ERROR_TIMEOUT) {
            return context.get("commands.music_general.music_error.timeout");
        }

        return context.get("commands.music_general.music_error.unknown");
    }

    private static class JoinListener extends CompletableFuture<Void> implements ConnectionListener {
        private static final Logger log = LoggerFactory.getLogger(JoinListener.class);

        private final long guildId;
        private int timeouts;

        private JoinListener(long guildId) {
            this.guildId = guildId;
        }

        @Override
        public void onPing(long ping) {

        }

        @Override
        public void onStatusChange(ConnectionStatus status) {
            if(status == ConnectionStatus.CONNECTED) {
                complete(null);
            }

            if(!status.shouldReconnect()) {
                log.error("Unexpected status found while trying to connect (guild = {}): {}", guildId, status);
                completeExceptionally(ERROR_UNKNOWN);
            }
            if(status == ConnectionStatus.ERROR_CONNECTION_TIMEOUT) {
                log.warn("Connection timed out (guild = {})", guildId);
                timeouts++;
                if(timeouts >= MAX_TIMEOUTS) {
                    log.error("Maximum amount of timeouts reached, aborting connection (guild = {})", guildId);
                    completeExceptionally(ERROR_TIMEOUT);
                }
            }
        }

        @Override
        public void onUserSpeaking(User user, boolean speaking) {

        }
    }
}
