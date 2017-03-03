package net.kodehawa.mantarobot.commands.currency.game;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.currency.inventory.TextChannelGround;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

public interface Game {

	boolean onStart(GuildMessageReceivedEvent event, GameReference type, EntityPlayer player);

	void call(GuildMessageReceivedEvent event, EntityPlayer player);

	boolean check(GuildMessageReceivedEvent event, GameReference type);

	default void endGame(GuildMessageReceivedEvent event, EntityPlayer player, boolean isTimeout){
		player.setCurrentGame(null, event.getChannel());
		TextChannelGround.of(event.getChannel()).removeEntity(player);
		event.getJDA().removeEventListener(this);
		String toSend = isTimeout ? EmoteReference.THINKING + "No correct reply on 60 seconds, ending game." : EmoteReference.CORRECT + "Game has correctly ended.";
		event.getChannel().sendMessage(toSend).queue();
	}
}