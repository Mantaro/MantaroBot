package net.kodehawa.mantarobot.utils.exporters;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import jdk.jfr.EventSettings;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;

import java.time.Duration;
import java.util.function.Consumer;

public class JFRExports {
    private static final double NANOSECONDS_PER_SECOND = 1E9;
    //jdk.SafepointBegin
    private static final Histogram SAFEPOINTS = Histogram.build()
            .name("jvm_safepoint_pauses_seconds")
            .help("Safepoint pauses by buckets")
            .buckets(0.005, 0.010, 0.025, 0.050, 0.100, 0.200, 0.400, 0.800, 1.600, 3, 5, 10)
            .create();
    //jdk.GarbageCollection
    private static final Histogram GC_PAUSES = Histogram.build()
            .name("jvm_gc_pauses_seconds")
            .help("Garbage collection pauses by buckets")
            .labelNames("name", "cause")
            .buckets(0.005, 0.010, 0.025, 0.050, 0.100, 0.200, 0.400, 0.800, 1.600, 3, 5, 10)
            .create();
    //jdk.GCReferenceStatistics
    private static final Gauge REFERENCE_STATISTICS = Gauge.build()
            .name("jvm_reference_statistics")
            .labelNames("type")
            .create();
    //jdk.ExecuteVMOperation
    private static final Counter VM_OPERATIONS = Counter.build()
            .name("jvm_vm_operations")
            .help("Executed VM operations")
            .labelNames("operation", "safepoint")
            .create();
    //jdk.NetworkUtilization
    private static final Gauge NETWORK_READ = Gauge.build()
            .name("jvm_network_utilization")
            .help("Network utilization")
            .labelNames("interface")
            .create();
    //jdk.NetworkUtilization
    private static final Gauge NETWORK_WRITE = Gauge.build()
            .name("jvm_network_utilization")
            .help("Network utilization")
            .labelNames("interface")
            .create();
    //jdk.JavaThreadStatistics
    private static final Gauge THREADS_CURRENT = Gauge.build()
            .name("jvm_threads_current")
            .help("Current thread count of the JVM")
            .create();
    //jdk.JavaThreadStatistics
    private static final Gauge THREADS_DAEMON = Gauge.build()
            .name("jvm_threads_daemon")
            .help("Daemon thread count of the JVM")
            .create();

    public static void register() {
        SAFEPOINTS.register();
        GC_PAUSES.register();
        REFERENCE_STATISTICS.register();
        VM_OPERATIONS.register();
        NETWORK_READ.register();
        NETWORK_WRITE.register();
        THREADS_CURRENT.register();
        THREADS_DAEMON.register();
        var rs = new RecordingStream();
        rs.setReuse(true);

        /*
         * jdk.SafepointBegin {
         *   startTime = 23:18:00.149
         *   duration = 53,3 ms
         *   safepointId = 32
         *   totalThreadCount = 16
         *   jniCriticalThreadCount = 0
         * }
         */
        event(rs, "jdk.SafepointBegin", e -> SAFEPOINTS
                .observe(e.getDuration().toNanos() / NANOSECONDS_PER_SECOND));

        /*
         * jdk.GarbageCollection {
         *   startTime = 23:28:04.913
         *   duration = 7,65 ms
         *   gcId = 1
         *   name = "G1New"
         *   cause = "G1 Evacuation Pause"
         *   sumOfPauses = 7,65 ms
         *   longestPause = 7,65 ms
         * }
         */
        event(rs, "jdk.GarbageCollection", e -> {
            GC_PAUSES.labels(e.getString("name"), e.getString("cause"))
                    .observe(e.getDuration("sumOfPauses").toNanos() / NANOSECONDS_PER_SECOND);
        });

        /*
         * jdk.GCReferenceStatistics {
         *   startTime = 23:36:09.323
         *   gcId = 1
         *   type = "Weak reference"
         *   count = 91
         * }
         */
        event(rs, "jdk.GCReferenceStatistics", e -> {
            REFERENCE_STATISTICS.labels(e.getString("type")).set(e.getLong("count"));
        });

        /*
         * jdk.ExecuteVMOperation {
         *   startTime = 01:03:41.642
         *   duration = 13,4 ms
         *   operation = "G1CollectFull"
         *   safepoint = true
         *   blocking = true
         *   caller = "main" (javaThreadId = 1)
         *   safepointId = 18
         * }
         */
        event(rs, "jdk.ExecuteVMOperation", e -> {
            VM_OPERATIONS.labels(e.getString("operation"), String.valueOf(e.getBoolean("safepoint"))).inc();
        });

        /*
         * jdk.NetworkUtilization {
         *   startTime = 23:28:03.716
         *   networkInterface = N/A
         *   readRate = 4,4 kbps
         *   writeRate = 3,3 kbps
         * }
         */
        event(rs ,"jdk.NetworkUtilization", e -> {
            var itf = e.getString("networkInterface");
            NETWORK_READ.labels(itf).set(e.getLong("readRate"));
            NETWORK_WRITE.labels(itf).set(e.getLong("writeRate"));
        }).withPeriod(Duration.ofSeconds(5));

        /*
         * jdk.JavaThreadStatistics {
         *   startTime = 01:13:57.686
         *   activeCount = 12
         *   daemonCount = 10
         *   accumulatedCount = 13
         *   peakCount = 13
         * }
         */
        event(rs, "jdk.JavaThreadStatistics", e -> {
            THREADS_CURRENT.set(e.getLong("activeCount"));
            THREADS_DAEMON.set(e.getLong("daemonCount"));
        }).withPeriod(Duration.ofSeconds(5));

        rs.startAsync();
    }

    private static EventSettings event(RecordingStream rs, String name, Consumer<RecordedEvent> c) {
        var s = rs.enable(name);
        rs.onEvent(name, c);
        return s;
    }
}
