package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;

import static net.kodehawa.mantarobot.commands.music.AudioCmdUtils.connectToVoiceChannel;
import static net.kodehawa.mantarobot.commands.music.AudioCmdUtils.getDurationMinutes;

public class MantaroAudioManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("MantaroAudioManager");
	private static final Map<String, MusicManager> musicManagers = new HashMap<>();
	private static AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

	static {
		AudioSourceManagers.registerRemoteSources(playerManager);
	}

	public static void clearQueue(MusicManager musicManager, GuildMessageReceivedEvent event, boolean askForSkip) {
		int TEMP_QUEUE_LENGHT = musicManager.getScheduler().getQueue().size();
		musicManager.getScheduler().getQueue().clear();
		if(event != null) event.getChannel().sendMessage("Removed **" + TEMP_QUEUE_LENGHT + " songs** from queue.").queue();
		if (askForSkip) musicManager.skipTrack(event);
	}

	public static void closeConnection(MusicManager musicManager, AudioManager audioManager, TextChannel channel) {
		musicManager.getScheduler().getQueue().clear();
		closeConnection(audioManager, channel);
	}

	public static void closeConnection(AudioManager audioManager, TextChannel channel) {
		audioManager.closeAudioConnection();
		channel.sendMessage("\uD83D\uDCE3 Closed audio connection.").queue();
	}

	public static MusicManager getGuildAudioPlayer(Guild guild) {
		return getGuildAudioPlayer(guild, guild.getPublicChannel());
	}

	public static MusicManager getGuildAudioPlayer(GuildMessageReceivedEvent event) {
		return getGuildAudioPlayer(event.getGuild(), event.getChannel());
	}

	public static MusicManager getGuildAudioPlayer(Guild guild, TextChannel channel) {
		return musicManagers.computeIfAbsent(guild.getId(), k -> {
			MusicManager manager = new MusicManager(playerManager, channel);
			guild.getAudioManager().setSendingHandler(manager.getSendHandler());
			return manager;
		});
	}

	public static Map<String, MusicManager> getMusicManagers() {
		return musicManagers;
	}

	public static int getTotalQueueSize() {
		return musicManagers.values().stream().filter(musicManager -> !musicManager.getScheduler().getQueue().isEmpty()).
			map(musicManager -> musicManager.getScheduler().getQueue().size()).mapToInt(Integer::intValue).sum();
	}

	public static void loadAndPlay(final GuildMessageReceivedEvent event, final String trackUrl) {
		TextChannel channel = event.getChannel();
		MusicManager musicManager = getGuildAudioPlayer(event.getGuild(), channel);

		if (!connectToVoiceChannel(event)) return;

		playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				loadTrack(event, musicManager, track, false);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				if (playlist.isSearchResult()) {
					onSearchResult(event, playlist, musicManager);
					return;
				}

				playlist.getTracks().forEach(audioTrack -> loadTrack(event, musicManager, audioTrack, true));

				event.getChannel().sendMessage(String.format(
					"Added **%d songs** to queue on playlist: **%s** *(%s)*",
					playlist.getTracks().size(),
					playlist.getName(),
					Utils.getDurationMinutes(playlist.getTracks().stream().mapToLong(temp -> temp.getInfo().length).sum())
				)).queue();
			}

			@Override
			public void noMatches() {
				channel.sendMessage("\u274C Nothing found on " + trackUrl).queue();
				closeConnection(musicManager, event.getGuild().getAudioManager(), event.getChannel());
			}

			@Override
			public void loadFailed(FriendlyException exception) {
				if (!exception.severity.equals(FriendlyException.Severity.FAULT)) {
					channel.sendMessage("\u274C Error while fetching music: " + exception.getMessage()).queue();
				} else {
					LOGGER.warn("Error caught while playing audio, the bot might be able to continue playing music.", exception);
				}
			}
		});
	}

	private static void loadTrack(GuildMessageReceivedEvent event, MusicManager musicManager, AudioTrack track, boolean isPlaylist) {
		TextChannel channel = event.getChannel();
		try {
			int trackDuration = Optional.ofNullable(MantaroData.getData().get().getGuild(event.getGuild(), false).songDurationLimit).isPresent() ?
				MantaroData.getData().get().getGuild(event.getGuild(), false).songDurationLimit : 600000;
			if (track.getDuration() > trackDuration && !MantaroData.getConfig().get().isOwner(event.getMember())) {
				channel.sendMessage("\u274C" + " Track added is longer than 10 minutes (>600000ms). Cannot add "
						+ track.getInfo().title
						+ " (Track length: " + getDurationMinutes(track) + ")"
				).queue();
				return;
			}

			musicManager.getScheduler().queue(track);

			if (!isPlaylist) {
				channel.sendMessage(
					"\uD83D\uDCE3 Added to queue -> **" + track.getInfo().title + "**"
						+ " **!(" + getDurationMinutes(track) + ")**"
				).queue();
			}
		} catch (Exception e) {
			LOGGER.warn("Exception thrown while loading/adding a song, might be worth checking", e);
		}
	}

	private static void onSearchResult(GuildMessageReceivedEvent event, AudioPlaylist playlist, MusicManager musicManager) {
		EmbedBuilder builder = new EmbedBuilder().setColor(Color.CYAN).setTitle("Song selection. Type the song number to continue.", null).setFooter("This timeouts in 10 seconds.", null);
		List<AudioTrack> tracks = playlist.getTracks();
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < 4 && i < tracks.size(); i++) {
			AudioTrack at = tracks.get(i);
			b.append('[').append(i + 1).append("] ").append(at.getInfo().title).append(" **(")
				.append(Utils.getDurationMinutes(at.getInfo().length)).append(")**").append("\n");
		}

		event.getChannel().sendMessage(builder.setDescription(b.toString()).build()).queue();
		IntConsumer consumer = (c) -> loadTrack(event, musicManager, playlist.getTracks().get(c - 1), false);
		DiscordUtils.selectInt(event, 5, consumer);
	}

	public static boolean isAlone(VoiceChannel channel) {
		for (Member member : channel.getMembers()) if (!member.getUser().isBot()) return false;
		return true;
	}
}
