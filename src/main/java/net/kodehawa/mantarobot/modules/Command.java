package net.kodehawa.mantarobot.modules;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public interface Command {
	MessageEmbed help(GuildMessageReceivedEvent event);

	void invoke(GuildMessageReceivedEvent event, String cmdName, String content);

	boolean isHiddenFromHelp();

	CommandPermission permissionRequired();
}
