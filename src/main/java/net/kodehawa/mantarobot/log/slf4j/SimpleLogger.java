package net.kodehawa.mantarobot.log.slf4j;

import com.google.common.base.Throwables;

public abstract class SimpleLogger extends MarkerUnawareLogger {

    @Override
    public void trace(String format, Object arg) {
        trace(String.format(format, arg));
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        trace(String.format(format, arg1, arg2));
    }

    @Override
    public void trace(String format, Object... arguments) {
        trace(String.format(format, arguments));
    }

    @Override
    public void trace(String msg, Throwable t) {
        trace("%s\n%s", msg, Throwables.getStackTraceAsString(t));
    }

    @Override
    public void debug(String format, Object arg) {
        debug(String.format(format, arg));
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        debug(String.format(format, arg1, arg2));
    }

    @Override
    public void debug(String format, Object... arguments) {
        debug(String.format(format, arguments));
    }

    @Override
    public void debug(String msg, Throwable t) {
        debug("%s\n%s", msg, Throwables.getStackTraceAsString(t));
    }

    @Override
    public void info(String format, Object arg) {
        info(String.format(format, arg));
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        info(String.format(format, arg1, arg2));
    }

    @Override
    public void info(String format, Object... arguments) {
        info(String.format(format, arguments));
    }

    @Override
    public void info(String msg, Throwable t) {
        info("%s\n%s", msg, Throwables.getStackTraceAsString(t));
    }

    @Override
    public void warn(String format, Object arg) {
        warn(String.format(format, arg));
    }

    @Override
    public void warn(String format, Object... arguments) {
        warn(String.format(format, arguments));
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        warn(String.format(format, arg1, arg2));
    }

    @Override
    public void warn(String msg, Throwable t) {
        warn("%s\n%s", msg, Throwables.getStackTraceAsString(t));
    }

    @Override
    public void error(String format, Object arg) {
        error(String.format(format, arg));
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        error(String.format(format, arg1, arg2));
    }

    @Override
    public void error(String format, Object... arguments) {
        error(String.format(format, arguments));
    }

    @Override
    public void error(String msg, Throwable t) {
        error("%s\n%s", msg, Throwables.getStackTraceAsString(t));
    }

}
