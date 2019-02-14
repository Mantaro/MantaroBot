package net.kodehawa.mantarobot.commands.custom.v3.interpreter;

import java.util.List;

public interface Operation {
    String apply(InterpreterContext context, List<Argument> args);

    interface Argument {
        String evaluate();
    }
}
