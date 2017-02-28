package net.kodehawa.mantarobot.commands;

import bsh.Interpreter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

public class OwnerCmds extends Module {
	private static final Logger LOGGER = LoggerFactory.getLogger("Owner");
	public static GuildMessageReceivedEvent tempEvt = null;

	public OwnerCmds() {
		super(Category.OWNER);
		add();
		eval();
		notifyMusic();
		shutdown();
		blacklist();
		owner();
	}

	private void add() {
		super.register("varadd", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				String v = splitArgs(content)[1];

				switch (args[0]) {
					case "pat":
						MantaroData.getPatting().get().add(v);
						MantaroData.getPatting().update();
						event.getChannel().sendMessage("Added to pat list: " + v).queue();
						break;
					case "hug":
						MantaroData.getHugs().get().add(v);
						MantaroData.getHugs().update();
						event.getChannel().sendMessage("Added to hug list: " + v).queue();
						break;
					case "greeting":
						MantaroData.getGreeting().get().add(content.replace("greeting ", ""));
						MantaroData.getGreeting().update();
						event.getChannel().sendMessage("Added to greet list: " + content.replace("greeting ", "")).queue();
						break;
					case "splash":
						MantaroData.getSplashes().get().add(content.replace("splash ", ""));
						MantaroData.getSplashes().update();
						event.getChannel().sendMessage("Added to splash list: " + content.replace("splash ", "")).queue();
						break;
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Add to list command")
					.setDescription("Adds a parameter to a list."
						+ "\n Arguments: \n pat <args[1]>, hug <args[1]>, greeting <content>, splash <content>")
					.build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.BOT_OWNER;
			}
		});
	}

	private void blacklist() {
		super.register("blacklist", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (args[0].equals("guild")) {
					if (args[1].equals("add")) {
						if (event.getJDA().getGuildById(args[2]) == null) return;
						MantaroData.getData().get().blacklistedGuilds.add(args[2]);
						event.getChannel().sendMessage("Blacklisted Guild: " + event.getJDA().getGuildById(args[2])).queue();
						MantaroData.getData().update();
					} else if (args[1].equals("remove")) {
						MantaroData.getData().get().blacklistedGuilds.remove(args[2]);
						event.getChannel().sendMessage("Unblacklisted Guild: " + args[2]).queue();
						MantaroData.getData().update();
					}
					return;
				}

				if (args[0].equals("user")) {
					if (args[1].equals("add")) {
						if (event.getJDA().getUserById(args[2]) == null) return;
						MantaroData.getData().get().blacklistedUsers.add(args[2]);
						event.getChannel().sendMessage("Blacklisted User: " + event.getJDA().getUserById(args[2])).queue();
						MantaroData.getData().update();
					} else if (args[1].equals("remove")) {
						if (event.getJDA().getUserById(args[2]) == null) return;
						MantaroData.getData().get().blacklistedUsers.remove(args[2]);
						event.getChannel().sendMessage("Unblacklisted User: " + event.getJDA().getUserById(args[2])).queue();
						MantaroData.getData().update();
					}
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Blacklist command")
					.setDescription("Blacklists a user (user argument) or a guild (guild argument) by id.")
					.build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.BOT_OWNER;
			}
		});
	}

	private void eval() {
		super.register("eval", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (!MantaroData.getConfig().get().owners.contains(event.getAuthor().getId())) {
					return;
				}
				if (args[0].equals("js")) {
					ScriptEngine script = new ScriptEngineManager().getEngineByName("nashorn");
					script.put("jda", event.getJDA());
					script.put("event", event);
					script.put("guild", event.getGuild());
					script.put("channel", event.getChannel());
					String evalString = content.replaceAll(args[0], "");
					Object toSend;
					EmbedBuilder embedBuilder = new EmbedBuilder().setAuthor("Eval", null, event.getAuthor().getAvatarUrl()).setFooter("Asked by " + event.getAuthor().getName(), null);
					try {
						script.eval("load(\"nashorn:mozilla_compat.js\"); imports = new JavaImporter(java.util, java.io, java.net)\n");
						toSend = script.eval("(function() {" +
							"with(imports) {"
							+ evalString + "\n}" +
							"})()");
					} catch (ScriptException e) {
						toSend = e.getMessage();
						embedBuilder.setDescription(toSend.toString());
					}
					String out = toSend == null ? "Executed with no errors and no returns" : toSend.toString();
					embedBuilder.setDescription(out);

					event.getChannel().sendMessage(embedBuilder.build()).queue();
					return;
				}

				tempEvt = event;
				try {
					Interpreter interpreter = new Interpreter();
					String evalHeader =
						"import *; "
							+ "private JDAImpl jda = MantaroBot.getJDA(); "
							+ "private GuildMessageReceivedEvent evt = net.kodehawa.mantarobot.commands.OwnerCmds.tempEvt;";
					Object toSendTmp = interpreter.eval(evalHeader + content);
					EmbedBuilder embed = new EmbedBuilder();
					String toSend = toSendTmp == null ? "Executed successfully with no objects returned" : toSendTmp.toString();
					embed.setAuthor("Executed eval with success", null, event.getAuthor().getAvatarUrl())
						.setDescription(toSend)
						.setFooter("Asked by: " + event.getAuthor().getName(), null);
					event.getChannel().sendMessage(embed.build()).queue();
				} catch (Exception e) {
					event.getChannel().sendMessage("Code evaluation returned ``" + e.getClass().getSimpleName() + "``, with cause:" +
						" ```md\n" + e.getMessage().replaceAll("``", "").replaceAll(": ", ":\n") + "```").queue();
					LOGGER.warn("Problem evaluating code!", e);
				}
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.BOT_OWNER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Eval command")
					.setDescription("Guess what, it evals (js for javascript and no arguments for normal java).")
					.build();
			}
		});
	}

	public CompletableFuture<Void> notifyMusic(String content) {
		return CompletableFuture.allOf(MantaroBot.getAudioManager().getMusicManagers().values()
			.stream()
			.filter(musicManager -> musicManager.getTrackScheduler().getCurrentTrack() != null)
			.filter(musicManager -> musicManager.getTrackScheduler().getCurrentTrack().getRequestedChannel() != null)
			.filter(musicManager -> musicManager.getTrackScheduler().getCurrentTrack().getRequestedChannel().canTalk())
			.map(musicManager -> musicManager.getTrackScheduler().getCurrentTrack().getRequestedChannel().sendMessage(content).submit())
			.map(future -> (CompletableFuture<Message>) future)
			.toArray(CompletableFuture[]::new));
	}

	private void notifyMusic() {
		super.register("notifymusic", new SimpleCommand() {
			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.BOT_OWNER;
			}

			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				MantaroBot.getAudioManager().getMusicManagers().values()
					.forEach(musicManager -> {
						if (musicManager.getTrackScheduler().getCurrentTrack() != null && musicManager.getTrackScheduler().getCurrentTrack().getRequestedChannel() != null && musicManager.getTrackScheduler().getCurrentTrack().getRequestedChannel().canTalk())
							musicManager.getTrackScheduler().getCurrentTrack().getRequestedChannel().sendMessage(content).queue();
					});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "MusicNotify")
					.setDescription("Notifies the Audio People of something.")
					.build();
			}
		});
	}

	private void owner() {
		super.register("owner", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (args.length < 1) {
					onHelp(event);
					return;
				}

				String option = args[0];

				if (option.equals("shutdown") || option.equals("restart")) {
					if (args.length == 2) {
						try {
							notifyMusic(args[1]).get();
						} catch (InterruptedException | ExecutionException ignored) {
						}
					}

					System.exit(option.equals("restart") ? 15 : 0);
					return;
				}

				if (args.length < 2) {
					onHelp(event);
					return;
				}

				String value = args[1];

				if(option.equals("notifymusic")) {
					notifyMusic(args[1]);
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null;
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.BOT_OWNER;
			}

			@Override
			protected String[] splitArgs(String content) {
				return SPLIT_PATTERN.split(content, 2);
			}
		});
	}

	private void shutdown() {
		super.register("shutdown", new SimpleCommand() {
			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.BOT_OWNER;
			}

			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (!MantaroData.getConfig().get().owners.contains(event.getAuthor().getId())) {
					event.getChannel().sendMessage("Seems like you cannot do that, you silly <3").queue();
					return;
				}

				try {
					shutdown(event);
				} catch (Exception e) {
					LOGGER.warn("Couldn't shut down." + e.toString(), e);
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Shutdown")
					.setDescription("Shutdowns the bot.")
					.build();
			}
		});
	}

	private synchronized void shutdown(GuildMessageReceivedEvent event) {
		MantaroData.getData().update();
		MantaroBot.getAudioManager().getMusicManagers().forEach((s, musicManager) -> {
			musicManager.getTrackScheduler().stop();
		});

		MantaroBot.getJDA().getRegisteredListeners().forEach(listener -> MantaroBot.getJDA().removeEventListener(listener));
		event.getChannel().sendMessage("*goes to sleep*").complete();
		MantaroBot.getJDA().shutdownNow(true);
		System.exit(0);
	}
}
