package net.kodehawa.mantarobot.commands.custom.kaiperscript.parser;

import java.io.PrintWriter;
import java.util.concurrent.ExecutionException;

public interface Evaluator {
    Object run(String string, PrintWriter out) throws ExecutionException;
}
