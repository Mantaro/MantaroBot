package net.kodehawa.mantarobot.core.listeners.operations.old;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@FunctionalInterface
public interface InteractiveOperationListener extends OperationListener {
    int run(GuildMessageReceivedEvent event);
}
