package net.kodehawa.mantarobot.modules;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.CommandProcessor.Arguments;

public interface Command {
	CommandPermission permissionRequired();

	MessageEmbed help(GuildMessageReceivedEvent event);

	void invoke(Arguments cmd);

	boolean isHiddenFromHelp();
}
