package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.entities.Message;

class SafeMessage extends SafeJDAObject<Message> {
    SafeMessage(Message message) {
        super(message);
    }

    public String getDisplay() {
        return object.getContentDisplay();
    }

    public String getRaw() {
        return object.getContentRaw();
    }

    public String getStripped() {
        return object.getContentStripped();
    }
}
