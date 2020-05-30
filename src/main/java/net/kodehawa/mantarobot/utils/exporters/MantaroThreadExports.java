package net.kodehawa.mantarobot.utils.exporters;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

public class MantaroThreadExports extends Collector {
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    
    void addThreadMetrics(List<MetricFamilySamples> sampleFamilies) {
        sampleFamilies.add(
                new GaugeMetricFamily(
                        "jvm_threads_current",
                        "Current thread count of a JVM",
                        threadBean.getThreadCount()));
        
        sampleFamilies.add(
                new GaugeMetricFamily(
                        "jvm_threads_daemon",
                        "Daemon thread count of a JVM",
                        threadBean.getDaemonThreadCount()));
        
        sampleFamilies.add(
                new GaugeMetricFamily(
                        "jvm_threads_peak",
                        "Peak thread count of a JVM",
                        threadBean.getPeakThreadCount()));
        
        sampleFamilies.add(
                new CounterMetricFamily(
                        "jvm_threads_started_total",
                        "Started thread count of a JVM",
                        threadBean.getTotalStartedThreadCount()));
    }
    
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
        addThreadMetrics(mfs);
        return mfs;
    }
}
