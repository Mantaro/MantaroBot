package net.kodehawa.mantarobot.commands.custom.kaiperscript.parser.internal;

import xyz.avarel.kaiper.exceptions.ComputeException;

public class LimitReachedException extends ComputeException {
    public LimitReachedException(String msg) {
        super(msg);
    }

    public LimitReachedException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public LimitReachedException(Throwable throwable) {
        super(throwable);
    }
}
