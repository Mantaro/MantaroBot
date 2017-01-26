package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.audio.MantaroAudioManager;
import net.kodehawa.mantarobot.commands.custom.DeathTimer;
import net.kodehawa.mantarobot.commands.custom.Holder;
import net.kodehawa.mantarobot.commands.custom.TextChannelLock;
import net.kodehawa.mantarobot.core.CommandProcessor.Arguments;
import net.kodehawa.mantarobot.core.listeners.FunctionListener;
import net.kodehawa.mantarobot.data.Data.GuildData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.*;
import net.kodehawa.mantarobot.utils.GsonDataManager;
import net.kodehawa.oldmantarobot.util.GeneralUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.custom.Mapifier.dynamicResolve;
import static net.kodehawa.mantarobot.commands.custom.Mapifier.map;
import static net.kodehawa.mantarobot.commands.info.HelpUtils.forType;

public class CustomCmds extends Module {
	private static Logger LOGGER = LoggerFactory.getLogger("UserCommands");

	private Command customCommand = new Command() {

		private Random r = new Random();

		@Override
		public CommandType commandType() {
			return CommandType.USER;
		}

		private void handle(GuildMessageReceivedEvent event, String cmdName, String rawArgs, String[] args) {
			Map<String, GuildData> guilds = MantaroData.getData().get().guilds;
			if (!guilds.containsKey(event.getGuild().getId()) || !guilds.get(event.getGuild().getId()).customCommands.containsKey(cmdName))
				return;
			List<String> responses = guilds.get(event.getGuild().getId()).customCommands.get(cmdName);
			String response = responses.get(r.nextInt(responses.size()));

			Map<String, String> dynamicMap = new HashMap<>();
			map("event", dynamicMap, event);
			for (int i = 0; i < args.length; i++) dynamicMap.put("event.args" + i, args[i]);
			response = dynamicResolve(response, dynamicMap);

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
				event.getChannel().sendMessage(new EmbedBuilder().setDescription(response.substring(6)).setTitle(cmdName).setColor(event.getMember().getColor()).build()).queue();
				return;
			}

			if (response.startsWith("imgembed:")) {
				event.getChannel().sendMessage(new EmbedBuilder().setImage(response.substring(9)).setTitle(cmdName).setColor(event.getMember().getColor()).build()).queue();
				return;
			}

			event.getChannel().sendMessage(response).queue();
		}

		@Override
		public MessageEmbed help(GuildMessageReceivedEvent event) {
			return null;
		}

		@Override
		public void invoke(Arguments args) {
			handle(args.event, args.cmdName, args.rawCommand, args.args);
		}

		@Override
		public boolean isHiddenFromHelp() {
			return true;
		}

	};

	public CustomCmds() {
		super(Category.CUSTOM);

		super.register("custom", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if (args.length < 1) {
					onHelp(event);
					return;
				}

				String action = args[0];
				Map<String, List<String>> customCommands = MantaroData.getData().get().guilds.getOrDefault(event.getGuild().getId(), new GuildData()).customCommands;

				if (action.equals("list") || action.equals("ls")) {
					EmbedBuilder builder = new EmbedBuilder()
						.setAuthor("Commands for this guild", null, event.getGuild().getIconUrl())
						.setColor(event.getMember().getColor());
					if (customCommands.isEmpty()) {
						builder.setDescription("There is nothing here, just dust.");
					} else if (action.equals("detailed")) {
						builder.setDescription(forType(customCommands.keySet()));
					} else {
						customCommands.entrySet().stream().sorted(Comparator.comparing(Entry::getKey)).forEach(entry -> builder.addField(entry.getKey(), entry.getValue().stream().map(s -> " - ``" + s + "``").collect(Collectors.joining("\n")), false));
					}

					event.getChannel().sendMessage(builder.build()).queue();
					return;
				}

				if (action.equals("debug")) {
					if (event.getMember().isOwner() || MantaroData.getConfig().get().isOwner(event.getMember())) {
						event.getChannel().sendMessage(
							"Guild Commands Debug: " + GeneralUtils.paste(
								GsonDataManager.GSON.toJson(customCommands)
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

				if (action.equals("add") || action.equals("make")) {
					Runnable unlock = TextChannelLock.adquireLock(event.getChannel());
					if (unlock == null) {
						event.getChannel().sendMessage("\u274C There's already an Interactive Operation happening on this TextChannel.").queue();
						return;
					}
					event.getChannel().sendMessage("\uD83D\uDCDD Started **\"Creation of Custom Command ``" + cmd + "``\"**!\nSend ``&~>stop`` to stop creation **without saving**.\nSend ``&~>save`` to stop creation an **save the new Command**. Send any text beggining with ``&`` to be added to the Command Responses.\nThis Interactive Operation ends without saving if 60 seconds of inactivity.").queue();

					Holder<DeathTimer> timer = new Holder<>();
					List<String> responses = new ArrayList<>();

					FunctionListener f = new FunctionListener(event.getChannel().getId(), (fl, e) -> {
						if (!e.getAuthor().equals(event.getAuthor())) return false;

						String tmp = e.getMessage().getRawContent();
						if (!tmp.startsWith("&")) return false;
						String s = tmp.substring(1);

						if (s.startsWith("~>stop")) {
							timer.get().disarm().explode();
							event.getChannel().sendMessage("\u274C There's already an Interactive Operation happening on this TextChannel.").queue();
							unlock.run();
							return true;
						}

						if (s.startsWith("~>save")) {
							String arg = s.substring(6).trim();
							String saveTo = !arg.isEmpty() ? arg : cmd;
							if (responses.isEmpty()) {
								event.getChannel().sendMessage("\u274C No Responses were added. Stopping creation without saving...").queue();
							} else {
								customCommands.put(saveTo, responses);
								MantaroData.getData().update();
								event.getChannel().sendMessage("\u2705 Saved to command ``" + saveTo + "``!").queue();

							}
							timer.get().disarm().explode();
							unlock.run();
							return true;
						}

						responses.add(s);
						e.getMessage().addReaction("\u2705").queue();
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
				//TODO Not sure if I want to do this. Look later
				//REASON Code for operation might be at least 60 lines long.
//				if (action.equals("edit")) {
//
//					return;
//				}
				if (action.equals("remove") || action.equals("rm")) {
					if (customCommands.remove(cmd) != null) {
						MantaroData.getData().update();
						event.getChannel().sendMessage("\uD83D\uDCDD Removed Custom Command ``" + cmd + "``!").queue();
					} else {
						event.getChannel().sendMessage("\u274C There's no Custom Command ``" + cmd + "`` in this Guild.").queue();
					}
					return;
				}
				//TODO ADD a HELP
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null;
			}
		});
	}

	@Override
	public void onPostLoad() {
		Set<String> invalidCmds = new HashSet<>();
		MantaroData.getData().get().guilds.values().forEach(guildData -> guildData.customCommands.keySet().forEach(cmd -> {
			if (!Manager.commands.containsKey(cmd)) Manager.commands.put(cmd, Pair.of(customCommand, null));
			else invalidCmds.add(cmd);
		}));
		MantaroData.getData().get().guilds.values().forEach(d -> d.customCommands.keySet().removeAll(invalidCmds));
		MantaroData.getData().update();
	}
}
