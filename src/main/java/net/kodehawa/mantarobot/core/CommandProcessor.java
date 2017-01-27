package net.kodehawa.mantarobot.core;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Module.Manager;

import java.util.ArrayList;
import java.util.Collections;

public final class CommandProcessor {
	public static class Arguments {
		public final String[] args;
		public final String cmdName;
		public final GuildMessageReceivedEvent event;
		public final String rawCommand;
		public final String[] splitBeheaded;
		public String content = "";

		public Arguments(GuildMessageReceivedEvent event, String cmdName, String[] args, String rawCommand, String[] splitBeheaded) {
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

		private Arguments() {
			args = null;
			rawCommand = null;
			event = null;
			cmdName = null;
			splitBeheaded = null;
		}

		public boolean onCommand() {
			if (MantaroBot.getStatus() != LoadState.POSTLOAD) return false;
			if (Manager.commands.containsKey(cmdName)) {
				Manager.commands.get(cmdName).getLeft().invoke(this);
				return true;
			}
			return false;
		}
	}

	private static final Arguments EMPTY = new Arguments() {
		@Override
		public boolean onCommand() {
			return false; //NOOP
		}
	};

	public static boolean run(GuildMessageReceivedEvent event) {
		if (MantaroBot.getStatus() != LoadState.POSTLOAD) return false;

		String cmd = event.getMessage().getRawContent();
		String defaultPrefix = MantaroData.getData().get().defaultPrefix;
		//String prefix = Parameters.getPrefixForServer(event.getGuild().getId());

		if (cmd.startsWith(defaultPrefix)) cmd = cmd.substring(defaultPrefix.length());
		//else if (cmd.startsWith(prefix)) cmd = cmd.substring(prefix.length());
		else return false;

		ArrayList<String> split = new ArrayList<>();
		String[] splitBeheaded = cmd.split(" ");
		Collections.addAll(split, splitBeheaded);

		String invoke = split.get(0);
		String[] args = new String[split.size() - 1];
		split.subList(1, split.size()).	toArray(args);
		return new Arguments(event, invoke, args, cmd, splitBeheaded).onCommand();
	}
}
