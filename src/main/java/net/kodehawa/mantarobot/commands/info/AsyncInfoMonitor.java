/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.info;

import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AsyncInfoMonitor {
    private static final ScheduledExecutorService POOL = Executors.newSingleThreadScheduledExecutor(
            task -> new Thread(task, "Mantaro-AsyncInfoMonitor")
    );

    private static final Logger log = LoggerFactory.getLogger(AsyncInfoMonitor.class);
    private static final Config config = MantaroData.config().get();

    private static int availableProcessors = Runtime.getRuntime().availableProcessors();
    private static double cpuUsage = 0;
    private static long freeMemory = 0;
    private static double lastProcessCpuTime = 0;
    private static long lastSystemTime = 0;
    private static long maxMemory = 0;
    private static boolean started = false;
    private static int threadCount = 0;
    private static long totalMemory = 0;
    private static double vpsCPUUsage = 0;
    private static long vpsFreeMemory = 0;
    private static long vpsMaxMemory = 0;
    private static long vpsUsedMemory = 0;

    public static int getAvailableProcessors() {
        check();
        return availableProcessors;
    }

    public static double getCpuUsage() {
        check();
        return cpuUsage;
    }

    public static long getFreeMemory() {
        check();
        return freeMemory;
    }

    public static long getMaxMemory() {
        check();
        return maxMemory;
    }

    public static int getThreadCount() {
        check();
        return threadCount;
    }

    public static long getTotalMemory() {
        check();
        return totalMemory;
    }

    public static double getInstanceCPUUsage() {
        check();
        return vpsCPUUsage;
    }

    public static long getVpsFreeMemory() {
        check();
        return vpsFreeMemory;
    }

    public static long getVpsMaxMemory() {
        check();
        return vpsMaxMemory;
    }

    public static long getVpsUsedMemory() {
        check();
        return vpsUsedMemory;
    }

    public static void start() {
        if (started)
            throw new IllegalStateException("Already Started.");

        String nodeSetName = "node-stats-" + config.getClientId();

        log.info("Started System Monitor! Monitoring system statistics since now!");
        log.info("Posting node system stats to redis on set {}", nodeSetName);

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();
        Runtime r = Runtime.getRuntime();

        lastSystemTime = System.nanoTime();
        lastProcessCpuTime = calculateProcessCpuTime(os);

        POOL.scheduleAtFixedRate(() -> {
            threadCount = thread.getThreadCount();
            availableProcessors = r.availableProcessors();
            freeMemory = Runtime.getRuntime().freeMemory();
            maxMemory = Runtime.getRuntime().maxMemory();
            totalMemory = Runtime.getRuntime().totalMemory();
            cpuUsage = calculateCpuUsage(os);
            vpsCPUUsage = getInstanceCPUUsage(os);
            vpsFreeMemory = calculateVPSFreeMemory(os);
            vpsMaxMemory = calculateVPSMaxMemory(os);
            vpsUsedMemory = vpsMaxMemory - vpsFreeMemory;

            try(Jedis j = MantaroData.getDefaultJedisPool().getResource()) {
                j.hset(nodeSetName,
                        "node-" + MantaroBot.getInstance().getNodeNumber(),
                        new JSONObject()
                                .put("uptime", ManagementFactory.getRuntimeMXBean().getUptime())
                                .put("thread_count", threadCount)
                                .put("available_processors", r.availableProcessors())
                                .put("free_memory", Runtime.getRuntime().freeMemory())
                                .put("max_memory", Runtime.getRuntime().maxMemory())
                                .put("total_memory", Runtime.getRuntime().totalMemory())
                                .put("used_memory", getTotalMemory() - getFreeMemory())
                                .put("cpu_usage", calculateCpuUsage(os))
                                .put("machine_cpu_usage", getInstanceCPUUsage(os))
                                .put("machine_free_memory", calculateVPSFreeMemory(os))
                                .put("machine_total_memory", calculateVPSMaxMemory(os))
                                .put("machine_used_memory", vpsMaxMemory - vpsFreeMemory)
                                .put("guild_count", MantaroBot.getInstance().getShardManager().getGuildCache().size())
                                .put("user_count", MantaroBot.getInstance().getShardManager().getUserCache().size())
                                .put("shard_slice", MantaroBot.getInstance().getShardSlice())
                                .toString()
                );
            }
        }, 5, 5, TimeUnit.SECONDS);

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

    private static long calculateVPSFreeMemory(OperatingSystemMXBean os) {
        return ((com.sun.management.OperatingSystemMXBean) os).getFreePhysicalMemorySize();
    }

    private static long calculateVPSMaxMemory(OperatingSystemMXBean os) {
        return ((com.sun.management.OperatingSystemMXBean) os).getTotalPhysicalMemorySize();
    }

    private static void check() {
        if (!started) throw new IllegalStateException("AsyncInfoMonitor not started");
    }

    private static double getInstanceCPUUsage(OperatingSystemMXBean os) {
        vpsCPUUsage = ((com.sun.management.OperatingSystemMXBean) os).getSystemCpuLoad() * 100;
        return vpsCPUUsage;
    }
}
