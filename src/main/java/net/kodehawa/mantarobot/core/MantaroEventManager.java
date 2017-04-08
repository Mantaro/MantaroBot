package net.kodehawa.mantarobot.core;

import br.com.brjdevs.java.utils.extensions.Async;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.InterfacedEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MantaroEventManager extends InterfacedEventManager {
	public static final Logger LOGGER = LoggerFactory.getLogger("ShardWatcher");
	public long LAST_EVENT;

	public MantaroEventManager() {
		LAST_EVENT = System.currentTimeMillis();
	}

	@Override
	public void handle(Event event) {
		LAST_EVENT = System.currentTimeMillis();
		Async.thread("Async EventHandling", () -> super.handle(event));
	}

	public void clearListeners() {
		for (Object o : super.getRegisteredListeners())
			super.unregister(o);
	}

	public void handleSync(Event event) {
		LAST_EVENT = System.currentTimeMillis();
		super.handle(event);
	}
}
