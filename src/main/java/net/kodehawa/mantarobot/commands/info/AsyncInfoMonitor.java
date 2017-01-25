package net.kodehawa.mantarobot.commands.info;

import net.kodehawa.mantarobot.utils.Async;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

public class AsyncInfoMonitor {
	private static int availableProcessors = Runtime.getRuntime().availableProcessors();
	private static double cpuUsage = 0;
	private static double freeMemory = 0;
	private static double lastProcessCpuTime = 0;
	private static long lastSystemTime = 0;
	private static double maxMemory = 0;
	private static boolean started = false;
	private static int threadCount = 0;
	private static double totalMemory = 0;

	public static double calculateCpuUsage(OperatingSystemMXBean os) {
		long systemTime = System.nanoTime();
		double processCpuTime = calculateProcessCpuTime(os);

		double cpuUsage = (processCpuTime - lastProcessCpuTime) / ((double) (systemTime - lastSystemTime));

		lastSystemTime = systemTime;
		lastProcessCpuTime = processCpuTime;

		return cpuUsage / availableProcessors;
	}

	private static double calculateProcessCpuTime(OperatingSystemMXBean os) {
		return ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuTime();
	}

	private static void check() {
		if (!started) throw new IllegalStateException("AsyncInfoMonitor not started");
	}

	public static int getAvailableProcessors() {
		check();
		return availableProcessors;
	}

	public static double getCpuUsage() {
		check();
		return cpuUsage;
	}

	public static double getFreeMemory() {
		check();
		return freeMemory;
	}

	public static double getMaxMemory() {
		check();
		return maxMemory;
	}

	public static int getThreadCount() {
		check();
		return threadCount;
	}

	public static double getTotalMemory() {
		check();
		return totalMemory;
	}

	public static void start(int everyMs) {
		if (started) throw new IllegalStateException("Already Started.");
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		ThreadMXBean thread = ManagementFactory.getThreadMXBean();
		Runtime r = Runtime.getRuntime();
		int mb = 0x100000;

		lastSystemTime = System.nanoTime();
		lastProcessCpuTime = calculateProcessCpuTime(os);

		Async.startAsyncTask("AsyncInfoMonitorThread", () -> {
			threadCount = thread.getThreadCount();
			availableProcessors = r.availableProcessors();
			freeMemory = Runtime.getRuntime().freeMemory() / mb;
			maxMemory = Runtime.getRuntime().maxMemory() / mb;
			totalMemory = Runtime.getRuntime().totalMemory() / mb;
			cpuUsage = calculateCpuUsage(os);
		}, everyMs);
		started = true;
	}
}
