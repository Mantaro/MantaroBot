/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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

package net.kodehawa.mantarobot.utils;

import java.io.OutputStream;
import java.io.PrintStream;

public class CompactPrintStream extends PrintStream {
    public CompactPrintStream(OutputStream out) {
        super(out);
    }

    @Override
    public void println(String s) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String current = stackTrace[2].toString();
        int i = 3;
        while((current.startsWith("sun.") || current.startsWith("java.")) && i < stackTrace.length)
            current = stackTrace[i++].toString();
        super.println("[" + current + "]: " + s);
    }
}
