package net.kodehawa.mantarobot.utils.commands.ratelimit;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RatelimitContext {
    private final Guild guild;
    private final Message message;
    private final GuildMessageChannel channel;
    private final MessageReceivedEvent event;
    private final SlashCommandInteractionEvent slashEvent;

    public RatelimitContext(Guild guild, Message message, GuildMessageChannel channel, MessageReceivedEvent event, SlashCommandInteractionEvent slashEvent) {
        this.guild = guild;
        this.message = message;
        this.channel = channel;
        this.event = event;
        this.slashEvent = slashEvent;
    }

    public Guild getGuild() {
        return guild;
    }

    public Message getMessage() {
        return message;
    }

    public GuildMessageChannel getChannel() {
        return channel;
    }

    public MessageReceivedEvent getEvent() {
        return event;
    }

    public SlashCommandInteractionEvent getSlashEvent() {
        return slashEvent;
    }

    public void send(String message) {
        if (slashEvent != null) {
            if (slashEvent.isAcknowledged()) {
                slashEvent.getHook().sendMessage(message).queue();
            } else {
                slashEvent.reply(message).queue();
            }
        } else {
            event.getChannel().sendMessage(message).queue();
        }
    }
}
