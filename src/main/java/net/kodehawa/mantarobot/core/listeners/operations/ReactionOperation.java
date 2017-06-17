package net.kodehawa.mantarobot.core.listeners.operations;

import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;

@FunctionalInterface
public interface ReactionOperation extends Operation {
    int add(MessageReactionAddEvent event);

    default int remove(MessageReactionRemoveEvent event) {
        return IGNORED;
    }

    default int removeAll(MessageReactionRemoveAllEvent event) {
        return IGNORED;
    }
}
