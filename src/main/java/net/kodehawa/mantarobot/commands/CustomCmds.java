package net.kodehawa.mantarobot.commands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.custom.compiler.CompiledCustomCommand;
import net.kodehawa.mantarobot.commands.custom.compiler.CustomCommandCompiler;
import net.kodehawa.mantarobot.commands.rpg.world.TextChannelWorld;
import net.kodehawa.mantarobot.core.CommandProcessor.Arguments;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.CustomCommand;
import net.kodehawa.mantarobot.modules.*;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.info.CommandStatsManager.log;
import static net.kodehawa.mantarobot.commands.info.HelpUtils.forType;
import static net.kodehawa.mantarobot.data.MantaroData.db;
import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

@Slf4j
public class CustomCmds extends Module {
	private Map<String, CompiledCustomCommand> compiledCommands = new ConcurrentHashMap<>();
	private final Command customCommand = new Command() {
		private Random r = new Random();

		@Override
		public MessageEmbed help(GuildMessageReceivedEvent event) {
			return null;
		}

		private void handle(String cmdName, GuildMessageReceivedEvent event) {
			CompiledCustomCommand consumer = compiledCommands.get(event.getGuild().getId() + ":" + cmdName);
			if (consumer == null) return;
			consumer.call(event);
		}

		@Override
		public CommandPermission permissionRequired() {
			return CommandPermission.USER;
		}

		@Override
		public void invoke(Arguments args) {
			try {
				handle(args.cmdName, args.event);
			} catch (Exception e) {
				log.error("An exception occurred while processing a custom command:", e);
			}
			log("custom command");
		}

		@Override
		public boolean isHiddenFromHelp() {
			return true;
		}
	};
	private final Pair<Command, Category> cmdPair = Pair.of(customCommand, null);

	public CustomCmds() {
		super(Category.UTILS);

		Pattern addPattern = Pattern.compile(";");

		super.register("custom", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (args.length < 1) {
					onHelp(event);
					return;
				}

				String action = args[0];

				if (action.equals("list") || action.equals("ls")) {
					String filter = event.getGuild().getId() + ":";
					List<String> commands = compiledCommands.keySet().stream()
						.filter(s -> s.startsWith(filter))
						.map(s -> s.substring(filter.length()))
						.collect(Collectors.toList());

					EmbedBuilder builder = new EmbedBuilder()
						.setAuthor("Commands for this guild", null, event.getGuild().getIconUrl())
						.setColor(event.getMember().getColor());
					builder.setDescription(commands.isEmpty() ? "There is nothing here, just dust." : forType(commands));

					event.getChannel().sendMessage(builder.build()).queue();
					return;
				}

				if (db().getGuild(event.getGuild()).getData().getCustomAdminLock() && !CommandPermission.ADMIN.test(event.getMember())) {
					event.getChannel().sendMessage("This guild only accepts custom commands from administrators.").queue();
					return;
				}

				if (action.equals("clear")) {
					if (CommandPermission.ADMIN.test(event.getMember())) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot do that, silly.").queue();
						return;
					}

					List<CustomCommand> customCommands = db().getCustomCommands(event.getGuild());

					if (customCommands.isEmpty()) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "There's no Custom Commands registered in this Guild.").queue();
					}
					int size = customCommands.size();
					customCommands.forEach(CustomCommand::deleteAsync);
					customCommands.forEach(c -> compiledCommands.remove(c.getId()));
					event.getChannel().sendMessage(EmoteReference.PENCIL + "Cleared **" + size + " Custom Commands**!").queue();
					return;
				}

				if (args.length < 2) {
					onHelp(event);
					return;
				}

				String cmd = args[1];

				if (action.equals("make")) {
					List<String> responses = new ArrayList<>();
					boolean created = InteractiveOperations.create(event.getChannel(), e -> {
						if (!e.getAuthor().equals(event.getAuthor())) return false;

						String c = e.getMessage().getRawContent();
						if (!c.startsWith("&")) return false;
						c = c.substring(1);

						if (c.startsWith("~>cancel") || c.startsWith("~>stop")) {
							event.getChannel().sendMessage(EmoteReference.CORRECT + "Command Creation canceled.").queue();
							return true;
						}

						if (c.startsWith("~>save")) {
							String arg = c.substring(6).trim();
							String saveTo = !arg.isEmpty() ? arg : cmd;

							if (Manager.commands.containsKey(saveTo) && !Manager.commands.get(saveTo).equals(cmdPair)) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "A command already exists with this name!").queue();
								return false;
							}

							if (responses.isEmpty()) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "No responses were added. Stopping creation without saving...").queue();
							} else {
								CustomCommand custom = CustomCommand.of(event.getGuild().getId(), saveTo, responses);

								//save at DB
								custom.saveAsync();

								//reflect at compiled
								compiledCommands.put(custom.getId(), CustomCommandCompiler.compile(custom.getValues()));

								//add mini-hack
								Manager.commands.put(saveTo, cmdPair);

								event.getChannel().sendMessage(EmoteReference.CORRECT + "Saved to command ``" + saveTo + "``!").queue();

								//easter egg :D
								TextChannelWorld.of(event).dropItemWithChance(8, 2);
							}
							return true;
						}

						responses.add(c);
						e.getMessage().addReaction(EmoteReference.CORRECT.getUnicode()).queue();
						return false;
					});

					if (created) {
						event.getChannel().sendMessage(EmoteReference.PENCIL + "Started **\"Creation of Custom Command ``" + cmd + "``\"**!\nSend ``&~>stop`` to stop creation **without saving**.\nSend ``&~>save`` to stop creation an **save the new Command**. Send any text beginning with ``&`` to be added to the Command Responses.\nThis Interactive Operation ends without saving after 60 seconds of inactivity.").queue();
					} else {
						event.getChannel().sendMessage(EmoteReference.ERROR + "There's already an Interactive Operation happening on this channel.").queue();
					}
				}

				if (action.equals("remove") || action.equals("rm")) {
					CustomCommand custom = db().getCustomCommand(event.getGuild(), cmd);
					if (custom == null) {
						event.getChannel().sendMessage(EmoteReference.ERROR2 + "There's no Custom Command ``" + cmd + "`` in this Guild.").queue();
						return;
					}

					//delete at DB
					custom.deleteAsync();

					//reflect at compiled
					compiledCommands.remove(custom.getId());

					//clear commands if none
					if (compiledCommands.keySet().stream().noneMatch(s -> s.endsWith(":" + cmd)))
						Manager.commands.remove(cmd);

					event.getChannel().sendMessage(EmoteReference.PENCIL + "Removed Custom Command ``" + cmd + "``!").queue();

					return;
				}

				if (action.equals("raw")) {
					//TODO AdrianTodt
					return;
				}

				if (action.equals("import")) {
					Map<String, Guild> mapped = MantaroBot.getInstance().getMutualGuilds(event.getAuthor()).stream()
						.collect(Collectors.toMap(ISnowflake::getId, g -> g));

					String any = "[\\d\\D]*?";

					List<Pair<Guild, CustomCommand>> filtered = MantaroData.db()
						.getCustomCommandsByName(any + Pattern.quote(cmd) + any).stream()
						.map(customCommand -> {
							Guild guild = mapped.get(customCommand.getGuildId());
							return guild == null ? null : Pair.of(guild, customCommand);
						})
						.filter(Objects::nonNull)
						.collect(Collectors.toList());

					if (filtered.size() == 0) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "There are no custom commands matching your search query.").queue();
						return;
					}

					DiscordUtils.selectList(
						event, filtered,
						pair -> "``" + pair.getValue().getName() + "`` - Guild: ``" + pair.getKey() + "``",
						s -> baseEmbed(event, "Select the Command:").setDescription(s).setFooter("(You can only select custom commands from guilds that you are a member of)", null).build(),
						pair -> {
							String cmdName = pair.getValue().getName();
							List<String> responses = pair.getValue().getValues();

							CustomCommand custom = CustomCommand.of(event.getGuild().getId(), cmdName, responses);

							//save at DB
							custom.saveAsync();

							//reflect at compiled
							compiledCommands.put(custom.getId(), CustomCommandCompiler.compile(custom.getValues()));

							event.getChannel().sendMessage(String.format("Imported custom command ``%s`` from guild `%s` with responses ``%s``", cmdName, pair.getKey().getName(), String.join("``, ``", responses))).queue();

							//easter egg :D
							TextChannelWorld.of(event).dropItemWithChance(8, 2);
						}
					);

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
						event.getChannel().sendMessage(EmoteReference.ERROR + "A command already exists with this name!").queue();
						return;
					}

					CustomCommand custom = CustomCommand.of(event.getGuild().getId(), cmd, responses);

					//save at DB
					custom.saveAsync();

					//reflect at compiled
					compiledCommands.put(custom.getId(), CustomCommandCompiler.compile(custom.getValues()));

					//add mini-hack
					Manager.commands.put(cmd, cmdPair);

					event.getChannel().sendMessage(EmoteReference.CORRECT + "Saved to command ``" + cmd + "``!").queue();

					//easter egg :D
					TextChannelWorld.of(event).dropItemWithChance(8, 2);
					return;
				}

				onHelp(event);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			protected String[] splitArgs(String content) {
				return SPLIT_PATTERN.split(content, 3);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "CustomCommand Manager")
					.addField("Description:", "Manages the Custom Commands of the Guild.", false)
					.addField(
						"Usage:",
						"`~>custom`: Shows this help\n" +
							"`~>custom <list|ls> [detailed]`: List all commands. If detailed is supplied, it prints the responses of each command.\n" +
							"`~>custom debug`: Gives a Hastebin of the Raw Custom Commands Data. **(OWNER-ONLY)**\n" +
							"`~>custom clear`: Remove all Custom Commands from this Guild. **(OWNER-ONLY)**\n" +
							"`~>custom add <name> <responses>`: Add a new Command with the response provided. (A list of modifiers can be found on [here](https://hastebin.com/xolijewitu.http)\n" +
							"`~>custom make <name>`: Starts a Interactive Operation to create a command with the specified name.\n" +
							"`~>custom <remove|rm> <name>`: Removes a command with an specific name.\n" +
							"`~>custom import <search>`: Imports a command from another guild you're in.",
						false
					).build();
			}
		});
	}

	@Override
	public void onPostLoad() {
		db().getCustomCommands().forEach(custom -> {
			if (Manager.commands.containsKey(custom.getName())) {
				custom.deleteAsync();
				custom = CustomCommand.of(custom.getGuildId(), "_" + custom.getName(), custom.getValues());
				custom.saveAsync();
			}

			compiledCommands.put(custom.getId(), CustomCommandCompiler.compile(custom.getValues()));
		});
	}
}
