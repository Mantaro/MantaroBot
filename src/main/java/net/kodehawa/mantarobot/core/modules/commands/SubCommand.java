package net.kodehawa.mantarobot.core.modules.commands;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.base.AssistedCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.base.InnerCommand;

public abstract class SubCommand implements InnerCommand, AssistedCommand {
    private CommandPermission permission = null;

    public SubCommand() {}

    public SubCommand(CommandPermission permission) {
        this.permission = permission;
    }

    protected abstract void call(GuildMessageReceivedEvent event, String content);

    @Override
    public CommandPermission permission() {
        return permission;
    }

    @Override
    public void run(GuildMessageReceivedEvent event, String commandName, String content) {
        call(event, content);
    }
}
