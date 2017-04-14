package br.com.brjdevs.highhacks.eventbus;

/**
 * Handles invocation of a listener
 */
@FunctionalInterface
public interface EventHandler {
	/**
	 * Handles an event
	 *
	 * @param event The event posted
	 */
	void handle(Object event);
}
