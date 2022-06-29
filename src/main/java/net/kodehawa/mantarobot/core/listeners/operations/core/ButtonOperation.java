package net.kodehawa.mantarobot.core.listeners.operations.core;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface ButtonOperation extends Operation {
    int click(ButtonInteractionEvent event);
}
