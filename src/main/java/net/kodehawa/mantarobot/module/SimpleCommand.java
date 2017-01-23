package net.kodehawa.mantarobot.module;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.module.Parser.CommandArguments;

public abstract class SimpleCommand implements Command {
	protected abstract void onCommand(String[] args, String content, GuildMessageReceivedEvent event);

	@Override
	public void invoke(CommandArguments cmd) {
		onCommand(cmd.args, cmd.content, cmd.event);
	}

	@Override
	public boolean isHiddenFromHelp() {
		return false;
	}
}
