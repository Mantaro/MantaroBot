package net.kodehawa.mantarobot.core.listeners.operations;

import lombok.SneakyThrows;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.utils.TimeAmount;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class InteractiveOperationBuilder {
    private String channelId;
    private TimeAmount increasingTimeout;
    private TimeAmount initialTimeout;
    private Predicate<GuildMessageReceivedEvent> onMessage;
    private Runnable onTimeout, onRemoved;

    InteractiveOperationBuilder() {
    }

    public InteractiveOperationBuilder channel(String channelId) {
        this.channelId = channelId;
        return this;
    }

    public InteractiveOperationBuilder channel(TextChannel channel) {
        this.channelId = channel.getId();
        return this;
    }

    public void create(Runnable success, Consumer<Throwable> failure) {
        try {
            new InteractiveOperation(
                    Objects.requireNonNull(channelId, "channelId"),
                    Objects.requireNonNull(initialTimeout, "initialTimeout"),
                    increasingTimeout,
                    Objects.requireNonNull(onMessage, "onMessage"),
                    onTimeout,
                    onRemoved
            );

            success.run();
        } catch(Exception e) {
            failure.accept(e);
        }
    }

    public void create(Runnable success) {
        create(
                success,
                new Consumer<Throwable>() {
                    @Override
                    @SneakyThrows
                    public void accept(Throwable e) {
                        if(e == null) return;
                        throw e;
                    }
                }
        );
    }

    public void create() {
        create(() -> {
        });
    }

    public void forceCreate() {
        InteractiveOperation.stopOperation(channelId);
        create();
    }

    public InteractiveOperationBuilder increasingTimeout(long amount, TimeUnit unit) {
        this.increasingTimeout = new TimeAmount(amount, unit);
        return this;
    }

    public InteractiveOperationBuilder initialTimeout(long amount, TimeUnit unit) {
        this.initialTimeout = new TimeAmount(amount, unit);
        return this;
    }

    public InteractiveOperationBuilder onMessage(Predicate<GuildMessageReceivedEvent> onMessage) {
        this.onMessage = onMessage;
        return this;
    }

    public InteractiveOperationBuilder onRemoved(Runnable onRemoved) {
        this.onRemoved = onRemoved;
        return this;
    }

    public InteractiveOperationBuilder onTimeout(Runnable onTimeout) {
        this.onTimeout = onTimeout;
        return this;
    }

    public InteractiveOperationBuilder timeout(long amount, TimeUnit unit) {
        this.initialTimeout = new TimeAmount(amount, unit);
        this.increasingTimeout = new TimeAmount(amount, unit);
        return this;
    }
}
