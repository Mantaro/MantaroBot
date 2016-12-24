package net.kodehawa.mantarobot.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AsyncHelper {
	
	private static final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 5);
	private final static AsyncHelper at = new AsyncHelper();
	
	/**
	 * @return the current thread pool
	 */
	public static ExecutorService getThreadPool() {
		return threadPool;
	}
	
	/**
	 * Start an Async single thread task every x seconds. Replacement for Timer.
	 * @param task
	 * @param scheduled
	 * @param everySeconds
	 */
	Thread thread;
	public void startAsyncTask(String task, Runnable scheduled, int everySeconds) {
		Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, task + " [Executor]")).scheduleAtFixedRate(scheduled, 0, everySeconds, TimeUnit.SECONDS);
	}
	
    public void startAsyncTask(String task, Consumer<ScheduledExecutorService> scheduled, int everySeconds) {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, task + " [Executor]"));
        scheduledExecutorService.scheduleAtFixedRate(() -> scheduled.accept(scheduledExecutorService), 0, everySeconds, TimeUnit.SECONDS);
    }
	
	private Runnable asyncThread(final Runnable doAsync) {
		return new Thread(doAsync)::start;
	}

	public Runnable asyncThread(final String name, final Runnable doAsync) {
		return new Thread(doAsync, name)::start;
	}

	private void sleep(int milis){
		try {
			Thread.sleep(milis);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Runnable asyncSleepThen(final int milis, final Runnable doAfter) {
		return asyncThread(() -> {
			sleep(milis);
			if (doAfter != null) doAfter.run();
		});
	}
	
	public static AsyncHelper instance(){
		return at;
	}
}
