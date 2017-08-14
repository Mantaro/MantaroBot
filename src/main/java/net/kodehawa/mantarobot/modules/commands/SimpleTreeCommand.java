package net.kodehawa.mantarobot.modules.commands;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.commands.base.AbstractCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.modules.commands.base.Command;
import net.kodehawa.mantarobot.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.HashMap;
import java.util.Map;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public abstract class SimpleTreeCommand extends AbstractCommand {
	private Map<String, Command> subCommands = new HashMap<>();

	public SimpleTreeCommand(Category category, Map<String, Command> subCommands) {
		super(category);
		this.subCommands = subCommands;
	}

	public SimpleTreeCommand(Category category) {
		super(category);
	}

	public SimpleTreeCommand(Category category, CommandPermission permission) {
		super(category, permission);
	}

	public SimpleTreeCommand(Category category, CommandPermission permission, Map<String, Command> subCommands) {
		super(category, permission);
		this.subCommands = subCommands;
	}

	/**
	 * Handling for when the Sub-Command isn't found.
	 *
	 * @param event       the Event
	 * @param commandName the Name of the not-found command.
	 */
	public void onNotFound(GuildMessageReceivedEvent event, String mainCommand, String commandName){
		event.getChannel().sendMessage(String.format("%sNo subcommand `%s` found in the `%s` command! Help for this command will be shown below.", EmoteReference.ERROR, commandName, mainCommand)).queue();
		onHelp(event);
	}

	/**
	 * Invokes the command to be executed.
	 *
	 * @param event       the event that triggered the command
	 * @param commandName the command name that was used
	 * @param content     the arguments of the command
	 */
	@Override
	public void run(GuildMessageReceivedEvent event, String commandName, String content) {
		String[] args = splitArgs(content, 2);

		if(subCommands.isEmpty()){
			throw new IllegalArgumentException("No subcommands registered!");
		}

		Command command = subCommands.get(args[0]);
		if (command == null) {
			onNotFound(event, commandName, args[0]);
			return;
		}

		command.run(event, commandName + " " + args[0], args[1]);
	}

	public SimpleTreeCommand addSubCommand(String name, Command command){
		subCommands.put(name, command);
		return this;
	}

	public Map<String, Command> getSubCommands() {
		return subCommands;
	}
}
