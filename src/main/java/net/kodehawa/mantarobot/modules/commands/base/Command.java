package net.kodehawa.mantarobot.modules.commands.base;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.commands.CommandPermission;
import net.kodehawa.mantarobot.options.Option;

/**
 * Interface used for handling commands within the bot.
 */
public interface Command {
    /**
     * The Command's {@link Category}
     *
     * @return a Nullable {@link Category}. Null means that the command should be hidden from Help.
     */
    Category category();

    /**
     * Embed to be used on help command
     *
     * @param event the event that triggered the help
     * @return a Nullable {@link MessageEmbed}
     */
    MessageEmbed help(GuildMessageReceivedEvent event);

    CommandPermission permission();

    /**
     * Invokes the command to be executed.
     *
     * @param event       the event that triggered the command
     * @param commandName the command name that was used
     * @param content     the arguments of the command
     */
    void run(GuildMessageReceivedEvent event, String commandName, String content);

    Command addOption(String call, Option option);
}
