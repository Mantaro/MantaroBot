package net.kodehawa.mantarobot.core.processor.core;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

/**
 * Remember that custom processor stuff that was planned for some stuff?
 * Yeah that's what's this for.
 */
public interface ICommandProcessor {
    boolean run(GuildMessageReceivedEvent event);
}
