package net.kodehawa.mantarobot.commands.music.utils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
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

	public static void closeAudioConnection(GuildMessageReceivedEvent event, AudioManager audioManager) {
		audioManager.closeAudioConnection();
		event.getChannel().sendMessage(EmoteReference.CORRECT + "Closed audio connection.").queue();
	}

	public static void embedForQueue(int page, GuildMessageReceivedEvent event, GuildMusicManager musicManager) {
		String toSend = AudioUtils.getQueueList(musicManager.getTrackScheduler().getQueue());
        Guild guild = event.getGuild();

		if(toSend.isEmpty()) {
            event.getChannel().sendMessage(new EmbedBuilder()
                    .setAuthor("Queue for server " + guild.getName(), null, guild.getIconUrl())
                    .setColor(Color.CYAN).setDescription("Nothing here, just dust. Why don't you queue some songs?")
                    .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png").build()).queue();
            return;
        }

		String[] lines = NEWLINE_PATTERN.split(toSend);

		if(!guild.getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION)) {
            String line = null;
            StringBuilder sb = new StringBuilder();
            int total; {
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
                int l = s.length()+1;
                if(l > MessageEmbed.TEXT_MAX_LENGTH) throw new IllegalArgumentException("Length for one of the pages is greater than the maximum");
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
                        .setAuthor("Queue for server " + guild.getName(), null, guild.getIconUrl())
                        .setColor(Color.CYAN).setDescription("Nothing here, just dust. Why don't you go back some pages?")
                        .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png").build()).queue();
            } else {
                long length = musicManager.getTrackScheduler().getQueue().stream().mapToLong(value -> value.getInfo().length).sum();
                EmbedBuilder builder = new EmbedBuilder()
                        .setAuthor("Queue for server " + guild.getName(), null, guild.getIconUrl())
                        .setColor(Color.CYAN);

                String nowPlaying = musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack() != null ?
                        "**[" + musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().getInfo().title
                                + "](" + musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().getInfo().uri +
                                ")** (" + Utils.getDurationMinutes(musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().getInfo().length) + ")" :
                        "Nothing or title/duration not found";
                VoiceChannel vch = guild.getSelfMember().getVoiceState().getChannel();
                builder
                        .addField("Currently playing", nowPlaying, false)
                        .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png")
                        .addField("Total queue time", "`" + Utils.getReadableTime(length) + "`", true)
                        .addField("Total queue size", "`" + musicManager.getTrackScheduler().getQueue().size() + " songs`", true)
                        .addField("Repeat / Pause", "`" + (musicManager.getTrackScheduler().getRepeatMode() == null ? "false" : musicManager.getTrackScheduler().getRepeatMode())
                                + " / " + String.valueOf(musicManager.getTrackScheduler().getAudioPlayer().isPaused()) + "`", true)
                        .addField("Playing in", vch == null ? "No channel :<" : "`" + vch.getName() + "`" , true)
                        .setFooter("Total pages: " + total + " -> Use ~>queue <page> to change pages. Currently in page " + page, guild.getIconUrl());
                event.getChannel().sendMessage(builder.setDescription(line).build()).queue();
            }
		    return;
        }

        DiscordUtils.list(event, 30, false, (p, total)->{
            long length = musicManager.getTrackScheduler().getQueue().stream().mapToLong(value -> value.getInfo().length).sum();
            EmbedBuilder builder = new EmbedBuilder()
                    .setAuthor("Queue for server " + guild.getName(), null, guild.getIconUrl())
                    .setColor(Color.CYAN);

            String nowPlaying = musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack() != null ?
                    "**[" + musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().getInfo().title
                            + "](" + musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().getInfo().uri +
                            ")** (" + Utils.getDurationMinutes(musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().getInfo().length) + ")" :
                    "Nothing or title/duration not found";
            VoiceChannel vch = guild.getSelfMember().getVoiceState().getChannel();
            builder
                    .addField("Currently playing", nowPlaying, false)
                    .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png")
                    .addField("Total queue time", "`" + Utils.getReadableTime(length) + "`", true)
                    .addField("Total queue size", "`" + musicManager.getTrackScheduler().getQueue().size() + " songs`", true)
                    .addField("Repeat / Pause", "`" + (musicManager.getTrackScheduler().getRepeatMode() == null ? "false" : musicManager.getTrackScheduler().getRepeatMode())
                            + " / " + String.valueOf(musicManager.getTrackScheduler().getAudioPlayer().isPaused()) + "`", true)
                    .addField("Playing in", vch == null ? "No channel :<" : "`" + vch.getName() + "`" , true)
                    .setFooter("Total pages: " + total + " -> React to change pages. Currently in page " + p, guild.getIconUrl());
            return builder;
        }, lines);
	}

	public static void openAudioConnection(GuildMessageReceivedEvent event, AudioManager audioManager, VoiceChannel userChannel) {
		if (userChannel.getUserLimit() <= userChannel.getMembers().size() && userChannel.getUserLimit() > 0 && !event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_CHANNEL)) {
			event.getChannel().sendMessage(EmoteReference.ERROR + "I can't connect to that channel because it is full!").queue();
			return;
		}

		try {
			audioManager.openAudioConnection(userChannel);
			event.getChannel().sendMessage(EmoteReference.CORRECT + "Connected to channel **" + userChannel.getName() + "**!").queue();
		} catch (NullPointerException e) {
			event.getChannel().sendMessage(EmoteReference.ERROR + "We received a non-existant channel as response. If you set a voice channel and then deleted it, that might be the cause." +
				"\n We reset your music channel for you, try to play the music again.").queue();
			MantaroData.db().getGuild(event.getGuild()).getData().setMusicChannel(null);
			MantaroData.db().getGuild(event.getGuild()).save();
		}
	}

	public static boolean connectToVoiceChannel(GuildMessageReceivedEvent event) {
		VoiceChannel userChannel = event.getMember().getVoiceState().getChannel();

		if (userChannel == null) {
			event.getChannel().sendMessage("\u274C **Please join a voice channel!**").queue();
			return false;
		}

		if (!event.getGuild().getSelfMember().hasPermission(userChannel, Permission.VOICE_CONNECT)) {
			event.getChannel().sendMessage(":heavy_multiplication_x: I cannot connect to this channel due to the lack of permission.").queue();
			return false;
		}

		if (!event.getGuild().getSelfMember().hasPermission(userChannel, Permission.VOICE_SPEAK)) {
			event.getChannel().sendMessage(":heavy_multiplication_x: I cannot speak in this channel due to the lack of permission.").queue();
			return false;
		}

		VoiceChannel guildMusicChannel = null;
		if (MantaroData.db().getGuild(event.getGuild()).getData().getMusicChannel() != null) {
			guildMusicChannel = event.getGuild().getVoiceChannelById(MantaroData.db().getGuild(event.getGuild()).getData().getMusicChannel());
		}

		AudioManager audioManager = event.getGuild().getAudioManager();

		if (guildMusicChannel != null) {
			if (!userChannel.equals(guildMusicChannel)) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "I can only play music on channel **" + guildMusicChannel.getName() + "**!").queue();
				return false;
			}

			if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
				audioManager.openAudioConnection(userChannel);
				event.getChannel().sendMessage(EmoteReference.CORRECT + "Connected to channel **" + userChannel.getName() + "**!").queue();
			}

			return true;
		}

		if (audioManager.isConnected() && !audioManager.getConnectedChannel().equals(userChannel)) {
			event.getChannel().sendMessage(String.format(EmoteReference.WARNING + "I'm already connected on channel **%s**! (Use the `move` command to move me to another channel)", audioManager.getConnectedChannel().getName())).queue();
			return false;
		}

		if (audioManager.isAttemptingToConnect() && !audioManager.getQueuedAudioConnection().equals(userChannel)) {
			event.getChannel().sendMessage(String.format(EmoteReference.ERROR + "I'm already trying to connect to channel **%s**! (Use the `move` command to move me to another channel)", audioManager.getQueuedAudioConnection().getName())).queue();
			return false;
		}

		if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
			openAudioConnection(event, audioManager, userChannel);
		}

		return true;
	}

	public static String getProgressBar(long now, long total) {
		int activeBlocks = (int) ((float) now / total * TOTAL_BLOCKS);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < TOTAL_BLOCKS; i++) builder.append(activeBlocks == i ? BLOCK_ACTIVE : BLOCK_INACTIVE);
		return builder.append(BLOCK_INACTIVE).toString();
	}
}
