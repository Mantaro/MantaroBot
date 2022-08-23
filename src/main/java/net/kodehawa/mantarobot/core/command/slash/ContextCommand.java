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

package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.EmbedBuilder;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;

import java.util.function.Predicate;

public abstract class ContextCommand<T> {
    private final String name;
    private final CommandPermission permission;
    private Predicate<InteractionContext<T>> predicate = c -> true;
    private final boolean guildOnly;
    private final boolean defer;

    // This is basically the same as NewCommand, but the handling ought to be different everywhere else.
    // There's no aliases either, too little slots.
    public ContextCommand() {
        var clazz = getClass();
        if (clazz.getAnnotation(Name.class) != null) {
            this.name = clazz.getAnnotation(Name.class).value();
        } else {
            this.name = clazz.getSimpleName().toLowerCase();
        }

        var p = clazz.getAnnotation(Permission.class);
        if (p == null) {
            this.permission = CommandPermission.USER;
        } else {
            this.permission = p.value();
        }

        this.defer = clazz.getAnnotation(Defer.class) != null;
        this.guildOnly = clazz.getAnnotation(GuildOnly.class) != null;
    }

    public String getName() {
        return name;
    }

    public CommandPermission getPermission() {
        return permission;
    }

    public boolean isGuildOnly() {
        return guildOnly;
    }

    public boolean defer() {
        return defer;
    }

    public void setPredicate(Predicate<InteractionContext<T>> predicate) {
        this.predicate = predicate;
    }

    // This is to be overriden.
    public Predicate<InteractionContext<T>> getPredicate() {
        return predicate;
    }

    protected abstract void process(InteractionContext<T> ctx);

    protected EmbedBuilder baseEmbed(InteractionContext<T> ctx, String name) {
        return baseEmbed(ctx, name, ctx.getMember().getEffectiveAvatarUrl());
    }

    protected EmbedBuilder baseEmbed(InteractionContext<T> ctx, String name, String image) {
        return new EmbedBuilder()
                .setAuthor(name, null, image)
                .setColor(ctx.getMember().getColor())
                .setFooter("Requested by: %s".formatted(ctx.getMember().getEffectiveName()),
                        ctx.getGuild().getIconUrl()
                );
    }

    public final void execute(InteractionContext<T> ctx) {
        // If this is over 2500ms, we should attempt to defer instead, as discord might be lagging.
        var averageLatencyMax = MantaroBot.getInstance().getCore().getRestPing() * 4;
        if (!getPredicate().test(ctx)) {
            return;
        }

        if (defer() || averageLatencyMax > 2500) {
            ctx.defer();
        }

        process(ctx);
    }
}
