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

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.kodehawa.mantarobot.core.command.slash.ContextCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;

import javax.annotation.Nonnull;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager {
    private final Map<String, NewCommand> commands = new HashMap<>();
    private final Map<String, SlashCommand> slashCommands = new HashMap<>();
    private final Map<String, ContextCommand<User>> contextUserCommand = new HashMap<>();
    private final Map<String, ContextCommand<Message>> contextMessageCommand = new HashMap<>();
    private final static List<CommandData> slashCommandsList = new ArrayList<>();
    private final static List<CommandData> contextUserCommandList = new ArrayList<>();
    private final static List<CommandData> contextMessageCommandList = new ArrayList<>();
    private final Map<String, String> aliases = new HashMap<>();

    public Map<String, NewCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    public Map<String, SlashCommand> slashCommands() {
        return Collections.unmodifiableMap(slashCommands);
    }

    public Map<String, ContextCommand<User>> contextUserCommands() {
        return Collections.unmodifiableMap(contextUserCommand);
    }

    public Map<String, ContextCommand<Message>> contextMessageCommands() {
        return Collections.unmodifiableMap(contextMessageCommand);
    }

    public <T extends NewCommand> T register(@Nonnull Class<T> clazz) {
        return register(instantiate(clazz));
    }
    public <T extends NewCommand> T register(@Nonnull T command) {
        if (commands.putIfAbsent(command.name(), command) != null) {
            throw new IllegalArgumentException("Duplicate command " + command.name());
        }
        for (var alias : command.aliases()) {
            if (aliases.putIfAbsent(alias, command.name()) != null) {
                throw new IllegalArgumentException("Duplicate alias " + alias);
            }
        }
        registerSubcommands(command);
        return command;
    }

    public <T extends SlashCommand> T registerSlash(@Nonnull Class<T> clazz) {
        return registerSlash(instantiate(clazz));
    }

    public <T extends ContextCommand<User>> T registerContextUser(@Nonnull Class<T> clazz) {
        return registerContextUser(instantiate(clazz));
    }

    public <T extends ContextCommand<Message>> T registerContextMessage(@Nonnull Class<T> clazz) {
        return registerContextMessage(instantiate(clazz));
    }

    private <T extends ContextCommand<User>> T registerContextUser(@Nonnull T command) {
        if (contextUserCommand.putIfAbsent(command.getName(), command) != null) {
            throw new IllegalArgumentException("Duplicate context command (user)" + command.getName());
        }

        CommandData commandData = Commands.user(command.getName()).setGuildOnly(true);
        contextUserCommand.put(command.getName(), command);
        contextUserCommandList.add(commandData);
        return command;
    }

    private <T extends ContextCommand<Message>> T registerContextMessage(@Nonnull T command) {
        if (contextMessageCommand.putIfAbsent(command.getName(), command) != null) {
            throw new IllegalArgumentException("Duplicate context command (message)" + command.getName());
        }

        CommandData commandData = Commands.user(command.getName()).setGuildOnly(true);
        contextMessageCommand.put(command.getName(), command);
        contextMessageCommandList.add(commandData);
        return command;
    }

    private <T extends SlashCommand> T registerSlash(@Nonnull T command) {
        if (slashCommands.putIfAbsent(command.getName(), command) != null) {
            throw new IllegalArgumentException("Duplicate command " + command.getName());
        }

        registerSubcommands(command);
        CommandData commandData;
        // So you can't have root commands if you have subcommands, why?
        if (command.getSubCommands().isEmpty()) {
            commandData = Commands.slash(command.getName(), "[%s] %s".formatted(command.getCategory().readableName(), command.getDescription()))
                    .setNSFW(command.isNsfw())
                    .setGuildOnly(true)
                    .addOptions(command.getOptions());

        } else {
            commandData = Commands.slash(command.getName(), "[%s] %s".formatted(command.getCategory().readableName(), command.getDescription()))
                    .setNSFW(command.isNsfw())
                    .setGuildOnly(true)
                    .addSubcommands(command.getSubCommandsRaw());
        }

        slashCommands.put(command.getName(), command);
        slashCommandsList.add(commandData);
        return command;
    }

    // Surprisingly simple?
    public void execute(@Nonnull SlashContext ctx) {
        slashCommands.get(ctx.getName()).execute(ctx);
    }

    public boolean execute(@Nonnull NewContext ctx) {
        var args = ctx.arguments();
        if (args.hasNext()) {
            var name = args.next().getValue().toLowerCase();
            var child = commands.get(name);
            if (child == null) {
                child = commands.get(aliases.getOrDefault(name, ""));
            }
            if (child != null) {
                child.execute(ctx);
                return true;
            }
        }
        return false;
    }

    public List<CommandData> getSlashCommandsList() {
        return slashCommandsList;
    }

    public List<CommandData> getContextUserCommandsList() {
        return contextUserCommandList;
    }

    private static <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to instantiate " + clazz, e);
        }
    }

    private static void registerSubcommands(SlashCommand command) {
        for (var inner : command.getClass().getDeclaredClasses()) {
            if (!SlashCommand.class.isAssignableFrom(inner)) continue;
            if (inner.isLocalClass() || inner.isAnonymousClass()) continue;
            if (!Modifier.isStatic(inner.getModifiers())) continue;
            if (Modifier.isAbstract(inner.getModifiers())) continue;

            var sub = (SlashCommand)instantiate(inner);
            sub.setCategory(command.getCategory());
            sub.setPredicate(command.getPredicate());

            command.addSubCommand(sub.getName(), sub);
        }

    }

    private static void registerSubcommands(NewCommand command) {
        for (var inner : command.getClass().getDeclaredClasses()) {
            if (!NewCommand.class.isAssignableFrom(inner)) continue;
            if (inner.isLocalClass() || inner.isAnonymousClass()) continue;
            if (!Modifier.isStatic(inner.getModifiers())) continue;
            if (Modifier.isAbstract(inner.getModifiers())) continue;
            var sub = (NewCommand)instantiate(inner);
            sub.registerParent(command);
            registerSubcommands(sub);
        }
    }
}
