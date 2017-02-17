package net.kodehawa.mantarobot.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.audio.MantaroAudioManager;
import net.kodehawa.mantarobot.commands.audio.MusicManager;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Utils;
import sun.util.resources.cldr.ta.TimeZoneNames_ta;

import java.net.URL;

import static net.kodehawa.mantarobot.commands.audio.AudioCmdUtils.embedForQueue;
import static net.kodehawa.mantarobot.commands.audio.MantaroAudioManager.*;

public class AudioCmds extends Module {
	public AudioCmds() {
		super(Category.AUDIO);
		np();
		pause();
		play();
		queue();
		removetrack();
		shuffle();
		skip();
		stop();
		volume();
		repeat();
	}

	private void np() {
		super.register("np", new SimpleCommand() {
			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = getGuildAudioPlayer(event);
				event.getChannel().sendMessage("\uD83D\uDCE3 Now playing ->``" + musicManager.getScheduler().getPlayer().getPlayingTrack().getInfo().title
					+ " (" + Utils.getDurationMinutes(musicManager.getScheduler().getPlayer().getPlayingTrack().getInfo().length) + ")``").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "NowPlaying (np) Command")
					.addField("Description", "Returns what track is playing now.", false).build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	private void pause() {
		super.register("pause", new SimpleCommand() {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Pause Command")
					.addField("Description", "Pauses or unpauses the current track.", false)
					.addField("Usage:", "~>pause true/false (pause/unpause)", false).build();
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = getGuildAudioPlayer(event);
				try {
					boolean paused = Boolean.parseBoolean(content);
					String toSend = paused ? ":mega: Player paused." : ":mega: Player unpaused.";
					musicManager.getScheduler().getPlayer().setPaused(paused);
					event.getChannel().sendMessage(toSend).queue();
				} catch (Exception e) {
					event.getChannel().sendMessage(":x " + "Error -> Not a boolean value").queue();
				}
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	public void play() {
		super.register("play", new SimpleCommand() {
			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (content.trim().isEmpty()) {
					onHelp(event);
					return;
				}

				try {
					new URL(content);
				} catch (Exception e) {
					content = "ytsearch: " + content;
				}

				MantaroAudioManager.loadAndPlay(event, content);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Play Command")
					.addField("Description", "Plays a song in the music voice channel.", false)
					.addField("Usage:", "~>play <song url> (Can be a YouTube song, a playlist or a search)", false).build();
			}

		});
	}

	public void queue() {
		super.register("queue", new SimpleCommand() {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Queue Command")
					.addField("Description", "Returns the current queue playing on the server or clears it.", false)
					.addField("Usage:", "~>queue\n~>queue clear", false).build();
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = getGuildAudioPlayer(event);
				if (content.isEmpty()) {
					event.getChannel().sendMessage(embedForQueue(event.getGuild(), musicManager)).queue();
				} else if (content.startsWith("clear")) {
					MantaroAudioManager.clearQueue(musicManager, event, true);
				}
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	public void removetrack() {
		super.register("removetrack", new SimpleCommand() {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "RemoveTrack Command")
					.addField("Description", "Removes the specified track from the queue.", false)
					.addField("Usage:", "~>removetrack [tracknumber] (as specified on the ~>queue command)", false).build();
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = getGuildAudioPlayer(event);
				int n = 0;
				for (AudioTrack audioTrack : musicManager.getScheduler().getQueue()) {
					try{
						if (n == Integer.parseInt(content) - 1) {
							event.getChannel().sendMessage("Removed track: " + audioTrack.getInfo().title).queue();
							musicManager.getScheduler().getQueue().remove(audioTrack);
							break;
						}
						n++;
					} catch (NumberFormatException ex){
						event.getChannel().sendMessage(":heavy_multiplication_x: That's not a number.").queue();
					}
				}
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	public void shuffle() {
		super.register("shuffle", new SimpleCommand() {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Shuffle Command")
					.addField("Description", "Shuffles the current queue.", false).build();
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				getGuildAudioPlayer(event).shuffle();
				event.getChannel().sendMessage("\uD83D\uDCE3 Randomized current queue order.").queue();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	public void skip() {
		super.register("skip", new SimpleCommand() {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Skip Command")
					.addField("Description", "Stops the track and continues to the next one, if there is one.", false).build();
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				getGuildAudioPlayer(event).skipTrack(event);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	public void stop() {
		super.register("stop", new SimpleCommand() {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Stop Command")
					.addField("Description", "Clears the queue and leaves the voice channel.", false).build();
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = getGuildAudioPlayer(event);
				clearQueue(musicManager, event, false);
				closeConnection(musicManager, event.getGuild().getAudioManager(), event.getChannel());
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	private void volume(){
		super.register("volume", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				AudioPlayer player = getGuildAudioPlayer(event).getScheduler().getPlayer();
				int volume;
				try {
					volume = Integer.parseInt(args[0]);
				} catch (Exception e){
					event.getChannel().sendMessage(":heavy_multiplication_x: Not a valid integer.").queue();
					return;
				}
				player.setVolume(volume);
				event.getChannel().sendMessage(String.format(":ok_hand: Volume set to %d", volume)).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Volume command")
						.addField("Usage", "~>volume <number>", false)
						.addField("Parameters", "number: Integer number from 1 to 99", false)
						.build();
			}
		});
	}

	private void repeat(){
		super.register("repeat", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = getGuildAudioPlayer(event);
				try {
					if(musicManager.getScheduler().getPlayer().getPlayingTrack() != null){
						boolean repeat = Boolean.parseBoolean(content);
						String toSend = repeat ? ":mega: Repeating current song." : ":mega: Continuing with normal queue.";
						if(repeat){
							musicManager.getScheduler().setRepeat(true, musicManager.getScheduler().getPlayer().getPlayingTrack());
						} else{
							musicManager.getScheduler().setRepeat(false, null);
						}

						event.getChannel().sendMessage(toSend).queue();
						return;
					}

					event.getChannel().sendMessage(":heavy_multiplication_x: Cannot repeat a non-existant track.").queue();
				} catch (Exception e) {
					event.getChannel().sendMessage(":heavy_multiplication_x: " + "Error -> Not a boolean value").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Repeat command")
						.setDescription("Repeats a song.")
						.addField("Usage", "~>repeat <true/false>", false)
						.addField("Parameters", "<true/false> true if you want it to repeat, false otherwise", false)
						.build();
			}
		});
	}
}
