package net.kodehawa.mantarobot.utils;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.*;
import net.kodehawa.mantarobot.data.MantaroData;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class Prometheus {
    public static final ThreadPoolCollector THREAD_POOL_COLLECTOR = new ThreadPoolCollector().register();

    private static final AtomicReference<State> STATE = new AtomicReference<>(State.DISABLED);
    private static volatile HTTPServer server;

    public static State currentState() {
        return STATE.get();
    }

    public static void enable() throws IOException {
        if(STATE.compareAndSet(State.DISABLED, State.ENABLING)) {
            new StandardExports().register();
            new MemoryPoolsExports().register();
            new BufferPoolsExports().register();
            new GarbageCollectorExports().register();
            new ClassLoadingExports().register();
            new VersionInfoExports().register();
            server = new HTTPServer(MantaroData.config().get().prometheusPort);
            STATE.set(State.ENABLED);
        }
    }

    public static void disable() {
        while(!STATE.compareAndSet(State.ENABLED, State.DISABLED)) {
            if(STATE.get() == State.DISABLED) return;
            Thread.yield();
        }
        server.stop();
    }

    public enum State {
        DISABLED, ENABLING, ENABLED
    }
}
