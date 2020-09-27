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

import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.options.core.Option;

import java.util.List;

public class AliasCommand implements Command {
    private final Command command;
    private final String commandName;
    private final String originalName;
    private final List<String> aliases;

    public AliasCommand(String commandName, String originalName, Command command) {
        this.commandName = commandName;
        this.command = command;
        this.originalName = originalName;
        this.aliases = command.getAliases();
    }

    public CommandCategory parentCategory() {
        return command.category();
    }

    public String parentName() {
        return originalName;
    }

    @Override
    public CommandCategory category() {
        return null; //Alias Commands are hidden
    }

    @Override
    public CommandPermission permission() {
        return command.permission();
    }

    @Override
    public void run(Context context, String ignored, String content) {
        command.run(context, commandName, content);
    }

    @Override
    public HelpContent help() {
        return command.help();
    }

    @Override
    public Command addOption(String call, Option option) {
        Option.addOption(call, option);
        return this;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    public Command getCommand() {
        return this.command;
    }

    public String getCommandName() {
        return this.commandName;
    }

    public String getOriginalName() {
        return this.originalName;
    }
}
