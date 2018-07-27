package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.entities.TextChannel;

public class SafeChannel extends SafeJDAObject<TextChannel> {

    SafeChannel(TextChannel channel) {
        super(channel);
    }

    public String getTopic() {
        return object.getTopic();
    }

    public boolean getIsNSFW() {
        return object.isNSFW();
    }

    public String getName() {
        return object.getName();
    }

    public String getMention() {
        return object.getAsMention();
    }

    @Override
    public String toString() {
        return "Channel(" + getId() + ")";
    }
}
