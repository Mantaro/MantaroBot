package net.kodehawa.mantarobot.core.listeners.operations;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.jodah.expiringmap.ExpiringMap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class ReactionOperations {
    private static final EventListener LISTENER = new EventListener() {
        @Override
        public void onEvent(Event e) {
            if(!(e instanceof MessageReactionAddEvent)) return;
            MessageReactionAddEvent event = (MessageReactionAddEvent)e;
            if(event.getReaction().isSelf()) return;
            long messageId = event.getMessageIdLong();
            Operation o = OPERATIONS.get(messageId);
            if(o == null) return;
            if(o.operation.run(event)) {
                OPERATIONS.remove(messageId);
                o.future.complete(null);
            } else {
                OPERATIONS.resetExpiration(messageId);
            }
        }
    };

    private static final ExpiringMap<Long, Operation> OPERATIONS = ExpiringMap.<Long, Operation>builder()
            .asyncExpirationListener((key, value) -> ((Operation)value).operation.onExpire())
            .variableExpiration()
            .build();

    public static Future<Void> get(Message message) {
        if(!message.getAuthor().equals(message.getJDA().getSelfUser())) throw new IllegalArgumentException("Must provide a message sent by the bot");
        return get(message.getIdLong());
    }

    public static Future<Void> get(long messageId) {
        Operation o = OPERATIONS.get(messageId);
        return o == null ? null : o.future;
    }

    public static Future<Void> createOrGet(Message message, long timeoutSeconds, ReactionOperation operation, String... defaultReactions) {
        if(!message.getAuthor().equals(message.getJDA().getSelfUser())) throw new IllegalArgumentException("Must provide a message sent by the bot");
        Future<Void> f = createOrGet(message.getIdLong(), timeoutSeconds, operation);
        if(defaultReactions.length > 0) {
            AtomicInteger index = new AtomicInteger();
            AtomicReference<Consumer<Void>> c = new AtomicReference<>();
            Consumer<Throwable> ignore = (t)->{};
            c.set(ignored->{
                int i = index.incrementAndGet();
                if(i < defaultReactions.length) {
                    message.addReaction(defaultReactions[i]).queue(c.get(), ignore);
                }
            });
            message.addReaction(defaultReactions[0]).queue(c.get(), ignore);
        }
        return f;
    }

    public static Future<Void> createOrGet(long messageId, long timeoutSeconds, ReactionOperation operation) {
        if(timeoutSeconds < 1) throw new IllegalArgumentException("Timeout < 1");
        if(operation == null) throw new NullPointerException("operation");
        Operation o = OPERATIONS.get(messageId);
        if(o != null) return o.future;
        o = new Operation(operation, new OperationFuture(messageId));
        OPERATIONS.put(messageId, o, timeoutSeconds, TimeUnit.SECONDS);
        return o.future;
    }

    public static Future<Void> create(Message message, long timeoutSeconds, ReactionOperation operation, String... defaultReactions) {
        if(!message.getAuthor().equals(message.getJDA().getSelfUser())) throw new IllegalArgumentException("Must provide a message sent by the bot");
        Future<Void> f = create(message.getIdLong(), timeoutSeconds, operation);
        if(defaultReactions.length > 0) {
            AtomicInteger index = new AtomicInteger();
            AtomicReference<Consumer<Void>> c = new AtomicReference<>();
            Consumer<Throwable> ignore = (t)->{};
            c.set(ignored->{
                int i = index.incrementAndGet();
                if(i < defaultReactions.length) {
                    message.addReaction(defaultReactions[i]).queue(c.get(), ignore);
                }
            });
            message.addReaction(defaultReactions[0]).queue(c.get(), ignore);
        }
        return f;
    }

    public static Future<Void> create(long messageId, long timeoutSeconds, ReactionOperation operation) {
        if(timeoutSeconds < 1) throw new IllegalArgumentException("Timeout < 1");
        if(operation == null) throw new NullPointerException("operation");
        Operation o = OPERATIONS.get(messageId);
        if(o != null) return null;
        o = new Operation(operation, new OperationFuture(messageId));
        OPERATIONS.put(messageId, o, timeoutSeconds, TimeUnit.SECONDS);
        return o.future;
    }

    public static EventListener listener() {
        return LISTENER;
    }

    private static class Operation {
        final ReactionOperation operation;
        final OperationFuture future;

        Operation(ReactionOperation operation, OperationFuture future) {
            this.operation = operation;
            this.future = future;
        }
    }

    private static class OperationFuture extends CompletableFuture<Void> {
        private final long id;

        OperationFuture(long id) {
            this.id = id;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            Operation o = OPERATIONS.remove(id);
            if(o == null) return false;
            o.operation.onCancel();
            return true;
        }
    }
}
