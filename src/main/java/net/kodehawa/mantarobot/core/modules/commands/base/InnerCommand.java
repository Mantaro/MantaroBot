package net.kodehawa.mantarobot.core.modules.commands.base;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public interface InnerCommand extends Command {
    @Override
    default Category category() {
        return null;
    }

    @Override
    default MessageEmbed help(GuildMessageReceivedEvent event) {
        return null;
    }
}