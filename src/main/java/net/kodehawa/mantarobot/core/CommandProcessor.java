package net.kodehawa.mantarobot.core;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Command;
import net.kodehawa.mantarobot.modules.Module.Manager;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public final class CommandProcessor {
	public static class Arguments {
		public final String[] args;
		public final String cmdName;
		public final String content;
		public final GuildMessageReceivedEvent event;

		private Arguments(GuildMessageReceivedEvent event, String cmdName, String content, String[] args) {
			this.event = event;
			this.cmdName = cmdName;
			this.args = args;
			this.content = content;
		}
	}

	private static boolean dispatchCommand(Arguments arguments) {
		if (MantaroBot.getStatus() != LoadState.POSTLOAD) return false;
		if (Manager.commands.containsKey(arguments.cmdName)) {
			Command command = Manager.commands.get(arguments.cmdName).getLeft();
			if (!command.permissionRequired().test(arguments.event.getMember())) return false;
			command.invoke(arguments);
			return true;
		}
		return false;
	}

	public static boolean run(GuildMessageReceivedEvent event) {
		if (MantaroBot.getStatus() != LoadState.POSTLOAD) return false;

		String rawCmd = event.getMessage().getRawContent();
		String defaultPrefix = MantaroData.getData().get().defaultPrefix;
		String prefix = MantaroData.getData().get().getPrefix(event.getGuild());

		if (rawCmd.startsWith(defaultPrefix)) rawCmd = rawCmd.substring(defaultPrefix.length());
		else if (rawCmd.startsWith(prefix)) rawCmd = rawCmd.substring(prefix.length());
		else return false;

		String[] parts = splitArgs(rawCmd, 2);

		return dispatchCommand(new Arguments(event, parts[0], parts[1], parts[1].split("\\s+")));
	}
}