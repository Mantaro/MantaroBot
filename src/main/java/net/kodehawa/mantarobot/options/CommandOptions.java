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

package net.kodehawa.mantarobot.options;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.kodehawa.mantarobot.commands.CustomCmds;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

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
        registerOption("server:command:disallow", "Command disallow",
                "Disallows a command from being triggered at all. Use the command name\n" +
                        "**Example:** `~>opts server command disallow 8ball`",
                "Disallows a command from being triggered at all.", (event, args, lang) -> {
                    if (args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.server_command_disallow.no_command"), EmoteReference.ERROR).queue();
                        return;
                    }

                    String commandName = args[0];
                    //Check for CCs too
                    boolean noCommand = DefaultCommandProcessor.REGISTRY.commands().get(commandName) == null &&
                            CustomCmds.getCustomCommand(event.getGuild().getId(), commandName) == null;
                    if (noCommand) {
                        event.getChannel().sendMessageFormat(lang.get("options.no_command"), EmoteReference.ERROR, commandName).queue();
                        return;
                    }

                    if (commandName.equals("opts") || commandName.equals("help")) {
                        event.getChannel().sendMessageFormat(lang.get("options.help_opts_notice"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.getDisabledCommands().add(commandName);
                    event.getChannel().sendMessageFormat(lang.get("options.server_command_disallow.success"), EmoteReference.MEGA, commandName).queue();
                    dbGuild.saveAsync();
                });
        addOptionAlias("server:command:disallow", "command:disable");

        registerOption("server:command:allow", "Command allow",
                "Allows a command from being triggered. Use the command name\n" +
                        "**Example:** `~>opts server command allow 8ball`",
                "Allows a command from being triggered.", (event, args, lang) -> {
                    if (args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.server_command_allow.no_command"), EmoteReference.ERROR).queue();
                        return;
                    }

                    String commandName = args[0];

                    //Check for CCs too
                    boolean noCommand = DefaultCommandProcessor.REGISTRY.commands().get(commandName) == null &&
                            CustomCmds.getCustomCommand(event.getGuild().getId(), commandName) == null;

                    if (noCommand) {
                        event.getChannel().sendMessageFormat(lang.get("options.no_command"), EmoteReference.ERROR, commandName).queue();
                        return;
                    }
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.getDisabledCommands().remove(commandName);
                    event.getChannel().sendMessageFormat(lang.get("options.server_command_allow.success"), EmoteReference.MEGA, commandName).queue();
                    dbGuild.saveAsync();
                });
        addOptionAlias("server:command:allow", "command:enable");

        registerOption("server:command:specific:disallow", "Specific command disallow",
                "Disallows a command from being triggered at all in a specific channel. Use the channel **name** and command name\n" +
                        "**Example:** `~>opts server command specific disallow general 8ball`",
                "Disallows a command from being triggered at all in a specific channel.", (event, args, lang) -> {
                    if (args.length < 2) {
                        event.getChannel().sendMessageFormat(lang.get("options.server_command_specific_disallow.invalid"), EmoteReference.ERROR).queue();
                        return;
                    }

                    String channelName = args[0];
                    String commandName = args[1];

                    //Check for CCs too
                    boolean noCommand = DefaultCommandProcessor.REGISTRY.commands().get(commandName) == null &&
                            CustomCmds.getCustomCommand(event.getGuild().getId(), commandName) == null;

                    if (noCommand) {
                        event.getChannel().sendMessageFormat(lang.get("options.no_command"), EmoteReference.ERROR, commandName).queue();
                        return;
                    }

                    if (commandName.equals("opts") || commandName.equals("help")) {
                        event.getChannel().sendMessageFormat(lang.get("options.opts_help_notice"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    TextChannel channel = Utils.findChannel(event, channelName);
                    if (channel == null) return;

                    String id = channel.getId();
                    guildData.getChannelSpecificDisabledCommands().computeIfAbsent(id, k -> new ArrayList<>());
                    guildData.getChannelSpecificDisabledCommands().get(id).add(commandName);

                    event.getChannel().sendMessageFormat(lang.get("options.server_command_specific_disallow.success"), EmoteReference.MEGA, commandName, channel.getName()).queue();
                    dbGuild.saveAsync();

                });
        addOptionAlias("server:command:specific:disallow", "command:specific:disable");

        registerOption("server:command:specific:allow", "Specific command allow",
                "Re-allows a command from being triggered in a specific channel. Use the channel **name** and command name\n" +
                        "**Example:** `~>opts server command specific allow general 8ball`",
                "Re-allows a command from being triggered in a specific channel.", ((event, args, lang) -> {
                    if (args.length < 2) {
                        event.getChannel().sendMessageFormat(lang.get("options.server_command_specific_allow.invalid"), EmoteReference.ERROR).queue();
                        return;
                    }

                    String channelName = args[0];
                    String commandName = args[1];
                    //Check for CCs too
                    boolean noCommand = DefaultCommandProcessor.REGISTRY.commands().get(commandName) == null &&
                            CustomCmds.getCustomCommand(event.getGuild().getId(), commandName) == null;

                    if (noCommand) {
                        event.getChannel().sendMessageFormat(lang.get("options.no_command"), EmoteReference.ERROR, commandName).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    TextChannel channel = Utils.findChannel(event, channelName);
                    if (channel == null) return;

                    String id = channel.getId();

                    guildData.getChannelSpecificDisabledCommands().computeIfAbsent(id, k -> new ArrayList<>());
                    guildData.getChannelSpecificDisabledCommands().get(id).remove(commandName);

                    event.getChannel().sendMessageFormat(lang.get("options.server_command_specific_allow.success"), EmoteReference.MEGA, commandName, channel.getName()).queue();
                    dbGuild.saveAsync();
                }));
        addOptionAlias("server:command:specific:allow", "command:specific:enable");

        registerOption("server:channel:disallow", "Channel disallow",
                "Disallows a channel from commands. Use the channel **name**\n" +
                        "**Example:** `~>opts server channel disallow general`",
                "Disallows a channel from commands.", (event, args, lang) -> {
                    if (args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.server_channel_disallow.no_channel"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    if ((guildData.getDisabledChannels().size() + 1) >= event.getGuild().getTextChannels().size()) {
                        event.getChannel().sendMessageFormat(lang.get("options.server_channel_disallow.too_many"), EmoteReference.ERROR).queue();
                        return;
                    }

                    Consumer<TextChannel> consumer = textChannel -> {
                        guildData.getDisabledChannels().add(textChannel.getId());
                        dbGuild.save();
                        event.getChannel().sendMessageFormat(lang.get("options.server_channel_disallow.success"), EmoteReference.OK, textChannel.getAsMention()).queue();
                    };

                    TextChannel channel = Utils.findChannelSelect(event, args[0], consumer);

                    if (channel != null) {
                        consumer.accept(channel);
                    }
                });
        addOptionAlias("server:channel:disallow", "channel:disable");

        registerOption("server:channel:allow", "Channel allow",
                "Allows a channel from commands. Use the channel **name**\n" +
                        "**Example:** `~>opts server channel allow general`",
                "Re-allows a channel from commands.", (event, args, lang) -> {
                    if (args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.server_channel_allow.no_channel"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    Consumer<TextChannel> consumer = textChannel -> {
                        guildData.getDisabledChannels().remove(textChannel.getId());
                        dbGuild.save();
                        event.getChannel().sendMessageFormat(lang.get("options.server_channel_allow.success"), EmoteReference.OK, textChannel.getAsMention()).queue();
                    };

                    TextChannel channel = Utils.findChannelSelect(event, args[0], consumer);

                    if (channel != null) {
                        consumer.accept(channel);
                    }
                });
        addOptionAlias("server:channel:allow", "channel:enable");

        registerOption("category:disable", "Disable categories",
                "Disables a specified category.\n" +
                        "If a non-valid category it's specified, it will display a list of valid categories\n" +
                        "You need the category name, for example ` ~>opts category disable Action`",
                "Disables a specified category", (event, args, lang) -> {
                    if (args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.category_disable.no_category"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    Category toDisable = Category.lookupFromString(args[0]);

                    if (toDisable == null) {
                        AtomicInteger at = new AtomicInteger();
                        event.getChannel().sendMessageFormat(lang.get("options.invalid_category"),
                                EmoteReference.ERROR, Category.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name)
                                        .collect(Collectors.joining("\n"))
                        ).queue();
                        return;
                    }

                    if (guildData.getDisabledCategories().contains(toDisable)) {
                        event.getChannel().sendMessageFormat(lang.get("options.category_disable.already_disabled"), EmoteReference.WARNING).queue();
                        return;
                    }

                    if (toDisable == Category.MODERATION) {
                        event.getChannel().sendMessageFormat(lang.get("options.category_disable.moderation_notice"), EmoteReference.WARNING).queue();
                        return;
                    }

                    guildData.getDisabledCategories().add(toDisable);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.category_disable.success"), EmoteReference.CORRECT, lang.get(toDisable.toString())).queue();
                });

        registerOption("category:enable", "Enable categories",
                "Enables a specified category.\n" +
                        "If a non-valid category it's specified, it will display a list of valid categories\n" +
                        "You need the category name, for example ` ~>opts category enable Action`",
                "Enables a specified category", (event, args, lang) -> {
                    if (args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.category_enable.no_category"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    Category toEnable = Category.lookupFromString(args[0]);

                    if (toEnable == null) {
                        AtomicInteger at = new AtomicInteger();
                        event.getChannel().sendMessageFormat(lang.get("options.invalid_category"),
                                EmoteReference.ERROR, Category.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name)
                                        .collect(Collectors.joining("\n"))
                        ).queue();
                        return;
                    }

                    guildData.getDisabledCategories().remove(toEnable);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.category_enable.success"), EmoteReference.CORRECT, lang.get(toEnable.toString())).queue();
                });

        registerOption("category:specific:disable", "Disable categories on a specific channel",
                "Disables a specified category on a specific channel.\n" +
                        "If a non-valid category it's specified, it will display a list of valid categories\n" +
                        "You need the category name and the channel name, for example ` ~>opts category specific disable Action general`",
                "Disables a specified category", (event, args, lang) -> {
                    if (args.length < 2) {
                        event.getChannel().sendMessageFormat(lang.get("options.category_specific_disable.invalid"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    Category toDisable = Category.lookupFromString(args[0]);

                    String channelName = args[1];
                    Consumer<TextChannel> consumer = selectedChannel -> {
                        if (toDisable == null) {
                            AtomicInteger at = new AtomicInteger();
                            event.getChannel().sendMessageFormat(lang.get("options.invalid_category"),
                                    EmoteReference.ERROR, Category.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name)
                                            .collect(Collectors.joining("\n"))
                            ).queue();
                            return;
                        }

                        guildData.getChannelSpecificDisabledCategories().computeIfAbsent(selectedChannel.getId(), t -> new ArrayList<>());

                        if (guildData.getChannelSpecificDisabledCategories().get(selectedChannel.getId()).contains(toDisable)) {
                            event.getChannel().sendMessageFormat(lang.get("options.category_specific_disable.already_disabled"), EmoteReference.WARNING).queue();
                            return;
                        }

                        if (toDisable == Category.MODERATION) {
                            event.getChannel().sendMessageFormat(lang.get("options.category_specific_disable.moderation_notice"), EmoteReference.WARNING).queue();
                            return;
                        }

                        guildData.getChannelSpecificDisabledCategories().get(selectedChannel.getId()).add(toDisable);
                        dbGuild.save();
                        event.getChannel().sendMessageFormat(lang.get("options.category_specific_disable.success"),
                                EmoteReference.CORRECT, lang.get(toDisable.toString()), selectedChannel.getAsMention()
                        ).queue();
                    };

                    TextChannel channel = Utils.findChannelSelect(event, channelName, consumer);

                    if (channel != null) {
                        consumer.accept(channel);
                    }
                });

        registerOption("category:specific:enable", "Enable categories on a specific channel",
                "Enables a specified category on a specific channel.\n" +
                        "If a non-valid category it's specified, it will display a list of valid categories\n" +
                        "You need the category name and the channel name, for example ` ~>opts category specific enable Action general`",
                "Enables a specified category", (event, args, lang) -> {
                    if (args.length < 2) {
                        event.getChannel().sendMessageFormat(lang.get("options.category_specific_enable.invalid"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    Category toEnable = Category.lookupFromString(args[0]);
                    String channelName = args[1];

                    Consumer<TextChannel> consumer = selectedChannel -> {
                        if (toEnable == null) {
                            AtomicInteger at = new AtomicInteger();
                            event.getChannel().sendMessageFormat(lang.get("options.invalid_category"),
                                    EmoteReference.ERROR, Category.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name)
                                            .collect(Collectors.joining("\n"))
                            ).queue();
                            return;
                        }

                        if (selectedChannel == null) {
                            event.getChannel().sendMessageFormat(lang.get("options.category_specific_enable.invalid_channel"), EmoteReference.ERROR).queue();
                            return;
                        }

                        List<?> l = guildData.getChannelSpecificDisabledCategories().computeIfAbsent(selectedChannel.getId(), uwu -> new ArrayList<>());
                        if (l.isEmpty() || !l.contains(toEnable)) {
                            event.getChannel().sendMessageFormat(lang.get("options.category_specific_enable.not_disabled"), EmoteReference.THINKING).queue();
                            return;
                        }
                        guildData.getChannelSpecificDisabledCategories().get(selectedChannel.getId()).remove(toEnable);
                        dbGuild.save();

                        event.getChannel().sendMessageFormat(lang.get("options.category_specific_enable.success"),
                                EmoteReference.CORRECT, lang.get(toEnable.toString()), selectedChannel.getAsMention()
                        ).queue();
                    };

                    TextChannel channel = Utils.findChannelSelect(event, channelName, consumer);

                    if (channel != null) {
                        consumer.accept(channel);
                    }
                });


        registerOption("server:role:specific:disallow", "Disallows a role from executing an specific command", "Disallows a role from executing an specific command\n" +
                "This command takes the command to disallow and the role name afterwards. If the role name contains spaces, wrap it in quotes \"like this\"\n" +
                "Example: `~>opts server role specific disallow daily Member`", "Disallows a role from executing an specific command", (event, args, lang) -> {
            if (args.length < 2) {
                event.getChannel().sendMessageFormat(lang.get("options.server_role_specific_disallow.invalid"), EmoteReference.ERROR).queue();
                return;
            }

            String commandDisallow = args[0];
            String roleDisallow = args[1];

            Consumer<Role> consumer = role -> {
                if (role == null) {
                    event.getChannel().sendMessageFormat(lang.get("options.invalid_role"), EmoteReference.ERROR).queue();
                    return;
                }

                //lol reusing strings
                if (role.isPublicRole()) {
                    event.getChannel().sendMessageFormat(lang.get("options.server_role_disallow.public_role"), EmoteReference.ERROR).queue();
                    return;
                }

                DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                GuildData guildData = dbGuild.getData();

                //Check for CCs too
                boolean noCommand = DefaultCommandProcessor.REGISTRY.commands().get(commandDisallow) == null &&
                        CustomCmds.getCustomCommand(event.getGuild().getId(), commandDisallow) == null;

                if (noCommand) {
                    event.getChannel().sendMessageFormat(lang.get("options.no_command"), EmoteReference.ERROR, commandDisallow).queue();
                    return;
                }

                if (commandDisallow.equals("opts") || commandDisallow.equals("help")) {
                    event.getChannel().sendMessageFormat(lang.get("options.help_opts_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                guildData.getRoleSpecificDisabledCommands().computeIfAbsent(role.getId(), key -> new ArrayList<>());

                if (guildData.getRoleSpecificDisabledCommands().get(role.getId()).contains(commandDisallow)) {
                    event.getChannel().sendMessageFormat(lang.get("options.server_role_specific_disallow.already_disabled"), EmoteReference.ERROR).queue();
                    return;
                }

                guildData.getRoleSpecificDisabledCommands().get(role.getId()).add(commandDisallow);
                dbGuild.save();
                event.getChannel().sendMessageFormat(lang.get("options.server_role_specific_disallow.success"),
                        EmoteReference.CORRECT, commandDisallow, role.getName()
                ).queue();
            };

            Role role = Utils.findRoleSelect(event, roleDisallow, consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });
        addOptionAlias("server:role:specific:disallow", "role:specific:disable");


        registerOption("server:role:specific:allow", "Allows a role from executing an specific command", "Allows a role from executing an specific command\n" +
                "This command takes either the role name, id or mention and the command to disallow afterwards. If the role name contains spaces, wrap it in quotes \"like this\"\n" +
                "Example: `~>opts server role specific allow daily Member`", "Allows a role from executing an specific command", (event, args, lang) -> {
            if (args.length < 2) {
                event.getChannel().sendMessageFormat(lang.get("options.server_role_specific_allow.invalid"), EmoteReference.ERROR).queue();
                return;
            }

            String commandAllow = args[0];
            String roleAllow = args[1];

            Consumer<Role> consumer = role -> {
                if (role == null) {
                    event.getChannel().sendMessageFormat(lang.get("options.invalid_role"), EmoteReference.ERROR).queue();
                    return;
                }

                DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                GuildData guildData = dbGuild.getData();

                //Check for CCs too
                boolean noCommand = DefaultCommandProcessor.REGISTRY.commands().get(commandAllow) == null &&
                        CustomCmds.getCustomCommand(event.getGuild().getId(), commandAllow) == null;

                if (noCommand) {
                    event.getChannel().sendMessageFormat(lang.get("options.no_command"), EmoteReference.ERROR, commandAllow).queue();
                    return;
                }

                List<?> l = guildData.getRoleSpecificDisabledCommands().computeIfAbsent(role.getId(), key -> new ArrayList<>());
                if (l.isEmpty() || !l.contains(commandAllow)) {
                    event.getChannel().sendMessageFormat(lang.get("options.server_role_specific_allow.not_disabled"), EmoteReference.THINKING).queue();
                    return;
                }

                guildData.getRoleSpecificDisabledCommands().get(role.getId()).remove(commandAllow);
                dbGuild.save();
                event.getChannel().sendMessageFormat(lang.get("options.server_role_specific_allow.success"),
                        EmoteReference.CORRECT, commandAllow, role.getName()
                ).queue();
            };

            Role role = Utils.findRoleSelect(event, roleAllow, consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });
        addOptionAlias("server:role:specific:allow", "role:specific:enable");

        registerOption("category:role:specific:disable", "Disables a role from executing commands in an specified category.", "Disables a role from executing commands in an specified category\n" +
                "This command takes the category name and the role to disable afterwards. If the role name contains spaces, wrap it in quotes \"like this\"\n" +
                "Example: `~>opts category role specific disable Currency Member`", "Disables a role from executing commands in an specified category.", (event, args, lang) -> {
            if (args.length < 2) {
                event.getChannel().sendMessageFormat(lang.get("options.category_role_specific_disable.invalid"), EmoteReference.ERROR).queue();
                return;
            }

            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            Category toDisable = Category.lookupFromString(args[0]);

            String roleName = args[1];
            Consumer<Role> consumer = role -> {
                if (toDisable == null) {
                    AtomicInteger at = new AtomicInteger();
                    event.getChannel().sendMessageFormat(lang.get("options.invalid_category"),
                            EmoteReference.ERROR, Category.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name)
                                    .collect(Collectors.joining("\n"))
                    ).queue();
                    return;
                }

                if (role == null) {
                    event.getChannel().sendMessageFormat(lang.get("options.invalid_role"), EmoteReference.ERROR).queue();
                    return;
                }

                //reusing strings v2
                if (role.isPublicRole()) {
                    event.getChannel().sendMessageFormat(lang.get("options.server_role_disallow.public_role"), EmoteReference.ERROR).queue();
                    return;
                }

                guildData.getRoleSpecificDisabledCategories().computeIfAbsent(role.getId(), cat -> new ArrayList<>());

                if (guildData.getRoleSpecificDisabledCategories().get(role.getId()).contains(toDisable)) {
                    event.getChannel().sendMessageFormat(lang.get("options.category_role_specific_disable.already_disabled"), EmoteReference.WARNING).queue();
                    return;
                }

                if (toDisable == Category.MODERATION) {
                    event.getChannel().sendMessageFormat(lang.get("options.category_role_specific_disable.moderation_notice"), EmoteReference.WARNING).queue();
                    return;
                }

                guildData.getRoleSpecificDisabledCategories().get(role.getId()).add(toDisable);
                dbGuild.save();
                event.getChannel().sendMessageFormat(lang.get("options.category_role_specific_disable.success"), EmoteReference.CORRECT, toDisable.toString(), role.getName()).queue();
            };

            Role role = Utils.findRoleSelect(event, roleName, consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });

        registerOption("category:role:specific:enable", "Enables a role from executing commands in an specified category.", "Enables a role from executing commands in an specified category\n" +
                "This command takes the category name and the role to enable afterwards. If the role name contains spaces, wrap it in quotes \"like this\"\n" +
                "Example: `~>opts category role specific enable Currency Member`", "Enables a role from executing commands in an specified category.", (event, args, lang) -> {
            if (args.length < 2) {
                event.getChannel().sendMessageFormat(lang.get("options.category_role_specific_enable.invalid"), EmoteReference.ERROR).queue();
                return;
            }

            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            Category toEnable = Category.lookupFromString(args[0]);
            String roleName = args[1];

            Consumer<Role> consumer = role -> {
                if (toEnable == null) {
                    AtomicInteger at = new AtomicInteger();
                    event.getChannel().sendMessageFormat(lang.get("options.invalid_category"),
                            EmoteReference.ERROR, Category.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name)
                                    .collect(Collectors.joining("\n"))
                    ).queue();
                    return;
                }

                if (role == null) {
                    event.getChannel().sendMessageFormat(lang.get("options.invalid_role"), EmoteReference.ERROR).queue();
                    return;
                }

                List<?> l = guildData.getRoleSpecificDisabledCategories().computeIfAbsent(role.getId(), cat -> new ArrayList<>());
                if (l.isEmpty() || !l.contains(toEnable)) {
                    event.getChannel().sendMessageFormat(lang.get("options.category_role_specific_enable.not_disabled"), EmoteReference.THINKING).queue();
                    return;
                }
                guildData.getRoleSpecificDisabledCategories().get(role.getId()).remove(toEnable);
                dbGuild.save();
                event.getChannel().sendMessageFormat(lang.get("options.category_role_specific_enable.success"), EmoteReference.CORRECT, toEnable.toString(), role.getName()).queue();
            };

            Role role = Utils.findRoleSelect(event, roleName, consumer);

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
