package net.kodehawa.mantarobot.utils.exporters;

import io.prometheus.client.Collector;
import io.prometheus.client.Histogram;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import java.lang.management.ManagementFactory;

//based on https://github.com/Frederikam/Lavalink/blob/a6f5631b0ddd4b25a7e6720ae3f08c3a0a9729b5/LavalinkServer/src/main/java/lavalink/server/metrics/GcNotificationListener.java
//modified based on https://github.com/prometheus/client_java/issues/478#issuecomment-486650679
//to avoid directly linking to com.sun classes
public class GCListener implements NotificationListener {
    //from com.sun.management.GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION
    private static final String GARBAGE_COLLECTION_NOTIFICATION =
            "com.sun.management.gc.notification";
    private static final Histogram GC_PAUSES = Histogram.build()
               .name("jvm_gc_pauses_seconds")
               .help("Garbage collection pauses by buckets")
               .labelNames("action", "cause", "name")
               .buckets(0.005, 0.010, 0.025, 0.050, 0.100, 0.200, 0.400, 0.800, 1.600, 3, 5, 10)
               .create();
    
    public static void register() {
        GC_PAUSES.register();
        var listener = new GCListener();
        for(var gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if(gcBean instanceof NotificationEmitter) {
                ((NotificationEmitter) gcBean)
                        .addNotificationListener(listener, null, gcBean);
            }
        }
    }
    
    @Override
    public void handleNotification(Notification notification, Object handback) {
        if(GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType())) {
            var userData = (CompositeData) notification.getUserData();
            var gcInfo = (CompositeData) userData.get("gcInfo");
            
            if(gcInfo != null && !"No GC".equals(userData.get("gcCause"))) {
                
                var duration = ((long)gcInfo.get("endTime")) - ((long)gcInfo.get("startTime"));
                GC_PAUSES.labels(
                        (String)userData.get("gcAction"),
                        (String)userData.get("gcCause"),
                        (String)userData.get("gcName")
                ).observe(duration / Collector.MILLISECONDS_PER_SECOND);
            }
        }
    }
}
