package net.kodehawa.mantarobot.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import gnu.trove.set.hash.TIntHashSet;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.AudioCmdUtils;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.commands.music.Repeat;
import net.kodehawa.mantarobot.commands.music.TrackScheduler;
import net.kodehawa.mantarobot.commands.rpg.TextChannelGround;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Commands;
import net.kodehawa.mantarobot.modules.RegisterCommand;
import net.kodehawa.mantarobot.modules.commands.Category;
import net.kodehawa.mantarobot.modules.commands.CommandPermission;
import net.kodehawa.mantarobot.modules.commands.SimpleCommandCompat;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.kodehawa.mantarobot.commands.music.AudioCmdUtils.embedForQueue;
import static org.apache.commons.lang3.StringUtils.replaceEach;

@RegisterCommand.Class
public class MusicCmds {

	@RegisterCommand
	public static void forceskip(CommandRegistry cr) {
		cr.register("forceskip", Commands.newSimple(Category.MUSIC)
			.permission(CommandPermission.ADMIN)
			.code((thiz, event, content, args) -> {
				if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState().getChannel().equals(event
					.getGuild().getAudioManager().getConnectedChannel())) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You aren't connected to the voice channel I'm in!").queue();
					return;
				}

				TrackScheduler scheduler = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler();
				event.getChannel().sendMessage(EmoteReference.CORRECT + "An admin decided to skip the current song.").queue();
				scheduler.next(true);
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "Force skip")
				.setDescription("Well, administrators should be able to forceskip, shouldn't they?")
				.build())
			.build());
	}

	private static boolean isDJ(Member member) {
		Role djRole = member.getGuild().getRolesByName("DJ", true).stream().findFirst().orElse(null);
		return member.isOwner() || (djRole != null && member.getRoles().contains(djRole));
	}

	@RegisterCommand
	public static void move(CommandRegistry cr) {
		cr.register("move", new SimpleCommandCompat(Category.MUSIC) {
			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				Guild guild = event.getGuild();
				if (content.isEmpty()) {
					AudioManager am = guild.getAudioManager();

					try {
						VoiceChannel vc = guild.getMember(event.getAuthor()).getVoiceState().getChannel();

						if (vc != guild.getMember(event.getJDA().getSelfUser()).getVoiceState().getChannel()) {
							event.getChannel().sendMessage(EmoteReference.THINKING + "I will try to move to the channel you're in").queue();
							AudioCmdUtils.closeAudioConnection(event, am);
							AudioCmdUtils.openAudioConnection(event, am, vc);
							return;
						}

						event.getChannel().sendMessage(EmoteReference.ERROR + "Failed to switch voice channels.").queue();
						return;
					} catch (Exception e) {
						if (e instanceof PermissionException) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot connect: I either don't have permission " +
								"to talk or to move channels.").queue();
							return;
						}

						event.getChannel().sendMessage(EmoteReference.ERROR + "I can't move to a non-existant channel!").queue();
						return;
					}
				}

				try {
					VoiceChannel vc = event.getGuild().getVoiceChannelsByName(content, true).get(0);
					AudioManager am = event.getGuild().getAudioManager();

					AudioCmdUtils.closeAudioConnection(event, am);
					AudioCmdUtils.openAudioConnection(event, am, vc);
					event.getChannel().sendMessage(EmoteReference.OK + "Moved bot to channel ``" + vc.getName() + "``").queue();
				} catch (IndexOutOfBoundsException e) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "Voice channel not found or you didn't specify one!").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Voice Channel move command")
					.setDescription("Move me from one VC to another")
					.addField("Usage", "~>move <vc>", false)
					.addField("Parameters", "vc: voice channel to move the bot to (exact name, case-insensitive).", false)
					.addField("Special cases", "If you don't specify a channel name, I will try to move to the channel you're in, " +
						"as long as it's not the same one I'm in currently!", false)
					.build();
			}
		});
	}

	@RegisterCommand
	public static void np(CommandRegistry cr) {
		cr.register("np", new SimpleCommandCompat(Category.MUSIC) {
			@Override
			public CommandPermission permission() {
				return CommandPermission.USER;
			}

			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
				if (musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack() == null) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "There either isn't track playing or we cannot seem to find it," +
						" try playing a song").queue();
					return;
				}

				event.getChannel().sendMessage(String.format(EmoteReference.MEGA + "Now playing -> ``%s (%s/%s)``",
					musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().getInfo().title,
					Utils.getDurationMinutes(musicManager.getTrackScheduler().getCurrentTrack().getPosition()),
					Utils.getDurationMinutes(musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().getDuration())))
					.queue();
				TextChannelGround.of(event).dropItemWithChance(0, 10);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Now Playing (np) Command")
					.addField("Description", "See what track is playing now.", false).build();
			}

		});
	}

	@RegisterCommand
	public static void pause(CommandRegistry cr) {
		cr.register("pause", new SimpleCommandCompat(Category.MUSIC) {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Pause Command")
					.addField("Description", "Pause or unpause the current track.", false)
					.addField("Usage:", "~>pause (if paused, I will unpause, vice versa)", false).build();
			}

			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState().getChannel().equals(event
					.getGuild().getAudioManager().getConnectedChannel())) {
					sendNotConnectedToMyChannel(event.getChannel());
					return;
				}

				GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
				boolean paused = !musicManager.getTrackScheduler().getAudioPlayer().isPaused();
				String toSend = EmoteReference.MEGA + (paused ? "Player paused." : "Player unpaused.");
				musicManager.getTrackScheduler().getAudioPlayer().setPaused(paused);
				event.getChannel().sendMessage(toSend).queue();
				TextChannelGround.of(event).dropItemWithChance(0, 10);
			}

			@Override
			public CommandPermission permission() {
				return CommandPermission.USER;
			}
		});
	}

	@RegisterCommand
	public static void play(CommandRegistry cr) {
		cr.register("play", Commands.newSimple(Category.MUSIC)
			.permission(CommandPermission.USER)
			.code((thiz, event, content, args) -> {
				if (content.trim().isEmpty()) {
					thiz.onHelp(event);
					return;
				}

				try {
					new URL(content);
				} catch (Exception e) {
					if (content.startsWith("soundcloud")) content = ("scsearch: " + content).replace("soundcloud ", "");
					else content = "ytsearch: " + content;
				}

				MantaroBot.getInstance().getAudioManager().loadAndPlay(event, content);
				TextChannelGround.of(event).dropItemWithChance(0, 5);
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "Play Command")
				.addField("Description", "Play songs!", false)
				.addField("Usage", "~>play <song url> (playlists and song names are also acceptable)", false)
				.addField("Tip", "If you do ~>play <search term> I'll search youtube (default), " +
					"but if you do ~>play soundcloud <search term> It will search soundcloud (not for usage w/links).", false)
				.build())
			.build());
	}

	@RegisterCommand
	public static void queue(CommandRegistry cr) {
		cr.register("queue", Commands.newSimple(Category.MUSIC)
			.permission(CommandPermission.USER)
			.code((thiz, event, content, args) -> {
				GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
				int page = 0;
				try {
					page = Integer.parseInt(args[0]) - 1;
				} catch (Exception ignored) {
				}
				event.getChannel().sendMessage(embedForQueue(page, event.getGuild(), musicManager)).queue();

				if (content.startsWith("clear")) {
					int TEMP_QUEUE_LENGTH = musicManager.getTrackScheduler().getQueue().size();
					MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler().getQueue().clear();
					event.getChannel().sendMessage(EmoteReference.CORRECT + "Removed **" + TEMP_QUEUE_LENGTH + " songs** from the queue" +
						".").queue();
					MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler().next(true);
					return;
				}
				TextChannelGround.of(event).dropItemWithChance(0, 10);
			})
			.help((thiz, event) -> thiz.baseEmbed(event, "Queue Command")
				.addField("Description", "Either returns the current queue playing on the server or clears it.", false)
				.addField("Usage:", "~>queue\n~>queue clear", false).build())
			.build());
	}

	@RegisterCommand
	public static void removetrack(CommandRegistry cr) {
		cr.register("removetrack", Commands.newSimple(Category.MUSIC)
			.permission(CommandPermission.USER)
			.code((thiz, event, content, args) -> {
				if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState().getChannel().equals(event
					.getGuild().getAudioManager().getConnectedChannel())) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You are not connected to the voice channel I am currently " +
						"playing!").queue();
					return;
				}

				MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler().getQueueAsList(list -> {
					TIntHashSet selected = new TIntHashSet();

					String last = Integer.toString(list.size() - 1);

					for (String param : args) {

						String arg = replaceEach(param,
							new String[]{"first", "next", "last", "all"},
							new String[]{"0", "0", last, "0-" + last}
						);

						if (arg.contains("-") || arg.contains("~")) {
							String[] range = content.split("[-~]");

							if (range.length != 2) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "``" + param + "`` is not a valid range!").queue();
								return;
							}

							try {
								int iStart = Integer.parseInt(range[0]) - 1, iEnd = Integer.parseInt(range[1]) - 1;

								if (iStart < 0 || iStart >= list.size()) {
									event.getChannel().sendMessage(EmoteReference.ERROR + "There isn't a queued track at the position ``" +
										iStart + "``!").queue();
									return;
								}

								if (iEnd < 0 || iEnd >= list.size()) {
									event.getChannel().sendMessage(EmoteReference.ERROR + "There isn't a queued track at the position ``" +
										iEnd + "``!").queue();
									return;
								}

								selected.addAll(IntStream.rangeClosed(iStart, iEnd).toArray());
							} catch (NumberFormatException ex) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "``" + param + "`` is not a valid number!").queue();
								return;
							}
						} else {
							try {
								int i = Integer.parseInt(content) - 1;

								if (i < 0 || i >= list.size()) {
									event.getChannel().sendMessage(EmoteReference.ERROR + "There isn't a queued track at the position ``"
										+ i + "``!").queue();
									return;
								}

								selected.add(i);
							} catch (NumberFormatException ex) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "``" + arg + "`` is not a valid number or range!").queue();
								return;
							}
						}
					}

					event.getChannel().sendMessage(
						EmoteReference.CORRECT +
							"Removed track(s) **" + (
							Arrays.stream(selected.toArray())
								.mapToObj(list::remove)
								.map(track -> track.getInfo().title)
								.collect(Collectors.joining("**, **"))
						) + "** from the queue."
					).queue();

					TextChannelGround.of(event).dropItemWithChance(0, 10);
				});
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "Remove Track Command")
				.addField("Description", "Remove the specified track from the queue.", false)
				.addField("Usage:", "~>removetrack <tracknumber/first/next/last> (as specified on the ~>queue command)", false)
				//TODO Update this
				.addField("Parameters:", "tracknumber: the number of the track to remove\n" +
					"first: remove the first track\n"
					+ "next: remove the next track\n"
					+ "last: remove the last track", false)
				.build())
			.build());
	}

	@RegisterCommand
	public static void repeat(CommandRegistry cr) {
		cr.register("repeat", Commands.newSimple(Category.MUSIC)
			.permission(CommandPermission.USER)
			.code((thiz, event, content, args) -> {
				if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState().getChannel().equals(event
					.getGuild().getAudioManager().getConnectedChannel())) {
					sendNotConnectedToMyChannel(event.getChannel());
					return;
				}

				GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
				try {
					switch (args[0].toLowerCase()) {
						case "queue":
							if (musicManager.getTrackScheduler().getRepeat() == Repeat.QUEUE) {
								musicManager.getTrackScheduler().setRepeat(null);
								event.getChannel().sendMessage(EmoteReference.CORRECT + "Continuing with the current queue.").queue();
							} else {
								musicManager.getTrackScheduler().setRepeat(Repeat.QUEUE);
								event.getChannel().sendMessage(EmoteReference.CORRECT + "Repeating the current queue.").queue();
							}
							break;
					}
				} catch (Exception e) {
					if (musicManager.getTrackScheduler().getRepeat() == Repeat.SONG) {
						musicManager.getTrackScheduler().setRepeat(null);
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Continuing with the normal queue.").queue();
					} else {
						musicManager.getTrackScheduler().setRepeat(Repeat.SONG);
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Repeating the current song.").queue();
					}
				}

				TextChannelGround.of(event).dropItemWithChance(0, 10);
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "Repeat command")
				.setDescription("Repeats a song.")
				.addField("Usage", "~>repeat (if the song isn't repeating, start repeating, or vice versa)", false)
				.addField("Warning", "Might not work correctly if I leave the voice channel after you have disabled repeat. To fix, just " +
					"add a song to the queue", true)
				.build())
			.build());
	}

	private static void sendNotConnectedToMyChannel(MessageChannel channel) {
		channel.sendMessage(EmoteReference.ERROR + "You aren't connected to the voice channel I'm currently " +
			"playing in!").queue();
	}

	@RegisterCommand
	public static void shuffle(CommandRegistry cr) {
		cr.register("shuffle", Commands.newSimple(Category.MUSIC)
			.permission(CommandPermission.USER)
			.code((thiz, event, content, args) -> {
				if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState().getChannel().equals(event
					.getGuild().getAudioManager().getConnectedChannel())) {
					sendNotConnectedToMyChannel(event.getChannel());
					return;
				}

				MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler().shuffle();
				event.getChannel().sendMessage(EmoteReference.OK + "Randomized the order of the current queue.").queue();
				TextChannelGround.of(event).dropItemWithChance(0, 10);
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "Shuffle Command")
				.addField("Description", "Shuffle the current queue!", false).build())
			.build());
	}

	@RegisterCommand
	public static void skip(CommandRegistry cr) {
		cr.register("skip", Commands.newSimple(Category.MUSIC)
			.permission(CommandPermission.USER)
			.code((thiz, event, content, args) -> {
				try {
					if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState().getChannel().equals
						(event.getGuild().getAudioManager().getConnectedChannel())) {
						sendNotConnectedToMyChannel(event.getChannel());
						return;
					}
					TrackScheduler scheduler = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild())
						.getTrackScheduler();
					if (scheduler.getCurrentTrack().getDJ() != null && scheduler.getCurrentTrack().getDJ().equals(event.getAuthor())
						|| isDJ(event.getMember())) {
						event.getChannel().sendMessage(EmoteReference.CORRECT + "The DJ has decided to skip!").queue();
						scheduler.next(true);
						return;
					}
					List<String> voteSkips = scheduler.getVoteSkips();
					int requiredVotes = scheduler.getRequiredSkipVotes();
					if (voteSkips.contains(event.getAuthor().getId())) {
						voteSkips.remove(event.getAuthor().getId());
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Your vote has been removed! " +
							(requiredVotes - voteSkips.size()) + " more are required to skip!").queue();
					} else {
						voteSkips.add(event.getAuthor().getId());
						if (voteSkips.size() >= requiredVotes) {
							event.getChannel().sendMessage(EmoteReference.CORRECT + "Reached the required amount of votes, skipping song...").queue();
							scheduler.next(true);
							return;
						}
						event.getChannel().sendMessage(EmoteReference.OK + "Your vote has been submitted! More " +
							(requiredVotes - voteSkips.size()) + " are required to skip!").queue();
					}
					TextChannelGround.of(event).dropItemWithChance(0, 10);
				} catch (NullPointerException e) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "There is no track to skip").queue();
				}
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "Skip Command")
				.addField("Description", "Stops the current track and continues to the next, if one exists.",
					false).build())
			.build());
	}

	@RegisterCommand
	public static void stop(CommandRegistry cr) {
		cr.register("stop", Commands.newSimple(Category.MUSIC)
			.permission(CommandPermission.USER)
			.code((thiz, event, content, args) -> {
				try {
					if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState().getChannel().equals
						(event.getGuild().getAudioManager().getConnectedChannel())) {
						sendNotConnectedToMyChannel(event.getChannel());
						return;
					}

					TrackScheduler scheduler = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild())
						.getTrackScheduler();
					if (isDJ(event.getMember())) {
						event.getChannel().sendMessage(EmoteReference.CORRECT + "The server DJ has decided to stop!").queue();
						stop(event);
						return;
					}
					List<String> stopVotes = scheduler.getVoteStop();
					int requiredVotes = scheduler.getRequiredSkipVotes();
					if (stopVotes.contains(event.getAuthor().getId())) {
						stopVotes.remove(event.getAuthor().getId());
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Your vote has been removed! More " +
							(requiredVotes - stopVotes.size()) + " are required to stop!").queue();
					} else {
						stopVotes.add(event.getAuthor().getId());
						if (stopVotes.size() >= requiredVotes) {
							event.getChannel().sendMessage(EmoteReference.CORRECT + "Reached the required amount of votes, stopping player...").queue();
							stop(event);
							return;
						}
						event.getChannel().sendMessage(EmoteReference.OK + "Your vote has been submitted! More "
							+ (requiredVotes - stopVotes.size()) + " are required to stop!").queue();
					}
				} catch (NullPointerException e) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "There is no player to stop!").queue();
				}
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "Stop Command")
				.addField("Description", "Clears the queue and leaves the voice channel.", false).build())
			.build());
	}

	private static void stop(GuildMessageReceivedEvent event) {
		GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
		if (musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack() != null && !musicManager.getTrackScheduler().getAudioPlayer().isPaused())
			musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().stop();
		int TEMP_QUEUE_LENGTH = musicManager.getTrackScheduler().getQueue().size();
		MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler().getQueue().clear();
		if (TEMP_QUEUE_LENGTH > 0) { //else we just don't show this, why shall we?
			event.getChannel().sendMessage(EmoteReference.OK + "Removed **" + TEMP_QUEUE_LENGTH + " songs** from the queue.").queue();
		}
		MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler().next(true);
		event.getGuild().getAudioManager().closeAudioConnection();
	}

	@RegisterCommand
	public static void volume(CommandRegistry cr) {
		cr.register("volume", Commands.newSimple(Category.MUSIC)
			.permission(CommandPermission.USER)
			.code((thiz, event, content, args) -> {
				if (MantaroData.db().getUser(event.getMember()).isPremium() ||
					MantaroData.db().getGuild(event.getMember()).isPremium() ||
					MantaroData.config().get().getOwners().contains(event.getAuthor().getId())) {
					if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState().getChannel().
						equals(event.getGuild().getAudioManager().getConnectedChannel())) {
						sendNotConnectedToMyChannel(event.getChannel());
						return;
					}

					AudioPlayer player = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler().getAudioPlayer();

					if (args[0].equals("check")) {
						event.getChannel().sendMessage(EmoteReference.ZAP + "The current volume for this session is: " + player.getVolume()).queue();
						return;
					}

					int volume;
					try {
						volume = Math.max(0, Math.min(100, Integer.parseInt(args[0])));
					} catch (Exception e) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid number.").queue();
						return;
					}
					player.setVolume(volume);
					event.getChannel().sendMessage(String.format(EmoteReference.OK + "Volume set to %d", volume)).queue();
				} else {
					event.getChannel().sendMessage(EmoteReference.ERROR + "This is a premium-only feature. In order to get" +
						" donator benefits like this one you can pledge on patreon (https://www.patreon.com/mantaro). Thanks for understanding.\n" +
						"Premium features can be either bound to an user or a server, please, if you donate, join the support guild and ask for it.")
						.queue();
				}
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "Volume command")
				.addField("Usage", "~>volume <number>", false)
				.addField("Parameters", "number: An integer between 1 and 100", false)
				.addField("Notice", "This is a donator-only feature\nTo check the current volume, do ~>volume check", false)
				.build())
			.build());
	}

}
