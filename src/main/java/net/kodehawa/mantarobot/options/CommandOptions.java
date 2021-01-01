/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.options;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.kodehawa.mantarobot.commands.CustomCmds;
import net.kodehawa.mantarobot.core.command.processor.CommandProcessor;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.FinderUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Option
public class CommandOptions extends OptionHandler {

    public CommandOptions() {
        setType(OptionType.COMMAND);
    }

    @Subscribe
    public void onRegister(OptionRegistryEvent e) {
        registerOption("server:command:disallow", "Command disallow", """
                Disallows a command from being triggered at all. Use the command name
                **Example:** `~>opts server command disallow 8ball`
                """, "Disallows a command from being triggered at all.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.server_command_disallow.no_command", EmoteReference.ERROR);
                return;
            }

            String commandName = args[0];
            //Check for CCs too
            boolean noCommand = CommandProcessor.REGISTRY.commands().get(commandName) == null &&
                    CustomCmds.getCustomCommand(ctx.getGuild().getId(), commandName) == null;
            if (noCommand) {
                ctx.sendLocalized("options.no_command", EmoteReference.ERROR, commandName);
                return;
            }

            if (commandName.equals("opts") || commandName.equals("help")) {
                ctx.sendLocalized("options.help_opts_notice", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.getDisabledCommands().add(commandName);
            ctx.sendLocalized("options.server_command_disallow.success", EmoteReference.MEGA, commandName);
            dbGuild.saveAsync();
        });
        addOptionAlias("server:command:disallow", "command:disable");

        registerOption("server:command:allow", "Command allow", """
                Allows a command from being triggered. Use the command name
                **Example:** `~>opts server command allow 8ball`
                """, "Allows a command from being triggered.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.server_command_allow.no_command", EmoteReference.ERROR);
                return;
            }

            String commandName = args[0];

            //Check for CCs too
            boolean noCommand = CommandProcessor.REGISTRY.commands().get(commandName) == null &&
                    CustomCmds.getCustomCommand(ctx.getGuild().getId(), commandName) == null;

            if (noCommand) {
                ctx.sendLocalized("options.no_command", EmoteReference.ERROR, commandName);
                return;
            }
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.getDisabledCommands().remove(commandName);
            ctx.sendLocalized("options.server_command_allow.success", EmoteReference.MEGA, commandName);
            dbGuild.saveAsync();
        });
        addOptionAlias("server:command:allow", "command:enable");

        registerOption("server:command:specific:disallow", "Specific command disallow", """
                Disallows a command from being triggered at all in a specific channel. Use the channel **name** and command name
                **Example:** `~>opts server command specific disallow general 8ball`
                """, "Disallows a command from being triggered at all in a specific channel.", (ctx, args) -> {
            if (args.length < 2) {
                ctx.sendLocalized("options.server_command_specific_disallow.invalid", EmoteReference.ERROR);
                return;
            }

            String channelName = args[0];
            String commandName = args[1];

            //Check for CCs too
            boolean noCommand = CommandProcessor.REGISTRY.commands().get(commandName) == null &&
                    CustomCmds.getCustomCommand(ctx.getGuild().getId(), commandName) == null;

            if (noCommand) {
                ctx.sendLocalized("options.no_command", EmoteReference.ERROR, commandName);
                return;
            }

            if (commandName.equals("opts") || commandName.equals("help")) {
                ctx.sendLocalized("options.help_opts_notice", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            TextChannel channel = FinderUtils.findChannel(ctx.getEvent(), channelName);
            if (channel == null) return;

            String id = channel.getId();
            guildData.getChannelSpecificDisabledCommands().computeIfAbsent(id, k -> new ArrayList<>());
            guildData.getChannelSpecificDisabledCommands().get(id).add(commandName);

            ctx.sendLocalized("options.server_command_specific_disallow.success", EmoteReference.MEGA, commandName, channel.getName());
            dbGuild.saveAsync();

        });
        addOptionAlias("server:command:specific:disallow", "command:specific:disable");

        registerOption("server:command:specific:allow", "Specific command allow", """
                Re-allows a command from being triggered in a specific channel. Use the channel **name** and command name
                **Example:** `~>opts server command specific allow general 8ball`
                """, "Re-allows a command from being triggered in a specific channel.", ((ctx, args) -> {
            if (args.length < 2) {
                ctx.sendLocalized("options.server_command_specific_allow.invalid", EmoteReference.ERROR);
                return;
            }

            String channelName = args[0];
            String commandName = args[1];
            //Check for CCs too
            boolean noCommand = CommandProcessor.REGISTRY.commands().get(commandName) == null &&
                    CustomCmds.getCustomCommand(ctx.getGuild().getId(), commandName) == null;

            if (noCommand) {
                ctx.sendLocalized("options.no_command", EmoteReference.ERROR, commandName);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            TextChannel channel = FinderUtils.findChannel(ctx.getEvent(), channelName);
            if (channel == null) return;

            String id = channel.getId();

            guildData.getChannelSpecificDisabledCommands().computeIfAbsent(id, k -> new ArrayList<>());
            guildData.getChannelSpecificDisabledCommands().get(id).remove(commandName);

            ctx.sendLocalized("options.server_command_specific_allow.success", EmoteReference.MEGA, commandName, channel.getName());
            dbGuild.saveAsync();
        }));
        addOptionAlias("server:command:specific:allow", "command:specific:enable");

        registerOption("server:channel:disallow", "Channel disallow", """
                Disallows a channel from commands. Use the channel **name**
                **Example:** `~>opts server channel disallow general`
                """, "Disallows a channel from commands.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.server_channel_disallow.no_channel", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if ((guildData.getDisabledChannels().size() + 1) >= ctx.getGuild().getTextChannels().size()) {
                ctx.sendLocalized("options.server_channel_disallow.too_many", EmoteReference.ERROR);
                return;
            }

            Consumer<TextChannel> consumer = textChannel -> {
                guildData.getDisabledChannels().add(textChannel.getId());
                dbGuild.save();
                ctx.sendLocalized("options.server_channel_disallow.success", EmoteReference.OK, textChannel.getAsMention());
            };

            TextChannel channel = FinderUtils.findChannelSelect(ctx.getEvent(), args[0], consumer);

            if (channel != null) {
                consumer.accept(channel);
            }
        });
        addOptionAlias("server:channel:disallow", "channel:disable");

        registerOption("server:channel:allow", "Channel allow", """
                Allows a channel from commands. Use the channel **name**
                **Example:** `~>opts server channel allow general`
                """, "Re-allows a channel from commands.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.server_channel_allow.no_channel", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            Consumer<TextChannel> consumer = textChannel -> {
                guildData.getDisabledChannels().remove(textChannel.getId());
                dbGuild.save();
                ctx.sendLocalized("options.server_channel_allow.success", EmoteReference.OK, textChannel.getAsMention());
            };

            TextChannel channel = FinderUtils.findChannelSelect(ctx.getEvent(), args[0], consumer);

            if (channel != null) {
                consumer.accept(channel);
            }
        });
        addOptionAlias("server:channel:allow", "channel:enable");

        registerOption("category:disable", "Disable categories", """
                        Disables a specified category.
                        If a non-valid category it's specified, it will display a list of valid categories
                        You need the category name, for example ` ~>opts category disable Action`""",
                "Disables a specified category", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.category_disable.no_category", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            CommandCategory toDisable = CommandCategory.lookupFromString(args[0]);

            if (toDisable == null) {
                AtomicInteger at = new AtomicInteger();
                ctx.sendLocalized("options.invalid_category",
                        EmoteReference.ERROR, CommandCategory.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name)
                                .collect(Collectors.joining("\n"))
                );
                return;
            }

            if (guildData.getDisabledCategories().contains(toDisable)) {
                ctx.sendLocalized("options.category_disable.already_disabled", EmoteReference.WARNING);
                return;
            }

            if (toDisable == CommandCategory.MODERATION) {
                ctx.sendLocalized("options.category_disable.moderation_notice", EmoteReference.WARNING);
                return;
            }

            guildData.getDisabledCategories().add(toDisable);
            dbGuild.save();
            ctx.sendLocalized("options.category_disable.success", EmoteReference.CORRECT, ctx.getLanguageContext().get(toDisable.toString()));
        });

        registerOption("category:enable", "Enable categories", """
                        Enables a specified category.
                        If a non-valid category it's specified, it will display a list of valid categories
                        You need the category name, for example ` ~>opts category enable Action`""",
                "Enables a specified category", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.category_enable.no_category", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            CommandCategory toEnable = CommandCategory.lookupFromString(args[0]);

            if (toEnable == null) {
                AtomicInteger at = new AtomicInteger();
                ctx.sendLocalized("options.invalid_category", EmoteReference.ERROR,
                        CommandCategory.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name)
                                .collect(Collectors.joining("\n"))
                );
                return;
            }

            guildData.getDisabledCategories().remove(toEnable);
            dbGuild.save();
            ctx.sendLocalized("options.category_enable.success", EmoteReference.CORRECT, ctx.getLanguageContext().get(toEnable.toString()));
        });

        registerOption("category:specific:disable", "Disable categories on a specific channel", """
                        Disables a specified category on a specific channel.
                        If a non-valid category it's specified, it will display a list of valid categories
                        You need the category name and the channel name, for example ` ~>opts category specific disable Action general`""",
                "Disables a specified category", (ctx, args) -> {
            if (args.length < 2) {
                ctx.sendLocalized("options.category_specific_disable.invalid", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            CommandCategory toDisable = CommandCategory.lookupFromString(args[0]);

            String channelName = args[1];
            Consumer<TextChannel> consumer = selectedChannel -> {
                if (toDisable == null) {
                    AtomicInteger at = new AtomicInteger();
                    ctx.sendLocalized("options.invalid_category",
                            EmoteReference.ERROR,
                            CommandCategory.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name)
                                    .collect(Collectors.joining("\n"))
                    );
                    return;
                }

                guildData.getChannelSpecificDisabledCategories().computeIfAbsent(selectedChannel.getId(), t -> new ArrayList<>());

                if (guildData.getChannelSpecificDisabledCategories().get(selectedChannel.getId()).contains(toDisable)) {
                    ctx.sendLocalized("options.category_specific_disable.already_disabled", EmoteReference.WARNING);
                    return;
                }

                if (toDisable == CommandCategory.MODERATION) {
                    ctx.sendLocalized("options.category_specific_disable.moderation_notice", EmoteReference.WARNING);
                    return;
                }

                guildData.getChannelSpecificDisabledCategories().get(selectedChannel.getId()).add(toDisable);
                dbGuild.save();
                ctx.sendLocalized("options.category_specific_disable.success", EmoteReference.CORRECT,
                        ctx.getLanguageContext().get(toDisable.toString()), selectedChannel.getAsMention()
                );
            };

            TextChannel channel = FinderUtils.findChannelSelect(ctx.getEvent(), channelName, consumer);

            if (channel != null) {
                consumer.accept(channel);
            }
        });

        registerOption("category:specific:enable", "Enable categories on a specific channel", """
                        Enables a specified category on a specific channel.
                        If a non-valid category it's specified, it will display a list of valid categories
                        You need the category name and the channel name, for example ` ~>opts category specific enable Action general`""",
                "Enables a specified category", (ctx, args) -> {
            if (args.length < 2) {
                ctx.sendLocalized("options.category_specific_enable.invalid", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            CommandCategory toEnable = CommandCategory.lookupFromString(args[0]);
            String channelName = args[1];

            Consumer<TextChannel> consumer = selectedChannel -> {
                if (toEnable == null) {
                    AtomicInteger at = new AtomicInteger();
                    ctx.sendLocalized("options.invalid_category",
                            EmoteReference.ERROR,
                            CommandCategory.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name)
                                    .collect(Collectors.joining("\n"))
                    );
                    return;
                }

                if (selectedChannel == null) {
                    ctx.sendLocalized("options.category_specific_enable.invalid_channel", EmoteReference.ERROR);
                    return;
                }

                List<?> l = guildData.getChannelSpecificDisabledCategories().computeIfAbsent(selectedChannel.getId(), uwu -> new ArrayList<>());
                if (l.isEmpty() || !l.contains(toEnable)) {
                    ctx.sendLocalized("options.category_specific_enable.not_disabled", EmoteReference.THINKING);
                    return;
                }
                guildData.getChannelSpecificDisabledCategories().get(selectedChannel.getId()).remove(toEnable);
                dbGuild.save();

                ctx.sendLocalized("options.category_specific_enable.success", EmoteReference.CORRECT,
                        ctx.getLanguageContext().get(toEnable.toString()), selectedChannel.getAsMention()
                );
            };

            TextChannel channel = FinderUtils.findChannelSelect(ctx.getEvent(), channelName, consumer);
            if (channel != null) {
                consumer.accept(channel);
            }
        });

        registerOption("server:role:specific:disallow", "Disallows a role from executing an specific command", """
                Disallows a role from executing an specific command
                This command takes the command to disallow and the role name afterwards. If the role name contains spaces, wrap it in quotes "like this"
                Example: `~>opts server role specific disallow daily Member`""",
                "Disallows a role from executing an specific command", (ctx, args) -> {
            if (args.length < 2) {
                ctx.sendLocalized("options.server_role_specific_disallow.invalid", EmoteReference.ERROR);
                return;
            }

            String commandDisallow = args[0];
            String roleDisallow = args[1];

            Consumer<Role> consumer = role -> {
                if (role == null) {
                    ctx.sendLocalized("options.invalid_role", EmoteReference.ERROR);
                    return;
                }

                //lol reusing strings
                if (role.isPublicRole()) {
                    ctx.sendLocalized("options.server_role_disallow.public_role", EmoteReference.ERROR);
                    return;
                }

                DBGuild dbGuild = ctx.getDBGuild();
                GuildData guildData = dbGuild.getData();

                //Check for CCs too
                boolean noCommand = CommandProcessor.REGISTRY.commands().get(commandDisallow) == null &&
                        CustomCmds.getCustomCommand(ctx.getGuild().getId(), commandDisallow) == null;

                if (noCommand) {
                    ctx.sendLocalized("options.no_command", EmoteReference.ERROR, commandDisallow);
                    return;
                }

                if (commandDisallow.equals("opts") || commandDisallow.equals("help")) {
                    ctx.sendLocalized("options.help_opts_notice", EmoteReference.ERROR);
                    return;
                }

                guildData.getRoleSpecificDisabledCommands().computeIfAbsent(role.getId(), key -> new ArrayList<>());

                if (guildData.getRoleSpecificDisabledCommands().get(role.getId()).contains(commandDisallow)) {
                    ctx.sendLocalized("options.server_role_specific_disallow.already_disabled", EmoteReference.ERROR);
                    return;
                }

                guildData.getRoleSpecificDisabledCommands().get(role.getId()).add(commandDisallow);
                dbGuild.save();
                ctx.sendLocalized("options.server_role_specific_disallow.success", EmoteReference.CORRECT, commandDisallow, role.getName());
            };

            Role role = FinderUtils.findRoleSelect(ctx.getEvent(), roleDisallow, consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });
        addOptionAlias("server:role:specific:disallow", "role:specific:disable");


        registerOption("server:role:specific:allow", "Allows a role from executing an specific command", """
                Allows a role from executing an specific command
                This command takes either the role name, id or mention and the command to disallow afterwards. If the role name contains spaces, wrap it in quotes "like this"
                Example: `~>opts server role specific allow daily Member`""",
                "Allows a role from executing an specific command", (ctx, args) -> {
            if (args.length < 2) {
                ctx.sendLocalized("options.server_role_specific_allow.invalid", EmoteReference.ERROR);
                return;
            }

            String commandAllow = args[0];
            String roleAllow = args[1];

            Consumer<Role> consumer = role -> {
                if (role == null) {
                    ctx.sendLocalized("options.invalid_role", EmoteReference.ERROR);
                    return;
                }

                DBGuild dbGuild = ctx.getDBGuild();
                GuildData guildData = dbGuild.getData();

                //Check for CCs too
                boolean noCommand = CommandProcessor.REGISTRY.commands().get(commandAllow) == null &&
                        CustomCmds.getCustomCommand(ctx.getGuild().getId(), commandAllow) == null;

                if (noCommand) {
                    ctx.sendLocalized("options.no_command", EmoteReference.ERROR, commandAllow);
                    return;
                }

                List<?> l = guildData.getRoleSpecificDisabledCommands().computeIfAbsent(role.getId(), key -> new ArrayList<>());
                if (l.isEmpty() || !l.contains(commandAllow)) {
                    ctx.sendLocalized("options.server_role_specific_allow.not_disabled", EmoteReference.THINKING);
                    return;
                }

                guildData.getRoleSpecificDisabledCommands().get(role.getId()).remove(commandAllow);
                dbGuild.save();
                ctx.sendLocalized("options.server_role_specific_allow.success", EmoteReference.CORRECT, commandAllow, role.getName());
            };

            Role role = FinderUtils.findRoleSelect(ctx.getEvent(), roleAllow, consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });
        addOptionAlias("server:role:specific:allow", "role:specific:enable");

        registerOption("category:role:specific:disable", "Disables a role from executing commands in an specified category.", """
                Disables a role from executing commands in an specified category
                This command takes the category name and the role to disable afterwards. If the role name contains spaces, wrap it in quotes "like this"
                Example: `~>opts category role specific disable Currency Member`""",
                "Disables a role from executing commands in an specified category.", (ctx, args) -> {
            if (args.length < 2) {
                ctx.sendLocalized("options.category_role_specific_disable.invalid", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            CommandCategory toDisable = CommandCategory.lookupFromString(args[0]);

            String roleName = args[1];
            Consumer<Role> consumer = role -> {
                if (toDisable == null) {
                    AtomicInteger at = new AtomicInteger();
                    ctx.sendLocalized("options.invalid_category",
                            EmoteReference.ERROR, CommandCategory.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name)
                                    .collect(Collectors.joining("\n"))
                    );
                    return;
                }

                if (role == null) {
                    ctx.sendLocalized("options.invalid_role", EmoteReference.ERROR);
                    return;
                }

                if (role.isPublicRole()) {
                    ctx.sendLocalized("options.server_role_disallow.public_role", EmoteReference.ERROR);
                    return;
                }

                guildData.getRoleSpecificDisabledCategories().computeIfAbsent(role.getId(), cat -> new ArrayList<>());

                if (guildData.getRoleSpecificDisabledCategories().get(role.getId()).contains(toDisable)) {
                    ctx.sendLocalized("options.category_role_specific_disable.already_disabled", EmoteReference.WARNING);
                    return;
                }

                if (toDisable == CommandCategory.MODERATION) {
                    ctx.sendLocalized("options.category_role_specific_disable.moderation_notice", EmoteReference.WARNING);
                    return;
                }

                guildData.getRoleSpecificDisabledCategories().get(role.getId()).add(toDisable);
                dbGuild.save();
                ctx.sendLocalized("options.category_role_specific_disable.success", EmoteReference.CORRECT, toDisable.toString(), role.getName());
            };

            Role role = FinderUtils.findRoleSelect(ctx.getEvent(), roleName, consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });

        registerOption("category:role:specific:enable", "Enables a role from executing commands in an specified category.", """
                Enables a role from executing commands in an specified category
                This command takes the category name and the role to enable afterwards. If the role name contains spaces, wrap it in quotes "like this"
                Example: `~>opts category role specific enable Currency Member`""",
                "Enables a role from executing commands in an specified category.", (ctx, args) -> {
            if (args.length < 2) {
                ctx.sendLocalized("options.category_role_specific_enable.invalid", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            CommandCategory toEnable = CommandCategory.lookupFromString(args[0]);
            String roleName = args[1];

            Consumer<Role> consumer = role -> {
                if (toEnable == null) {
                    AtomicInteger at = new AtomicInteger();
                    ctx.sendLocalized("options.invalid_category",
                            EmoteReference.ERROR, CommandCategory.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name)
                                    .collect(Collectors.joining("\n"))
                    );
                    return;
                }

                if (role == null) {
                    ctx.sendLocalized("options.invalid_role", EmoteReference.ERROR);
                    return;
                }

                List<?> l = guildData.getRoleSpecificDisabledCategories().computeIfAbsent(role.getId(), cat -> new ArrayList<>());
                if (l.isEmpty() || !l.contains(toEnable)) {
                    ctx.sendLocalized("options.category_role_specific_enable.not_disabled", EmoteReference.THINKING);
                    return;
                }
                guildData.getRoleSpecificDisabledCategories().get(role.getId()).remove(toEnable);
                dbGuild.save();
                ctx.sendLocalized("options.category_role_specific_enable.success", EmoteReference.CORRECT, toEnable.toString(), role.getName());
            };

            Role role = FinderUtils.findRoleSelect(ctx.getEvent(), roleName, consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });

        registerOption("server:role:disallow", "Role disallow", """
                Disallows all users with a role from executing commands.
                You need to provide the name of the role to disallow from Mantaro on this server.
                Example: `~>opts server role disallow bad`, `~>opts server role disallow \"No commands\"`
                """,
                "Disallows all users with a role from executing commands.", (ctx, args) -> {
                    if (args.length == 0) {
                        ctx.sendLocalized("options.server_role_disallow.no_name", EmoteReference.ERROR);
                        return;
                    }

                    DBGuild dbGuild = ctx.getDBGuild();
                    GuildData guildData = dbGuild.getData();
                    String roleName = String.join(" ", args);

                    Consumer<Role> consumer = (role) -> {
                        guildData.getDisabledRoles().add(role.getId());
                        dbGuild.saveAsync();
                        ctx.sendLocalized("options.server_role_disallow.success", EmoteReference.CORRECT, role.getName());
                    };

                    Role role = FinderUtils.findRoleSelect(ctx.getEvent(), roleName, consumer);

                    if (role != null && role.isPublicRole()) {
                        ctx.sendLocalized("options.server_role_disallow.public_role", EmoteReference.ERROR);
                        return;
                    }

                    if (role != null) {
                        consumer.accept(role);
                    }
                });

        registerOption("server:role:allow", "Role allow", """
                Allows all users with a role from executing commands.
                You need to provide the name of the role to allow from mantaro. Has to be already disabled.
                Example: `~>opts server role allow bad`, `~>opts server role allow \"No commands\"`
                """, "Allows all users with a role from executing commands (Has to be already disabled)", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.server_role_allow.no_name", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            String roleName = String.join(" ", args);

            Consumer<Role> consumer = (role) -> {
                if (!guildData.getDisabledRoles().contains(role.getId())) {
                    ctx.sendLocalized("options.server_role_allow.not_disabled", EmoteReference.ERROR);
                    return;
                }

                guildData.getDisabledRoles().remove(role.getId());
                dbGuild.saveAsync();
                ctx.sendLocalized("options.server_role_allow.success", EmoteReference.CORRECT, role.getName());
            };

            Role role = FinderUtils.findRoleSelect(ctx.getEvent(), roleName, consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });
    }

    @Override
    public String description() {
        return "Command related options. Disabling/enabling commands or categories belong here.";
    }
}
