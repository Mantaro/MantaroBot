package net.kodehawa.mantarobot.modules.commands.base;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.commands.SubCommand;

import java.util.Map;

public interface ITreeCommand extends Command {
    ITreeCommand createSubCommandAlias(String name, String alias);
    ITreeCommand addSubCommand(String name, SubCommand command);
    Map<String, SubCommand> getSubCommands();
    Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName);
}
