package net.kodehawa.mantarobot.core;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.InterfacedEventManager;
import org.slf4j.Logger;

@Slf4j
public class MantaroEventManager extends InterfacedEventManager {
    private long LAST_JDA_EVENT;

    public static Logger getLog() {
        return log;
    }

    @Override
    public void handle(Event event) {
        if(!(event instanceof ShardMonitorEvent)) {
            LAST_JDA_EVENT = System.currentTimeMillis();
        }

        super.handle(event);
    }

    public long getLastJDAEventTimeDiff() {
        return System.currentTimeMillis() - LAST_JDA_EVENT;
    }
}