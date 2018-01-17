/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public abstract class SimpleTreeCommand extends AbstractCommand implements ITreeCommand {
    private Map<String, InnerCommand> subCommands = new HashMap<>();
    private Predicate<GuildMessageReceivedEvent> predicate = event -> true;

    public SimpleTreeCommand(Category category) {
        super(category);
    }

    public SimpleTreeCommand(Category category, CommandPermission permission) {
        super(category, permission);
    }

    /**
     * Handling for when the Sub-Command isn't found.
     *
     * @param event       the Event
     * @param commandName the Name of the not-found command.
     */
    public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
        if(commandName.isEmpty()) commandName = "none";
        event.getChannel().sendMessage(String.format("%sNo subcommand `%s` found in the `%s` command! Help for this command will be shown below.", EmoteReference.ERROR, commandName, mainCommand)).queue();
        onHelp(event);

        return null;
    }

    /**
     * Invokes the command to be executed.
     *
     * @param event       the event that triggered the command
     * @param commandName the command name that was used
     * @param content     the arguments of the command
     */
    @Override
    public void run(GuildMessageReceivedEvent event, String commandName, String content) {
        String[] args = splitArgs(content, 2);

        if(subCommands.isEmpty()) {
            throw new IllegalArgumentException("No subcommands registered!");
        }

        Command command = subCommands.get(args[0]);

        if(command == null) {
            defaultTrigger(event, commandName, args[0]);
            return;
        }

        if(!predicate.test(event)) return;
        command.run(event, commandName + " " + args[0], args[1]);
    }

    public ITreeCommand setPredicate(Predicate<GuildMessageReceivedEvent> predicate) {
        this.predicate = predicate;
        return this;
    }

    public SimpleTreeCommand addSubCommand(String name, SubCommand command) {
        subCommands.put(name, command);
        return this;
    }

    @Override
    public Map<String, InnerCommand> getSubCommands() {
        return subCommands;
    }

    @Override
    public SimpleTreeCommand createSubCommandAlias(String name, String alias) {
        InnerCommand cmd = subCommands.get(name);

        if(cmd == null) {
            throw new IllegalArgumentException("Cannot create an alias of a non-existent sub command!");
        }

        subCommands.put(alias, cmd);

        return this;
    }
}
