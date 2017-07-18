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