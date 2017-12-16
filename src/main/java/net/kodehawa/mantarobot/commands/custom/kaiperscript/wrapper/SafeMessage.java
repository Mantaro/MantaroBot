package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.entities.Message;

class SafeMessage extends SafeISnowflake<Message> {
    SafeMessage(Message message) {
        super(message);
    }

    public String getDisplay() {
        return snowflake.getContentDisplay();
    }

    public String getRaw() {
        return snowflake.getContentRaw();
    }

    public String getStripped() {
        return snowflake.getContentStripped();
    }
}
