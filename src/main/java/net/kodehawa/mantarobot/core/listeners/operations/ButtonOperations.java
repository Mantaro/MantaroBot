/*
 * Copyright (C) 2016 Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core.listeners.operations;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
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

        // TODO: i18n
        if (!message.getChannel().canTalk()) {
            message.editMessage("The bot needs View Channel and Message Write on this channel (or Send Messages in Threads if in a thread) to display buttons.").queue();
            return null;
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

        if (!message.getChannel().canTalk()) {
            message.editMessage("The bot needs View Channel and Message Write on this channel (or Send Messages in Threads if in a thread) to display buttons.").queue();
            return null;
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

        if (!message.getChannel().canTalk()) {
            message.editMessage("The bot needs View Channel and Message Write on this channel (or Send Messages in Threads if in a thread) to display buttons.").queue();
            return null;
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
            if (e instanceof ButtonInteractionEvent evt) {
                var guild = evt.getGuild();
                var member = evt.getMember();

                if (guild == null || member == null) {
                    return;
                }

                if (member.getIdLong() == guild.getSelfMember().getIdLong()) {
                    return;
                }

                var messageId = evt.getMessageIdLong();
                ButtonOperations.RunningOperation o = OPERATIONS.get(messageId);
                if (o == null) {
                    return;
                }

                // Forward this event to the anonymous class.
                evt.deferEdit().queue();
                int i = o.operation.click(evt);
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

    private record RunningOperation(ButtonOperation operation, OperationFuture future) { }

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
