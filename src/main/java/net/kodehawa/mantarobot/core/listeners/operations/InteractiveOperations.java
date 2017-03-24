package net.kodehawa.mantarobot.core.listeners.operations;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.kodehawa.mantarobot.core.listeners.OptimizedListener;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class InteractiveOperations {
	private static final Map<String, InteractiveOperation> OPERATIONS = new ConcurrentHashMap<>();
	private static final EventListener LISTENER = new OptimizedListener<GuildMessageReceivedEvent>(GuildMessageReceivedEvent.class) {
		@Override
		public void event(GuildMessageReceivedEvent event) {
			String id = event.getChannel().getId();

			InteractiveOperation operation = OPERATIONS.get(id);

			if (operation != null) {
				if (operation.run(event)) {
					OPERATIONS.remove(id);
				} else {
					//TODO Extend the Timeout
				}
			}
		}
	};

	public static boolean create(TextChannel channel, InteractiveOperation operation) {
		Objects.requireNonNull(channel, "channel");
		Objects.requireNonNull(operation, "operation");

		String id = channel.getId();
		if (OPERATIONS.containsKey(id)) return false;
		OPERATIONS.put(id, operation);
		//TODO Add to a "Timeout Thread"
		return true;
	}

	public static EventListener listener() {
		return LISTENER;
	}
}
