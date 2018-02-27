package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import lombok.Getter;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@Getter
public class SafeGuildMessageReceivedEvent {
    private final GuildMessageReceivedEvent event;
    private final SafeChannel channel;
    private final SafeMember author;
    private final SafeMember me;
    private final SafeGuild guild;
    private final SafeMentions mentions;
    private final SafeMessage message;

    public SafeGuildMessageReceivedEvent(GuildMessageReceivedEvent event) {
        this.event = event;
        this.channel = new SafeChannel(event.getChannel());
        this.author = new SafeMember(event.getMember());
        this.guild = new SafeGuild(event.getGuild(), channel);
        this.message = new SafeMessage(event.getMessage());
        this.me = new SafeMember(event.getGuild().getSelfMember());
        this.mentions = new SafeMentions(event.getMessage().getMentionedMembers());
    }

    @Override
    public String toString() {
        return "GuildMessageReceivedEvent";
    }
}
