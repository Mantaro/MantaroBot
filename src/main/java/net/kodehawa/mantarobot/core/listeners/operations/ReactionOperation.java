package net.kodehawa.mantarobot.core.listeners.operations;

import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;

@FunctionalInterface
public interface ReactionOperation {
    boolean run(MessageReactionAddEvent event);

    default void onCancel() {

    }

    default void onExpire() {

    }
}
