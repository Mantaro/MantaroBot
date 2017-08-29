/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

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

        if(subCommands.isEmpty()) {
            throw new IllegalArgumentException("No subcommands registered!");
        }

        Command command = subCommands.get(args[0]);
        if(command == null) command = defaultTrigger(event, commandName, args[0]);
        if(command == null) return; //Use SimpleTreeCommand then?

        command.run(event, commandName + " " + args[0], args[1]);
    }

    @Override
    public ITreeCommand addSubCommand(String name, SubCommand command) {
        subCommands.put(name, command);
        return this;
    }

    @Override
    public Map<String, InnerCommand> getSubCommands() {
        return subCommands;
    }

    @Override
    public TreeCommand createSubCommandAlias(String name, String alias) {
        InnerCommand cmd = subCommands.get(name);

        if(cmd == null) {
            throw new IllegalArgumentException("Cannot create an alias of a non-existent sub command!");
        }

        subCommands.put(alias, cmd);

        return this;
    }
}