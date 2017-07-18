package net.kodehawa.mantarobot.modules;

import net.kodehawa.mantarobot.modules.commands.base.Command;

import java.util.Map;

public interface CommandRegistry {
    Map<String, Command> commands();

    Command register(String commandName, Command command);

    void registerAlias(String commandName, String originalCommand);
}
