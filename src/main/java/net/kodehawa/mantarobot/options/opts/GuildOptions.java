/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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

package net.kodehawa.mantarobot.options.opts;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.commands.OptsCmd;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.OptionType;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.OptsCmd.optsCmd;

@Option
public class GuildOptions extends OptionHandler {

    public GuildOptions() {
        setType(OptionType.GUILD);
    }

    @Subscribe
    public void onRegistry(OptionRegistryEvent e) {
        //region opts birthday
        registerOption("birthday:enable", "Birthday Monitoring enable",
                "Enables birthday monitoring. You need the channel **name** and the role name (it assigns that role on birthday)\n" +
                        "**Example:** `~>opts birthday enable general Birthday`, `~>opts birthday enable general \"Happy Birthday\"`",
                "Enables birthday monitoring.", (event, args) -> {
                    if(args.length < 2) {
                        OptsCmd.onHelp(event);
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    try {
                        String channel = args[0];
                        String role = args[1];

                        boolean isId = channel.matches("^[0-9]*$");
                        String channelId = isId ? channel : event.getGuild().getTextChannelsByName(channel, true).get(0)
                                .getId();
                        Role roleObj = event.getGuild().getRolesByName(role.replace(channelId, ""), true).get(0);

                        if(roleObj.isPublicRole()) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot set the everyone role as a birthday role! " +
                                    "Remember that the birthday role is a role that gets assigned to the person when the birthday comes, and then removes when the day passes away.").queue();
                            return;
                        }

                        if(guildData.getGuildAutoRole() != null && roleObj.getId().equals(guildData.getGuildAutoRole())) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot set the autorole role as a birthday role! " +
                                    "Remember that the birthday role is a role that gets assigned to the person when the birthday comes, and then removes when the day passes away.").queue();
                            return;
                        }

                        event.getChannel().sendMessage(EmoteReference.WARNING + "Remember that the birthday role is a role that gets assigned to the person when the birthday comes, and then removes when the day passes away.\n" +
                                "The role *has to be a newly created role or a role you don't use for anyone else*. It MUST NOT be a role you already have on your users.\n" +
                                "This is because of how the birthday assigner works: It assigns a temporary role to the person having its birthday, and unassigns it when the birthday day has passed. " +
                                "**This means that everyone with the birthday role will get the role removed the day the birthday passes through**. Please take caution when choosing what role to use, as a misconfiguration might make bad things happen! " +
                                "If you have any doubts on how to configure it, you can always join our support guild and ask. You can also check `~>opts help birthday enable` for an example.\n\n" +
                                "Type **yes** if you agree to set the role " + roleObj.getName() + " as a birthday role, and **no** to cancel. This timeouts in 30 seconds.").queue();
                        InteractiveOperations.createOverriding(event.getChannel(), 30, interactiveEvent -> {
                            String content = interactiveEvent.getMessage().getContentRaw();
                            if(content.equalsIgnoreCase("yes")) {
                                String roleId = roleObj.getId();
                                guildData.setBirthdayChannel(channelId);
                                guildData.setBirthdayRole(roleId);
                                dbGuild.saveAsync();
                                event.getChannel().sendMessage(
                                        String.format(EmoteReference.MEGA + "Birthday logging enabled on this server with parameters -> Channel: `#%s (%s)` and role: `%s (%s)`",
                                                channel, channelId, role, roleId
                                        )).queue();
                                return Operation.COMPLETED;
                            } else if (content.equalsIgnoreCase("no")) {
                                interactiveEvent.getChannel().sendMessage(EmoteReference.CORRECT + "Cancelled request.").queue();
                                return Operation.COMPLETED;
                            }

                            return Operation.IGNORED;
                        });

                    } catch(Exception ex) {
                        if(ex instanceof IndexOutOfBoundsException) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "I didn't find a channel or role!\n " +
                                    "**Remember, you don't have to mention neither the role or the channel, rather just type its " +
                                    "name, order is <channel> <role>, without the leading \"<>\".**").queue();
                            return;
                        }
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You supplied invalid arguments for this command " + EmoteReference.SAD).queue();
                        OptsCmd.onHelp(event);
                    }
                });

        registerOption("birthday:disable", "Birthday disable", "Disables birthday monitoring.", (event) -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            guildData.setBirthdayChannel(null);
            guildData.setBirthdayRole(null);
            dbGuild.saveAsync();
            event.getChannel().sendMessage(EmoteReference.MEGA + "Birthday logging has been disabled on this server").queue();
        });
        //endregion

        //region prefix
        //region set
        registerOption("prefix:set", "Prefix set",
                "Sets the server prefix.\n" +
                        "**Example:** `~>opts prefix set .`",
                "Sets the server prefix.", (event, args) -> {
                    if(args.length < 1) {
                        onHelp(event);
                        return;
                    }
                    String prefix = args[0];

                    if(prefix.length() > 200) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Don't you think that's a bit too long?").queue();
                        return;
                    }

                    if(prefix.isEmpty()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot set the guild prefix to nothing...").queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setGuildCustomPrefix(prefix);
                    dbGuild.save();
                    event.getChannel().sendMessage(EmoteReference.MEGA + "Your server's custom prefix has been set to " + prefix).queue();
                });//endregion

        //region clear
        registerOption("prefix:clear", "Prefix clear",
                "Clear the server prefix.\n" +
                        "**Example:** `~>opts prefix clear`",
                "Resets the server prefix.", (event) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setGuildCustomPrefix(null);
                    dbGuild.save();
                    event.getChannel().sendMessage(EmoteReference.MEGA + "Your server's custom prefix has been disabled").queue();
                });//endregion
        // endregion

        //region autorole
        //region set
        registerOption("autorole:set", "Autorole set",
                "Sets the server autorole. This means every user who joins will get this role. **You need to use the role name, if it contains spaces" +
                        " you need to wrap it in quotation marks**\n" +
                        "**Example:** `~>opts autorole set Member`, `~>opts autorole set \"Magic Role\"`",
                "Sets the server autorole.", (event, args) -> {
                    if(args.length == 0) {
                        onHelp(event);
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    String name = args[0];
                    List<Role> roles = event.getGuild().getRolesByName(name, true);

                    if(roles.isEmpty()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't find a role with that name").queue();
                        return;
                    }

                    if(roles.size() <= 1) {
                        if(!event.getMember().canInteract(roles.get(0))) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "This role is placed higher than your highest role, therefore you cannot put it as autorole!").queue();
                            return;
                        }

                        guildData.setGuildAutoRole(roles.get(0).getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessage(EmoteReference.CORRECT + "The server autorole is now set to: **" + roles.get
                                (0).getName() + "** (Position: " + roles.get(0).getPosition() + ")").queue();
                        return;
                    }

                    event.getChannel().sendMessage(new EmbedBuilder().setTitle("Selection", null).setDescription("").build
                            ()).queue();

                    DiscordUtils.selectList(event, roles,
                            role -> String.format("%s (ID: %s)  | Position: %s", role.getName(), role.getId(), role.getPosition()),
                            s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Role:").setDescription(s).build(),
                            role -> {
                                if(!event.getMember().canInteract(role)) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "This role is placed higher than your highest role, therefore you cannot put it as autorole!").queue();
                                    return;
                                }

                                guildData.setGuildAutoRole(role.getId());
                                dbGuild.saveAsync();
                                event.getChannel().sendMessage(EmoteReference.OK + "The server autorole is now set to role: **" +
                                        role.getName() + "** (Position: " + role.getPosition() + ")").queue();
                            }
                    );
                });//endregion

        //region unbind
        registerOption("autorole:unbind", "Autorole clear",
                "Clear the server autorole.\n" +
                        "**Example:** `~>opts autorole unbind`",
                "Resets the servers autorole.", (event) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setGuildAutoRole(null);
                    dbGuild.saveAsync();
                    event.getChannel().sendMessage(EmoteReference.OK + "The autorole for this server has been removed.").queue();
                });//endregion
        //endregion

        //region usermessage
        //region resetchannel
        registerOption("usermessage:resetchannel", "Reset message channel",
                "Clears the join/leave message channel.\n" +
                        "**Example:** `~>opts usermessage resetchannel`",
                "Clears the join/leave message channel.", (event) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setLogJoinLeaveChannel(null);
                    guildData.setLogLeaveChannel(null);
                    guildData.setLogJoinChannel(null);
                    dbGuild.save();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Sucessfully reset the join/leave channel.").queue();
                });//endregion

        //region resetdata
        registerOption("usermessage:resetdata", "Reset join/leave message data",
                "Resets the join/leave message data.\n" +
                        "**Example:** `~>opts usermessage resetdata`",
                "Resets the join/leave message data.", (event) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setLeaveMessage(null);
                    guildData.setJoinMessage(null);
                    dbGuild.save();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Sucessfully reset the join/leave message.").queue();
                });
        //endregion

        //region channel

        registerOption("usermessage:join:channel", "Sets the join message channel", "Sets the join channel, you need the channel **name**\n" +
                "**Example:** `~>opts usermessage join channel join-magic`\n" +
                "You can reset it by doing `~>opts usermessage join channel reset_channel`", "Sets the join message channel", (event, args) -> {
            if(args.length == 0) {
                onHelp(event);
                return;
            }

            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            String channelName = args[0];

            List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
                    .filter(textChannel -> textChannel.getName().contains(channelName))
                    .collect(Collectors.toList());

            if(textChannels.isEmpty()) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "There were no channels matching your search.").queue();
                return;
            }

            if(textChannels.size() <= 1) {
                guildData.setLogJoinChannel(textChannels.get(0).getId());
                dbGuild.saveAsync();
                event.getChannel().sendMessage(EmoteReference.CORRECT + "The join log channel is set to: " +
                        textChannels.get(0).getAsMention()).queue();
                return;
            }

            DiscordUtils.selectList(event, textChannels,
                    textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                    s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                    textChannel -> {
                        guildData.setLogJoinChannel(textChannel.getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessage(EmoteReference.OK + "The join log channel is set to: " +
                                textChannel.getAsMention()).queue();
                    }
            );
        });

        registerOption("usermessage:join:channel:reset", "Resets the join message channel", "Resets the join message channel", event -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            guildData.setLogJoinChannel(null);
            dbGuild.saveAsync();
            event.getChannel().sendMessage(EmoteReference.CORRECT + "Successfully reset log join channel!").queue();
        });

        registerOption("usermessage:leave:channel", "Sets the leave message channel", "Sets the leave channel, you need the channel **name**\n" +
                "**Example:** `~>opts usermessage leave channel leave-magic`\n" +
                "You can reset it by doing `~>opts usermessage leave channel reset_channel`", "Sets the leave message channel", (event, args) -> {
            if(args.length == 0) {
                onHelp(event);
                return;
            }

            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            String channelName = args[0];
            List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
                    .filter(textChannel -> textChannel.getName().contains(channelName))
                    .collect(Collectors.toList());

            if(textChannels.isEmpty()) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "There were no channels matching your search.").queue();
                return;
            }

            if(textChannels.size() <= 1) {
                guildData.setLogLeaveChannel(textChannels.get(0).getId());
                dbGuild.saveAsync();
                event.getChannel().sendMessage(EmoteReference.CORRECT + "The join leave channel is set to: " +
                        textChannels.get(0).getAsMention()).queue();
                return;
            }

            DiscordUtils.selectList(event, textChannels,
                    textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                    s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                    textChannel -> {
                        guildData.setLogLeaveChannel(textChannel.getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessage(EmoteReference.OK + "The join leave channel is set to: " +
                                textChannel.getAsMention()).queue();
                    }
            );
        });

        registerOption("usermessage:leave:channel:reset", "Resets the leave message channel", "Resets the leave message channel", event -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            guildData.setLogLeaveChannel(null);
            dbGuild.saveAsync();
            event.getChannel().sendMessage(EmoteReference.CORRECT + "Successfully reset log leave channel!").queue();
        });

        registerOption("usermessage:channel", "Set message channel",
                "Sets the join/leave message channel. You need the channel **name**\n" +
                        "**Example:** `~>opts usermessage channel join-magic`\n" +
                        "Warning: if you set this, you cannot set individual join/leave channels unless you reset the channel.",
                "Sets the join/leave message channel.", (event, args) -> {
                    if(args.length == 0) {
                        onHelp(event);
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    String channelName = args[0];

                    List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
                            .filter(textChannel -> textChannel.getName().contains(channelName))
                            .collect(Collectors.toList());

                    if(textChannels.isEmpty()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "There were no channels matching your search.").queue();
                        return;
                    }

                    if(textChannels.size() <= 1) {
                        guildData.setLogJoinLeaveChannel(textChannels.get(0).getId());
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.CORRECT + "The logging Join/Leave channel is set to: " +
                                textChannels.get(0).getAsMention()).queue();
                        return;
                    }

                    DiscordUtils.selectList(event, textChannels,
                            textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                            s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                            textChannel -> {
                                guildData.setLogJoinLeaveChannel(textChannel.getId());
                                dbGuild.save();
                                event.getChannel().sendMessage(EmoteReference.OK + "The logging Join/Leave channel is set to: " +
                                        textChannel.getAsMention()).queue();
                            }
                    );
                });//endregion

        //region joinmessage
        registerOption("usermessage:joinmessage", "User join message",
                "Sets the join message.\n" +
                        "**Example:** `~>opts usermessage joinmessage Welcome $(event.user.name) to the $(event.guild.name) server! Hope you have a great time`",
                "Sets the join message.", (event, args) -> {
                    if(args.length == 0) {
                        onHelp(event);
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    String joinMessage = String.join(" ", args);
                    guildData.setJoinMessage(joinMessage);
                    dbGuild.save();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Server join message set to: " + joinMessage).queue();
                });//endregion

        //region leavemessage
        registerOption("usermessage:leavemessage", "User leave message",
                "Sets the leave message.\n" +
                        "**Example:** `~>opts usermessage leavemessage Sad to see you depart, $(event.user.name)`",
                "Sets the leave message.", (event, args) -> {
                    if(args.length == 0) {
                        onHelp(event);
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    String leaveMessage = String.join(" ", args);
                    guildData.setLeaveMessage(leaveMessage);
                    dbGuild.save();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Server leave message set to: " + leaveMessage).queue();
                });//endregion
        //endregion
        //region autoroles
        //region add
        registerOption("autoroles:add", "Autoroles add",
                "Adds a role to the `~>iam` list.\n" +
                        "You need the name of the iam and the name of the role. If the role contains spaces wrap it in quotation marks.\n" +
                        "**Example:** `~>opts autoroles add member Member`, `~>opts autoroles add wew \"A role with spaces on its name\"`",
                "Adds an auto-assignable role to the iam lists.", (event, args) -> {
                    if(args.length < 2) {
                        onHelp(event);
                        return;
                    }

                    String roleName = args[1];

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    List<Role> roleList = event.getGuild().getRolesByName(roleName, true);
                    if(roleList.size() == 0) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "I didn't find a role with that name!").queue();
                    } else if(roleList.size() == 1) {
                        Role role = roleList.get(0);

                        if(!event.getMember().canInteract(role)) {
                            event.getChannel().sendMessage(EmoteReference.ERROR +
                                    "This role is placed higher than your highest role, therefore you cannot put it as an auto-assignable role!").queue();
                            return;
                        }

                        guildData.getAutoroles().put(args[0], role.getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessage(EmoteReference.OK + "Added autorole **" + args[0] + "**, which gives the role " +
                                "**" +
                                role.getName() + "**").queue();
                    } else {
                        DiscordUtils.selectList(event, roleList, role -> String.format("%s (ID: %s)  | Position: %s", role.getName(),
                                role.getId(), role.getPosition()), s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Role:")
                                        .setDescription(s).build(),
                                role -> {
                                    if(!event.getMember().canInteract(role)) {
                                        event.getChannel().sendMessage(EmoteReference.ERROR +
                                                "This role is placed higher than your highest role, therefore you cannot put it as an auto-assignable role!").queue();
                                        return;
                                    }

                                    guildData.getAutoroles().put(args[0], role.getId());
                                    dbGuild.saveAsync();
                                    event.getChannel().sendMessage(EmoteReference.OK + "Added autorole **" + args[0] + "**, which gives the " +
                                            "role " +
                                            "**" +
                                            role.getName() + "**").queue();
                                });
                    }
                });

        //region remove
        registerOption("autoroles:remove", "Autoroles remove",
                "Removes a role from the `~>iam` list.\n" +
                        "You need the name of the iam.\n" +
                        "**Example:** `~>opts autoroles remove iamname`",
                "Removes an auto-assignable role from iam.", (event, args) -> {
                    if(args.length == 0) {
                        onHelp(event);
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    HashMap<String, String> autoroles = guildData.getAutoroles();
                    if(autoroles.containsKey(args[0])) {
                        autoroles.remove(args[0]);
                        dbGuild.saveAsync();
                        event.getChannel().sendMessage(EmoteReference.OK + "Removed autorole " + args[0]).queue();
                    } else {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't find an autorole with that name").queue();
                    }
                });//endregion

        //region clear
        registerOption("autoroles:clear", "Autoroles clear",
                "Removes all autoroles.",
                "Removes all autoroles.", (event, args) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    dbGuild.getData().getAutoroles().clear();
                    dbGuild.saveAsync();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Cleared all autoroles!").queue();
                }
        ); //endregion

        //region custom
        registerOption("admincustom", "Admin custom commands",
                "Locks custom commands to admin-only.\n" +
                        "Example: `~>opts admincustom true`",
                "Locks custom commands to admin-only.", (event, args) -> {
                    if(args.length == 0) {
                        OptsCmd.onHelp(event);
                        return;
                    }

                    String action = args[0];
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    try {
                        guildData.setCustomAdminLock(Boolean.parseBoolean(action));
                        dbGuild.save();
                        String toSend = EmoteReference.CORRECT + (Boolean.parseBoolean(action) ? "``Permission -> User command creation " +
                                "is now admin only.``" : "``Permission -> User command creation can be done by anyone.``");
                        event.getChannel().sendMessage(toSend).queue();
                    } catch(Exception ex) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Silly, that's not a boolean value!").queue();
                    }
                });
        //endregion

        registerOption("actionmention:toggle", "Action mention toggle",
                "Toggles action mention (double-mention). On by default.\n" +
                        "Example: `~>opts actionmention toggle`",
                "Deprecated.", event ->
                        event.getChannel().sendMessage(EmoteReference.ERROR + "This option has been deprecated. (Action commands don't double-mention anymore)").queue()
        );

        registerOption("timedisplay:set", "Time display set", "Toggles between 12h and 24h time display.\n" +
                "Example: `~>opts timedisplay 24h`", "Toggles between 12h and 24h time display.", (event, args) -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();

            if(args.length == 0) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify a mode (12h or 24h)").queue();
                return;
            }

            String mode = args[0];

            switch(mode) {
                case "12h":
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Set time display mode to 12h").queue();
                    guildData.setTimeDisplay(1);
                    dbGuild.save();
                    break;
                case "24h":
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Set time display mode to 24h").queue();
                    guildData.setTimeDisplay(0);
                    dbGuild.save();
                    break;
                default:
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid choice. Valid choices: **24h**, **12h**").queue();
                    break;
            }
        });

        registerOption("server:role:disallow", "Role disallow", "Disallows all users with a role from executing commands.\n" +
                        "You need to provide the name of the role to disallow from mantaro.\n" +
                        "Example: `~>opts server role disallow bad`, `~>opts server role disallow \"No commands\"`",
                "Disallows all users with a role from executing commands.", (event, args) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify the name of the role!").queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    String roleName = String.join(" ", args);

                    List<Role> roleList = event.getGuild().getRolesByName(roleName, true);
                    if(roleList.size() == 0) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "I didn't find a role with that name!").queue();
                    } else if(roleList.size() == 1) {
                        Role role = roleList.get(0);
                        guildData.getDisabledRoles().add(role.getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessage(EmoteReference.CORRECT + "Disabled role " + role.getName() + " from executing commands.").queue();
                    } else {
                        DiscordUtils.selectList(event, roleList, role -> String.format("%s (ID: %s)  | Position: %s", role.getName(),
                                role.getId(), role.getPosition()), s -> OptsCmd.getOpts().baseEmbed(event, "Select the Mute Role:")
                                        .setDescription(s).build(),
                                role -> {
                                    guildData.getDisabledRoles().add(role.getId());
                                    dbGuild.saveAsync();
                                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Disabled role " + role.getName() + " from executing commands.").queue();
                                });
                    }
                });

        registerOption("server:role:allow", "Role allow", "Allows all users with a role from executing commands.\n" +
                        "You need to provide the name of the role to allow from mantaro. Has to be already disabled.\n" +
                        "Example: `~>opts server role allow bad`, `~>opts server role allow \"No commands\"`",
                "Allows all users with a role from executing commands (Has to be already disabled)", (event, args) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify the name of the role!").queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    String roleName = String.join(" ", args);

                    List<Role> roleList = event.getGuild().getRolesByName(roleName, true);
                    if(roleList.size() == 0) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "I didn't find a role with that name!").queue();
                    } else if(roleList.size() == 1) {
                        Role role = roleList.get(0);

                        if(!guildData.getDisabledRoles().contains(role.getId())) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "This role isn't disabled!").queue();
                            return;
                        }

                        guildData.getDisabledRoles().remove(role.getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessage(EmoteReference.CORRECT + "Allowed role " + role.getName() + " to execute commands.").queue();
                    } else {
                        DiscordUtils.selectList(event, roleList, role -> String.format("%s (ID: %s)  | Position: %s", role.getName(),
                                role.getId(), role.getPosition()), s -> OptsCmd.getOpts().baseEmbed(event, "Select the Mute Role:")
                                        .setDescription(s).build(),
                                role -> {
                                    if(!guildData.getDisabledRoles().contains(role.getId())) {
                                        event.getChannel().sendMessage(EmoteReference.ERROR + "This role isn't disabled!").queue();
                                        return;
                                    }

                                    guildData.getDisabledRoles().remove(role.getId());
                                    dbGuild.saveAsync();
                                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Allowed role " + role.getName() + " to execute commands.").queue();
                                });
                    }
                });

        registerOption("server:ignorebots:autoroles:toggle",
                "Bot autorole ignore", "Toggles between ignoring bots on autorole assign and not.", (event) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    boolean ignore = guildData.isIgnoreBotsAutoRole();
                    guildData.setIgnoreBotsAutoRole(!ignore);
                    dbGuild.saveAsync();

                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Set bot autorole ignore to: **" + guildData.isIgnoreBotsAutoRole() + "**").queue();
                });

        registerOption("server:ignorebots:joinleave:toggle",
                "Bot join/leave ignore", "Toggles between ignoring bots on join/leave message.", (event) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    boolean ignore = guildData.isIgnoreBotsWelcomeMessage();
                    guildData.setIgnoreBotsWelcomeMessage(!ignore);
                    dbGuild.saveAsync();

                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Set bot autorole ignore to: **" + guildData.isIgnoreBotsWelcomeMessage() + "**").queue();
                });

        registerOption("levelupmessages:toggle", "Level-up toggle",
                "Toggles level up messages, remember that after this you have to set thee channel and the message!", "Toggles level up messages", event -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    boolean ignore = guildData.isEnabledLevelUpMessages();
                    guildData.setEnabledLevelUpMessages(!ignore);
                    dbGuild.saveAsync();

                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Set level up messages to: **" + guildData.isEnabledLevelUpMessages() + "**").queue();
                });

        registerOption("levelupmessages:message:set", "Level-up message", "Sets the message to display on level up",
                "Sets the level up message", (event, args) -> {
                    if(args.length == 0) {
                        onHelp(event);
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    String levelUpMessage = String.join(" ", args);
                    guildData.setLevelUpMessage(levelUpMessage);
                    dbGuild.saveAsync();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Server level-up message set to: " + levelUpMessage).queue();
                });

        registerOption("levelupmessages:message:clear", "Level-up message clear", "Clears the message to display on level up",
                "Clears the message to display on level up", (event, args) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    guildData.setLevelUpMessage(null);
                    dbGuild.saveAsync();

                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Cleared level-up message!").queue();
                });

        registerOption("levelupmessages:channel:set", "Level-up message channel",
                "Sets the channel to display level up messages", "Sets the channel to display level up messages",
                (event, args) -> {
                    if(args.length == 0) {
                        onHelp(event);
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    String channelName = args[0];
                    List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
                            .filter(textChannel -> textChannel.getName().contains(channelName))
                            .collect(Collectors.toList());

                    if(textChannels.isEmpty()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "There were no channels matching your search.").queue();
                        return;
                    }

                    if(textChannels.size() <= 1) {
                        guildData.setLevelUpChannel(textChannels.get(0).getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessage(EmoteReference.CORRECT + "The level-up channel has been set to: " +
                                textChannels.get(0).getAsMention()).queue();
                    } else {
                        DiscordUtils.selectList(event, textChannels,
                                textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                                s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                                textChannel -> {
                                    guildData.setLevelUpChannel(textChannel.getId());
                                    dbGuild.saveAsync();
                                    event.getChannel().sendMessage(EmoteReference.OK + "The level-up channel has been set to: " +
                                            textChannel.getAsMention()).queue();
                                }
                        );
                    }
                });
    }

    @Override
    public String description() {
        return "Guild Configuration";
    }
}
