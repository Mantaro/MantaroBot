/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.info;

import org.slf4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AsyncInfoMonitor {
    private static final ScheduledExecutorService POOL = Executors.newSingleThreadScheduledExecutor(task -> {
        return new Thread(task, "AsyncInfoMonitor");
    });

    private static final double gb = 1024 * 1024 * 1024;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AsyncInfoMonitor.class);
    private static int availableProcessors = Runtime.getRuntime().availableProcessors();
    private static double cpuUsage = 0;
    private static double freeMemory = 0;
    private static double lastProcessCpuTime = 0;
    private static long lastSystemTime = 0;
    private static double maxMemory = 0;
    private static boolean started = false;
    private static int threadCount = 0;
    private static double totalMemory = 0;
    private static double vpsCPUUsage = 0;
    private static double vpsFreeMemory = 0;
    private static double vpsMaxMemory = 0;
    private static double vpsUsedMemory = 0;

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

    public static double getInstanceCPUUsage() {
        check();
        return vpsCPUUsage;
    }

    public static double getVpsFreeMemory() {
        check();
        return vpsFreeMemory;
    }

    public static double getVpsMaxMemory() {
        check();
        return vpsMaxMemory;
    }

    public static double getVpsUsedMemory() {
        check();
        return vpsUsedMemory;
    }

    public static void start() {
        if(started) throw new IllegalStateException("Already Started.");

        log.debug("Started AsyncInfoMonitor... Monitoring system statistics since now!");
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();
        Runtime r = Runtime.getRuntime();
        int mb = 0x100000;

        lastSystemTime = System.nanoTime();
        lastProcessCpuTime = calculateProcessCpuTime(os);

        POOL.scheduleAtFixedRate(()->{
            threadCount = thread.getThreadCount();
            availableProcessors = r.availableProcessors();
            freeMemory = Runtime.getRuntime().freeMemory() / mb;
            maxMemory = Runtime.getRuntime().maxMemory() / mb;
            totalMemory = Runtime.getRuntime().totalMemory() / mb;
            cpuUsage = calculateCpuUsage(os);
            vpsCPUUsage = getInstanceCPUUsage(os);
            vpsFreeMemory = calculateVPSFreeMemory(os);
            vpsMaxMemory = calculateVPSMaxMemory(os);
            vpsUsedMemory = vpsMaxMemory - vpsFreeMemory;
        }, 1, 1, TimeUnit.SECONDS);
        started = true;
    }

    private static double calculateCpuUsage(OperatingSystemMXBean os) {
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

    private static double calculateVPSFreeMemory(OperatingSystemMXBean os) {
        return ((com.sun.management.OperatingSystemMXBean) os).getFreePhysicalMemorySize() / gb;
    }

    private static double calculateVPSMaxMemory(OperatingSystemMXBean os) {
        return ((com.sun.management.OperatingSystemMXBean) os).getTotalPhysicalMemorySize() / gb;
    }

    private static void check() {
        if(!started) throw new IllegalStateException("AsyncInfoMonitor not started");
    }

    private static double getInstanceCPUUsage(OperatingSystemMXBean os) {
        vpsCPUUsage = ((com.sun.management.OperatingSystemMXBean) os).getSystemCpuLoad() * 100;
        return vpsCPUUsage;
    }
}
