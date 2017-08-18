package net.kodehawa.mantarobot.core.modules.commands;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.base.AbstractCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.utils.StringUtils;

public abstract class SimpleCommand extends AbstractCommand {
	public SimpleCommand(Category category) {
		super(category);
	}

	public SimpleCommand(Category category, CommandPermission permission) {
		super(category, permission);
	}

	protected abstract void call(GuildMessageReceivedEvent event, String content, String[] args);

	@Override
	public void run(GuildMessageReceivedEvent event, String commandName, String content) {
		call(event, content, splitArgs(content));
	}

	protected String[] splitArgs(String content) {
		return StringUtils.advancedSplitArgs(content, 0);
	}
}
