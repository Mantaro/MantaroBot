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

package net.kodehawa.mantarobot.utils.exporters;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

public class ThreadPoolCollector extends Collector {
    private final ConcurrentMap<String, ThreadPoolExecutor> executors = new ConcurrentHashMap<>();

    public ThreadPoolExecutor remove(String name) {
        return executors.remove(name);
    }

    public boolean add(String name, ThreadPoolExecutor executor) {
        Objects.requireNonNull(name, "Name may not be null");
        Objects.requireNonNull(executor, "Executor may not be null");
        return executors.putIfAbsent(name, executor) == null;
    }

    public boolean add(String name, Executor executor) {
        Objects.requireNonNull(executor, "Executor may not be null");
        if (executor instanceof ThreadPoolExecutor) {
            return add(name, (ThreadPoolExecutor) executor);
        }

        throw new IllegalArgumentException("Provided executor is not a ThreadPoolExecutor");
    }

    public boolean add(ThreadPoolExecutor executor) {
        Objects.requireNonNull(executor, "Executor may not be null");
        return add(executor.toString(), executor);
    }

    public boolean add(Executor executor) {
        Objects.requireNonNull(executor, "Executor may not be null");
        return add(executor.toString(), executor);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> list = new ArrayList<>(8);
        GaugeMetricFamily activeCount = new GaugeMetricFamily(
                "executor_active_count",
                "Approximate number of threads that are actively executing tasks.",
                Collections.singletonList("executor")
        );

        list.add(activeCount);
        GaugeMetricFamily completedTaskCount = new GaugeMetricFamily(
                "executor_completed_task_count",
                "Approximate total number of tasks that have completed execution.",
                Collections.singletonList("executor")
        );

        list.add(completedTaskCount);
        GaugeMetricFamily corePoolSize = new GaugeMetricFamily(
                "executor_core_pool_size",
                "Core number of threads.",
                Collections.singletonList("executor")
        );

        list.add(corePoolSize);
        GaugeMetricFamily keepAliveTimeMs = new GaugeMetricFamily(
                "executor_keep_alive_time_milliseconds",
                "Thread keep-alive time, which is the amount of time that threads may remain idle before being terminated.",
                Collections.singletonList("executor")
        );

        list.add(keepAliveTimeMs);
        GaugeMetricFamily largestPoolSize = new GaugeMetricFamily(
                "executor_largest_pool_size",
                "Largest number of threads that have ever simultaneously been in the pool.",
                Collections.singletonList("executor")
        );

        list.add(largestPoolSize);
        GaugeMetricFamily maximumPoolSize = new GaugeMetricFamily(
                "executor_maximum_pool_size",
                "Maximum allowed number of threads.",
                Collections.singletonList("executor")
        );

        list.add(maximumPoolSize);
        GaugeMetricFamily poolSize = new GaugeMetricFamily(
                "executor_pool_size",
                "Current number of threads in the pool.",
                Collections.singletonList("executor")
        );

        list.add(poolSize);
        GaugeMetricFamily taskCount = new GaugeMetricFamily(
                "executor_task_count",
                "Approximate total number of tasks that have ever been scheduled for execution.",
                Collections.singletonList("executor")
        );

        list.add(taskCount);
        for (var entry : executors.entrySet()) {
            var name = Collections.singletonList(entry.getKey());
            var executor = entry.getValue();

            activeCount.addMetric(name, executor.getActiveCount());
            completedTaskCount.addMetric(name, executor.getCompletedTaskCount());
            corePoolSize.addMetric(name, executor.getCorePoolSize());
            keepAliveTimeMs.addMetric(name, executor.getKeepAliveTime(TimeUnit.MILLISECONDS));
            largestPoolSize.addMetric(name, executor.getLargestPoolSize());
            maximumPoolSize.addMetric(name, executor.getMaximumPoolSize());
            poolSize.addMetric(name, executor.getPoolSize());
            taskCount.addMetric(name, executor.getTaskCount());
        }

        return list;
    }
}
