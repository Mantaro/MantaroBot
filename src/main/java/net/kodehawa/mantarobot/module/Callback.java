package net.kodehawa.mantarobot.module;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public interface Callback extends Container {
    void onCommand(String[] args, String content, MessageReceivedEvent event);
}
