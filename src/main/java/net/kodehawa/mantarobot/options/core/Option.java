/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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

package net.kodehawa.mantarobot.options.core;

import br.com.brjdevs.java.utils.functions.interfaces.TriConsumer;
import lombok.Getter;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Option {

    @Getter
    private static final Map<String, Option> optionMap = new HashMap<>();
    //Display names + desc in the avaliable options list.
    @Getter
    private static final List<String> avaliableOptions = new ArrayList<>();
    @Getter
    private static String shortDescription = "Not set.";
    @Getter
    private final String description;
    @Getter
    private final String optionName;
    @Getter
    private final OptionType type;
    @Getter
    private TriConsumer<GuildMessageReceivedEvent, String[], I18nContext> eventConsumer;

    public Option(String displayName, String description, OptionType type) {
        this.optionName = displayName;
        this.description = description;
        this.type = type;
    }

    public static void addOption(String name, Option option) {
        Option.optionMap.put(name, option);
        String toAdd = String.format(
                "%-34s" + " | %s",
                name.replace(":", " "),
                getShortDescription()
        );
        Option.avaliableOptions.add(toAdd);
    }

    public static void addOptionAlias(String current, String name) {
        Option.optionMap.put(name, optionMap.get(current));
        String toAdd = String.format(
                "%-34s" + " | %s (Alias) ",
                name.replace(":", " "),
                getShortDescription()
        );
        Option.avaliableOptions.add(toAdd);
    }

    public Option setAction(Consumer<GuildMessageReceivedEvent> code) {
        eventConsumer = (event, ignored, ignored2) -> code.accept(event);
        return this;
    }

    public Option setActionLang(BiConsumer<GuildMessageReceivedEvent, I18nContext> code) {
        eventConsumer = (event, ignored, ctx) -> code.accept(event, ctx);
        return this;
    }

    public Option setAction(BiConsumer<GuildMessageReceivedEvent, String[]> code) {
        eventConsumer = (event, c, ignored) -> code.accept(event, c);
        return this;
    }

    public Option setActionLang(TriConsumer<GuildMessageReceivedEvent, String[], I18nContext> code) {
        eventConsumer = code;
        return this;
    }

    public Option setShortDescription(String sd) {
        shortDescription = sd;
        return this;
    }
}
