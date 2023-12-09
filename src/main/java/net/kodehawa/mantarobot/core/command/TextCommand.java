/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core.command;

import net.kodehawa.mantarobot.core.command.helpers.CommandPermission;
import net.kodehawa.mantarobot.core.command.meta.Alias;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// common superclass for either commands or options
public abstract class TextCommand extends AnnotatedCommand<TextContext> {
    private final Map<String, TextCommand> children = new HashMap<>();
    private final Map<String, String> childrenAliases = new HashMap<>();
    private final List<String> aliases;

    private TextCommand parent;

    public TextCommand() {
        super();
        var clazz = getClass();
        this.aliases = Arrays.stream(clazz.getAnnotationsByType(Alias.class))
                .map(Alias::value).toList();
    }

    public List<String> aliases() {
        return aliases;
    }

    @Override
    public CommandPermission getPermission() {
        if (permission != CommandPermission.INHERIT) {
            return permission;
        }
        if (parent == null) {
            return category == null ? CommandPermission.OWNER : category.permission;
        }
        return parent.getPermission();
    }

    @Override
    public final void execute(TextContext ctx) {
        var args = ctx.arguments();
        if (args.hasNext()) {
            var name = args.next().getValue().toLowerCase();
            var child = children.get(name);
            if (child == null) {
                child = children.get(childrenAliases.getOrDefault(name, ""));
            }
            if (child != null) {
                child.execute(ctx);
                return;
            }
            args.back();
        }
        process(ctx);
    }

    void registerParent(TextCommand parent) {
        this.parent = parent;
        parent.children.put(name, this);
        aliases.forEach(a -> parent.childrenAliases.put(a, name));
    }

    @Override
    protected CommandPermission getDefaultPermission() {
        return CommandPermission.INHERIT;
    }
}
