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
