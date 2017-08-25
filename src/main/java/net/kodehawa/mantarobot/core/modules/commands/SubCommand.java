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
