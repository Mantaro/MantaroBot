package net.kodehawa.mantarobot.modules.commands;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.commands.base.CommandPermission;

import static net.kodehawa.mantarobot.commands.info.CommandStatsManager.log;

public abstract class SubCommand extends NoArgsCommand {
    public SubCommand() {
        super(null);
    }

    public SubCommand(CommandPermission permission) {
        super(null, permission);
    }

    @Override
    public MessageEmbed help(GuildMessageReceivedEvent event) {
        return null;
    }

    @Override
    public void run(GuildMessageReceivedEvent event, String commandName, String content) {
        call(event, content);
        log(commandName);
    }
}
