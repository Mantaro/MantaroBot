package net.kodehawa.mantarobot.core.listeners.events;

/**
 * A custom, self-defined event that propagates through JDAs event manager.
 * Useful when in need to catch stuff.
 */
public interface MantaroEvent {

    /**
     * This fires whenever a custom event is handled by the EventManager. Allows to do stuff outside of what the users
     * will be able to handle.
     */
    void onPropagation();
}
