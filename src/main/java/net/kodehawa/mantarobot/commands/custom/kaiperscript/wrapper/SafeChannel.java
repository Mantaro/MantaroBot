package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.entities.TextChannel;

class SafeChannel extends SafeISnowflake<TextChannel> {
    private int messages = 0;

    SafeChannel(TextChannel channel) {
        super(channel);
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
