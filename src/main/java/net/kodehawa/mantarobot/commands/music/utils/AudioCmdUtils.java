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
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.awt.*;

import static net.kodehawa.mantarobot.utils.data.SimpleFileDataManager.NEWLINE_PATTERN;

public class AudioCmdUtils {

    private final static String BLOCK_INACTIVE = "\u25AC";
    private final static String BLOCK_ACTIVE = "\uD83D\uDD18";
    private static final int TOTAL_BLOCKS = 10;

    public static void closeAudioConnection(GuildMessageReceivedEvent event, AudioManager audioManager, I18nContext lang) {
        audioManager.closeAudioConnection();
        event.getChannel().sendMessageFormat(lang.get("commands.music_general.closed_connection"), EmoteReference.CORRECT).queue();
    }

    public static void embedForQueue(int page, GuildMessageReceivedEvent event, GuildMusicManager musicManager, I18nContext lang) {
        String toSend = AudioUtils.getQueueList(musicManager.getTrackScheduler().getQueue());
        Guild guild = event.getGuild();
        String nowPlaying = musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack() != null ?
                "**[" + musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().getInfo().title
                        + "](" + musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().getInfo().uri +
                        ")** (" + Utils.getDurationMinutes(musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().getInfo().length) + ")" :
                "Nothing or title/duration not found";

        if(toSend.isEmpty()) {
            event.getChannel().sendMessage(new EmbedBuilder()
                    .setAuthor(String.format("Queue for server %s", guild.getName()), null, guild.getIconUrl())
                    .setColor(Color.CYAN).setDescription("Nothing here, just dust. Why don't you queue some songs?\n" +
                            "If you think there are songs here but they don't appear, try using `~>queue 1`.\n\n" +
                            "**If there is a song playing and you didn't add more songs, then there is actually just dust here. You can queue more songs as you desire!**")
                    .addField("Currently playing", nowPlaying, false)
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
                        .setAuthor(String.format("Queue for server %s", guild.getName()), null, guild.getIconUrl())
                        .setColor(Color.CYAN).setDescription("Nothing here, just dust. Why don't you go back some pages?\n" +
                                "If you think there are songs here but they don't appear, try using `~>queue 1`.")
                        .addField("Currently playing", nowPlaying, false)
                        .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png").build()).queue();
            } else {
                long length = musicManager.getTrackScheduler().getQueue().stream().mapToLong(value -> value.getInfo().length).sum();
                EmbedBuilder builder = new EmbedBuilder()
                        .setAuthor(String.format("Queue for server %s", guild.getName()), null, guild.getIconUrl())
                        .setColor(Color.CYAN);

                VoiceChannel vch = guild.getSelfMember().getVoiceState().getChannel();
                builder.addField("Currently playing", nowPlaying, false)
                        .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png")
                        .addField("Total queue time", "`" + Utils.getReadableTime(length) + "`", true)
                        .addField("Total queue size", "`" + musicManager.getTrackScheduler().getQueue().size() + " songs`", true)
                        .addField("Repeat / Pause", "`" + (musicManager.getTrackScheduler().getRepeatMode() == null ? "false" : musicManager.getTrackScheduler().getRepeatMode())
                                + " / " + String.valueOf(musicManager.getTrackScheduler().getAudioPlayer().isPaused()) + "`", true)
                        .addField("Playing in", vch == null ? "No channel :<" : "`" + vch.getName() + "`", true)
                        .setFooter(String.format("Total pages: %d%s. Currently in page %d", total, total == 1 ? "" : " -> Use ~>queue <page> to change pages", page), guild.getIconUrl());
                event.getChannel().sendMessage(builder.setDescription(line).build()).queue();
            }
            return;
        }

        DiscordUtils.list(event, 30, false, (p, total) -> {
            long length = musicManager.getTrackScheduler().getQueue().stream().mapToLong(value -> value.getInfo().length).sum();
            EmbedBuilder builder = new EmbedBuilder()
                    .setAuthor("Queue for server " + guild.getName(), null, guild.getIconUrl())
                    .setColor(Color.CYAN);

            VoiceChannel vch = guild.getSelfMember().getVoiceState().getChannel();
            builder.addField("Currently playing", nowPlaying, false)
                    .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png")
                    .addField("Total queue time", "`" + Utils.getReadableTime(length) + "`", true)
                    .addField("Total queue size", "`" + musicManager.getTrackScheduler().getQueue().size() + " songs`", true)
                    .addField("Repeat / Pause", "`" + (musicManager.getTrackScheduler().getRepeatMode() == null ? "false" : musicManager.getTrackScheduler().getRepeatMode())
                            + " / " + String.valueOf(musicManager.getTrackScheduler().getAudioPlayer().isPaused()) + "`", true)
                    .addField("Playing in", vch == null ? "No channel :<" : "`" + vch.getName() + "`", true)
                    .setFooter(String.format("Total pages: %d%s. Currently in page %d", total, total == 1 ? "" : " -> React to change pages", p), guild.getIconUrl());
            return builder;
        }, lines);
    }

    public static void openAudioConnection(GuildMessageReceivedEvent event, AudioManager audioManager, VoiceChannel userChannel, I18nContext lang) {
        if(userChannel.getUserLimit() <= userChannel.getMembers().size() && userChannel.getUserLimit() > 0 && !event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            event.getChannel().sendMessage(String.format("%sI can't connect to that channel because it is full!", EmoteReference.ERROR)).queue();
            return;
        }

        try {
            audioManager.openAudioConnection(userChannel);
            event.getChannel().sendMessage(String.format("%sConnected to channel **%s**!", EmoteReference.CORRECT, userChannel.getName())).queue();
        } catch(NullPointerException e) {
            event.getChannel().sendMessage(String.format("%sWe received a non-existent channel as response. If you set a voice channel and then deleted it, that might be the cause.\n We reset your music channel for you, try to play the music again.", EmoteReference.ERROR)).queue();
            MantaroData.db().getGuild(event.getGuild()).getData().setMusicChannel(null);
            MantaroData.db().getGuild(event.getGuild()).saveAsync();
        }
    }

    public static boolean connectToVoiceChannel(GuildMessageReceivedEvent event, I18nContext lang) {
        VoiceChannel userChannel = event.getMember().getVoiceState().getChannel();

        if(userChannel == null) {
            event.getChannel().sendMessage("\u274C **Please join a voice channel!**").queue();
            return false;
        }

        if(!event.getGuild().getSelfMember().hasPermission(userChannel, Permission.VOICE_CONNECT)) {
            event.getChannel().sendMessage(":heavy_multiplication_x: I cannot connect to this channel due to the lack of permission (%s).").queue();
            return false;
        }

        if(!event.getGuild().getSelfMember().hasPermission(userChannel, Permission.VOICE_SPEAK)) {
            event.getChannel().sendMessage(":heavy_multiplication_x: I cannot speak in this channel due to the lack of permission (%s).").queue();
            return false;
        }

        VoiceChannel guildMusicChannel = null;
        if(MantaroData.db().getGuild(event.getGuild()).getData().getMusicChannel() != null) {
            guildMusicChannel = event.getGuild().getVoiceChannelById(MantaroData.db().getGuild(event.getGuild()).getData().getMusicChannel());
        }

        AudioManager audioManager = event.getGuild().getAudioManager();

        if(guildMusicChannel != null) {
            if(!userChannel.equals(guildMusicChannel)) {
                event.getChannel().sendMessage(String.format("%sI can only play music on channel **%s**!", EmoteReference.ERROR, guildMusicChannel.getName())).queue();
                return false;
            }

            if(!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
                audioManager.openAudioConnection(userChannel);
                event.getChannel().sendMessageFormat(lang.get("commands.music_general.connect.success"), EmoteReference.CORRECT, userChannel.getName()).queue();
            }

            return true;
        }

        if(audioManager.isConnected() && !audioManager.getConnectedChannel().equals(userChannel)) {
            event.getChannel().sendMessage(String.format("%sI'm already connected on channel **%s**! (Use the `move` command to move me to another channel)", EmoteReference.WARNING, audioManager.getConnectedChannel().getName())).queue();
            return false;
        }

        if(audioManager.isAttemptingToConnect() && !audioManager.getQueuedAudioConnection().equals(userChannel)) {
            event.getChannel().sendMessage(String.format("%sI'm already trying to connect to channel **%s**! (Use the `move` command to move me to another channel)", EmoteReference.ERROR, audioManager.getQueuedAudioConnection().getName())).queue();
            return false;
        }

        if(!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            openAudioConnection(event, audioManager, userChannel, lang);
        }

        return true;
    }

    public static String getProgressBar(long now, long total) {
        int activeBlocks = (int) ((float) now / total * TOTAL_BLOCKS);
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < TOTAL_BLOCKS; i++) builder.append(activeBlocks == i ? BLOCK_ACTIVE : BLOCK_INACTIVE);
        return builder.append(BLOCK_INACTIVE).toString();
    }
}
