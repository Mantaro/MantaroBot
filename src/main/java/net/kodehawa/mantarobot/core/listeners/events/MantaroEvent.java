package net.kodehawa.mantarobot.core.listeners.events;

/**
 * A custom, self-defined event that propagates through JDAs event manager.
 * Useful when in need to catch stuff.
 */
public interface MantaroEvent {
    void onPropagation();
}
