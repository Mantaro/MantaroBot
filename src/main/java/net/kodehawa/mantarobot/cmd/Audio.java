package net.kodehawa.mantarobot.cmd;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.audio.MusicManager;
import net.kodehawa.mantarobot.cmd.guild.Parameters;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.listeners.generic.FunctionListener;
import net.kodehawa.mantarobot.log.Log;
import net.kodehawa.mantarobot.log.Type;
import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.Category;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.thread.AsyncHelper;
import net.kodehawa.mantarobot.util.GeneralUtils;

import java.awt.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Audio extends Module {
	private static void connectToNamedVoiceChannel(String voiceId, AudioManager audioManager) {
		if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
			for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
				if (voiceChannel.getId().equals(voiceId)) {
					audioManager.openAudioConnection(voiceChannel);
					break;
				}
			}
		}
	}

	private static void connectToUserVoiceChannel(GuildMessageReceivedEvent event) {
		if (event.getMember().getVoiceState().getChannel() != null) {
			AudioManager audioManager = event.getGuild().getAudioManager();
			if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
				audioManager.openAudioConnection(event.getMember().getVoiceState().getChannel());
			}
		}
	}

	private final Map<Long, MusicManager> musicManagers;
	private final AudioPlayerManager playerManager;

	public Audio() {
		super.setCategory(Category.AUDIO);
		this.musicManagers = new HashMap<>();
		this.playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager);
		AudioSourceManagers.registerLocalSource(playerManager);
		this.registerCommands();
	}

	@Override
	public void registerCommands() {
		super.register("play", "Plays a song in the music voice channel.", new Callback() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				try {
					new URL(content);
				} catch (Exception e) {
					content = "ytsearch: " + content;
				}

				loadAndPlay(event, content);
			}

			@Override
			public String help() {
				return "Plays a song in the music voice channel.\n"
					+ "Usage:\n"
					+ "~>play [youtubesongurl] (Can be a song, a playlist or a search)";
			}

		});

		super.register("skip", "Stops the track and continues to the next one, if there is one.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				skipTrack(event);
			}

			@Override
			public String help() {
				return "Stops the track and continues to the next one, if there is one.\n"
					+ "Usage:\n"
					+ "~>skip";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("shuffle", "Shuffles the current playlist", new Callback() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = musicManagers.get(Long.parseLong(event.getGuild().getId()));
				shuffle(musicManager);
				event.getChannel().sendMessage(":mega: Randomized current queue order.").queue();
			}

			@Override
			public String help() {
				return null;
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("stop", "Clears queue and leaves the voice channel.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = musicManagers.get(Long.parseLong(event.getGuild().getId()));
				clearQueue(musicManager, event, false);
				closeConnection(musicManager, event.getGuild().getAudioManager(), event.getChannel());
			}

			@Override
			public String help() {
				return "Clears the queue and leaves the voice channel.\n"
					+ "Usage:\n"
					+ "~>stop";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("queue", "Returns the current track list playing on the server.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = musicManagers.get(Long.parseLong(event.getGuild().getId()));
				if (content.isEmpty()) {
					event.getChannel().sendMessage(embedQueueList(event.getGuild(), musicManager)).queue();
				} else if (content.startsWith("clear")) {
					clearQueue(musicManager, event, true);
				}
			}

			@Override
			public String help() {
				return "Returns the current queue playing on the server or clears it.\n"
					+ "Usage:\n"
					+ "~>queue"
					+ "~>queue clear";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("removetrack", "Removes the specified track from the queue.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = musicManagers.get(Long.parseLong(event.getGuild().getId()));
				int n = 0;
				for (AudioTrack audioTrack : musicManager.getScheduler().getQueue()) {
					if (n == Integer.parseInt(content) - 1) {
						event.getChannel().sendMessage("Removed track: " + audioTrack.getInfo().title).queue();
						musicManager.getScheduler().getQueue().remove(audioTrack);
						break;
					}
					n++;
				}
			}

			@Override
			public String help() {
				return "Removes the specified track from the queue.\n"
					+ "Usage:\n"
					+ "~>removetrack [tracknumber] (as specified on the ~>queue command)";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("pause", "Pauses the player.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = musicManagers.get(Long.parseLong(event.getGuild().getId()));
				try {
					musicManager.getScheduler().getPlayer().setPaused(Boolean.parseBoolean(content));
				} catch (Exception e) {
					event.getChannel().sendMessage(":heavy_multiplication_x " + "Error -> Not a boolean value");
				}
			}

			@Override
			public String help() {
				return "Pauses or unpauses the current track.\n"
					+ "Usage:\n"
					+ "~>pause true/false (pause/unpause)";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("np", "What's playing now?", new Callback() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = musicManagers.get(Long.parseLong(event.getGuild().getId()));
				event.getChannel().sendMessage(":mega: Now playing ->``" + musicManager.getScheduler().getPlayer().getPlayingTrack().getInfo().title
					+ " (" + GeneralUtils.instance().getDurationMinutes(musicManager.getScheduler().getPlayer().getPlayingTrack().getInfo().length) + ")``").queue();
			}

			@Override
			public String help() {
				return "Returns what track is playing now.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});
	}

	private void clearQueue(MusicManager musicManager, GuildMessageReceivedEvent event, boolean askForSkip) {
		int TEMP_QUEUE_LENGHT = musicManager.getScheduler().getQueue().size();
		for (AudioTrack audioTrack : musicManager.getScheduler().getQueue()) {
			musicManager.getScheduler().getQueue().remove(audioTrack);
		}
		event.getChannel().sendMessage("Removed **" + TEMP_QUEUE_LENGHT + " songs** from queue.").queue();
		if (askForSkip) skipTrack(event);
	}

	private void closeConnection(MusicManager musicManager, AudioManager audioManager, TextChannel channel) {
		musicManager.getScheduler().getQueue().clear();
		audioManager.closeAudioConnection();
		channel.sendMessage(":mega: Closed audio connection.").queue();
	}

	private MessageEmbed embedQueueList(Guild guild, MusicManager musicManager) {
		String toSend = musicManager.getScheduler().getQueueList();
		String[] lines = toSend.split("\r\n|\r|\n");
		List<String> lines2 = new ArrayList<>(Arrays.asList(lines));
		StringBuilder stringBuilder = new StringBuilder();
		int temp = 0;

		for (int i = 0; i < lines2.size(); i++) {
			temp++;
			if (i <= 14) {
				stringBuilder.append
					(lines2.get(i))
					.append("\n");
			} else {
				lines2.remove(i);
			}
		}

		if (temp > 15) stringBuilder.append("\nShowing only first **15** results.");

		long templength = 0;
		for (AudioTrack temp1 : musicManager.getScheduler().getQueue()) {
			templength = templength
				+ temp1.getInfo().length;
		}

		EmbedBuilder builder = new EmbedBuilder();
		builder.setAuthor("Queue for server " + guild.getName(), null, guild.getIconUrl());
		builder.setColor(Color.CYAN);
		if (!toSend.isEmpty()) {
			builder.setDescription(stringBuilder.toString());
			builder.addField("Queue runtime", GeneralUtils.instance().getDurationMinutes(templength), true);
			builder.addField("Total queue size", String.valueOf(musicManager.getScheduler().getQueue().size()), true);
		} else {
			builder.setDescription("Nothing here, just dust.");
		}

		return builder.build();
	}

	private String getDurationMinutes(AudioTrack track) {
		long TRACK_LENGHT = track.getInfo().length;
		return String.format("%d:%02d minutes",
			TimeUnit.MILLISECONDS.toMinutes(TRACK_LENGHT),
			TimeUnit.MILLISECONDS.toSeconds(TRACK_LENGHT) -
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(TRACK_LENGHT))
		);
	}

	private synchronized MusicManager getGuildAudioPlayer(GuildMessageReceivedEvent event) {
		long guildId = Long.parseLong(event.getGuild().getId());
		MusicManager musicManager = musicManagers.computeIfAbsent(guildId, k -> new MusicManager(playerManager, event));
		event.getGuild().getAudioManager().setSendingHandler(musicManager.getSendHandler());
		return musicManager;
	}

	private void loadAndPlay(final GuildMessageReceivedEvent event, final String trackUrl) {
		TextChannel channel = event.getChannel();
		MusicManager musicManager = getGuildAudioPlayer(event);
		playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				loadTrack(event, musicManager, track, false);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				int i = 0;
				if (playlist.isSearchResult()) {
					onSearchResult(event, playlist, musicManager);
				} else {
					for (AudioTrack audioTrack : playlist.getTracks()) {
						if (i <= 60) loadTrack(event, musicManager, audioTrack, true);
						else break;
						i++;
					}
					long templength = 0;
					for (AudioTrack temp : playlist.getTracks()) {
						templength = templength
							+ temp.getInfo().length;
					}
					event.getChannel().sendMessage("Added **" + playlist.getTracks().size()
						+ " songs** to queue on playlist: **"
						+ playlist.getName() + "**" + " *("
						+ GeneralUtils.instance().getDurationMinutes(templength) + ")*"
					).queue();
				}
			}

			@Override
			public void noMatches() {
				channel.sendMessage(":heavy_multiplication_x: Nothing found on " + trackUrl).queue();
			}

			@Override
			public void loadFailed(FriendlyException exception) {
				if (!exception.severity.equals(FriendlyException.Severity.FAULT)) {
					Log.instance().print("Couldn't play music", this.getClass(), Type.WARNING, exception);
					channel.sendMessage(":heavy_multiplication_x: Couldn't play music: " + exception.getMessage() + " SEVERITY: " + exception.severity).queue();
				} else {
					exception.printStackTrace();
				}
			}
		});
	}

	private void loadTrack(GuildMessageReceivedEvent event, MusicManager musicManager, AudioTrack track, boolean isPlaylist) {
		TextChannel channel = event.getChannel();
		try {
			if (track.getDuration() > 600000) {
				channel.sendMessage(
					":heavy_multiplication_x:"
						+ " Track added is longer than 10 minutes (>600000ms). Cannot add "
						+ track.getInfo().title
						+ " (Track length: " + getDurationMinutes(track) + ")"
				).queue();
				return;
			}

			if (Parameters.getMusicVChannelForServer(event.getGuild().getId()).isEmpty()) {
				play(event, musicManager, track);

				if (!isPlaylist)
					channel.sendMessage(
						":mega: Added to queue -> **" + track.getInfo().title + "**"
							+ " **!(" + getDurationMinutes(track) + ")**"
					).queue();
			} else {
				play(Parameters.getMusicVChannelForServer(
					event.getGuild().getId()), event.getGuild(), musicManager, track);

				if (!isPlaylist)
					channel.sendMessage(
						":mega: Added to queue -> **" + track.getInfo().title + "**"
							+ " **(" + getDurationMinutes(track) + ")**"
					).queue();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean nextTrackAvailable(MusicManager musicManager) {
		return musicManager.getScheduler().getQueueSize() > 0;
	}

	private void onSearchResult(GuildMessageReceivedEvent event, AudioPlaylist playlist, MusicManager musicManager) {
		EmbedBuilder builder = new EmbedBuilder().setColor(Color.CYAN).setTitle("Song selection. Type the song number to continue.").setFooter("This timeouts in 10 seconds.", null);
		List<AudioTrack> tracks = playlist.getTracks();
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < 4 && i < tracks.size(); i++) {
			AudioTrack at = tracks.get(i);
			b.append('[').append(i).append("] ").append(at.getInfo().title).append(" **(")
				.append(GeneralUtils.instance().getDurationMinutes(at.getInfo().length)).append(")**").append("\n");
		}

		event.getChannel().sendMessage(builder.setDescription(b.toString()).build()).queue();

		FunctionListener functionListener = new FunctionListener(event.getChannel().getId(), (l, e) -> {
			if (!e.getAuthor().equals(event.getAuthor())) return false;

			try {
				int choose = Integer.parseInt(e.getMessage().getContent());
				if (choose < 1 || choose > 4) return false;
				loadTrack(e, musicManager, playlist.getTracks().get(choose - 1), false);
				return true;
			} catch (Exception ignored) {
			}
			return false;
		});

		Mantaro.instance().getSelf().addEventListener(functionListener);

		AsyncHelper.instance().asyncSleepThen(10000, () -> {
			if (!functionListener.isDone()) {
				Mantaro.instance().getSelf().removeEventListener(functionListener);
				event.getChannel().sendMessage(":heavy_multiplication_x: Timeout: No reply in 10 seconds").queue();
			}
		});
	}

	private void play(GuildMessageReceivedEvent event, MusicManager musicManager, AudioTrack track) {
		connectToUserVoiceChannel(event);
		musicManager.getScheduler().queue(track);
	}

	private void play(String cid, Guild guild, MusicManager musicManager, AudioTrack track) {
		connectToNamedVoiceChannel(cid, guild.getAudioManager());
		musicManager.getScheduler().queue(track);
	}

	private void shuffle(MusicManager musicManager) {
		java.util.List<AudioTrack> temp = new ArrayList<>();
		BlockingQueue<AudioTrack> bq = musicManager.getScheduler().getQueue();
		if (!bq.isEmpty()) bq.drainTo(temp);
		bq.clear();

		java.util.Random rand = new java.util.Random();
		Collections.shuffle(temp, new java.util.Random(rand.nextInt(18975545)));

		for (AudioTrack track : temp) bq.add(track);

		temp.clear();
	}

	private void skipTrack(GuildMessageReceivedEvent event) {
		MusicManager musicManager = getGuildAudioPlayer(event);
		if (nextTrackAvailable(musicManager)) {
			musicManager.getScheduler().nextTrack();
			event.getChannel().sendMessage(":mega: Skipped to next track -> **" + musicManager.getScheduler().getPlayer().getPlayingTrack().getInfo().title + "**").queue();
		} else {
			event.getChannel().sendMessage("No tracks next. Disconnecting...").queue();
			closeConnection(musicManager, event.getGuild().getAudioManager(), event.getChannel());
		}
	}
}