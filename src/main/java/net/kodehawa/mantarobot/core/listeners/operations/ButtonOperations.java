package net.kodehawa.mantarobot.core.listeners.operations;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.jodah.expiringmap.ExpiringMap;
import net.kodehawa.mantarobot.core.listeners.operations.core.ButtonOperation;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ButtonOperations {
    private static final EventListener LISTENER = new ButtonOperations.ButtonListener();

    private static final ExpiringMap<Long, ButtonOperations.RunningOperation> OPERATIONS = ExpiringMap.builder()
            .asyncExpirationListener((key, value) -> ((ButtonOperations.RunningOperation) value).operation.onExpire())
            .variableExpiration()
            .build();

    public static Future<Void> get(Long messageId) {
        RunningOperation o = OPERATIONS.get(messageId);
        return o == null ? null : o.future;
    }

    public static Future<Void> create(Message message, long timeoutSeconds, ButtonOperation operation, Button... defaultButtons) {
        if (!message.getAuthor().equals(message.getJDA().getSelfUser())) {
            throw new IllegalArgumentException("Must provide a message sent by the bot");
        }

        Future<Void> f = create(message.getIdLong(), timeoutSeconds, operation);
        if (f == null) {
            return null;
        }

        if (defaultButtons.length > 0) {
            message.editMessageComponents(ActionRow.of(defaultButtons)).queue();
        }

        return f;
    }

    public static Future<Void> create(Message message, long timeoutSeconds, ButtonOperation operation, Collection<Button> defaultButtons) {
        if (!message.getAuthor().equals(message.getJDA().getSelfUser())) {
            throw new IllegalArgumentException("Must provide a message sent by the bot");
        }

        Future<Void> f = create(message.getIdLong(), timeoutSeconds, operation);
        if (f == null) {
            return null;
        }

        if (defaultButtons.size() > 0) {
            message.editMessageComponents(ActionRow.of(defaultButtons)).queue();
        }

        return f;
    }

    public static Future<Void> createRows(Message message, long timeoutSeconds, ButtonOperation operation, Collection<ActionRow> defaultButtons) {
        if (!message.getAuthor().equals(message.getJDA().getSelfUser())) {
            throw new IllegalArgumentException("Must provide a message sent by the bot");
        }

        Future<Void> f = create(message.getIdLong(), timeoutSeconds, operation);
        if (f == null) {
            return null;
        }

        if (defaultButtons.size() > 0) {
            message.editMessageComponents(defaultButtons).queue();
        }

        return f;
    }


    public static Future<Void> create(long messageId, long timeoutSeconds, ButtonOperation operation) {
        if (timeoutSeconds < 1)
            throw new IllegalArgumentException("Timeout is less than 1 second");

        if (operation == null)
            throw new IllegalArgumentException("Operation cannot be null!");

        RunningOperation o = OPERATIONS.get(messageId);

        //Already running?
        if (o != null)
            return null;

        o = new RunningOperation(operation, new OperationFuture(messageId));
        OPERATIONS.put(messageId, o, timeoutSeconds, TimeUnit.SECONDS);

        return o.future;
    }

    public static class ButtonListener implements EventListener {
        @Override
        public void onEvent(@Nonnull GenericEvent e) {
            if (e instanceof ButtonClickEvent) {
                var event = (ButtonClickEvent) e;
                var guild = event.getGuild();
                var member = event.getMember();

                if (guild == null || member == null || event.getButton() == null) {
                    return;
                }

                if (member.getIdLong() == guild.getSelfMember().getIdLong()) {
                    return;
                }

                var messageId = event.getMessageIdLong();
                ButtonOperations.RunningOperation o = OPERATIONS.get(messageId);
                if (o == null) {
                    return;
                }

                // Forward this event to the anonymous class.
                event.deferEdit().queue();
                int i = o.operation.click(event);
                if (i == Operation.COMPLETED) {
                    //Operation has been completed. We can remove this from the running operations list and go on.
                    OPERATIONS.remove(messageId);
                    o.future.complete(null);
                } else if (i == Operation.RESET_TIMEOUT) {
                    //Reset the expiration of this specific operation.
                    OPERATIONS.resetExpiration(messageId);
                }
            }
        }
    }

    public static EventListener listener() {
        return LISTENER;
    }

    private static class RunningOperation {
        final ButtonOperations.OperationFuture future;
        final ButtonOperation operation;

        RunningOperation(ButtonOperation operation, ButtonOperations.OperationFuture future) {
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
            ButtonOperations.RunningOperation o = OPERATIONS.remove(id);

            if (o == null) {
                return false;
            }

            o.operation.onCancel();
            return true;
        }
    }
}
