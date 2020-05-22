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

package net.kodehawa.mantarobot.options.core;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.TriConsumer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class OptionHandler {
    protected OptionType type = OptionType.GENERAL;

    public abstract String description();

    protected void registerOption(String name, String displayName, String description, Consumer<GuildMessageReceivedEvent> code) {
        Option.addOption(name, new Option(displayName, description, type).setAction(code).setShortDescription(description));
    }

    protected void registerOption(String name, String displayName, String description, String shortDescription, Consumer<GuildMessageReceivedEvent> code) {
        Option.addOption(name, new Option(displayName, description, type).setAction(code).setShortDescription(shortDescription));
    }

    protected void registerOption(String name, String displayName, String description, String shortDescription, BiConsumer<GuildMessageReceivedEvent, String[]> code) {
        Option.addOption(name, new Option(displayName, description, type).setAction(code).setShortDescription(shortDescription));
    }

    protected void registerOptionShort(String name, String displayName, String description, String shortDescription, BiConsumer<GuildMessageReceivedEvent, I18nContext> code) {
        Option.addOption(name, new Option(displayName, description, type).setActionLang(code).setShortDescription(shortDescription));
    }

    protected void registerOption(String name, String displayName, String description, BiConsumer<GuildMessageReceivedEvent, I18nContext> code) {
        Option.addOption(name, new Option(displayName, description, type).setActionLang(code).setShortDescription(description));
    }

    protected void registerOption(String name, String displayName, String description, String shortDescription,
                                  TriConsumer<GuildMessageReceivedEvent, String[], I18nContext> code) {
        Option.addOption(name, new Option(displayName, description, type).setActionLang(code).setShortDescription(shortDescription));
    }

    protected void addOptionAlias(String original, String alias) {
        Option.addOptionAlias(original, alias);
    }

    public void setType(OptionType type) {
        this.type = type;
    }
}
