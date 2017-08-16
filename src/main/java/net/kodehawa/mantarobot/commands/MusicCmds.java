package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.RateLimiter;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.info.GuildStatsManager;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.commands.music.requester.TrackScheduler;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.commands.music.utils.AudioUtils;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils.embedForQueue;
import static org.apache.commons.lang3.StringUtils.replaceEach;

@Module
@Slf4j
public class MusicCmds {
	@Subscribe
	public void forceskip(CommandRegistry cr) {
			cr.register("forceskip", new SimpleCommand(Category.MUSIC, CommandPermission.ADMIN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState()
						.getChannel().equals(event
								.getGuild().getAudioManager().getConnectedChannel())) {
					event.getChannel().sendMessage(
							EmoteReference.ERROR + "You aren't connected to the voice channel I'm in!").queue();
					return;
				}

				TrackScheduler scheduler = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild())
						.getTrackScheduler();
				event.getChannel().sendMessage(EmoteReference.CORRECT + "An admin decided to skip the current song.")
						.queue();
				scheduler.nextTrack(true, true);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Force skip")
						.setDescription("Well, administrators should be able to forceskip, shouldn't they?")
						.build();
			}
		});

		cr.registerAlias("forceskip", "fs");
	}

	@Subscribe
	public void move(CommandRegistry cr) {
		cr.register("move", new SimpleCommand(Category.MUSIC) {
			final RateLimiter rl = new RateLimiter(TimeUnit.SECONDS, 20);

			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				Guild guild = event.getGuild();

				if (!rl.process(event.getAuthor().getId())) {
					event.getChannel().sendMessage(EmoteReference.STOPWATCH +
							"S-Stop please, I'm getting dizzy! `(Only usable every 20 seconds)`").queue();
					return;
				}

				if (content.isEmpty()) {
					AudioManager am = guild.getAudioManager();

					try {
						VoiceChannel vc = guild.getMember(event.getAuthor()).getVoiceState().getChannel();

						if (vc != guild.getMember(event.getJDA().getSelfUser()).getVoiceState().getChannel()) {
							event.getChannel().sendMessage(
									EmoteReference.THINKING + "I will try to move to the channel you're in").queue();
							AudioCmdUtils.openAudioConnection(event, am, vc);
							return;
						}

						event.getChannel().sendMessage(EmoteReference.ERROR + "Failed to switch voice channels.")
								.queue();
						return;
					} catch (Exception e) {
						if (e instanceof PermissionException) {
							event.getChannel().sendMessage(
									EmoteReference.ERROR + "Cannot connect: I either don't have permission " +
											"to talk or to move channels.").queue();
							return;
						}

						event.getChannel().sendMessage(EmoteReference.ERROR + "I can't move to a non-existant channel!")
								.queue();
						return;
					}
				}

				try {
					VoiceChannel vc = event.getGuild().getVoiceChannelsByName(content, true).get(0);
					AudioManager am = event.getGuild().getAudioManager();

					AudioCmdUtils.closeAudioConnection(event, am);
					AudioCmdUtils.openAudioConnection(event, am, vc);
					event.getChannel().sendMessage(EmoteReference.OK + "Moved bot to channel ``" + vc.getName() + "``")
							.queue();
				} catch (IndexOutOfBoundsException e) {
					event.getChannel().sendMessage(
							EmoteReference.ERROR + "Voice channel not found or you didn't specify one!").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Voice Channel move command")
						.setDescription("Move me from one VC to another")
						.addField("Usage", "~>move <vc>", false)
						.addField(
								"Parameters", "vc: voice channel to move the bot to (exact name, case-insensitive).", false)
						.addField(
								"Special cases",
								"If you don't specify a channel name, I will try to move to the channel you're in, " +
										"as long as it's not the same one I'm in currently!", false
						)
						.build();
			}

		});
	}

	@Subscribe
	public void np(CommandRegistry cr) {
		cr.register("np", new SimpleCommand(Category.MUSIC) {

			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(
						event.getGuild());
				if (musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack() == null) {
					event.getChannel().sendMessage(
							EmoteReference.ERROR + "There either isn't track playing or we cannot seem to find it," +
									" try playing a song").queue();
					return;
				}

				EmbedBuilder npEmbed = new EmbedBuilder();
				long now = musicManager.getTrackScheduler().getCurrentTrack().getPosition();
				long total = musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().getDuration();
				npEmbed.setAuthor("Now Playing", null, event.getGuild().getIconUrl())
						.setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png")
						.setDescription("\n\u23ef " + AudioCmdUtils.getProgressBar(now, total) + "\n\n" +
								"**[" + musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack()
								.getInfo().title + "]"
								+ "(" + musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack()
									.getInfo().uri + ")** "
								+ String.format("`(%s/%s)`", Utils.getDurationMinutes(now), Utils.getDurationMinutes(total)))
						.setFooter("Enjoy the music! <3", event.getAuthor().getAvatarUrl());

				event.getChannel().sendMessage(npEmbed.build()).queue();
				TextChannelGround.of(event).dropItemWithChance(0, 10);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Now Playing (np) Command")
						.addField("Description", "See what track is playing now.", false).build();
			}

		});
	}


	@Subscribe
	public void pause(CommandRegistry cr) {
		cr.register("pause", new SimpleCommand(Category.MUSIC) {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Pause Command")
						.addField("Description", "Pause or unpause the current track.", false)
						.addField("Usage:", "~>pause (if paused, I will unpause, vice versa)", false).build();
			}

			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState()
						.getChannel().equals(event
								.getGuild().getAudioManager().getConnectedChannel())) {
					sendNotConnectedToMyChannel(event.getChannel());
					return;
				}

				GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(
						event.getGuild());
				boolean paused = !musicManager.getTrackScheduler().getAudioPlayer().isPaused();
				String toSend = EmoteReference.MEGA + (paused ? "Player paused." : "Player unpaused.");
				musicManager.getTrackScheduler().getAudioPlayer().setPaused(paused);
				event.getChannel().sendMessage(toSend).queue();
				TextChannelGround.of(event).dropItemWithChance(0, 10);
			}
		});
	}

	@Subscribe
	public void play(CommandRegistry cr) {
		cr.register("play", new SimpleCommand(Category.MUSIC) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (content.trim().isEmpty()) {
					onHelp(event);
					return;
				}

				try {
					new URL(content);
				} catch (Exception e) {
					if (content.startsWith("soundcloud")) content = ("scsearch: " + content).replace("soundcloud ", "");
					else content = "ytsearch: " + content;
				}

				MantaroBot.getInstance().getAudioManager().loadAndPlay(event, content, false);
				TextChannelGround.of(event).dropItemWithChance(0, 5);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Play Command")
						.addField("Description", "Play songs!", false)
						.addField("Usage", "~>play <song url> (playlists and song names are also acceptable)", false)
						.addField(
								"Tip", "If you do ~>play <search term> I'll search youtube (default), " +
										"but if you do ~>play soundcloud <search term> It will search soundcloud (not for usage w/links).",
								false
						)
						.build();
			}
		});
	}

	@Subscribe
	public void forceplay(CommandRegistry cr) {
		cr.register("forceplay", new SimpleCommand(Category.MUSIC) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (content.trim().isEmpty()) {
					onHelp(event);
					return;
				}
				try {
					new URL(content);
				} catch (Exception e) {
					if (content.startsWith("soundcloud")) content = ("scsearch: " + content).replace("soundcloud ", "");
					else content = "ytsearch: " + content;
				}

				MantaroBot.getInstance().getAudioManager().loadAndPlay(event, content, true);
				TextChannelGround.of(event).dropItemWithChance(0, 5);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Forceplay Command")
						.addField("Description", "Play the first song I find in your search", false)
						.addField("Usage", "~>forceplay <song url> (playlists and song names are also acceptable)", false)
						.addField("Tip", "If you do ~>forceplay <search term> I'll search youtube (default), " +
										"but if you do ~>forceplay soundcloud <search term> It will search soundcloud (not for usage w/links).",
								false)
						.build();
			}
		});
	}

	@Subscribe
	public void rewind(CommandRegistry cr) {
		cr.register("rewind", new SimpleCommand(Category.MUSIC) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (args.length == 0) {
					onHelp(event);
					return;
				}
				GuildMusicManager manager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
				if (manager.getAudioPlayer().getPlayingTrack() == null) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "I'm not currently playing anything").queue();
					return;
				}
				if (isDJ(event.getMember())) {
					try {
						long amt = Utils.parseTime(args[0]);
						if (amt < 0) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Positive integers only").queue();
							return;
						}
						AudioTrack track = manager.getAudioPlayer().getPlayingTrack();
						long position = track.getPosition();
						if (position - amt < 0) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You can't skip past the beginning of a song").queue();
							return;
						}
						track.setPosition(position - amt);
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Rewound to: " + AudioUtils.getLength(position - amt) + ".").queue();
					} catch (NumberFormatException ex) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "You need to provide a valid number").queue();
					}
				} else
					event.getChannel().sendMessage(EmoteReference.ERROR + "You need to be a music DJ to use this command!").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Rewind Command")
						.addField("Description", "Rewind the current song a specified amount of seconds", false)
						.addField("Usage", "~>rewind <time>\nTime is in this format: 1m29s (1 minute and 29s)", false)
						.build();
			}
		});
	}

	@Subscribe
	public void skipahead(CommandRegistry cr) {
		cr.register("skipahead", new SimpleCommand(Category.MUSIC) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (args.length == 0) {
					onHelp(event);
					return;
				}
				GuildMusicManager manager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
				if (manager.getAudioPlayer().getPlayingTrack() == null) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "I'm not currently playing anything").queue();
					return;
				}
				if (isDJ(event.getMember())) {
					try {
						long amt = Utils.parseTime(args[0]);
						if (amt < 0) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Positive integers only").queue();
							return;
						}
						AudioTrack track = manager.getAudioPlayer().getPlayingTrack();
						long position = track.getPosition();
						if (position + amt > track.getDuration()) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You can't skip past the duration of a song").queue();
							return;
						}
						track.setPosition(position + amt);
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Skipped ahead to " + AudioUtils.getLength(position + amt) + ".").queue();
					} catch (NumberFormatException ex) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "You need to provide a valid query.").queue();
					}
				} else
					event.getChannel().sendMessage(EmoteReference.ERROR + "You need to be a music DJ to use this command!").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Skip Ahead Command")
						.addField("Description", "Fast forward the current song a specified amount of time", false)
						.addField("Usage", "~>skipahead <time>\nTime is in this format: 1m29s (1 minute and 29s)", false)
						.build();
			}
		});
	}


	@Subscribe
	public void queue(CommandRegistry cr) {
		cr.register("queue", new SimpleCommand(Category.MUSIC) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(
						event.getGuild());
				int page = 0;
				try {
					page = Math.max(Integer.parseInt(args[0]), 1);
				} catch (Exception ignored) {
				}

				if (content.startsWith("clear")) {
					if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState()
							.getChannel().equals
									(event.getGuild().getAudioManager().getConnectedChannel())) {
						sendNotConnectedToMyChannel(event.getChannel());
						return;
					}

					if (isDJ(event.getMember())) {
						event.getChannel().sendMessage(
								EmoteReference.CORRECT + "The server DJ has decided to clear the queue!").queue();
						int TEMP_QUEUE_LENGTH = musicManager.getTrackScheduler().getQueue().size();
						MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler()
								.getQueue().clear();
						event.getChannel().sendMessage(
								EmoteReference.CORRECT + "Removed **" + TEMP_QUEUE_LENGTH + " songs** from the queue" +
										".").queue();
						MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler()
								.stop();
						return;
					}

					event.getChannel().sendMessage(
							EmoteReference.ERROR + "Either you're not connected to the VC or you're not the DJ.").queue();
					return;
				}
				embedForQueue(page, event, musicManager);
				TextChannelGround.of(event).dropItemWithChance(0, 10);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Queue Command")
						.setDescription("**Either returns the current queue playing on the server or clears it.**")
						.addField("Usage:", "`~>queue` - **Shows the queue**\n" +
										"`~>queue clear` - **Clears the queue**",
								false
						)
						.addField("Considerations", "If music is playing at 2x speed please do `~>opts musicspeedup fix`", false)
						.build();
			}
		});

		cr.registerAlias("queue", "q");
	}

	@Subscribe
	public void removetrack(CommandRegistry cr) {
		cr.register("removetrack", new SimpleCommand(Category.MUSIC) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState()
						.getChannel().equals(event
								.getGuild().getAudioManager().getConnectedChannel())) {
					event.getChannel().sendMessage(
							EmoteReference.ERROR + "You are not connected to the voice channel I am currently " +
									"playing!").queue();
					return;
				}

				MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler()
						.getQueueAsList(list -> {
							TIntHashSet selected = new TIntHashSet();

							String last = Integer.toString(list.size() - 1);

							for (String param : args) {

								String arg = replaceEach(
										param,
										new String[]{"first", "next", "last", "all"},
										new String[]{"0", "0", last, "0-" + last}
								);

								if (arg.contains("-") || arg.contains("~")) {
									String[] range = content.split("[-~]");

									if (range.length != 2) {
										event.getChannel().sendMessage(
												EmoteReference.ERROR + "``" + param + "`` is not a valid range!").queue();
										return;
									}

									try {
										int iStart = Integer.parseInt(range[0]) - 1, iEnd = Integer.parseInt(range[1]) - 1;

										if (iStart < 0 || iStart >= list.size()) {
											event.getChannel().sendMessage(
													EmoteReference.ERROR + "There isn't a queued track at the position ``" +
															iStart + "``!").queue();
											return;
										}

										if (iEnd < 0 || iEnd >= list.size()) {
											event.getChannel().sendMessage(
													EmoteReference.ERROR + "There isn't a queued track at the position ``" +
															iEnd + "``!").queue();
											return;
										}

										selected.addAll(IntStream.rangeClosed(iStart, iEnd).toArray());
									} catch (NumberFormatException ex) {
										event.getChannel().sendMessage(
												EmoteReference.ERROR + "``" + param + "`` is not a valid number!").queue();
										return;
									}
								} else {
									try {
										int i = Integer.parseInt(content) - 1;

										if (i < 0 || i >= list.size()) {
											event.getChannel().sendMessage(
													EmoteReference.ERROR + "There isn't a queued track at the position ``"
															+ i + "``!").queue();
											return;
										}

										selected.add(i);
									} catch (NumberFormatException ex) {
										event.getChannel().sendMessage(
												EmoteReference.ERROR + "``" + arg + "`` is not a valid number or range!")
												.queue();
										return;
									}
								}
							}

							TIntIterator i = selected.iterator();
							while (i.hasNext()) list.remove(i.next());

							event.getChannel().sendMessage(
									EmoteReference.CORRECT +
											"Removed **" + selected.size() + "** track(s) from the queue."
							).queue();

							TextChannelGround.of(event).dropItemWithChance(0, 10);
						});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Remove Track Command")
						.setDescription("**Remove the specified track from the queue.**")
						.addField(
								"Usage:", "`~>removetrack <tracknumber/first/next/last>` (as specified on the ~>queue command)",
								false
						)
						.addField("Parameters:", "`tracknumber`: the number of the track to remove\n" +
								"`first`: remove the first track\n"
								+ "`next`: remove the next track\n"
								+ "`last`: remove the last track\n"
								+ "You can also specify a range (1-10, for example) to delete the first 10 tracks of the queue.", false)
						.build();
			}
		});
	}

	@Subscribe
	public void repeat(CommandRegistry cr) {
		cr.register("repeat", new SimpleCommand(Category.MUSIC) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState()
						.getChannel().equals(event
								.getGuild().getAudioManager().getConnectedChannel())) {
					sendNotConnectedToMyChannel(event.getChannel());
					return;
				}

				GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(
						event.getGuild());
				try {
					switch (args[0].toLowerCase()) {
						case "queue":
							if (musicManager.getTrackScheduler().getRepeatMode() == TrackScheduler.Repeat.QUEUE) {
								musicManager.getTrackScheduler().setRepeatMode(null);
								event.getChannel().sendMessage(
										EmoteReference.CORRECT + "Continuing with the current queue.").queue();
							} else {
								musicManager.getTrackScheduler().setRepeatMode(TrackScheduler.Repeat.QUEUE);
								event.getChannel().sendMessage(EmoteReference.CORRECT + "Repeating the current queue.")
										.queue();
							}
							break;
					}
				} catch (Exception e) {
					if (musicManager.getTrackScheduler().getRepeatMode() == TrackScheduler.Repeat.SONG) {
						musicManager.getTrackScheduler().setRepeatMode(null);
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Continuing with the normal queue.")
								.queue();
					} else {
						musicManager.getTrackScheduler().setRepeatMode(TrackScheduler.Repeat.SONG);
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Repeating the current song.").queue();
					}
				}

				TextChannelGround.of(event).dropItemWithChance(0, 10);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Repeat command")
						.setDescription("**Repeats a song.**")
						.addField("Usage", "`~>repeat` - **Toggles repeat**\n" +
								"`~>repeat queue` - **Repeats the entire queue**.", false)
						.addField(
								"Warning",
								"Might not work correctly if I leave the voice channel after you have disabled repeat. *To fix, just " +
										"add a song to the queue*", true
						)
						.build();
			}
		});
	}

	@Subscribe
	public void shuffle(CommandRegistry cr) {
		cr.register("shuffle", new SimpleCommand(Category.MUSIC) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState()
						.getChannel().equals(event
								.getGuild().getAudioManager().getConnectedChannel())) {
					sendNotConnectedToMyChannel(event.getChannel());
					return;
				}

				MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler()
						.shuffle();
				event.getChannel().sendMessage(EmoteReference.OK + "Randomized the order of the current queue.")
						.queue();
				TextChannelGround.of(event).dropItemWithChance(0, 10);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Shuffle Command")
						.addField("Description", "**Shuffle the current queue!**", false)
						.build();
			}
		});
	}

	@Subscribe
	public void skip(CommandRegistry cr) {
		cr.register("skip", new SimpleCommand(Category.MUSIC) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				try {

					if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState()
							.getChannel().equals
									(event.getGuild().getAudioManager().getConnectedChannel())) {
						sendNotConnectedToMyChannel(event.getChannel());
						return;
					}
					TrackScheduler scheduler = MantaroBot.getInstance().getAudioManager().getMusicManager(
							event.getGuild())
							.getTrackScheduler();
					if (scheduler.getCurrentTrack().getUserData() != null && String.valueOf(scheduler.getCurrentTrack().getUserData()).equals(
							event.getAuthor().getId())
							|| isDJ(event.getMember())) {
						event.getChannel().sendMessage(EmoteReference.CORRECT + "The DJ has decided to skip!").queue();
						scheduler.nextTrack(true, true);
						return;
					}
					List<String> voteSkips = scheduler.getVoteSkips();
					int requiredVotes = scheduler.getRequiredVotes();
					if (voteSkips.contains(event.getAuthor().getId())) {
						voteSkips.remove(event.getAuthor().getId());
						event.getChannel().sendMessage(EmoteReference.CORRECT + "**Your vote has been removed!** You need " +
								(requiredVotes - voteSkips.size()) + " more vote(s) to skip!").queue();
					} else {
						voteSkips.add(event.getAuthor().getId());
						if (voteSkips.size() >= requiredVotes) {
							event.getChannel().sendMessage(
									EmoteReference.CORRECT + "Reached the required amount of votes, skipping song...")
									.queue();
							scheduler.nextTrack(true, true);
							return;
						}
						event.getChannel().sendMessage(EmoteReference.OK + "**Your vote has been submitted!** You need " +
								(requiredVotes - voteSkips.size()) + " more vote(s) to skip!").queue();
					}
					TextChannelGround.of(event).dropItemWithChance(0, 10);
				} catch (NullPointerException e) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "There seems to be no track to skip, so I cannot do anything...").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Skip Command")
						.setDescription("**Stops the current track and continues to the next, if one exists.**")
						.build();
			}
		});
	}

	@Subscribe
	public void stop(CommandRegistry cr) {
		cr.register("stop", new SimpleCommand(Category.MUSIC) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				try {

					if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState()
							.getChannel().equals
									(event.getGuild().getAudioManager().getConnectedChannel())) {
						sendNotConnectedToMyChannel(event.getChannel());
						return;
					}

					TrackScheduler scheduler = MantaroBot.getInstance().getAudioManager().getMusicManager(
							event.getGuild())
							.getTrackScheduler();
					if (isDJ(event.getMember())) {
						event.getChannel().sendMessage(EmoteReference.CORRECT + "The server DJ has decided to stop!")
								.queue();
						stop(event);
						return;
					}

					List<String> stopVotes = scheduler.getVoteStop();
					int requiredVotes = scheduler.getRequiredVotes();
					if (stopVotes.contains(event.getAuthor().getId())) {
						stopVotes.remove(event.getAuthor().getId());
						event.getChannel().sendMessage(EmoteReference.CORRECT + "**Your vote has been removed!** You need " +
								(requiredVotes - stopVotes.size()) + " more vote(s) to stop!").queue();
					} else {
						stopVotes.add(event.getAuthor().getId());
						if (stopVotes.size() >= requiredVotes) {
							event.getChannel().sendMessage(
									EmoteReference.CORRECT + "Reached the required amount of votes, stopping player...")
									.queue();
							stop(event);
							return;
						}
						event.getChannel().sendMessage(EmoteReference.OK + "**Your vote has been submitted!** You need "
								+ (requiredVotes - stopVotes.size()) + " more vote(s) to stop!").queue();
					}
				} catch (NullPointerException e) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "There is no player to stop... I cannot stop anything without one.").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Stop Command")
						.setDescription("**Clears the queue and leaves the voice channel.**")
						.build();
			}
		});
	}

	@Subscribe
	public void volume(CommandRegistry cr) {
		cr.register("volume", new SimpleCommand(Category.MUSIC) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (MantaroData.db().getUser(event.getMember()).isPremium() ||
						MantaroData.db().getGuild(event.getMember()).isPremium() ||
						MantaroData.config().get().getOwners().contains(event.getAuthor().getId())) {

					if (!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState()
							.getChannel().equals(event.getGuild().getAudioManager().getConnectedChannel())) {
						sendNotConnectedToMyChannel(event.getChannel());
						return;
					}

					if (args.length == 0) {
						onError(event);
						return;
					}

					AudioPlayer player = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild())
							.getTrackScheduler().getAudioPlayer();

					if (args[0].equals("check")) {
						event.getChannel().sendMessage(
								EmoteReference.ZAP + "The current volume for this session is: " + player.getVolume() + "\n\n" +
										GuildStatsManager.bar(player.getVolume(), 50))
								.queue();
						return;
					}

					int volume;
					try {
						volume = Math.max(4, Math.min(100, Integer.parseInt(args[0])));
					} catch (Exception e) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid number.").queue();
						return;
					}
					player.setVolume(volume);
					event.getChannel().sendMessage(String.format(EmoteReference.OK + "Volume set to %d\n\n%s", volume,
							GuildStatsManager.bar(volume, 50)))
							.queue();
				} else {
					event.getChannel().sendMessage(
							EmoteReference.ERROR + "This is a premium-only feature. In order to get" +
									" donator benefits like this one you can pledge on patreon (https://www.patreon.com/mantaro). Thanks for understanding.\n" +
									"Premium features can be either bound to an user or a server, please, if you donate, join the support guild and ask for it.")
							.queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Volume command")
						.addField("Usage", "`~>volume <number>` - **Sets the volume**", false)
						.addField("Parameters", "`number` - **An integer between 1 and 100**", false)
						.addField("Notice", "**This is a *donator-only* feature!**" +
								"\nTo check the current volume, do `~>volume check. Minimum volume is 4.`", false)
						.build();
			}
		});
	}

	private boolean isDJ(Member member) {
		Role djRole = member.getGuild().getRolesByName("DJ", true).stream().findFirst().orElse(null);
		return member.isOwner() || member.hasPermission(Permission.MANAGE_SERVER) || member.hasPermission(Permission.ADMINISTRATOR) ||
				(djRole != null && member.getRoles().contains(djRole));
	}

	private void sendNotConnectedToMyChannel(MessageChannel channel) {
		channel.sendMessage(EmoteReference.ERROR + "You aren't connected to the voice channel I'm currently playing in!").queue();
	}

	/**
	 * This only fires on manual stop!
	 * @param event wha
	 */
	private void stop(GuildMessageReceivedEvent event) {
		GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
		TrackScheduler trackScheduler = musicManager.getTrackScheduler();
		if (trackScheduler.getAudioPlayer().getPlayingTrack() != null && !trackScheduler.getAudioPlayer().isPaused()) {
			trackScheduler.getAudioPlayer().getPlayingTrack().stop();
		}

		int TEMP_QUEUE_LENGTH = trackScheduler.getQueue().size();
		trackScheduler.getQueue().clear();

		if (TEMP_QUEUE_LENGTH > 0) {
			event.getChannel().sendMessage(EmoteReference.OK + "Removed **" + TEMP_QUEUE_LENGTH + " songs** from the queue.").queue();
		}

		//This ends up calling TrackScheduler#onTrackStart -> currentTrack == null -> TrackScheduler#onStop!
		//Beware to not close the connection twice...
		trackScheduler.nextTrack(true, true);
	}
}
