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

package net.kodehawa.mantarobot.core.modules.commands.base;

import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.options.core.Option;

import java.util.List;

/**
 * Interface used for handling commands within the bot.
 */
public interface Command {
    /**
     * The Command's {@link CommandCategory}
     *
     * @return a Nullable {@link CommandCategory}. Null means that the command should be hidden from Help.
     */
    CommandCategory category();

    CommandPermission permission();

    /**
     * Invokes the command to be executed.
     *
     * @param context     the context of the event that triggered the command
     * @param commandName the command name that was used
     * @param content     the arguments of the command
     */
    void run(Context context, String commandName, String content);

    HelpContent help();

    Command addOption(String call, Option option);

    List<String> getAliases();

    default boolean isOwnerCommand() {
        return permission() == CommandPermission.OWNER;
    }
}
