package net.kodehawa.mantarobot.commands.currency.game;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.currency.inventory.TextChannelGround;
import net.kodehawa.mantarobot.core.listeners.OptimizedListener;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

public abstract class Game extends OptimizedListener<GuildMessageReceivedEvent> {

	Game() {
		super(GuildMessageReceivedEvent.class);
	}

	abstract boolean onStart(GuildMessageReceivedEvent event, GameReference type, EntityPlayer player);

	abstract void call(GuildMessageReceivedEvent event, EntityPlayer player);

	public boolean check(GuildMessageReceivedEvent event, GameReference type){
		if(type == null) return true;

		return !TextChannelGround.of(event.getChannel()).getRunningGames().containsKey(type);
	}

	void endGame(GuildMessageReceivedEvent event, EntityPlayer player, boolean isTimeout){
		player.setCurrentGame(null, event.getChannel());
		TextChannelGround.of(event.getChannel()).removeEntity(player);
		event.getJDA().removeEventListener(this);
		String toSend = isTimeout ? EmoteReference.THINKING + "No correct reply on 60 seconds, ending game." : EmoteReference.CORRECT + "Game has correctly ended.";
		event.getChannel().sendMessage(toSend).queue();
	}

	@Override
	public void event(GuildMessageReceivedEvent event){
		call(event, EntityPlayer.getPlayer(event.getAuthor()));
	}
}