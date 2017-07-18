package net.kodehawa.mantarobot.commands.interaction.polls;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.junit.Assert;

public class PollBuilder {
    private String[] options;
    private GuildMessageReceivedEvent event;
    private long timeout;
    private String name = "";

    public PollBuilder setEvent(GuildMessageReceivedEvent event) {
        this.event = event;
        return this;
    }

    public PollBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public PollBuilder setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public PollBuilder setOptions(String... options) {
        this.options = options;
        return this;
    }

    public Poll build() {
        Assert.assertNotNull("Cannot create a poll with null options", options);
        Assert.assertNotNull("What is event :S", event);
        Assert.assertNotNull("You need to specify the timeout, pls.", timeout);

        return new Poll(event, name, timeout, options);
    }
}
