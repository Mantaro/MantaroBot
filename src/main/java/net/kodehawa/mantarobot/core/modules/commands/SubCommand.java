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

import net.kodehawa.mantarobot.core.modules.commands.base.AssistedCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.base.InnerCommand;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;

public abstract class SubCommand implements InnerCommand, AssistedCommand {
    public boolean child;

    private CommandPermission permission = null;

    public SubCommand() {
    }

    public SubCommand(CommandPermission permission) {
        this.permission = permission;
    }

    /**
     * Creates a copy of a SubCommand, usually to assign child status to it.
     *
     * @param original The original SubCommand to copy.
     * @return The copy of the original SubCommand, without the description.
     */
    public static SubCommand copy(SubCommand original) {
        return new SubCommand(original.permission) {
            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                original.call(ctx, languageContext, content);
            }

            @Override
            public String description() {
                return null;
            }
        };
    }

    protected abstract void call(Context ctx, I18nContext languageContext, String content);

    @Override
    public CommandPermission permission() {
        return permission;
    }

    @Override
    public void run(Context ctx, String commandName, String content) {
        call(ctx, ctx.getLanguageContext(), content);
    }

    public boolean isChild() {
        return this.child;
    }

    public void setChild(boolean child) {
        this.child = child;
    }
}
