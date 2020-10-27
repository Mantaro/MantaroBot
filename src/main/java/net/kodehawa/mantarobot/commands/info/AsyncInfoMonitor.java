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
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.exporters.JFRExports;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AsyncInfoMonitor {
    private static final ScheduledExecutorService POOL = Executors.newSingleThreadScheduledExecutor(
            task -> new Thread(task, "Mantaro-AsyncInfoMonitor")
    );

    private static final Logger log = LoggerFactory.getLogger(AsyncInfoMonitor.class);
    private static final Config config = MantaroData.config().get();

    private static final int availableProcessors = Runtime.getRuntime().availableProcessors();
    private static long freeMemory = 0;
    private static long maxMemory = 0;
    private static boolean started = false;
    private static long threadCount = 0;
    private static long totalMemory = 0;
    private static float processCpuUsage;
    private static double vpsCPUUsage = 0;
    private static long vpsFreeMemory = 0;
    private static long vpsMaxMemory = 0;

    public static int getAvailableProcessors() {
        check();
        return availableProcessors;
    }

    public static long getFreeMemory() {
        check();
        return freeMemory;
    }

    public static long getMaxMemory() {
        check();
        return maxMemory;
    }

    public static long getThreadCount() {
        check();
        return threadCount;
    }

    public static long getTotalMemory() {
        check();
        return totalMemory;
    }

    public static double getInstanceCPUUsage() {
        check();
        return processCpuUsage;
    }

    // The following methods are used by JFRExports to set values in this class

    public static void setProcessCpuUsage(float usage) {
        processCpuUsage = usage;
    }

    public static void setThreadCount(long count) {
        threadCount = count;
    }

    public static void setMachineMemoryUsage(long used, long total) {
        vpsMaxMemory = total;
        vpsFreeMemory = total - used;
    }

    public static void setMachineCPUUsage(float usage) {
        vpsCPUUsage = usage;
    }

    public static void start() {
        if (started) {
            throw new IllegalStateException("Already Started.");
        }

        // Some stats are set by JFRExports
        // By some I mean basically most of them
        JFRExports.register();
        var bot = MantaroBot.getInstance();

        var nodeSetName = "node-stats-" + config.getClientId();

        log.info("Started System Monitor! Monitoring system statistics since now!");
        log.info("Posting node system stats to redis on set {}", nodeSetName);

        POOL.scheduleAtFixedRate(() -> {
            freeMemory = Runtime.getRuntime().freeMemory();
            maxMemory = Runtime.getRuntime().maxMemory();
            totalMemory = Runtime.getRuntime().totalMemory();

            try(var jedis = MantaroData.getDefaultJedisPool().getResource()) {
                jedis.hset(nodeSetName,
                        "node-" + bot.getNodeNumber(),
                        new JSONObject()
                                .put("uptime", ManagementFactory.getRuntimeMXBean().getUptime())
                                .put("thread_count", threadCount)
                                .put("available_processors", availableProcessors)
                                .put("free_memory", freeMemory)
                                .put("max_memory", maxMemory)
                                .put("total_memory", totalMemory)
                                .put("used_memory", totalMemory - freeMemory)
                                .put("cpu_usage", processCpuUsage)
                                .put("machine_cpu_usage", vpsCPUUsage)
                                .put("machine_free_memory", vpsFreeMemory)
                                .put("machine_total_memory", vpsMaxMemory)
                                .put("machine_used_memory", vpsMaxMemory - vpsFreeMemory)
                                .put("guild_count", bot.getShardManager().getGuildCache().size())
                                .put("user_count", bot.getShardManager().getUserCache().size())
                                .put("shard_slice", bot.getShardSlice())
                                .put("queue_size", bot.getAudioManager().getTotalQueueSize())
                                .put("commands_ran", CommandListener.getCommandTotal())
                                .toString()
                );
            }
        }, 15, 30, TimeUnit.SECONDS);

        started = true;
    }

    private static void check() {
        if (!started) throw new IllegalStateException("AsyncInfoMonitor not started");
    }
}
