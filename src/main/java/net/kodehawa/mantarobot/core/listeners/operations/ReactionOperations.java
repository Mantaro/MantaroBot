package net.kodehawa.mantarobot.core.listeners.operations;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.jodah.expiringmap.ExpiringMap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class ReactionOperations {
    private static final EventListener LISTENER = new ReactionListener();

    private static final ExpiringMap<Long, RunningOperation> OPERATIONS = ExpiringMap.<Long, RunningOperation>builder()
            .asyncExpirationListener((key, value) -> ((RunningOperation)value).operation.onExpire())
            .variableExpiration()
            .build();

    public static Future<Void> get(Message message) {
        if(!message.getAuthor().equals(message.getJDA().getSelfUser())) throw new IllegalArgumentException("Must provide a message sent by the bot");
        return get(message.getIdLong());
    }

    public static Future<Void> get(long messageId) {
        RunningOperation o = OPERATIONS.get(messageId);
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
                if(f.isCancelled()) return;
                int i = index.incrementAndGet();
                if(i < defaultReactions.length) {
                    message.addReaction(reaction(defaultReactions[i])).queue(c.get(), ignore);
                }
            });
            message.addReaction(reaction(defaultReactions[0])).queue(c.get(), ignore);
        }
        return f;
    }

    public static Future<Void> createOrGet(long messageId, long timeoutSeconds, ReactionOperation operation) {
        if(timeoutSeconds < 1) throw new IllegalArgumentException("Timeout < 1");
        if(operation == null) throw new NullPointerException("operation");
        RunningOperation o = OPERATIONS.get(messageId);
        if(o != null) return o.future;
        o = new RunningOperation(operation, new OperationFuture(messageId));
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
                if(f.isCancelled()) return;
                int i = index.incrementAndGet();
                if(i < defaultReactions.length) {
                    message.addReaction(reaction(defaultReactions[i])).queue(c.get(), ignore);
                }
            });
            message.addReaction(reaction(defaultReactions[0])).queue(c.get(), ignore);
        }
        return f;
    }

    public static Future<Void> create(long messageId, long timeoutSeconds, ReactionOperation operation) {
        if(timeoutSeconds < 1) throw new IllegalArgumentException("Timeout < 1");
        if(operation == null) throw new NullPointerException("operation");
        RunningOperation o = OPERATIONS.get(messageId);
        if(o != null) return null;
        o = new RunningOperation(operation, new OperationFuture(messageId));
        OPERATIONS.put(messageId, o, timeoutSeconds, TimeUnit.SECONDS);
        return o.future;
    }

    public static EventListener listener() {
        return LISTENER;
    }

    private static String reaction(String r) {
        if(r.startsWith("<")) return r.replaceAll("<:(\\S+?)>", "$1");
        return r;
    }

    public static class ReactionListener implements EventListener {
        @Override
        public void onEvent(Event e) {
            if(e instanceof MessageReactionAddEvent) {
                MessageReactionAddEvent event = (MessageReactionAddEvent) e;
                if(event.getReaction().isSelf()) return;
                long messageId = event.getMessageIdLong();
                RunningOperation o = OPERATIONS.get(messageId);
                if(o == null) return;
                int i = o.operation.add(event);
                if(i == Operation.COMPLETED) {
                    OPERATIONS.remove(messageId);
                    o.future.complete(null);
                } else if(i == Operation.RESET_TIMEOUT) {
                    OPERATIONS.resetExpiration(messageId);
                }
                return;
            }
            if(e instanceof MessageReactionRemoveEvent) {
                MessageReactionRemoveEvent event = (MessageReactionRemoveEvent) e;
                if(event.getReaction().isSelf()) return;
                long messageId = event.getMessageIdLong();
                RunningOperation o = OPERATIONS.get(messageId);
                if(o == null) return;
                int i = o.operation.remove(event);
                if(i == Operation.COMPLETED) {
                    OPERATIONS.remove(messageId);
                    o.future.complete(null);
                } else if(i == Operation.RESET_TIMEOUT) {
                    OPERATIONS.resetExpiration(messageId);
                }
                return;
            }
            if(e instanceof MessageReactionRemoveAllEvent) {
                MessageReactionRemoveAllEvent event = (MessageReactionRemoveAllEvent)e;
                long messageId = event.getMessageIdLong();
                RunningOperation o = OPERATIONS.get(messageId);
                if(o == null) return;
                int i = o.operation.removeAll(event);
                if(i == Operation.COMPLETED) {
                    OPERATIONS.remove(messageId);
                    o.future.complete(null);
                } else if(i == Operation.RESET_TIMEOUT) {
                    OPERATIONS.resetExpiration(messageId);
                }
            }
        }
    }

    private static class RunningOperation {
        final ReactionOperation operation;
        final OperationFuture future;

        RunningOperation(ReactionOperation operation, OperationFuture future) {
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
            super.cancel(mayInterruptIfRunning);
            RunningOperation o = OPERATIONS.remove(id);
            if(o == null) return false;
            o.operation.onCancel();
            return true;
        }
    }
}
