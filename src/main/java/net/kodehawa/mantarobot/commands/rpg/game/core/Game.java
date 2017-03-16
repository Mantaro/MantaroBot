package net.kodehawa.mantarobot.commands.rpg.game.core;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.rpg.world.TextChannelWorld;
import net.kodehawa.mantarobot.core.listeners.OptimizedListener;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;

import java.util.Random;

public abstract class Game {

	public abstract void call(GuildMessageReceivedEvent event, EntityPlayer player);

	public abstract boolean onStart(GuildMessageReceivedEvent event, GameReference type, EntityPlayer player);

	public abstract GameReference type();

	public boolean check(GuildMessageReceivedEvent event) {
		return TextChannelWorld.of(event.getChannel()).getRunningGames().isEmpty();
	}

	public void endGame(GuildMessageReceivedEvent event, EntityPlayer player, Game game, boolean isTimeout) {
		TextChannelWorld.of(event.getChannel()).getRunningGames().clear();
		player.setCurrentGame(null, event.getChannel());
		String toSend = isTimeout ? EmoteReference.THINKING + "No correct reply on 120 seconds, ending game." : EmoteReference.CORRECT + "Game has correctly ended.";
		event.getChannel().sendMessage(toSend).queue();
	}

	protected void onError(Logger logger, GuildMessageReceivedEvent event, Game game, EntityPlayer player, Exception e) {
		event.getChannel().sendMessage(EmoteReference.ERROR + "We cannot start this game due to an unknown error. My owners have been notified.").queue();
		if (e == null) logger.error("Error while setting up/handling a game");
		else logger.error("Error while setting up/handling a game", e);
		endGame(event, player, game, false);
	}

	protected void onSuccess(EntityPlayer player, GuildMessageReceivedEvent event, Game game, double multiplier) {
		long moneyAward = (long) ((player.getMoney() * multiplier) + new Random().nextInt(350));
		event.getChannel().sendMessage(EmoteReference.OK + "That's the correct answer, you won " + moneyAward + " credits for this.").queue();
		player.addMoney(moneyAward);
		player.setCurrentGame(null, event.getChannel());
		player.save();
		endGame(event, player, game, false);
	}

	protected void onSuccess(EntityPlayer player, Game game, GuildMessageReceivedEvent event) {
		long moneyAward = (long) ((player.getMoney() * 0.1) + new Random().nextInt(350));
		event.getChannel().sendMessage(EmoteReference.OK + "That's the correct answer, you won " + moneyAward + " credits for this.").queue();
		player.addMoney(moneyAward);
		player.setCurrentGame(null, event.getChannel());
		player.save();
		endGame(event, player, game, false);
	}
}