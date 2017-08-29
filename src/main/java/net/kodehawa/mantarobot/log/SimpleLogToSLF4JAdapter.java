/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.log;

import net.dv8tion.jda.core.utils.SimpleLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author sedmelluq
 */
public class SimpleLogToSLF4JAdapter implements SimpleLog.LogListener {
    private static final Map<SimpleLog, Logger> logS = new WeakHashMap<>();

    public static void install() {
        SimpleLog.addListener(new SimpleLogToSLF4JAdapter());
        SimpleLog.LEVEL = SimpleLog.Level.OFF;
    }

    @Override
    public void onLog(SimpleLog simpleLog, SimpleLog.Level logLevel, Object message) {
        Logger log = convert(simpleLog);
        switch(logLevel) {
            case TRACE:
                if(log.isTraceEnabled()) {
                    log.trace(message.toString());
                }
                break;
            case DEBUG:
                if(log.isDebugEnabled()) {
                    log.debug(message.toString());
                }
                break;
            case INFO:
                log.info(message.toString());
                break;
            case WARNING:
                log.warn(message.toString());
                break;
            case FATAL:
                log.error(message.toString());
                break;
        }
    }

    @Override
    public void onError(SimpleLog simpleLog, Throwable err) {
        convert(simpleLog).error("An exception occurred", err);
    }

    private Logger convert(SimpleLog log) {
        return logS.computeIfAbsent(log, ignored -> LoggerFactory.getLogger(log.name));
    }
}