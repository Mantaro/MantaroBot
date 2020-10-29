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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public abstract class TreeCommand extends AbstractCommand implements ITreeCommand {

    private final Map<String, SubCommand> subCommands = new HashMap<>();
    //By default let all commands pass.
    private Predicate<Context> predicate = event -> true;

    public TreeCommand(CommandCategory category) {
        super(category);
    }

    public TreeCommand(CommandCategory category, CommandPermission permission) {
        super(category, permission);
    }

    @Override
    public void run(Context context, String commandName, String content) {
        String[] args = splitArgs(content, 2);

        if (subCommands.isEmpty()) {
            throw new IllegalArgumentException("No subcommands registered!");
        }

        Command command = subCommands.get(args[0]);
        boolean isDefault = false;
        if (command == null) {
            command = defaultTrigger(context, commandName, content);
            isDefault = true;
        }
        if (command == null)
            return; //Use SimpleTreeCommand then?

        var ct = isDefault ? content : args[1];

        if (!predicate.test(context)) return;

        command.run(new Context(context.getEvent(), context.getLanguageContext(), ct), commandName + (isDefault ? "" : " " + args[0]), ct);
    }

    public TreeCommand addSubCommand(String name, BiConsumer<Context, String> command) {
        subCommands.put(name, new SubCommand() {
            @Override
            protected void call(Context context, I18nContext lang, String content) {
                command.accept(context, content);
            }
        });
        return this;
    }

    public void setPredicate(Predicate<Context> predicate) {
        this.predicate = predicate;
    }

    @Override
    public TreeCommand createSubCommandAlias(String name, String alias) {
        SubCommand cmd = subCommands.get(name);
        if (cmd == null) {
            throw new IllegalArgumentException("Cannot create an alias of a non-existent sub command!");
        }

        //Creates a fully new instance. Without this, it'd be dependant on the original instance, and changing the child status would change it's parent's status too.
        SubCommand clone = SubCommand.copy(cmd);
        clone.setChild(true);
        subCommands.put(alias, clone);

        return this;
    }

    @Override
    public ITreeCommand addSubCommand(String name, SubCommand command) {
        subCommands.put(name, command);
        return this;
    }

    @Override
    public Map<String, SubCommand> getSubCommands() {
        return subCommands;
    }
}
