package net.kodehawa.mantarobot.modules.commands;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.commands.base.AbstractCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.modules.commands.base.Command;
import net.kodehawa.mantarobot.modules.commands.base.CommandPermission;

import java.util.HashMap;
import java.util.Map;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public abstract class TreeCommand extends AbstractCommand {

    private Map<String, Command> subCommands = new HashMap<>();

    public TreeCommand(Category category) {
        super(category);
    }

    public TreeCommand(Category category, CommandPermission permission) {
        super(category, permission);
    }

    protected abstract Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName);

    @Override
    public void run(GuildMessageReceivedEvent event, String commandName, String content) {
        String[] args = splitArgs(content, 2);

        if(subCommands.isEmpty()){
            throw new IllegalArgumentException("No subcommands registered!");
        }

        Command command = subCommands.get(args[0]);
        if (command == null) command = defaultTrigger(event, commandName, args[0]);
        if(command == null) return; //Use SimpleTreeCommand then?

        command.run(event, commandName + " " + args[0], args[1]);
    }

    public TreeCommand addSubCommand(String name, Command command){
        subCommands.put(name, command);
        return this;
    }

    public Map<String, Command> getSubCommands() {
        return subCommands;
    }
}