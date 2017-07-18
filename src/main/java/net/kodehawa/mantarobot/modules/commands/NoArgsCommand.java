package net.kodehawa.mantarobot.modules.commands;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.commands.base.AbstractCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;

import static net.kodehawa.mantarobot.commands.info.CommandStatsManager.log;

public abstract class NoArgsCommand extends AbstractCommand {
    public NoArgsCommand(Category category) {
        super(category);
    }

    public NoArgsCommand(Category category, CommandPermission permission) {
        super(category, permission);
    }

    protected abstract void call(GuildMessageReceivedEvent event, String content);

    @Override
    public void run(GuildMessageReceivedEvent event, String commandName, String content) {
        call(event, content);
        log(commandName);
    }
}