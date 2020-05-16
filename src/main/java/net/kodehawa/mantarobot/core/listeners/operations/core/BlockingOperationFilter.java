package net.kodehawa.mantarobot.core.listeners.operations.core;

import net.dv8tion.jda.api.entities.Message;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

@FunctionalInterface
public interface BlockingOperationFilter {
    enum Result { ACCEPT, IGNORE, RESET_TIMEOUT }
    
    @Nonnull
    @CheckReturnValue
    Result test(@Nonnull Message m);
}
