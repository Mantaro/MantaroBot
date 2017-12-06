package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import lombok.Getter;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@Getter
public class SafeGuildMessageReceivedEvent {
    private final GuildMessageReceivedEvent event;
    private final SafeChannel channel;
    private final SafeUser author;
    private final SafeMember me;
    private final SafeGuild guild;
    private final SafeMessage message;

    public SafeGuildMessageReceivedEvent(GuildMessageReceivedEvent event, int maxMessages) {
        this.event = event;
        this.channel = new SafeChannel(event.getChannel(), maxMessages);
        this.author = new SafeMember(event.getMember());
        this.guild = new SafeGuild(event.getGuild(), channel);
        this.message = new SafeMessage(event.getMessage());
        this.me = new SafeMember(event.getGuild().getSelfMember());
    }

    @Override
    public String toString() {
        return event.toString();
    }
}
