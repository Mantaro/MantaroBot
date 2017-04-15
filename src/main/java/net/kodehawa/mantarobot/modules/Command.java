package net.kodehawa.mantarobot.modules;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public interface Command {
	Category category();

	MessageEmbed help(GuildMessageReceivedEvent event);

	boolean isHiddenFromHelp();

	CommandPermission permission();

	void run(GuildMessageReceivedEvent event, String commandName, String content);
}
