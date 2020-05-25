package net.kodehawa.mantarobot.core.listeners.operations.core;

import net.dv8tion.jda.api.entities.Message;
import net.kodehawa.mantarobot.data.MantaroData;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    
    @Nonnull
    @CheckReturnValue
    static BlockingOperationFilter fromUser(long id) {
        return acceptIf(m -> m.getAuthor().getIdLong() == id);
    }
    
    @Nonnull
    @CheckReturnValue
    static BlockingOperationFilter withContentAfterPrefix(@Nullable String customPrefix, @Nonnull String... values) {
        return acceptIf(m -> {
            var message = m.getContentRaw().toLowerCase();
            for (String s : MantaroData.config().get().prefix) {
                if (message.startsWith(s)) {
                    message = message.substring(s.length());
                }
            }
            if (customPrefix != null && !customPrefix.isEmpty() && message.startsWith(customPrefix)) {
                message = message.substring(customPrefix.length());
            }
            for(var v : values) {
                if(message.equalsIgnoreCase(v)) return true;
            }
            return false;
        });
    }
}
