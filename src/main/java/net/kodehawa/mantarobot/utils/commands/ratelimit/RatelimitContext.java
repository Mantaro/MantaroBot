package net.kodehawa.mantarobot.utils.commands.ratelimit;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class RatelimitContext {
    private final Guild guild;
    private final Message message;
    private final TextChannel channel;
    private final GuildMessageReceivedEvent event;
    private final SlashCommandEvent slashEvent;

    public RatelimitContext(Guild guild, Message message, TextChannel channel, GuildMessageReceivedEvent event, SlashCommandEvent slashEvent) {
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

    public TextChannel getChannel() {
        return channel;
    }

    public GuildMessageReceivedEvent getEvent() {
        return event;
    }

    public SlashCommandEvent getSlashEvent() {
        return slashEvent;
    }

    public void send(String message) {
        if (slashEvent != null) {
            slashEvent.reply(message).queue();
        } else {
            event.getChannel().sendMessage(message).queue();
        }
    }
}
