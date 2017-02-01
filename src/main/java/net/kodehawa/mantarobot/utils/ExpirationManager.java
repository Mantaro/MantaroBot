package net.kodehawa.mantarobot.utils;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class ExpirationManager {
	private final Map<Long, List<Runnable>> EXPIRATIONS;
	private boolean updated = false;

	public ExpirationManager() {
		this(new ConcurrentHashMap<>());
	}

	public ExpirationManager(Map<Long, List<Runnable>> expirations) {
		EXPIRATIONS = new ConcurrentHashMap<>(Collections.synchronizedMap(expirations));

		Thread thread = new Thread(this::threadcode, "ExpirationManager Thread");
		thread.setDaemon(true);
		thread.start();
	}

	public void letExpire(Long milis, Runnable onExpire) {
		Objects.requireNonNull(onExpire);
		EXPIRATIONS.computeIfAbsent(milis, k -> new ArrayList<>()).add(onExpire);
		updated = true;
		synchronized (this) {
			notify();
		}
	}

	private void threadcode() {
		//noinspection InfiniteLoopStatement
		while (true) {
			if (EXPIRATIONS.isEmpty()) {
				try {
					synchronized (this) {
						wait();
						updated = false;
					}
				} catch (InterruptedException ignored) {
				}
			}

			//noinspection OptionalGetWithoutIsPresent
			Entry<Long, List<Runnable>> firstEntry = EXPIRATIONS.entrySet().stream().sorted(Comparator.comparingLong(Entry::getKey)).findFirst().get();

			try {
				long timeout = firstEntry.getKey() - System.currentTimeMillis();
				if (timeout > 0) {
					synchronized (this) {
						wait(timeout);
					}
				}
			} catch (InterruptedException ignored) {
			}

			if (!updated) {
				EXPIRATIONS.remove(firstEntry.getKey());
				List<Runnable> runnables = firstEntry.getValue();
				runnables.remove(null);
				runnables.forEach(r -> ThreadPoolHelper.defaultPool().startThread("Expiration Executable", r));
			} else updated = false; //and the loop will restart and resolve it
		}
	}
}