package net.kodehawa.mantarobot.commands.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeathTimer {
	private static final Runnable EMPTY = () -> {
	};
	private static final Logger LOGGER = LoggerFactory.getLogger("DeathTimer");
	private final Runnable onTimeout;
	private final long timeout;
	private boolean updated = false, armed = true;

	public DeathTimer(long timeout, Runnable onTimeout) {
		this.timeout = timeout;
		this.onTimeout = onTimeout == null ? EMPTY : onTimeout;
		this.updated = true;

		Thread thread = new Thread(this::threadCode, "DeathTimer#" + Integer.toHexString(hashCode()) + " DThread");
		thread.setDaemon(true);
		thread.start();
	}

	public DeathTimer arm() {
		armed = true;
		return this;
	}

	public DeathTimer disarm() {
		armed = false;
		return this;
	}

	public DeathTimer explode() {
		synchronized (this) {
			notify();
		}
		return this;
	}

	public DeathTimer reset() {
		updated = true;
		return explode();
	}

	private void threadCode() {
		while (updated) {
			updated = false;
			try {
				synchronized (this) {
					wait(timeout);
				}
			} catch (InterruptedException e) {
				LOGGER.warn("DeathTimer thread was interrupted for some reason, check it out.", e);
			}
		}

		if (armed) onTimeout.run();
	}
}
