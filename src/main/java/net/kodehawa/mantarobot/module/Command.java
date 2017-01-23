package net.kodehawa.mantarobot.module;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.module.Parser.CommandArguments;

public abstract class Command {
	public abstract CommandType commandType();

	public abstract String help();

	protected abstract void onCommand(String[] args, String content, GuildMessageReceivedEvent event);

	public void invoke(CommandArguments cmd) {
		onCommand(cmd.args, cmd.content, cmd.event);
	}

	public boolean isHiddenFromHelp() {
		return false;
	}
}
