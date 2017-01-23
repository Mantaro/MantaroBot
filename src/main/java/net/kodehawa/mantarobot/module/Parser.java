package net.kodehawa.mantarobot.module;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.guild.Parameters;
import net.kodehawa.mantarobot.module.Module.Manager;

import java.util.ArrayList;
import java.util.Collections;

public final class Parser {
	public class CommandArguments {
		public final String[] args;
		public final String cmdName;
		public final GuildMessageReceivedEvent event;
		public final String rawCommand;
		public final String[] splitBeheaded;
		public String content = "";

		public CommandArguments(GuildMessageReceivedEvent event, String cmdName, String[] args, String rawCommand, String[] splitBeheaded) {
			this.event = event;
			this.cmdName = cmdName;
			this.args = args;

			this.rawCommand = rawCommand;
			this.splitBeheaded = splitBeheaded;
			if (this.rawCommand.contains(" ")) {
				content = rawCommand.replace(splitBeheaded[0] + " ", "");
			} else {
				content = rawCommand.replace(splitBeheaded[0], "");
			}
		}

		private CommandArguments() {
			args = null;
			rawCommand = null;
			event = null;
			cmdName = null;
			splitBeheaded = null;
		}

		public boolean onCommand() {
			if (Manager.modules.containsKey(cmdName)) {
				Manager.modules.get(cmdName).invoke(this);

				return true;
			}
			return false;
		}
	}

	private final CommandArguments EMPTY = new CommandArguments() {
		@Override
		public boolean onCommand() {
			return false; //NOOP
		}
	};

	public CommandArguments parse(GuildMessageReceivedEvent event) {
		String cmd = event.getMessage().getRawContent();
		String defaultPrefix = Parameters.getDefaultPrefix();
		String prefix = Parameters.getPrefixForServer(event.getGuild().getId());

		if (cmd.startsWith(defaultPrefix)) cmd = cmd.substring(defaultPrefix.length());
		else if (cmd.startsWith(prefix)) cmd = cmd.substring(prefix.length());
		else return EMPTY;

		ArrayList<String> split = new ArrayList<>();
		String[] splitBeheaded = cmd.split(" ");
		Collections.addAll(split, splitBeheaded);

		String invoke = split.get(0);
		String[] args = new String[split.size() - 1];
		split.subList(1, split.size()).toArray(args);
		return new CommandArguments(event, invoke, args, cmd, splitBeheaded);
	}
}
