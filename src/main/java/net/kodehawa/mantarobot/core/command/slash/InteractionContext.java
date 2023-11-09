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

import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericContextInteractionEvent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;

public class InteractionContext<T> extends BaseInteractionContext<GenericContextInteractionEvent<T>> {

    public InteractionContext(GenericContextInteractionEvent<T> event, I18nContext i18n) {
        super(event, i18n);
    }

    public T getTarget() {
        return event.getTarget();
    }

    @SuppressWarnings("unused")
    public GuildChannel getGuildChannel() {
        return event.getGuildChannel();
    }

    @Override
    public boolean defaultEphemeral() {
        return true;
    }
}
