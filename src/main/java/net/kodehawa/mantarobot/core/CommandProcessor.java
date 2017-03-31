package net.kodehawa.mantarobot.core;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Command;
import net.kodehawa.mantarobot.modules.Module.Manager;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public class CommandProcessor {
	protected Command getCommand(String name) {
		return Optional.of(Manager.commands.get(name)).map(Pair::getLeft).orElse(null);
	}

	public boolean run(GuildMessageReceivedEvent event) {
		if (MantaroBot.getInstance().getLoadStatus() != LoadState.POSTLOAD) return false;

		if (MantaroBot.getInstance().getLoadStatus() != LoadState.POSTLOAD) return false;
		if (MantaroData.db().getMantaroData().getBlackListedUsers().contains(event.getAuthor().getId())) return false;

		String rawCmd = event.getMessage().getRawContent();

		String defaultPrefix = MantaroData.config().get().prefix;
		//String prefix = MantaroData.db().getGuild(event.getGuild()).getData().getGuildCustomPrefix();

		if (rawCmd.startsWith(defaultPrefix)) rawCmd = rawCmd.substring(defaultPrefix.length());
			//else if (prefix != null && rawCmd.startsWith(prefix)) rawCmd = rawCmd.substring(prefix.length());
		else return false;

		String[] parts = splitArgs(rawCmd, 2);

		String cmdName = parts[0], content = parts[1];

		Command command = getCommand(cmdName);

		if (command == null) return false;

		if (!command.permissionRequired().test(event.getMember())) {
			event.getChannel().sendMessage(EmoteReference.STOP + "You have no permissions to trigger this command").queue();
			return false;
		}

		if (!event.getGuild().getSelfMember().getPermissions(event.getChannel()).contains(Permission.MESSAGE_EMBED_LINKS)) {
			event.getChannel().sendMessage(EmoteReference.STOP + "I require the permission ``Embed Links``. All Commands will be refused until you give me that permission.").queue();
			return false;
		}

		command.invoke(event, cmdName, content);

		return true;
	}
}