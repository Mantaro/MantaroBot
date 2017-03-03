package net.kodehawa.mantarobot.commands.currency.game;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.currency.game.core.Game;
import net.kodehawa.mantarobot.commands.currency.game.core.GameReference;

public class Hangman extends Game {

	@Override
	public boolean onStart(GuildMessageReceivedEvent event, GameReference type, EntityPlayer player) {
		return false;
	}

	@Override
	public void call(GuildMessageReceivedEvent event, EntityPlayer player) {

	}
}
