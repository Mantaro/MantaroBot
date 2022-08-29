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

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.jodah.expiringmap.ExpiringMap;
import net.kodehawa.mantarobot.core.listeners.operations.core.ModalOperation;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// This one is probably the shortest one, since we already have to reply with a modal.
public class ModalOperations {
    private static final EventListener LISTENER = new ModalListener();
    private static final ExpiringMap<String, RunningOperation> OPERATIONS = ExpiringMap.builder()
            .asyncExpirationListener((key, value) -> ((RunningOperation) value).operation.onExpire())
            .variableExpiration()
            .build();

    public static Future<Void> get(String interactionId) {
        RunningOperation o = OPERATIONS.get(interactionId);
        return o == null ? null : o.future;
    }

    // We have to already reply with a modal seemingly.
    public static void create(String modalId, long timeoutSeconds, ModalOperation operation) {
        if (timeoutSeconds < 1)
            throw new IllegalArgumentException("Timeout is less than 1 second");

        if (operation == null)
            throw new IllegalArgumentException("Operation cannot be null!");

        RunningOperation o = OPERATIONS.get(modalId);
        //Already running?
        if (o != null) {
            o.operation.onExpire();
            o.future.cancel(true);
            OPERATIONS.remove(modalId);
        }

        o = new RunningOperation(operation, new OperationFuture(modalId));
        OPERATIONS.put(modalId, o, timeoutSeconds, TimeUnit.SECONDS);

    }

    public static class ModalListener implements EventListener {
        @Override
        public void onEvent(@Nonnull GenericEvent e) {
            if (e instanceof ModalInteractionEvent evt) {
                var guild = evt.getGuild();
                var member = evt.getMember();

                if (guild == null || member == null) {
                    return;
                }

                if (member.getIdLong() == guild.getSelfMember().getIdLong()) {
                    return;
                }

                var interactionId = evt.getModalId();
                RunningOperation o = OPERATIONS.get(interactionId);
                if (o == null) {
                    return;
                }

                // Forward this event to the anonymous class.
                int i = o.operation.modal(evt);
                if (i == Operation.COMPLETED) {
                    // Operation has been completed. We can remove this from the running operations list and go on.
                    OPERATIONS.remove(interactionId);
                    o.future.complete(null);
                } else if (i == Operation.RESET_TIMEOUT) {
                    // Reset the expiration of this specific operation.
                    OPERATIONS.resetExpiration(interactionId);
                }
            }
        }
    }

    public static EventListener listener() {
        return LISTENER;
    }

    private record RunningOperation(ModalOperation operation, OperationFuture future) { }

    private static class OperationFuture extends CompletableFuture<Void> {
        private final String id;

        OperationFuture(String id) {
            this.id = id;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            super.cancel(mayInterruptIfRunning);
            RunningOperation o = OPERATIONS.remove(id);

            if (o == null) {
                return false;
            }

            o.operation.onCancel();
            return true;
        }
    }

}
