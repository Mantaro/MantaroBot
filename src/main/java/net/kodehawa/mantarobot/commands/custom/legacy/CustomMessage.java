package net.kodehawa.mantarobot.commands.custom.legacy;

import net.dv8tion.jda.api.entities.Message;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public class CustomMessage {
    private Message message;
    private String prefix;

    public CustomMessage(Message message, String prefix) {
        this.message = message;
        this.prefix = prefix;
    }

    public String getContentRaw() {
        if(prefix.isEmpty()) {
            return splitArgs(message.getContentRaw(), 2)[1];
        }

        return splitArgs(message.getContentRaw().replace(prefix, "").trim(), 2)[1];
    }

    public String getContentDisplay() {
        if(prefix.isEmpty()) {
            return splitArgs(message.getContentDisplay(), 2)[1];
        }

        return splitArgs(message.getContentDisplay().replace(prefix, "").trim(), 2)[1];
    }

    public String getContentStripped() {
        if(prefix.isEmpty()) {
            return splitArgs(message.getContentStripped(), 2)[1];
        }

        return splitArgs(message.getContentStripped().replace(prefix, "").trim(), 2)[1];
    }
}
