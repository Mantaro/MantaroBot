package net.kodehawa.mantarobot.core.modules.commands;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.base.*;

import java.util.HashMap;
import java.util.Map;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public abstract class TreeCommand extends AbstractCommand implements ITreeCommand {

    private Map<String, InnerCommand> subCommands = new HashMap<>();

    public TreeCommand(Category category) {
        super(category);
    }

    public TreeCommand(Category category, CommandPermission permission) {
        super(category, permission);
    }

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

    @Override
    public ITreeCommand addSubCommand(String name, SubCommand command){
        subCommands.put(name, command);
        return this;
    }

    @Override
    public Map<String, InnerCommand> getSubCommands() {
        return subCommands;
    }

    @Override
    public TreeCommand createSubCommandAlias(String name, String alias){
        InnerCommand cmd = subCommands.get(name);

        if(cmd == null){
            throw new IllegalArgumentException("Cannot create an alias of a non-existent sub command!");
        }

        subCommands.put(alias, cmd);

        return this;
    }
}