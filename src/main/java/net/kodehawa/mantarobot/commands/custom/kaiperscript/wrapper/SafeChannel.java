package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.entities.TextChannel;
import xyz.avarel.kaiper.exceptions.ComputeException;

class SafeChannel extends SafeISnowflake<TextChannel> {
    private final int maxMessages;
    private int messages = 0;

    SafeChannel(TextChannel channel, int maxMessages) {
        super(channel);
        this.maxMessages = maxMessages;
    }

    public void sendMessage(String message) {
        if(++messages >= maxMessages) throw new ComputeException("Maximum amount of messages reached");
        snowflake.sendMessage(message).queue();
    }

    public String getTopic() {
        return snowflake.getTopic();
    }

    public boolean isNSFW() {
        return snowflake.isNSFW();
    }

    public String getName() {
        return snowflake.getName();
    }

    public String getMention() {
        return snowflake.getAsMention();
    }
}
