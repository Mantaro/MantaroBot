package net.kodehawa.mantarobot.thread;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolHelper {
	private static final ThreadPoolHelper defaultPool = new ThreadPoolHelper();

	public static ThreadPoolHelper DEFAULT() {
		return defaultPool;
	}

	private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

	public void purge() {
		executor.purge();
	}

	public void startThread(String task, Runnable thread) {
		executor.execute(thread);
	}

	public void startThread(String task, ThreadPoolExecutor exec, Runnable thread) {
		exec.execute(thread);
	}
}
