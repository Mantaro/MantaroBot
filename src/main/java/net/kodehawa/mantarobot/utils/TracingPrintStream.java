/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.utils;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Locale;

public class TracingPrintStream extends PrintStream {
    public TracingPrintStream(PrintStream out) {
        super(new TracingOutputStream(out));
    }

    private static String getCaller() {
        var caller = StackWalker.getInstance(Collections.emptySet(), 15).walk(
                s -> s.dropWhile(f -> !f.getClassName().startsWith(TracingPrintStream.class.getName()))
                        .dropWhile(
                                f -> f.getClassName().startsWith(TracingPrintStream.class.getName()) ||
                                        f.getClassName().startsWith("java.") ||
                                        f.getClassName().startsWith("sun.")
                        )
                        .findFirst()
                        .orElseThrow()
        ).toStackTraceElement();
        if (caller.getClassName().startsWith("ch.qos.")) return null;
        return caller.toString();
    }

    @Override
    public void close() {
        super.println("Closing stream");
        super.close();
    }

    @Override
    public void write(int b) {
        super.write(b);
    }

    @Override
    public void write(@NotNull byte[] buf, int off, int len) {
        super.write(buf, off, len);
    }

    @Override
    public void print(boolean b) {
        super.print(b);
    }

    @Override
    public void print(char c) {
        super.print(c);
    }

    @Override
    public void print(int i) {
        super.print(i);
    }

    @Override
    public void print(long l) {
        super.print(l);
    }

    @Override
    public void print(float f) {
        super.print(f);
    }

    @Override
    public void print(double d) {
        super.print(d);
    }

    @Override
    public void print(@NotNull char[] s) {
        super.print(s);
    }

    @Override
    public void print(String s) {
        super.print(s);
    }

    @Override
    public void print(Object obj) {
        super.print(obj);
    }

    @Override
    public void println() {
        super.println();
    }

    @Override
    public void println(boolean x) {
        super.println(x);
    }

    @Override
    public void println(char x) {
        super.println(x);
    }

    @Override
    public void println(int x) {
        super.println(x);
    }

    @Override
    public void println(long x) {
        super.println(x);
    }

    @Override
    public void println(float x) {
        super.println(x);
    }

    @Override
    public void println(double x) {
        super.println(x);
    }

    @Override
    public void println(@NotNull char[] x) {
        super.println(x);
    }

    @Override
    public void println(String x) {
        super.println(x);
    }

    @Override
    public void println(Object x) {
        super.println(x);
    }

    @Override
    public PrintStream printf(@NotNull String format, Object... args) {
        return super.printf(format, args);
    }

    @Override
    public PrintStream printf(Locale l, @NotNull String format, Object... args) {
        return super.printf(l, format, args);
    }

    @Override
    public PrintStream format(@NotNull String format, Object... args) {
        return super.format(format, args);
    }

    @Override
    public PrintStream format(Locale l, @NotNull String format, Object... args) {
        return super.format(l, format, args);
    }

    @Override
    public PrintStream append(CharSequence csq) {
        return super.append(csq);
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        return super.append(csq, start, end);
    }

    @Override
    public PrintStream append(char c) {
        return super.append(c);
    }

    private static class TracingOutputStream extends OutputStream {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final PrintStream destination;

        private TracingOutputStream(PrintStream destination) {
            this.destination = destination;
        }

        @Override
        public void write(int i) {
            if (i == '\n') {
                flush();
            } else {
                buffer.write(i);
            }
        }

        @Override
        public void flush() {
            if (buffer.size() == 0) return;
            var caller = getCaller();
            if (caller != null) {
                destination.print("[" + getCaller() + "] ");
            }
            destination.println(buffer.toString());
            buffer.reset();
        }

        @Override
        public void close() {
            destination.close();
        }
    }
}
