package net.kodehawa.mantarobot.commands.currency.game;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public interface Game {
	void call(GuildMessageReceivedEvent event, GameReference game);
}