package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.Region;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.data.SimpleFileDataManager.NEWLINE_PATTERN;

public class AudioCmdUtils {

	private final static String BLOCK_INACTIVE = "\u25AC";
	private final static String BLOCK_ACTIVE = "\uD83D\uDD18";
	private static final int TOTAL_BLOCKS = 10;
	private static final Pattern pattern = Pattern.compile("\\d+?[a-zA-Z]");

	public static void closeAudioConnection(GuildMessageReceivedEvent event, AudioManager audioManager) {
		audioManager.closeAudioConnection();
		event.getChannel().sendMessage(EmoteReference.CORRECT + "Closed audio connection.").queue();
	}

	public static MessageEmbed embedForQueue(int page, Guild guild, GuildMusicManager musicManager) {
		String toSend = AudioUtils.getQueueList(musicManager.getTrackScheduler().getQueue());
		List<String> lines = Arrays.asList(NEWLINE_PATTERN.split(toSend));
		List<List<String>> list = new ArrayList<>();

		if (lines.size() >= 15) {

			int pages = list.size() % 2 == 0 ? lines.size() / 2 : (lines.size() / 2) + 1;
			if (lines.size() <= 60) pages = lines.size() % 5 == 0 ? lines.size() / 5 : (lines.size() / 4) + 1; //5 or 4 pages
			else if (lines.size() <= 100) pages = lines.size() % 8 == 0 ? lines.size() / 8 : (lines.size() / 7) + 1; //8 or 7 pages
			else if (lines.size() <= 200) pages = lines.size() % 16 == 0 ? lines.size() / 16 : (lines.size() / 15) + 1; //16 or 15 pages
			else if (lines.size() <= 300) pages = lines.size() % 24 == 0 ? lines.size() / 24 : (lines.size() / 23) + 1; //24 or 23 pages

			list = chunks(lines, pages);

			try {
				toSend = list.get(page).stream().collect(Collectors.joining("\n"));
			} catch (IndexOutOfBoundsException e) {
				toSend = "Nothing on this page, just dust";
			}
		}

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
		if (!toSend.isEmpty()) {
			builder.setDescription(toSend)
				.addField("Currently playing", nowPlaying, false)
				.setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png")
				.addField("Total queue time", "`" + Utils.getReadableTime(length) + "`", true)
				.addField("Total queue size", "`" + musicManager.getTrackScheduler().getQueue().size() + " songs`", true)
				.addField("Repeat / Pause", "`" + (musicManager.getTrackScheduler().getRepeat() == null ? "false" : musicManager.getTrackScheduler().getRepeat())
					+ " / " + String.valueOf(musicManager.getTrackScheduler().getAudioPlayer().isPaused()) + "`", true)
				.addField("Playing in", vch == null ? "No channel :<" : "`" + vch.getName() + "`" , true)
				.setFooter("Total pages: " + list.size() + " -> Do ~>queue <page> to go to next page. Currently in page " + (page + 1), guild.getIconUrl());
		} else {
			builder.setDescription("Nothing here, just dust. Why don't you queue some songs?")
					.setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png");
		}

		return builder.build();
	}

	public static String getDurationMinutes(long length) {
		return String.format("%d:%02d minutes",
			TimeUnit.MILLISECONDS.toMinutes(length),
			TimeUnit.MILLISECONDS.toSeconds(length) -
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(length))
		);
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
				"\n We resetted your music channel for you, try to play the music again.").queue();
			MantaroData.db().getGuild(event.getGuild()).getData().setMusicChannel(null);
			MantaroData.db().getGuild(event.getGuild()).save();
		}
	}

	static boolean connectToVoiceChannel(GuildMessageReceivedEvent event) {
		VoiceChannel userChannel = event.getMember().getVoiceState().getChannel();

		if (userChannel == null) {
			event.getChannel().sendMessage("\u274C **Please join a voice channel!**").queue();
			return false;
		}

		if (!event.getGuild().getMember(event.getJDA().getSelfUser()).hasPermission(userChannel, Permission.VOICE_CONNECT)) {
			event.getChannel().sendMessage(":heavy_multiplication_x: I cannot connect to this channel due to the lack of permission.").queue();
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

	static String getDurationMinutes(AudioTrack track) {
		return getDurationMinutes(track.getInfo().length);
	}

	private static List<List<String>> chunks(List<String> bigList, int n) {
		List<List<String>> chunks = new ArrayList<>();

		for (int i = 0; i < bigList.size(); i += n) {
			List<String> chunk = bigList.subList(i, Math.min(bigList.size(), i + n));
			chunks.add(chunk);
		}

		return chunks;
	}

	public static String getProgressBar(long percent, long duration) {
		int activeBlocks = (int) ((float) percent / duration * TOTAL_BLOCKS);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < TOTAL_BLOCKS; i++) builder.append(activeBlocks == i ? BLOCK_ACTIVE : BLOCK_INACTIVE);
		return builder.append(BLOCK_INACTIVE).toString();
	}

	private static Iterable<String> iterate(Matcher matcher) {
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					@Override
					public boolean hasNext() {
						return matcher.find();
					}

					@Override
					public String next() {
						return matcher.group();
					}
				};
			}

			@Override
			public void forEach(Consumer<? super String> action) {
				while (matcher.find()) {
					action.accept(matcher.group());
				}
			}
		};
	}


	public static long parseTime(String s) {
		s = s.toLowerCase();
		long[] time = {0};
		iterate(pattern.matcher(s)).forEach(string -> {
			String l = string.substring(0, string.length() - 1);
			TimeUnit unit;
			switch (string.charAt(string.length() - 1)) {
				case 's':
					unit = TimeUnit.SECONDS;
					break;
				case 'm':
					unit = TimeUnit.MINUTES;
					break;
				case 'h':
					unit = TimeUnit.HOURS;
					break;
				case 'd':
					unit = TimeUnit.DAYS;
					break;
				default:
					unit = TimeUnit.SECONDS;
					break;
			}
			time[0] += unit.toMillis(Long.parseLong(l));
		});
		return time[0];
	}
}
