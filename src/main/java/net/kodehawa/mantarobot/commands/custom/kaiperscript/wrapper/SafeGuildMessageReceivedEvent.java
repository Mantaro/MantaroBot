package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import lombok.Getter;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@Getter
public class SafeGuildMessageReceivedEvent {
    private final GuildMessageReceivedEvent event;
    private final SafeChannel channel;
    private final SafeUser author;
    private final SafeUser me;
    private final SafeMember member;
    private final SafeGuild guild;
    private final SafeMessage message;

    public SafeGuildMessageReceivedEvent(GuildMessageReceivedEvent event, int maxMessages) {
        this.event = event;
        this.channel = new SafeChannel(event.getChannel(), maxMessages);
        this.author = new SafeUser(event.getAuthor());
        this.member = new SafeMember(event.getMember());
        this.guild = new SafeGuild(event.getGuild(), channel);
        this.message = new SafeMessage(event.getMessage());
        this.me = new SafeUser(event.getJDA().getSelfUser());
    }

    @Override
    public String toString() {
        return event.toString();
    }
}
