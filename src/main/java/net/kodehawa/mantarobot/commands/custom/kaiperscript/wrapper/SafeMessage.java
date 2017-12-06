package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.entities.Message;

class SafeMessage extends SafeISnowflake<Message> {
    SafeMessage(Message message) {
        super(message);
    }

    public String getContent() {
        return snowflake.getContent();
    }

    public String getRaw() {
        return snowflake.getRawContent();
    }

    public String getStripped() {
        return snowflake.getStrippedContent();
    }
}
