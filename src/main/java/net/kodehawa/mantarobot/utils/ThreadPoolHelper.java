package net.kodehawa.mantarobot.utils;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolHelper {
	private static final ThreadPoolHelper defaultPool = new ThreadPoolHelper();

	public static ThreadPoolHelper defaultPool() {
		return defaultPool;
	}

	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 25,
		60L, TimeUnit.SECONDS,
		new SynchronousQueue<>());

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
