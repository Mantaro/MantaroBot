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

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;

import java.util.List;

public class SlashContext extends BaseInteractionContext<SlashCommandInteractionEvent> {

    public SlashContext(SlashCommandInteractionEvent event, I18nContext i18n) {
        super(event, i18n);
    }

    public String getSubCommand() {
        return event.getSubcommandName();
    }

    public OptionMapping getOption(String name) {
        return event.getOption(name);
    }

    // This is a little cursed, but I guess we can make do.
    @SuppressWarnings("unused")
    public List<OptionMapping> getOptions() {
        return event.getOptions();
    }

    // Cursed wrapper to get around null checks on getAsX
    public Role getOptionAsRole(String name) {
        var option = getOption(name);
        if (option == null) {
            return null;
        }

        return option.getAsRole();
    }

    public User getOptionAsGlobalUser(String name) {
        return getOptionAsGlobalUser(name, null);
    }

    public User getOptionAsGlobalUser(String name, User def) {
        var option = getOption(name);
        if (option == null) {
            return def;
        }

        return option.getAsUser();
    }

    public User getOptionAsUser(String name) {
        return getOptionAsUser(name, null);
    }

    public Member getOptionAsMember(String name) {
        return getOptionAsMember(name, null);
    }

    public Member getOptionAsMember(String name, Member def) {
        var option = getOption(name);
        if (option == null || option.getAsMember() == null) {
            return def;
        }

        return option.getAsMember();
    }

    public User getOptionAsUser(String name, User def) {
        var option = getOption(name);
        if (option == null || option.getAsMember() == null) {
            return def;
        }

        return option.getAsUser();
    }

    public String getOptionAsString(String name) {
        return getOptionAsString(name, null);
    }

    public String getOptionAsString(String name, String def) {
        var option = getOption(name);
        if (option == null) {
            return def;
        }

        return option.getAsString();
    }

    @SuppressWarnings("unused")
    public long getOptionAsLong(String name) {
        return getOptionAsLong(name, 0);
    }

    public long getOptionAsLong(String name, long def) {
        var option = getOption(name);
        if (option == null) {
            return def;
        }

        // This is very much just making sure...
        return Math.max(1, Math.abs(option.getAsLong()));
    }

    public int getOptionAsInteger(String name) {
        return getOptionAsInteger(name, 0);
    }

    public int getOptionAsInteger(String name, int def) {
        var option = getOption(name);
        if (option == null) {
            return def;
        }

        return (int) Math.max(1, Math.abs(option.getAsLong()));
    }

    public boolean getOptionAsBoolean(String name) {
        var option = getOption(name);
        if (option == null) {
            return false;
        }

        return option.getAsBoolean();
    }
}
