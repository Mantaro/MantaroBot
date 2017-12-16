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

/**
 * Utility class to create, get or use a {@link InteractiveOperation}.
 * An InteractiveOperation is an Operation that listens for upcoming messages. It can be used for all kind of stuff, like listening for user input, etc.
 */
public class InteractiveOperations {
    //The listener used to check interactive operations.
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

    /**
     * Creates a new {@link InteractiveOperation} or gets an already running Operation if there is one.
     * If a running on is found, the return type is the running Operation.
     *
     * @param channelId      The id of the {@link net.dv8tion.jda.core.entities.TextChannel} we want this Operation to run on.
     * @param timeoutSeconds How much seconds until it stops listening to us.
     * @param operation      The {@link InteractiveOperation} itself.
     * @return The uncompleted {@link Future<Void>} of this InteractiveOperation.
     */
    public static Future<Void> createOrGet(MessageChannel channel, long timeoutSeconds, InteractiveOperation operation) {
        return createOrGet(channel.getIdLong(), timeoutSeconds, operation);
    }

    /**
     * Creates a new {@link InteractiveOperation} or gets an already running Operation if there is one.
     * If a running on is found, the return type is the running Operation.
     *
     * @param channelId      The id of the {@link net.dv8tion.jda.core.entities.TextChannel} we want this Operation to run on.
     * @param timeoutSeconds How much seconds until it stops listening to us.
     * @param operation      The {@link InteractiveOperation} itself.
     * @return The uncompleted {@link Future<Void>} of this InteractiveOperation.
     */
    public static Future<Void> createOrGet(long channelId, long timeoutSeconds, InteractiveOperation operation) {
        if(timeoutSeconds < 1)
            throw new IllegalArgumentException("Timeout is less than 1 second");

        if(operation == null)
            throw new IllegalArgumentException("Operation cannot be null");

        RunningOperation o = OPERATIONS.get(channelId);

        if(o != null)
            return o.future;

        o = new RunningOperation(operation, new OperationFuture(channelId));
        OPERATIONS.put(channelId, o, timeoutSeconds, TimeUnit.SECONDS);

        return o.future;
    }

    /**
     * Creates a new {@link InteractiveOperation} on the specified {@link net.dv8tion.jda.core.entities.TextChannel} id provided.
     * This method will not make a new {@link InteractiveOperation} if there's already another one running.
     * You can check the return type to give a response to the user.
     *
     * @param channelId      The id of the {@link net.dv8tion.jda.core.entities.TextChannel} we want this Operation to run on.
     * @param timeoutSeconds How much seconds until it stops listening to us.
     * @param operation      The {@link InteractiveOperation} itself.
     * @return The uncompleted {@link Future<Void>} of this InteractiveOperation.
     */
    public static Future<Void> create(MessageChannel channel, long timeoutSeconds, InteractiveOperation operation) {
        return create(channel.getIdLong(), timeoutSeconds, operation);
    }

    /**
     * Creates a new {@link InteractiveOperation} on the specified {@link net.dv8tion.jda.core.entities.TextChannel} id provided.
     * This method will not make a new {@link InteractiveOperation} if there's already another one running.
     * You can check the return type to give a response to the user.
     *
     * @param channelId      The id of the {@link net.dv8tion.jda.core.entities.TextChannel} we want this Operation to run on.
     * @param timeoutSeconds How much seconds until it stops listening to us.
     * @param operation      The {@link InteractiveOperation} itself.
     * @return The uncompleted {@link Future<Void>} of this InteractiveOperation.
     */
    public static Future<Void> create(long channelId, long timeoutSeconds, InteractiveOperation operation) {
        if(timeoutSeconds < 1)
            throw new IllegalArgumentException("Timeout is less than 1 second");

        if(operation == null)
            throw new IllegalArgumentException("Operation cannot be null");

        RunningOperation o = OPERATIONS.get(channelId);

        if(o != null)
            return null;

        o = new RunningOperation(operation, new OperationFuture(channelId));
        OPERATIONS.put(channelId, o, timeoutSeconds, TimeUnit.SECONDS);

        return o.future;
    }

    /**
     * Creates a new {@link InteractiveOperation} on the specified {@link net.dv8tion.jda.core.entities.TextChannel} id provided.
     * This method does NOT take into account if there is already a running operation. Useful for when we want to override the existing one.
     * An InteractiveOperation is an Operation that listens for upcoming messages. It can be used for all kind of stuff, like listening if someone says "yes" or "no" to
     * an specific question.
     *
     * @param channelId      The id of the {@link net.dv8tion.jda.core.entities.TextChannel} we want this Operation to run on.
     * @param timeoutSeconds How much seconds until it stops listening to us.
     * @param operation      The {@link InteractiveOperation} itself.
     * @return The uncompleted {@link Future<Void>} of this InteractiveOperation.
     */
    public static Future<Void> createOverriding(MessageChannel channel, long timeoutSeconds, InteractiveOperation operation) {
        return createOverriding(channel.getIdLong(), timeoutSeconds, operation);
    }

    /**
     * Creates a new {@link InteractiveOperation} on the specified {@link net.dv8tion.jda.core.entities.TextChannel} id provided.
     * This method does NOT take into account if there is already a running operation. Useful for when we want to override the existing one.
     * An InteractiveOperation is an Operation that listens for upcoming messages. It can be used for all kind of stuff, like listening if someone says "yes" or "no" to
     * an specific question.
     *
     * @param channelId      The id of the {@link net.dv8tion.jda.core.entities.TextChannel} we want this Operation to run on.
     * @param timeoutSeconds How much seconds until it stops listening to us.
     * @param operation      The {@link InteractiveOperation} itself.
     * @return The uncompleted {@link Future<Void>} of this InteractiveOperation.
     */
    public static Future<Void> createOverriding(long channelId, long timeoutSeconds, InteractiveOperation operation) {
        if(timeoutSeconds < 1)
            throw new IllegalArgumentException("Timeout is less than 1 second");

        if(operation == null)
            throw new IllegalArgumentException("Operation cannot be null");

        RunningOperation o = new RunningOperation(operation, new OperationFuture(channelId));
        RunningOperation running = OPERATIONS.get(channelId);

        if(running != null) {
            running.operation.onExpire();
            running.future.cancel(true);
        }

        OPERATIONS.put(channelId, o, timeoutSeconds, TimeUnit.SECONDS);

        return o.future;
    }

    /**
     * @return The listener used to check for the InteractiveOperations.
     */
    public static EventListener listener() {
        return LISTENER;
    }

    public static class InteractiveListener implements EventListener {
        @Override
        public void onEvent(Event e) {
            if(!(e instanceof GuildMessageReceivedEvent))
                return;

            GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) e;

            //Don't listen to ourselves...
            if(event.getAuthor().equals(event.getJDA().getSelfUser()))
                return;

            long channelId = event.getChannel().getIdLong();
            RunningOperation o = OPERATIONS.get(channelId);

            if(o == null)
                return;

            //Forward this to the class handling this.
            int i = o.operation.run(event);

            if(i == Operation.COMPLETED) {
                //We finished this Operation. We can remove it from the map and move on.
                OPERATIONS.remove(channelId);
                o.future.complete(null);
            } else if(i == Operation.RESET_TIMEOUT) {
                //Reset the expiration of this Operation. Use with caution!
                OPERATIONS.resetExpiration(channelId);
            }
        }
    }

    //Represents an eventually-running Operation.
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

            if(o == null)
                return false;

            o.operation.onCancel();
            return true;
        }
    }
}
