package net.kodehawa.mantarobot.commands.moderation;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@FunctionalInterface
public interface Callable {
	boolean call(GuildMessageReceivedEvent event, String value);
}
