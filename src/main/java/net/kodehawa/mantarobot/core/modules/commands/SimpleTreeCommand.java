/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.core.modules.commands;

import net.kodehawa.mantarobot.core.modules.commands.base.*;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public abstract class SimpleTreeCommand extends AbstractCommand implements ITreeCommand {
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private Predicate<Context> predicate = event -> true;

    public SimpleTreeCommand(CommandCategory category) {
        super(category);
    }

    public SimpleTreeCommand(CommandCategory category, CommandPermission permission) {
        super(category, permission);
    }

    /**
     * Invokes the command to be executed.
     *
     * @param context     the context of the event that triggered the command
     * @param commandName the command name that was used
     * @param content     the arguments of the command
     */
    @Override
    public void run(Context context, String commandName, String content) {
        var args = splitArgs(content, 2);

        if (subCommands.isEmpty()) {
            throw new IllegalArgumentException("No subcommands registered!");
        }

        var command = subCommands.get(args[0]);

        if (command == null) {
            defaultTrigger(context, commandName, args[0]);
            return;
        }

        if (!predicate.test(context)) {
            return;
        }

        command.run(new Context(context.getEvent(), context.getLanguageContext(), args[1]), commandName + " " + args[0], args[1]);
    }

    public void setPredicate(Predicate<Context> predicate) {
        this.predicate = predicate;
    }

    public SimpleTreeCommand addSubCommand(String name, String description, BiConsumer<Context, String> command) {
        subCommands.put(name, new SubCommand() {
            @Override
            public String description() {
                return description;
            }

            @Override
            protected void call(Context context, I18nContext languageContext, String content) {
                command.accept(context, content);
            }
        });

        return this;
    }

    public SimpleTreeCommand addSubCommand(String name, BiConsumer<Context, String> command) {
        return addSubCommand(name, null, command);
    }

    public SimpleTreeCommand addSubCommand(String name, SubCommand command) {
        subCommands.put(name, command);
        return this;
    }

    @Override
    public SimpleTreeCommand createSubCommandAlias(String name, String alias) {
        var cmd = subCommands.get(name);
        if (cmd == null) {
            throw new IllegalArgumentException("Cannot create an alias of a non-existent sub command!");
        }

        //Creates a fully new instance. Without this, it'd be dependant on the original instance, and changing the child status would change it's parent's status too.
        var clone = SubCommand.copy(cmd);
        clone.setChild(true);
        subCommands.put(alias, clone);

        return this;
    }

    @Override
    public Map<String, SubCommand> getSubCommands() {
        return subCommands;
    }

    /**
     * Handling for when the Sub-Command isn't found.
     *
     * @param ctx         the context of the event that triggered the command
     * @param commandName the Name of the not-found command.
     */
    public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
        //why?
        if (commandName.isEmpty()) {
            commandName = "none";
        }

        ctx.sendStripped(String.format(
                "%1$sNo subcommand `%2$s` found in the `%3$s` command!. Check `~>help %3$s` for available subcommands",
                EmoteReference.ERROR, commandName, mainCommand)
        );

        return null;
    }
}
