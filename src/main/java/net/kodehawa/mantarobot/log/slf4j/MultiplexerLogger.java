package net.kodehawa.mantarobot.log.slf4j;

import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MultiplexerLogger extends MarkerUnawareLogger {
    private final List<Logger> loggers;

    public MultiplexerLogger(Logger... loggers) {
        this.loggers = new CopyOnWriteArrayList<>(loggers);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void trace(String msg) {
        for(Logger l : loggers) l.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        for(Logger l : loggers) l.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        for(Logger l : loggers) l.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        for(Logger l : loggers) l.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        for(Logger l : loggers) l.trace(msg);
    }

    @Override
    public void debug(String msg) {
        for(Logger l : loggers) l.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        for(Logger l : loggers) l.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        for(Logger l : loggers) l.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        for(Logger l : loggers) l.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        for(Logger l : loggers) l.debug(msg);
    }

    @Override
    public void info(String msg) {
        for(Logger l : loggers) l.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        for(Logger l : loggers) l.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        for(Logger l : loggers) l.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        for(Logger l : loggers) l.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        for(Logger l : loggers) l.info(msg, t);
    }

    @Override
    public void warn(String msg) {
        for(Logger l : loggers) l.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        for(Logger l : loggers) l.warn(format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        for(Logger l : loggers) l.warn(format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        for(Logger l : loggers) l.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        for(Logger l : loggers) l.warn(msg, t);
    }

    @Override
    public void error(String msg) {
        for(Logger l : loggers) l.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        for(Logger l : loggers) l.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        for(Logger l : loggers) l.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        for(Logger l : loggers) l.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        for(Logger l : loggers) l.error(msg, t);
    }

    @Override
    public String toString() {
        return "Multiplexer" + loggers.toString();
    }
}
