package net.kodehawa.mantarobot.core;

import br.com.brjdevs.java.utils.extensions.Async;
import com.google.common.primitives.Ints;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.InterfacedEventManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.utils.UnsafeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class MantaroEventManager extends InterfacedEventManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("ShardWatcher");

    @Override
    public void handle(Event event) {
        Async.thread("Async EventHandling", () -> super.handle(event));
    }

    public void clearListeners() {
        for(Object o : super.getRegisteredListeners())
            super.unregister(o);
    }
    
    public void handleSync(Event event) {
        super.handle(event);
    }
}
