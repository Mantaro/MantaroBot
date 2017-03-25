package net.kodehawa.mantarobot.commands.custom.compiler;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;

public interface CompiledCustomCommand {
	void call(GuildMessageReceivedEvent event);

	List<String> source();
}
