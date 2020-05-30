package net.kodehawa.mantarobot.utils;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SafepointExports extends Collector {
    private static final Method GET_SAFEPOINT_COUNT;
    private static final Method GET_TOTAL_SAFEPOINT_TIME;
    private static final Method GET_SAFEPOINT_SYNC_TIME;
    private static final Object BEAN;
    
    static {
        Method safepointCount, totalSafepointTime, safepointSyncTime;
        Object bean;
        try {
            var hotspotRuntimeMBean = Class.forName("sun.management.HotspotRuntimeMBean");
            safepointCount = hotspotRuntimeMBean.getMethod("getSafepointCount");
            totalSafepointTime = hotspotRuntimeMBean.getMethod("getTotalSafepointTime");
            safepointSyncTime = hotspotRuntimeMBean.getMethod("getSafepointSyncTime");
            var managementFactory = Class.forName("sun.management.ManagementFactoryHelper");
            var m = managementFactory.getMethod("getHotspotRuntimeMBean");
            bean = m.invoke(null);
        } catch(Exception e) {
            bean = safepointCount = totalSafepointTime = safepointSyncTime = null;
        }
        GET_SAFEPOINT_COUNT = safepointCount;
        GET_TOTAL_SAFEPOINT_TIME = totalSafepointTime;
        GET_SAFEPOINT_SYNC_TIME = safepointSyncTime;
        BEAN = bean;
    }
    
    void addThreadMetrics(List<MetricFamilySamples> sampleFamilies) throws InvocationTargetException, IllegalAccessException {
        sampleFamilies.add(
                new GaugeMetricFamily(
                        "jvm_safepoint_count",
                        "The number of safepoints taken place since the Java virtual machine started.",
                        (long)GET_SAFEPOINT_COUNT.invoke(BEAN)));
        
        sampleFamilies.add(
                new GaugeMetricFamily(
                        "jvm_safepoint_time_seconds",
                        "The accumulated time spent at safepoints. " +
                                "This is the accumulated elapsed time that the application has " +
                                "been stopped for safepoint operations.",
                        (long)GET_TOTAL_SAFEPOINT_TIME.invoke(BEAN) / MILLISECONDS_PER_SECOND));
        
        sampleFamilies.add(
                new GaugeMetricFamily(
                        "jvm_safepoint_sync_time_seconds",
                        "The accumulated time spent getting to safepoints.",
                        (long)GET_SAFEPOINT_SYNC_TIME.invoke(BEAN) / MILLISECONDS_PER_SECOND));
    }
    
    @Override
    public List<MetricFamilySamples> collect() {
        if(BEAN == null) {
            return Collections.emptyList();
        }
        var mfs = new ArrayList<MetricFamilySamples>();
        try {
            addThreadMetrics(mfs);
        } catch(Exception ignored) {}
        return mfs;
    }
}
