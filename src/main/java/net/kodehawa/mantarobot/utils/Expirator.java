package net.kodehawa.mantarobot.utils;

import br.com.brjdevs.java.utils.extensions.Async;
import net.kodehawa.mantarobot.utils.Expirator.Expirable;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class Expirator<T extends Expirable> {
	public interface Expirable {
		static Expirable asExpirable(Runnable runnable) {
			return runnable::run;
		}

		void onExpire();
	}

	private final Map<Long, List<Expirable>> EXPIRATIONS = new ConcurrentHashMap<>();
	private boolean updated = false;

	public Expirator() {

		Thread thread = new Thread(this::threadcode, "ExpirationManager Thread");
		thread.setDaemon(true);
		thread.start();
	}

	public void letExpire(long milis, Expirable expirable) {
		Objects.requireNonNull(expirable);
		EXPIRATIONS.computeIfAbsent(milis, k -> new ArrayList<>()).add(expirable);
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
			Entry<Long, List<Expirable>> firstEntry = EXPIRATIONS.entrySet().stream()
				.sorted(Comparator.comparingLong(Entry::getKey))
				.findFirst()
				.orElse(null);

			long timeout = firstEntry.getKey() - System.currentTimeMillis();
			if (timeout > 0) {
				synchronized (this) {
					try {
						wait(timeout);
					} catch (InterruptedException ignored) {
					}
				}
			}

			if (!updated) {
				EXPIRATIONS.remove(firstEntry.getKey());
				List<Expirable> runnables = firstEntry.getValue();
				runnables.remove(null);
				runnables.forEach(expirable -> Async.thread("Expiration Executable", expirable::onExpire));
			} else updated = false; //and the loop will restart and resolve it
		}
	}

	public void unletExpire(Expirable expirable) {
		Objects.requireNonNull(expirable);
		EXPIRATIONS.values().forEach(list -> list.remove(expirable));
		updated = true;
		synchronized (this) {
			notify();
		}
	}

	public void updateExpire(long milis, Expirable expirable) {
		Objects.requireNonNull(expirable);

		EXPIRATIONS.values().forEach(list -> list.remove(expirable));
		EXPIRATIONS.computeIfAbsent(milis, k -> new ArrayList<>()).add(expirable);

		updated = true;
		synchronized (this) {
			notify();
		}
	}
}