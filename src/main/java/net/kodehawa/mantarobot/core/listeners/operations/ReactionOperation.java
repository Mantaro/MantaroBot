package net.kodehawa.mantarobot.core.listeners.operations;

import br.com.brjdevs.java.utils.async.threads.builder.ThreadBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.kodehawa.mantarobot.core.listeners.external.OptimizedListener;
import net.kodehawa.mantarobot.utils.TimeAmount;

import javax.xml.ws.Holder;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ReactionOperation {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(
            new ThreadBuilder().setName("ReactionOperations Executor")
    );

    private static final Map<String, ReactionOperation> OPERATIONS = new ConcurrentHashMap<>();
    private static final EventListener LISTENER = new OptimizedListener<MessageReactionAddEvent>(
            MessageReactionAddEvent.class
    ) {
        @Override
        public void event(MessageReactionAddEvent event) {
            if(event.getReaction().isSelf()) return;

            String id = event.getMessageId();
            ReactionOperation operation = OPERATIONS.get(id);

            if(operation != null && operation.onReaction.test(event)) {
                if(operation.timeoutFuture != null) {
                    operation.timeoutFuture.cancel(true);
                }

                OPERATIONS.remove(id, operation);
            }
        }
    };
    private final String messageId;
    private final Predicate<MessageReactionAddEvent> onReaction;
    private final Runnable onRemoved;
    private Future<?> timeoutFuture;
    ReactionOperation(Message message, Collection<String> reactions, TimeAmount timeout, Predicate<MessageReactionAddEvent> onReaction, Runnable onTimeout, Runnable onRemoved, boolean force) {
        this.messageId = message.getId();
        this.onReaction = onReaction;
        this.onRemoved = onRemoved;

        if(!force && OPERATIONS.containsKey(messageId))
            throw new IllegalStateException("Operation already happening at messageId");

        OPERATIONS.put(messageId, this);

        timeoutFuture = EXECUTOR.schedule(
                () -> {
                    OPERATIONS.remove(messageId, this);
                    if(onTimeout != null) {
                        onTimeout.run();
                    }
                }, timeout.getAmount(), timeout.getUnit()
        );

        if(!reactions.isEmpty()) {
            Iterator<String> iterator = reactions.iterator();
            Holder<Consumer<Void>> chain = new Holder<>();
            chain.value = nil -> {
                if(iterator.hasNext()) {
                    message.addReaction(iterator.next()).queue(chain.value);
                }
            };

            message.clearReactions().queue(chain.value);
        }
    }

    public static ReactionOperationBuilder builder() {
        return new ReactionOperationBuilder();
    }

    public static EventListener listener() {
        return LISTENER;
    }

    public static void stopOperation(String messageId) {
        ReactionOperation operation = OPERATIONS.remove(messageId);

        if(operation != null) {
            if(operation.timeoutFuture != null) {
                operation.timeoutFuture.cancel(true);
                operation.timeoutFuture = null;
            }

            if(operation.onRemoved != null) {
                operation.onRemoved.run();
            }
        }
    }
}
