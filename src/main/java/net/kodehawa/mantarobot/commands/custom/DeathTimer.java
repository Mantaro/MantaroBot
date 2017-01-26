package net.kodehawa.mantarobot.commands.custom;

public class DeathTimer {
	private static final Runnable EMPTY = () -> {
	};

	private final Runnable onTimeout;
	private final long timeout;
	private boolean updated = false, armed = true;

	public DeathTimer(long timeout, Runnable onTimeout) {
		this.timeout = timeout;
		this.onTimeout = onTimeout == null ? EMPTY : onTimeout;

		Thread thread = new Thread(this::threadcode, "DeathTimer#" + Integer.toHexString(hashCode()) + " DThread");
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

	private void threadcode() {
		while (updated) {
			try {
				synchronized (this) {
					wait(timeout);
				}
			} catch (InterruptedException ignored) {
			}
		}

		if (armed) onTimeout.run();
	}
}
