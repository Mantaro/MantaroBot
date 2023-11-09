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

import net.kodehawa.mantarobot.MantaroBot;

import java.util.function.Predicate;

public abstract class ContextCommand<T> extends DeferrableCommand<InteractionContext<T>> {
    private Predicate<InteractionContext<T>> predicate = c -> true;

    public ContextCommand() {
        super();
    }

    @SuppressWarnings("unused")
    public void setPredicate(Predicate<InteractionContext<T>> predicate) {
        this.predicate = predicate;
    }

    // This is to be overriden.
    public Predicate<InteractionContext<T>> getPredicate() {
        return predicate;
    }

    @Override
    public final void execute(InteractionContext<T> ctx) {
        // If this is over 2500ms, we should attempt to defer instead, as discord might be lagging.
        var averageLatencyMax = MantaroBot.getInstance().getCore().getRestPing() * 4;
        if (!getPredicate().test(ctx)) {
            return;
        }

        if ((defer() || averageLatencyMax > 2500) && !modal) {
            ctx.defer();
        }

        ctx.setForceEphemeral(true);
        process(ctx);
    }
}
