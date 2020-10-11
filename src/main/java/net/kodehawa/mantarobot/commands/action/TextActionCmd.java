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

package net.kodehawa.mantarobot.commands.action;

import net.kodehawa.mantarobot.core.modules.commands.NoArgsCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;

import java.util.List;
import java.util.Random;

public class TextActionCmd extends NoArgsCommand {
    private final String desc;
    private final String format;
    private final List<String> strings;
    private final Random rand = new Random();

    public TextActionCmd(String desc, String format, List<String> strings) {
        super(CommandCategory.ACTION);
        this.desc = desc;
        this.format = format;
        this.strings = strings;
    }

    @Override
    protected void call(Context ctx, String content) {
        ctx.send(String.format(format, strings.get(rand.nextInt(strings.size()))));
    }

    @Override
    public HelpContent help() {
        return new HelpContent.Builder()
                .setDescription(desc)
                .build();
    }
}
