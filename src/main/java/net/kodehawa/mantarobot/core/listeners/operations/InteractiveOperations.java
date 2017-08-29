/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.core.listeners.operations;

import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.jodah.expiringmap.ExpiringMap;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class InteractiveOperations {
    private static final EventListener LISTENER = new InteractiveListener();

    private static final ExpiringMap<Long, RunningOperation> OPERATIONS = ExpiringMap.<Long, RunningOperation>builder()
            .asyncExpirationListener((key, value) -> ((RunningOperation) value).operation.onExpire())
            .variableExpiration()
            .build();

    public static Future<Void> get(MessageChannel channel) {
        return get(channel.getIdLong());
    }

    public static Future<Void> get(long channelId) {
        RunningOperation o = OPERATIONS.get(channelId);
        return o == null ? null : o.future;
    }

    public static Future<Void> createOrGet(MessageChannel channel, long timeoutSeconds, InteractiveOperation operation) {
        return createOrGet(channel.getIdLong(), timeoutSeconds, operation);
    }

    public static Future<Void> createOrGet(long channelId, long timeoutSeconds, InteractiveOperation operation) {
        if(timeoutSeconds < 1) throw new IllegalArgumentException("Timeout < 1");
        if(operation == null) throw new NullPointerException("operation");
        RunningOperation o = OPERATIONS.get(channelId);
        if(o != null) return o.future;
        o = new RunningOperation(operation, new OperationFuture(channelId));
        OPERATIONS.put(channelId, o, timeoutSeconds, TimeUnit.SECONDS);
        return o.future;
    }

    public static Future<Void> create(MessageChannel channel, long timeoutSeconds, InteractiveOperation operation) {
        return create(channel.getIdLong(), timeoutSeconds, operation);
    }

    public static Future<Void> create(long channelId, long timeoutSeconds, InteractiveOperation operation) {
        if(timeoutSeconds < 1) throw new IllegalArgumentException("Timeout < 1");
        if(operation == null) throw new NullPointerException("operation");
        RunningOperation o = OPERATIONS.get(channelId);
        if(o != null) return null;
        o = new RunningOperation(operation, new OperationFuture(channelId));
        OPERATIONS.put(channelId, o, timeoutSeconds, TimeUnit.SECONDS);
        return o.future;
    }

    public static Future<Void> createOverriding(MessageChannel channel, long timeoutSeconds, InteractiveOperation operation) {
        return createOverriding(channel.getIdLong(), timeoutSeconds, operation);
    }

    public static Future<Void> createOverriding(long channelId, long timeoutSeconds, InteractiveOperation operation) {
        if(timeoutSeconds < 1) throw new IllegalArgumentException("Timeout < 1");
        if(operation == null) throw new NullPointerException("operation");
        RunningOperation o = new RunningOperation(operation, new OperationFuture(channelId));
        RunningOperation running = OPERATIONS.get(channelId);
        if(running != null) {
            running.operation.onExpire();
            running.future.cancel(true);
        }
        OPERATIONS.put(channelId, o, timeoutSeconds, TimeUnit.SECONDS);
        return o.future;
    }

    public static EventListener listener() {
        return LISTENER;
    }

    public static class InteractiveListener implements EventListener {
        @Override
        public void onEvent(Event e) {
            if(!(e instanceof GuildMessageReceivedEvent)) return;
            GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) e;
            if(event.getAuthor().equals(event.getJDA().getSelfUser())) return;
            long channelId = event.getChannel().getIdLong();
            RunningOperation o = OPERATIONS.get(channelId);
            if(o == null) return;
            int i = o.operation.run(event);
            if(i == Operation.COMPLETED) {
                OPERATIONS.remove(channelId);
                o.future.complete(null);
            } else if(i == Operation.RESET_TIMEOUT) {
                OPERATIONS.resetExpiration(channelId);
            }
        }
    }

    private static class RunningOperation {
        final OperationFuture future;
        final InteractiveOperation operation;

        RunningOperation(InteractiveOperation operation, OperationFuture future) {
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
            RunningOperation o = OPERATIONS.remove(id);
            if(o == null) return false;
            o.operation.onCancel();
            return true;
        }
    }
}
