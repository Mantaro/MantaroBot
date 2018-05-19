package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.entities.Message;

public class SafeMessage extends SafeJDAObject<Message> {
    SafeMessage(Message message) {
        super(message);
    }

    public String getDisplay() {
        return object.getContentDisplay().replace("@everyone", "\u200Deveryone").replace("@here", "\u200Dhere");
    }

    public String getRaw() {
        return object.getContentRaw().replace("@everyone", "\u200Deveryone").replace("@here", "\u200Dhere");
    }

    public String getStripped() {
        return object.getContentStripped().replace("@everyone", "\u200Deveryone").replace("@here", "\u200Dhere");
    }
}
