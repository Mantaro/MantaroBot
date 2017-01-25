package net.kodehawa.mantarobot.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolHelper {
	private static final ThreadPoolHelper defaultPool = new ThreadPoolHelper();

	public static ThreadPoolHelper defaultPool() {
		return defaultPool;
	}

	private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

	public ThreadPoolExecutor getThreadPool() {
		return executor;
	}

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
