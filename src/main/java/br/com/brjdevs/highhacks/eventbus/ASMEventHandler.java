package br.com.brjdevs.highhacks.eventbus;

public abstract class ASMEventHandler implements EventHandler {
	/**
	 * Returns the event class this ASMEventHandler handles
	 *
	 * @return the event class
	 */
	public abstract Class<?> getEventClass();

	/**
	 * Returns the class defining the method this ASMEventHandler invokes
	 *
	 * @return the defining class
	 */
	public abstract Class<?> getTargetClass();

	@Override
	public String toString() {
		return "ASMEventHandler{" + getClass().getName() + " -> " + getTargetClass().getName() + "}";
	}
}
