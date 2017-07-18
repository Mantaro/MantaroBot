/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 * <p>
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 * <p>
 * or (per the licensee's choosing)
 * <p>
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package net.kodehawa.mantarobot.log.slf4j;

import lombok.Getter;
import org.slf4j.event.Level;

import java.time.OffsetDateTime;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public final class DatabaseLogger extends SimpleLogger {
    private static final Queue<QueuedLog> queue = new LinkedBlockingQueue<>();
    private String name;
    public DatabaseLogger(String name) {
        this.name = name;
    }

    public DatabaseLogger() {
        this("");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void trace(String msg) {
        log(msg, Level.TRACE);
    }

    @Override
    public void debug(String msg) {
        log(msg, Level.DEBUG);
    }

    @Override
    public void info(String msg) {
        log(msg, Level.INFO);
    }

    @Override
    public void warn(String msg) {
        log(msg, Level.WARN);
    }

    @Override
    public void error(String msg) {
        log(msg, Level.ERROR);
    }

    private void log(String msg, Level level) {
        queue.offer(new QueuedLog(level, msg));
    }

    @Getter
    public static class QueuedLog {
        private final String level;
        private final String message;
        private final OffsetDateTime timestamp = OffsetDateTime.now();

        public QueuedLog(Level level, String message) {
            this.level = level.toString();
            this.message = message;
        }
    }
}
