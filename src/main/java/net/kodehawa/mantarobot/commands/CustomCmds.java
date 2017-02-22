package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.inventory.TextChannelGround;
import net.kodehawa.mantarobot.commands.custom.DeathTimer;
import net.kodehawa.mantarobot.commands.custom.Holder;
import net.kodehawa.mantarobot.commands.custom.TextChannelLock;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.core.CommandProcessor.Arguments;
import net.kodehawa.mantarobot.core.listeners.FunctionListener;
import net.kodehawa.mantarobot.data.Data.GuildData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.*;
import net.kodehawa.mantarobot.utils.GsonDataManager;
import net.kodehawa.mantarobot.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.custom.Mapifier.dynamicResolve;
import static net.kodehawa.mantarobot.commands.custom.Mapifier.map;
import static net.kodehawa.mantarobot.commands.info.CommandStatsManager.log;
import static net.kodehawa.mantarobot.commands.info.HelpUtils.forType;
import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

public class CustomCmds extends Module {
	private static Logger LOGGER = LoggerFactory.getLogger("UserCommands");
	private final Command customCommand = new Command() {
		private Random r = new Random();

		@Override
		public MessageEmbed help(GuildMessageReceivedEvent event) {
			return null;
		}

		private void handle(GuildMessageReceivedEvent event, String cmdName, String[] args) {
			Map<String, GuildData> guilds = MantaroData.getData().get().guilds;
			if (!guilds.containsKey(event.getGuild().getId()) || !guilds.get(event.getGuild().getId()).customCommands.containsKey(cmdName))
				return;
			List<String> responses = guilds.get(event.getGuild().getId()).customCommands.get(cmdName);
			String response = responses.get(r.nextInt(responses.size()));

			if (response.contains("$(")) {
				Map<String, String> dynamicMap = new HashMap<>();
				map("event", dynamicMap, event);
				for (int i = 0; i < args.length; i++) dynamicMap.put("event.args" + i, args[i]);
				response = dynamicResolve(response, dynamicMap);
			}

			if (response.startsWith("play:")) {
				String toSend = response.substring(5);
				try {
					new URL(toSend);
				} catch (Exception e) {
					toSend = "ytsearch: " + toSend;
				}

				MantaroAudioManager.loadAndPlay(event, toSend);
				return;
			}

			if (response.startsWith("embed:")) {
				event.getChannel().sendMessage(new EmbedBuilder().setDescription(response.substring(6)).setTitle(cmdName, null).setColor(event.getMember().getColor()).build()).queue();
				return;
			}

			if (response.startsWith("imgembed:")) {
				event.getChannel().sendMessage(new EmbedBuilder().setImage(response.substring(9)).setTitle(cmdName, null).setColor(event.getMember().getColor()).build()).queue();
				return;
			}

			event.getChannel().sendMessage(response).queue();
		}

		@Override
		public CommandPermission permissionRequired() {
			return CommandPermission.USER;
		}

		@Override
		public void invoke(Arguments args) {
			handle(args.event, args.cmdName, SPLIT_PATTERN.split(args.content));
			log("custom command");
		}

		@Override
		public boolean isHiddenFromHelp() {
			return true;
		}

	};

	private final Pair<Command, Category> cmdPair = Pair.of(customCommand, null);

	public CustomCmds() {
		super(Category.CUSTOM);

		Pattern addPattern = Pattern.compile(";");

		super.register("custom", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (MantaroData.getData().get().getGuild(event.getGuild(), false).customCommandsAdminOnly && !event.getGuild().getMember(event.getAuthor()).hasPermission(Permission.ADMINISTRATOR)) {
					event.getChannel().sendMessage("This guild only accepts custom commands from administrators.").queue();
					return;
				}

				if (args.length < 1) {
					onHelp(event);
					return;
				}

				String action = args[0];
				Map<String, List<String>> customCommands = MantaroData.getData().get().getGuild(event.getGuild(), true).customCommands;

				if (action.equals("list") || action.equals("ls")) {
					EmbedBuilder builder = new EmbedBuilder()
						.setAuthor("Commands for this guild", null, event.getGuild().getIconUrl())
						.setColor(event.getMember().getColor());
					if (customCommands.isEmpty()) {
						builder.setDescription("There is nothing here, just dust.");
					} else if (args.length > 1 && args[1].equals("detailed")) {
						customCommands.entrySet().stream().sorted(Comparator.comparing(Entry::getKey)).forEach(entry -> builder.addField(entry.getKey(), entry.getValue().stream().map(s -> " - ``" + s + "``").collect(Collectors.joining("\n")), false));
					} else {
						builder.setDescription(forType(customCommands.keySet()));
					}

					event.getChannel().sendMessage(builder.build()).queue();
					return;
				}

				if (action.equals("debug")) {
					if (event.getMember().isOwner() || MantaroData.getConfig().get().isOwner(event.getMember())) {
						event.getChannel().sendMessage(
							"Guild Commands Debug: " + Utils.paste(
								GsonDataManager.GSON_PRETTY.toJson(customCommands)
							)
						).queue();
					} else {
						event.getChannel().sendMessage("\u274C You cannot do that, silly.").queue();
					}
					return;
				}

				if (action.equals("clear")) {
					if (!event.getMember().isOwner() && !MantaroData.getConfig().get().isOwner(event.getMember())) {
						event.getChannel().sendMessage("\u274C You cannot do that, silly.").queue();
						return;
					}

					if (customCommands.isEmpty()) {
						event.getChannel().sendMessage("\u274C There's no Custom Commands registered in this Guild.").queue();
					}
					int size = customCommands.size();
					customCommands.clear();
					MantaroData.getData().update();
					event.getChannel().sendMessage("\uD83D\uDCDD Cleared **" + size + " Custom Commands**!").queue();
					return;
				}

				if (args.length < 2) {
					onHelp(event);
					return;
				}

				String cmd = args[1];

				if (action.equals("make")) {
					Runnable unlock = TextChannelLock.adquireLock(event.getChannel());
					if (unlock == null) {
						event.getChannel().sendMessage("\u274C There's already an Interactive Operation happening on this channel.").queue();
						return;
					}
					event.getChannel().sendMessage("\uD83D\uDCDD Started **\"Creation of Custom Command ``" + cmd + "``\"**!\nSend ``&~>stop`` to stop creation **without saving**.\nSend ``&~>save`` to stop creation an **save the new Command**. Send any text beginning with ``&`` to be added to the Command Responses.\nThis Interactive Operation ends without saving after 60 seconds of inactivity.").queue();

					Holder<DeathTimer> timer = new Holder<>();
					List<String> responses = new ArrayList<>();

					FunctionListener f = new FunctionListener(event.getChannel().getId(), (fl, e) -> {
						if (!e.getAuthor().equals(event.getAuthor())) return false;

						String tmp = e.getMessage().getRawContent();
						if (!tmp.startsWith("&")) return false;
						String s = tmp.substring(1);

						if (s.startsWith("~>stop")) {
							timer.get().disarm().explode();
							event.getChannel().sendMessage("\u274C Command Creation canceled.").queue();
							unlock.run();
							return true;
						}

						if (s.startsWith("~>save")) {
							String arg = s.substring(6).trim();
							String saveTo = !arg.isEmpty() ? arg : cmd;

							if (Manager.commands.containsKey(saveTo) && !Manager.commands.get(saveTo).equals(cmdPair)) {
								event.getChannel().sendMessage("\u274C A command already exists with this name!").queue();
								return false;
							}

							if (responses.isEmpty()) {
								event.getChannel().sendMessage("\u274C No responses were added. Stopping creation without saving...").queue();
							} else {
								customCommands.put(saveTo, responses);
								Manager.commands.put(saveTo, cmdPair);
								MantaroData.getData().update();
								event.getChannel().sendMessage("\u2705 Saved to command ``" + saveTo + "``!").queue();
								TextChannelGround.of(event).dropWithChance(8,70);
							}
							timer.get().disarm().explode();
							unlock.run();
							return true;
						}

						responses.add(s);
						e.getMessage().addReaction("\u2705").queue();
						timer.get().reset();
						return false;
					});

					timer.accept(new DeathTimer(60000, () -> {
						MantaroBot.getJDA().removeEventListener(f);
						event.getChannel().sendMessage("\u274C Interactive Operation **\"Creation of Custom Command ``" + cmd + "``\"** expired due to inactivity.").queue();
						unlock.run();
					}));

					MantaroBot.getJDA().addEventListener(f);
					return;
				}

				if (action.equals("remove") || action.equals("rm")) {
					Optional.ofNullable(customCommands.remove(cmd))
						.ifPresent((command) -> {
							MantaroData.getData().update();
							if (customCommands.values().stream().flatMap(Collection::stream).noneMatch(cmd::equals)) {
								Manager.commands.remove(cmd);
							}
							event.getChannel().sendMessage("\uD83D\uDCDD Removed Custom Command ``" + cmd + "``!").queue();
						});
					if (cmd == null)
						event.getChannel().sendMessage("\u274C There's no such Custom Command in this Guild.").queue();
					return;
				}

				if (args.length < 3) {
					onHelp(event);
					return;
				}

				String value = args[2];

				if (action.equals("add")) {
					List<String> responses = Arrays.asList(addPattern.split(value));
					if (Manager.commands.containsKey(cmd) && !Manager.commands.get(cmd).equals(cmdPair)) {
						event.getChannel().sendMessage("\u274C A command already exists with this name!").queue();
						return;
					}

					Manager.commands.put(cmd, cmdPair);
					customCommands.put(cmd, responses);
					MantaroData.getData().update();
					event.getChannel().sendMessage(String.format("Added custom command ``%s`` with responses ``%s`` -> ``Guild: %s``", cmd, responses.stream().collect(Collectors.joining("``, ")), event.getGuild().getId())).queue();

					TextChannelGround.of(event).dropWithChance(8,90);
					return;
				}

				onHelp(event);
			}

			@Override
			protected String[] splitArgs(String content) {
				return SPLIT_PATTERN.split(content, 3);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "CustomCommand Manager")
					.addField("Description:", "Manages the Custom Commands of the Guild.", false)
					.addField(
						"Usage:",
						"`~>custom`: Shows this help\n" +
							"`~>custom <list|ls> [detailed]`: List all commands. If detailed is supplied, it prints the responses of each command.\n" +
							"`~>custom debug`: Gives a Hastebin of the Raw Custom Commands Data. **(OWNER-ONLY)**\n" +
							"`~>custom clear`: Remove all Custom Commands from this Guild. **(OWNER-ONLY)**\n" +
							"`~>custom add <name> <responses>`: Add a new Command with the response provided. (A list of modifiers can be found on [here](https://hastebin.com/xolijewitu.http)\n" +
							"`~>custom make <name>`: Starts a Interactive Operation to create a command with the specified name.\n" +
							"`~>custom <remove|rm> <name>`: Removes a command with an specific name.",
						false
					).build();
			}
		});
	}

	@Override
	public void onPostLoad() {
		MantaroData.getData().get().guilds.values().forEach((GuildData guildData) -> {
			guildData.customCommands.values().removeIf(List::isEmpty);
			guildData.customCommands.keySet().removeIf(Manager.commands::containsKey);
			guildData.customCommands.keySet().forEach(cmd -> Manager.commands.put(cmd, cmdPair));
		});
		MantaroData.getData().update();
	}
}
