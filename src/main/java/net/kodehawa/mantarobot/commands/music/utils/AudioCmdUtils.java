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

import lavalink.client.io.Link;
import lavalink.client.io.jda.JdaLink;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.commands.music.requester.TrackScheduler;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static net.kodehawa.mantarobot.utils.data.SimpleFileDataManager.NEWLINE_PATTERN;

public class AudioCmdUtils {
    private final static String BLOCK_INACTIVE = "\u25AC";
    private final static String BLOCK_ACTIVE = "\uD83D\uDD18";
    private static final int TOTAL_BLOCKS = 10;

    public static void embedForQueue(int page, GuildMessageReceivedEvent event, GuildMusicManager musicManager, I18nContext lang) {
        final TrackScheduler trackScheduler = musicManager.getTrackScheduler();
        final String toSend = AudioUtils.getQueueList(trackScheduler.getQueue());
        final Guild guild = event.getGuild();
        String nowPlaying = trackScheduler.getMusicPlayer().getPlayingTrack() != null ?
                "**[" + trackScheduler.getMusicPlayer().getPlayingTrack().getInfo().title
                        + "](" + trackScheduler.getMusicPlayer().getPlayingTrack().getInfo().uri +
                        ")** (" + Utils.getDurationMinutes(trackScheduler.getMusicPlayer().getPlayingTrack().getInfo().length) + ")" :
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
                                String.format("`%s / %s`", trackScheduler.getRepeatMode() == null ? "false" : trackScheduler.getRepeatMode(), String.valueOf(trackScheduler.getMusicPlayer().isPaused())), true)
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
                                    trackScheduler.getRepeatMode(), String.valueOf(trackScheduler.getMusicPlayer().isPaused())), true)
                    .addField(lang.get("commands.music_general.queue.playing_in"),
                            vch == null ? lang.get("commands.music_general.queue.no_channel") : "`" + vch.getName() + "`", true)
                    .setFooter(String.format(lang.get("commands.music_general.queue.footer"), total, total == 1 ? "" : lang.get("commands.music_general.queue.page_react"), p), guild.getIconUrl());
            return builder;
        }, lines);
    }

    public static CompletionStage<Void> openAudioConnection(GuildMessageReceivedEvent event, JdaLink link, VoiceChannel userChannel, I18nContext lang) {
        if(userChannel.getUserLimit() <= userChannel.getMembers().size() && userChannel.getUserLimit() > 0 && !event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.full_channel"), EmoteReference.ERROR).queue();
            return completedFuture(null);
        }

        try {
            //This used to be a CompletableFuture that went through a listener which is now useless bc im 99% sure you can't listen to the connection status on LL.
            joinVoiceChannel(link, userChannel);
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.success"), EmoteReference.CORRECT, userChannel.getName()).queue();
            return completedFuture(null);
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

        JdaLink link = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getLavaLink();
        if(guildMusicChannel != null) {
            if(!userChannel.equals(guildMusicChannel)) {
                event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.channel_locked"), EmoteReference.ERROR, guildMusicChannel.getName()).queue();
                return completedFuture(false);
            }

            if(link.getState() != Link.State.CONNECTED && link.getState() != Link.State.CONNECTING) {
                return openAudioConnection(event, link, userChannel, lang)
                        .thenApply(__ -> true);
            }

            return completedFuture(true);
        }

        if(link.getState() == Link.State.CONNECTED && link.getLastChannel() != null && !link.getLastChannel().equals(userChannel.getId())) {
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.already_connected"), EmoteReference.WARNING, event.getGuild().getVoiceChannelById(link.getLastChannel()).getName()).queue();
            return completedFuture(false);
        }

        //Assume last channel it's the one it was attempting to connect to?
        if(link.getState() == Link.State.CONNECTING && link.getLastChannel() != null && !link.getLastChannel().equals(userChannel.getId())) {
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.attempting_to_connect"), EmoteReference.ERROR, event.getGuild().getVoiceChannelById(link.getLastChannel()).getName()).queue();
            return completedFuture(false);
        }

        if(link.getState() != Link.State.CONNECTED && link.getState() != Link.State.CONNECTING) {
            return openAudioConnection(event, link, userChannel, lang)
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

    private static void joinVoiceChannel(JdaLink manager, VoiceChannel channel) {
        manager.connect(channel);
    }
}
