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

package net.kodehawa.mantarobot.commands.custom.v3.interpreter;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;

import java.util.HashMap;
import java.util.Map;

public class InterpreterContext {
    private final Map<String, Object> custom = new HashMap<>();
    private final Map<String, String> vars;
    private final Map<String, Operation> operations;
    private final Context commandContext;

    public InterpreterContext(Map<String, String> vars, Map<String, Operation> operations, Context ctx) {
        this.vars = vars;
        this.operations = operations;
        this.commandContext = ctx;
    }

    public Map<String, String> vars() {
        return vars;
    }

    public Map<String, Operation> operations() {
        return operations;
    }

    public GuildMessageReceivedEvent event() {
        return commandContext.getEvent();
    }

    public void set(String key, Object value) {
        custom.put(key, value);
    }

    public Context getCommandContext() {
        return commandContext;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) custom.get(key);
    }
}
