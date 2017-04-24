package net.kodehawa.mantarobot.modules.commands;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.modules.commands.base.Command;

public class AliasCommand implements Command {
	private final Command command;
	private final String commandName;

	public AliasCommand(String commandName, Command command) {
		this.commandName = commandName;
		this.command = command;
	}

	@Override
	public Category category() {
		return command.category();
	}

	@Override
	public MessageEmbed help(GuildMessageReceivedEvent event) {
		return command.help(event);
	}

	@Override
	public CommandPermission permission() {
		return command.permission();
	}

	@Override
	public void run(GuildMessageReceivedEvent event, String ignored, String content) {
		command.run(event, commandName, content);
	}
}
