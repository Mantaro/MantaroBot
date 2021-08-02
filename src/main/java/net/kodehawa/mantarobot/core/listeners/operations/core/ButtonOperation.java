package net.kodehawa.mantarobot.core.listeners.operations.core;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public interface ButtonOperation extends Operation {
    int click(ButtonClickEvent event);
    default int remove(ButtonClickEvent event) {
        return IGNORED;
    }
}
