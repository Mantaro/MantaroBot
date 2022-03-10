package net.kodehawa.mantarobot.utils.commands;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class UtilsContext {
    private final Guild guild;
    private final Member member;
    private final TextChannel channel;
    private final SlashCommandEvent slashEvent;

    public UtilsContext(Guild guild, Member member, TextChannel channel, SlashCommandEvent event) {
        this.guild = guild;
        this.member = member;
        this.channel = channel;
        this.slashEvent = event;
    }

    public Guild getGuild() {
        return guild;
    }

    public Member getMember() {
        return member;
    }

    public TextChannel getChannel() {
        return channel;
    }

    public User getAuthor() {
        return member.getUser();
    }

    public Message send(String message) {
        if (slashEvent == null)
            return channel.sendMessage(message).complete();
        else
            return slashEvent.getHook().editOriginal(message).complete();
    }
}
