/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core.listeners.operations;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.utils.Prometheus;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Utility class to create, get or use a {@link InteractiveOperation}.
 * An InteractiveOperation is an Operation that listens for upcoming messages. It can be used for all kind of stuff, like listening for user input, etc.
 */
public class InteractiveOperations {
    //The listener used to check interactive operations.
    private static final EventListener LISTENER = new InteractiveListener();
    
    private static final ConcurrentHashMap<Long, List<RunningOperation>> OPS = new ConcurrentHashMap<>();
    
    static {
        ScheduledExecutorService s = Executors.newScheduledThreadPool(10, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("InteractiveOperations-Timeout-Processor");
            return t;
        });
        
        Prometheus.THREAD_POOL_COLLECTOR.add("interactive-operations-timeout", s);
        
        s.scheduleAtFixedRate(() -> OPS.values().removeIf(list -> {
            list.removeIf(RunningOperation::isTimedOut);
            return list.isEmpty();
        }), 1, 1, TimeUnit.SECONDS);
    }
    
    /**
     * Returns a Future<Void> representing the current RunningOperation instance on the specified channel.
     *
     * @param channel The MessageChannel to check.
     * @return Future<Void> or null if there's none.
     */
    public static List<Future<Void>> get(MessageChannel channel) {
        return get(channel.getIdLong());
    }
    
    /**
     * Returns a Future<Void> representing the current RunningOperation instance on the specified channel.
     *
     * @param channelId The ID of the channel to check.
     * @return Future<Void> or null if there's none.
     */
    public static List<Future<Void>> get(long channelId) {
        List<RunningOperation> l = OPS.get(channelId);
        
        return l == null ? Collections.emptyList() : l.stream().map(o -> o.future).collect(Collectors.toList());
    }
    
    /**
     * Creates a new {@link InteractiveOperation} on the specified {@link net.dv8tion.jda.api.entities.TextChannel} id provided.
     * This method will not make a new {@link InteractiveOperation} if there's already another one running.
     * You can check the return type to give a response to the user.
     *
     * @param channel        The id of the {@link net.dv8tion.jda.api.entities.TextChannel} we want this Operation to run on.
     * @param timeoutSeconds How much seconds until it stops listening to us.
     * @param operation      The {@link InteractiveOperation} itself.
     * @return The uncompleted {@link Future<Void>} of this InteractiveOperation.
     */
    public static Future<Void> create(MessageChannel channel, long userId, long timeoutSeconds, InteractiveOperation operation) {
        return create(channel.getIdLong(), userId, timeoutSeconds, operation);
    }
    
    /**
     * Creates a new {@link InteractiveOperation} on the specified {@link net.dv8tion.jda.api.entities.TextChannel} id provided.
     * This method will not make a new {@link InteractiveOperation} if there's already another one running.
     * You can check the return type to give a response to the user.
     *
     * @param channelId      The id of the {@link net.dv8tion.jda.api.entities.TextChannel} we want this Operation to run on.
     * @param timeoutSeconds How much seconds until it stops listening to us.
     * @param operation      The {@link InteractiveOperation} itself.
     * @return The uncompleted {@link Future<Void>} of this InteractiveOperation.
     */
    public static Future<Void> create(long channelId, long userId, long timeoutSeconds, InteractiveOperation operation) {
        if(timeoutSeconds < 1)
            throw new IllegalArgumentException("Timeout is less than 1 second");
        
        if(operation == null)
            throw new IllegalArgumentException("Operation cannot be null");
        
        List<RunningOperation> l = OPS.computeIfAbsent(channelId, ignored -> new CopyOnWriteArrayList<>());
        
        RunningOperation current = l.stream().filter(op -> op.userId == userId).findFirst().orElse(null);
        if(current != null) {
            //Always override old player operation.
            current.future.cancel(true);
            l.remove(current);
        }
        
        RunningOperation o = new RunningOperation(operation, userId, channelId, timeoutSeconds * 1000);
        l.add(o);
        OPS.put(channelId, l);
        
        return o.future;
    }
    
    /**
     * @return The listener used to check for the InteractiveOperations.
     */
    public static EventListener listener() {
        return LISTENER;
    }
    
    /**
     * This class listens for all RunningOperation instances. Basically handles the operation run and termination procedures.
     */
    public static class InteractiveListener implements EventListener {
        @Override
        public void onEvent(GenericEvent e) {
            if(!(e instanceof GuildMessageReceivedEvent))
                return;
            
            GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) e;
            
            //Don't listen to ourselves...
            if(event.getAuthor().equals(event.getJDA().getSelfUser()))
                return;
            
            long channelId = event.getChannel().getIdLong();
            List<RunningOperation> l = OPS.get(channelId);
            
            if(l == null || l.isEmpty())
                return;
            
            l.removeIf(o -> {
                try {
                    int i = o.operation.run(event);
                    if(i == Operation.COMPLETED) {
                        o.future.complete(null);
                        return true;
                    }
                    if(i == Operation.RESET_TIMEOUT) {
                        o.resetTimeout();
                    }
                    return false;
                } catch(Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            });
        }
    }
    
    //Represents an eventually-running Operation.
    private static final class RunningOperation {
        final OperationFuture future;
        final InteractiveOperation operation;
        final long timeout;
        long timeoutTime;
        long userId;
        
        //timeout (argument) is in millis, field is in nanos
        RunningOperation(InteractiveOperation operation, long userId, long channelId, long timeout) {
            this.operation = operation;
            this.future = new OperationFuture(channelId, this);
            this.timeout = timeout * 1_000_000;
            this.userId = userId;
            resetTimeout();
        }
        
        boolean isTimedOut() {
            return timeoutTime - System.nanoTime() < 0;
        }
        
        void resetTimeout() {
            timeoutTime = System.nanoTime() + timeout;
        }
    }
    
    private static final class OperationFuture extends CompletableFuture<Void> {
        private final long id;
        private final RunningOperation operation;
        
        OperationFuture(long id, RunningOperation operation) {
            this.id = id;
            this.operation = operation;
        }
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            List<RunningOperation> l = OPS.get(id);
            
            if(l == null || !l.remove(operation))
                return false;
            
            operation.operation.onCancel();
            return true;
        }
    }
}
