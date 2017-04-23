package net.kodehawa.mantarobot.core;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.InterfacedEventManager;
import org.slf4j.Logger;

@Slf4j
public class MantaroEventManager extends InterfacedEventManager {
	public static Logger getLog() {
		return log;
	}

	public long LAST_EVENT;

	@Override
	public void handle(Event event) {
		LAST_EVENT = System.currentTimeMillis();
		super.handle(event);
	}
}