/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.utils;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.BufferPoolsExports;
import io.prometheus.client.hotspot.ClassLoadingExports;
import io.prometheus.client.hotspot.GarbageCollectorExports;
import io.prometheus.client.hotspot.MemoryPoolsExports;
import io.prometheus.client.hotspot.StandardExports;
import io.prometheus.client.hotspot.VersionInfoExports;
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
