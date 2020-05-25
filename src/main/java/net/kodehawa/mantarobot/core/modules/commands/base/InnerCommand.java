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

import java.util.Collections;
import java.util.List;

public interface InnerCommand extends Command {
    @Override
    default Category category() {
        return null;
    }

    @Override
    default HelpContent help() {
        return null;
    }

    default List<String> getAliases() {
        return Collections.emptyList();
    }

    //Override this for the subcommand usage to appear on help!
    default String description() {
        return "";
    }

    default boolean isChild() {
        return false;
    }
}
