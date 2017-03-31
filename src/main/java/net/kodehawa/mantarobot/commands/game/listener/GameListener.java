package net.kodehawa.mantarobot.commands.game.listener;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.listeners.external.OptimizedListener;

/**
 * @deprecated use {@link net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations} now
 */
@Deprecated
public class GameListener extends OptimizedListener<GuildMessageReceivedEvent> {

	public GameListener() {
		super(GuildMessageReceivedEvent.class);
	}

	@Override
	public void event(GuildMessageReceivedEvent event) {
		/*try {
			TextChannelWorld world = TextChannelWorld.of(event);
			if (world.getRunningGames().isEmpty()) return;
			if (world == null || player == null || player.getGame() == null) return; //it's not always false, trust me.

			world.getRunningGames().get(player).call(event, player);
		} catch (Exception ignored) {
		}*/
	}
}