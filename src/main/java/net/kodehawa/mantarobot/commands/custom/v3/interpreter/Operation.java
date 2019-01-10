package net.kodehawa.mantarobot.commands.custom.v3.interpreter;

import java.util.List;

public interface Operation {
    String apply(InterpreterContext context, List<String> args);
}
