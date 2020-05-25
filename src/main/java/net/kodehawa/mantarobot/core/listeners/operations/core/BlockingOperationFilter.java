package net.kodehawa.mantarobot.core.listeners.operations.core;

import net.dv8tion.jda.api.entities.Message;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.function.Predicate;

@FunctionalInterface
public interface BlockingOperationFilter {
    enum Result { ACCEPT, IGNORE, RESET_TIMEOUT }
    
    @Nonnull
    @CheckReturnValue
    Result test(@Nonnull Message m);
    
    @Nonnull
    @CheckReturnValue
    default BlockingOperationFilter andThen(@Nonnull BlockingOperationFilter next) {
        return m -> {
            var r = test(m);
            if(r == Result.ACCEPT) return next.test(m);
            return r;
        };
    }
    
    @Nonnull
    @CheckReturnValue
    default BlockingOperationFilter orElse(@Nonnull BlockingOperationFilter next) {
        return m -> {
            var r = test(m);
            if(r == Result.IGNORE) return next.test(m);
            return r;
        };
    }
    
    @Nonnull
    @CheckReturnValue
    static BlockingOperationFilter acceptIf(@Nonnull Predicate<Message> filter) {
        return m -> filter.test(m) ? Result.ACCEPT : Result.IGNORE;
    }
    
    @Nonnull
    @CheckReturnValue
    static BlockingOperationFilter resetTimeoutIf(@Nonnull Predicate<Message> filter) {
        return m -> filter.test(m) ? Result.RESET_TIMEOUT : Result.IGNORE;
    }
    
    @Nonnull
    @CheckReturnValue
    static BlockingOperationFilter ignoreIf(@Nonnull Predicate<Message> filter) {
        return m -> filter.test(m) ? Result.IGNORE : Result.ACCEPT;
    }
    
    @Nonnull
    @CheckReturnValue
    static BlockingOperationFilter withContent(@Nonnull String... values) {
        return acceptIf(m -> {
            var c = m.getContentRaw();
            for(var v : values) {
                if(c.equalsIgnoreCase(v)) return true;
            }
            return false;
        });
    }
}
