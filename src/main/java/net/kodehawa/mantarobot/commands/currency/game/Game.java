package net.kodehawa.mantarobot.commands.currency.game;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.commands.currency.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.currency.inventory.TextChannelGround;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

public abstract class Game extends ListenerAdapter {

	abstract boolean onStart(GuildMessageReceivedEvent event, GameReference type, EntityPlayer player);

	abstract void call(GuildMessageReceivedEvent event, EntityPlayer player);

	abstract boolean check(GuildMessageReceivedEvent event, GameReference type);

	void endGame(GuildMessageReceivedEvent event, EntityPlayer player, boolean isTimeout){
		player.setCurrentGame(null, event.getChannel());
		TextChannelGround.of(event.getChannel()).removeEntity(player);
		event.getJDA().removeEventListener(this);
		String toSend = isTimeout ? EmoteReference.THINKING + "No correct reply on 60 seconds, ending game." : EmoteReference.CORRECT + "Game has correctly ended.";
		event.getChannel().sendMessage(toSend).queue();
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event){
		call(event, EntityPlayer.getPlayer(event.getAuthor()));
	}
}