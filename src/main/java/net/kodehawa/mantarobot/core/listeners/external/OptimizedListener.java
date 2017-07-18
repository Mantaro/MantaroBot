package net.kodehawa.mantarobot.core.listeners.external;

import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.EventListener;

public abstract class OptimizedListener<T extends Event> implements EventListener {
    private transient final Class<T> c;

    public OptimizedListener(Class<T> eventClass) {
        this.c = eventClass;
    }

    public abstract void event(T event);

    @Override
    public final void onEvent(Event event) {
        if(c.isInstance(event)) event(c.cast(event));
    }
}