package net.kodehawa.mantarobot.modules.commands;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.commands.base.AbstractCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.StringUtils;

import static net.kodehawa.mantarobot.commands.info.CommandStatsManager.log;

public abstract class SimpleCommand extends AbstractCommand {
	public SimpleCommand(Category category) {
		super(category);
	}

	protected abstract void call(GuildMessageReceivedEvent event, String content, String[] args);

	@Override
	public void run(GuildMessageReceivedEvent event, String commandName, String content) {
		call(event, content, splitArgs(content));
		log(commandName);
	}

	@Override
	public CommandPermission permission() {
		return CommandPermission.USER;
	}

	protected String[] splitArgs(String content) {
		return StringUtils.advancedSplitArgs(content, 0);
	}
}
