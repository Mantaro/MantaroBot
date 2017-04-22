package net.kodehawa.mantarobot.utils;

import br.com.brjdevs.java.utils.extensions.Async;
import net.kodehawa.mantarobot.utils.Expirator.Expirable;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualConcurrentHashBidiMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

public class Expirator<T extends Expirable> {
	public interface Expirable {
		static Expirable asExpirable(Runnable runnable) {
			return runnable::run;
		}

		void onExpire();
	}

	private final BidiMap<Long, List<Expirable>> EXPIRATIONS = new DualConcurrentHashBidiMap<>();
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

	public Long timeToExpire(Expirable expirable) {
		Long key = EXPIRATIONS.getKey(expirable);
		return key == null ? null : key - System.currentTimeMillis();
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