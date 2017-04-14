package net.kodehawa.mantarobot.modules;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public interface Command {
    void run(GuildMessageReceivedEvent event, String commandName, String content);
    CommandPermission permission();

    String description();

    MessageEmbed help(GuildMessageReceivedEvent event);

    Category category();

    boolean isHiddenFromHelp();
}
