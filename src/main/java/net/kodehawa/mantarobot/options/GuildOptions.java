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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.FinderUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static net.kodehawa.mantarobot.commands.OptsCmd.optsCmd;

@Option
public class GuildOptions extends OptionHandler {

    public GuildOptions() {
        setType(OptionType.GUILD);
    }

    @Subscribe
    public void onRegistry(OptionRegistryEvent e) {
        //region opts language
        //ironically, don't translate this one.
        registerOption("language:set", "Sets the language of this guild", "Sets the language of this guild. Languages use a language code (example en_US or de_DE).\n" +
                "**Example:** `~>opts language set es_CL`", "Sets the language of this guild", ((ctx, args) -> {
            if (args.length < 1) {
                ctx.sendFormat(
                        "%1$sYou need to specify the display language that you want the bot to use on this server. (To see avaliable lang codes, use `~>lang`)",
                        EmoteReference.ERROR
                );

                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            String language = args[0];

            if (!I18n.isValidLanguage(language)) {
                ctx.sendFormat("%s`%s` is not a valid language or it's not yet supported by Mantaro.", EmoteReference.ERROR2, language);
                return;
            }

            guildData.setLang(language);
            dbGuild.saveUpdating();
            ctx.sendFormat("%sSuccessfully set the language of this server to `%s`", EmoteReference.CORRECT, language);
        }));

        //endregion
        //region opts birthday
        registerOption("birthday:test", "Tests if the birthday assigner works.",
                "Tests if the birthday assigner works properly. You need to input an user mention/id/tag to test it with.",
                "Tests if the birthday assigner works.", (ctx, args) -> {
            if (args.length < 1) {
                ctx.sendLocalized("options.birthday_test.no_user", EmoteReference.ERROR2);
                return;
            }

            String query = String.join(" ", args);
            ctx.findMember(query, ctx.getMessage()).onSuccess(members -> {
                final var m = CustomFinderUtil.findMemberDefault(query, members, ctx, ctx.getMember());
                if (m == null) {
                    return;
                }

                final var dbGuild = ctx.getDBGuild();
                final var guildData = dbGuild.getData();
                final var guild = ctx.getGuild();

                TextChannel birthdayChannel = guildData.getBirthdayChannel() == null ? null : guild.getTextChannelById(guildData.getBirthdayChannel());
                Role birthdayRole = guildData.getBirthdayRole() == null ? null : guild.getRoleById(guildData.getBirthdayRole());

                if (birthdayChannel == null) {
                    ctx.sendLocalized("options.birthday_test.no_bd_channel", EmoteReference.ERROR);
                    return;
                }

                if (birthdayRole == null) {
                    ctx.sendLocalized("options.birthday_test.no_bd_role", EmoteReference.ERROR);
                    return;
                }

                if (!birthdayChannel.canTalk()) {
                    ctx.sendLocalized("options.birthday_test.no_talk_permission", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getSelfMember().canInteract(birthdayRole)) {
                    ctx.sendLocalized("options.birthday_test.cannot_interact", EmoteReference.ERROR);
                    return;
                }

                User user = m.getUser();
                String message = String.format("%s**%s is a year older now! Wish them a happy birthday.** :tada: (test)", EmoteReference.POPPER, m.getEffectiveName());
                if (dbGuild.getData().getBirthdayMessage() != null) {
                    message = dbGuild.getData().getBirthdayMessage().replace("$(user)", m.getEffectiveName()).replace("$(usermention)", m.getAsMention());
                }

                //Value used in lambda... blabla :c
                final String finalMessage = message;
                guild.addRoleToMember(m, birthdayRole).queue(success -> {
                    birthdayChannel.sendMessage(finalMessage).queue(s -> {
                        ctx.sendLocalized("options.birthday_test.success", EmoteReference.CORRECT, birthdayChannel.getName(), user.getName(), birthdayRole.getName());
                    }, error -> {
                        ctx.sendLocalized("options.birthday_test.error", EmoteReference.CORRECT, birthdayChannel.getName(), user.getName(), birthdayRole.getName());
                    });
                });
            });
        });

        registerOption("birthday:enable", "Birthday Monitoring enable",
                "Enables birthday monitoring. You need the channel **name** and the role name (it assigns that role on birthday)\n" +
                        "**Example:** `~>opts birthday enable general Birthday`, `~>opts birthday enable general \"Happy Birthday\"`",
                "Enables birthday monitoring.", (ctx, args) -> {
            if (args.length < 2) {
                ctx.sendLocalized("options.birthday_enable.no_args", EmoteReference.ERROR);
                return;
            }

            var lang = ctx.getLanguageContext();
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            try {
                String channel = args[0];
                String role = args[1];

                TextChannel channelObj = FinderUtils.findChannel(ctx.getEvent(), channel);
                if (channelObj == null)
                    return;

                String channelId = channelObj.getId();

                Role roleObj = FinderUtils.findRole(ctx.getEvent(), role);
                if (roleObj == null)
                    return;

                if (roleObj.isPublicRole()) {
                    ctx.sendLocalized("options.birthday_enable.public_role", EmoteReference.ERROR);
                    return;
                }

                if (guildData.getGuildAutoRole() != null && roleObj.getId().equals(guildData.getGuildAutoRole())) {
                    ctx.sendLocalized("options.birthday_enable.autorole", EmoteReference.ERROR);
                    return;
                }

                ctx.sendFormat(String.join("\n", lang.get("options.birthday_enable.warning"),
                        lang.get("options.birthday_enable.warning_1"),
                        lang.get("options.birthday_enable.warning_2"),
                        lang.get("options.birthday_enable.warning_3"),
                        lang.get("options.birthday_enable.warning_4")),
                        EmoteReference.WARNING, roleObj.getName()
                );

                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 45, interactiveEvent -> {
                    String content = interactiveEvent.getMessage().getContentRaw();
                    if (content.equalsIgnoreCase("yes")) {
                        String roleId = roleObj.getId();
                        guildData.setBirthdayChannel(channelId);
                        guildData.setBirthdayRole(roleId);
                        dbGuild.saveUpdating();
                        ctx.sendLocalized("options.birthday_enable.success", EmoteReference.MEGA, channelObj.getName(), channelId, role, roleId);
                        return Operation.COMPLETED;
                    } else if (content.equalsIgnoreCase("no")) {
                        ctx.sendLocalized("general.cancelled", EmoteReference.CORRECT);
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });

            } catch (IndexOutOfBoundsException ex1) {
                ctx.sendFormat(lang.get("options.birthday_enable.error_channel_1") + "\n" + lang.get("options.birthday_enable.error_channel_2"),
                        EmoteReference.ERROR
                );
            } catch (Exception ex) {
                ctx.send(lang.get("general.invalid_syntax") + "\nCheck https://github.com/Mantaro/MantaroBot/wiki/Configuration for more information.");
            }
        });

        registerOption("birthday:disable", "Birthday disable", "Disables birthday monitoring.", (ctx) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setBirthdayChannel(null);
            guildData.setBirthdayRole(null);
            dbGuild.saveAsync();
            ctx.sendLocalized("options.birthday_disable.success", EmoteReference.MEGA);
        });
        //endregion

        //region prefix
        //region set
        registerOption("prefix:set", "Prefix set",
                "Sets the server prefix.\n" +
                        "**Example:** `~>opts prefix set .`",
                "Sets the server prefix.", (ctx, args) -> {
            if (args.length < 1) {
                ctx.sendLocalized("options.prefix_set.no_prefix", EmoteReference.ERROR);
                return;
            }
            String prefix = args[0];

            if (prefix.length() > 50) {
                ctx.sendLocalized("options.prefix_set.too_long", EmoteReference.ERROR);
                return;
            }

            if (prefix.isEmpty()) {
                ctx.sendLocalized("options.prefix_set.empty_prefix", EmoteReference.ERROR);
                return;
            }

            if (prefix.equals("/tts")) {
                var tts = ctx.getSelfMember().hasPermission(ctx.getChannel(), Permission.MESSAGE_TTS);
                ctx.getChannel().sendMessage("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwww")
                        .tts(tts)
                        .queue();
                return;
            }

            if (prefix.equals("/shrug") || prefix.equals("¯\\_(ツ)_/¯")) {
                ctx.send("¯\\_(ツ)_/¯");
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setGuildCustomPrefix(prefix);
            dbGuild.save();

            ctx.sendLocalized("options.prefix_set.success", EmoteReference.MEGA, prefix);
        });//endregion

        //region clear
        registerOption("prefix:clear", "Prefix clear",
                "Clear the server prefix.\n**Example:** `~>opts prefix clear`", (ctx) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setGuildCustomPrefix(null);
            dbGuild.save();
            ctx.sendLocalized("options.prefix_clear.success", EmoteReference.MEGA);
        });//endregion
        // endregion

        //region autorole
        //region set
        registerOption("autorole:set", "Autorole set",
                "Sets the server autorole. This means every user who joins will get this role. **You need to use the role name, if it contains spaces" +
                        " you need to wrap it in quotation marks**\n" +
                        "**Example:** `~>opts autorole set Member`, `~>opts autorole set \"Magic Role\"`",
                "Sets the server autorole.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.autorole_set.no_role", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            Consumer<Role> consumer = (role) -> {
                if (!ctx.getMember().canInteract(role)) {
                    ctx.sendLocalized("options.autorole_set.hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getSelfMember().canInteract(role)) {
                    ctx.sendLocalized("options.autorole_set.self_hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                guildData.setGuildAutoRole(role.getId());
                dbGuild.saveAsync();
                ctx.sendLocalized("options.autorole_set.success", EmoteReference.CORRECT, role.getName(), role.getPosition());
            };

            Role role = FinderUtils.findRoleSelect(ctx.getEvent(), String.join(" ", args), consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });//endregion

        //region unbind
        registerOption("autorole:unbind", "Autorole clear",
                "Clear the server autorole.\n" +
                        "**Example:** `~>opts autorole unbind`",
                "Resets the servers autorole.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setGuildAutoRole(null);
            dbGuild.saveAsync();
            ctx.sendLocalized("options.autorole_unbind.success", EmoteReference.OK);
        });//endregion
        //endregion

        //region usermessage
        //region resetchannel
        registerOption("usermessage:resetchannel", "Reset message channel",
                "Clears the join/leave message channel.\n" +
                        "**Example:** `~>opts usermessage resetchannel`",
                "Clears the join/leave message channel.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setLogJoinLeaveChannel(null);
            guildData.setLogLeaveChannel(null);
            guildData.setLogJoinChannel(null);
            dbGuild.save();
            ctx.sendLocalized("options.usermessage_resetchannel.success", EmoteReference.CORRECT);
        });//endregion

        //region resetdata
        registerOption("usermessage:resetdata", "Reset join/leave message data",
                "Resets the join/leave message data.\n" +
                        "**Example:** `~>opts usermessage resetdata`",
                "Resets the join/leave message data.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setLeaveMessage(null);
            guildData.setJoinMessage(null);
            guildData.setLogJoinLeaveChannel(null);

            dbGuild.save();
            ctx.sendLocalized("options.usermessage_resetdata.success", EmoteReference.CORRECT);
        });
        //endregion

        //region channel

        registerOption("usermessage:join:channel", "Sets the join message channel", "Sets the join channel, you need the channel **name**\n" +
                "**Example:** `~>opts usermessage join channel join-magic`\n" +
                "You can reset it by doing `~>opts usermessage join resetchannel`", "Sets the join message channel", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.usermessage_join_channel.no_channel", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            String channelName = args[0];
            Consumer<TextChannel> consumer = tc -> {
                guildData.setLogJoinChannel(tc.getId());
                dbGuild.saveAsync();
                ctx.sendLocalized("options.usermessage_join_channel.success", EmoteReference.OK, tc.getAsMention());
            };

            TextChannel channel = FinderUtils.findChannelSelect(ctx.getEvent(), channelName, consumer);

            if (channel != null) {
                consumer.accept(channel);
            }
        });
        addOptionAlias("usermessage:join:channel", "joinchannel");

        registerOption("usermessage:join:resetchannel", "Resets the join message channel", "Resets the join message channel", (ctx) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setLogJoinChannel(null);
            dbGuild.saveAsync();
            ctx.sendLocalized("options.usermessage_join_resetchannel.success", EmoteReference.CORRECT);
        });
        addOptionAlias("usermessage:join:resetchannel", "resetjoinchannel");


        registerOption("usermessage:leave:channel", "Sets the leave message channel", "Sets the leave channel, you need the channel **name**\n" +
                "**Example:** `~>opts usermessage leave channel leave-magic`\n" +
                "You can reset it by doing `~>opts usermessage leave resetchannel`", "Sets the leave message channel", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.usermessage_leave_channel.no_channel", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            String channelName = args[0];

            Consumer<TextChannel> consumer = tc -> {
                guildData.setLogLeaveChannel(tc.getId());
                dbGuild.saveAsync();
                ctx.sendLocalized("options.usermessage_leave_channel.success", EmoteReference.CORRECT, tc.getAsMention());
            };

            TextChannel channel = FinderUtils.findChannelSelect(ctx.getEvent(), channelName, consumer);

            if (channel != null) {
                consumer.accept(channel);
            }
        });
        addOptionAlias("usermessage:leave:channel", "leavechannel");

        registerOption("usermessage:leave:resetchannel", "Resets the leave message channel", "Resets the leave message channel", (ctx) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setLogLeaveChannel(null);
            dbGuild.saveAsync();
            ctx.sendLocalized("options.usermessage_leave_resetchannel.success", EmoteReference.CORRECT);
        });
        addOptionAlias("usermessage:leave:resetchannel", "resetleavechannel");

        registerOption("usermessage:channel", "Set message channel",
                "Sets the join/leave message channel. You need the channel **name**\n" +
                        "**Example:** `~>opts usermessage channel join-magic`\n" +
                        "Warning: if you set this, you cannot set individual join/leave channels unless you reset the channel.",
                "Sets the join/leave message channel.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.usermessage_channel.no_channel", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            String channelName = args[0];

            Consumer<TextChannel> consumer = textChannel -> {
                guildData.setLogJoinLeaveChannel(textChannel.getId());
                dbGuild.save();
                ctx.sendLocalized("options.usermessage_channel.success", EmoteReference.OK, textChannel.getAsMention());
            };

            TextChannel channel = FinderUtils.findChannelSelect(ctx.getEvent(), channelName, consumer);

            if (channel != null) {
                consumer.accept(channel);
            }
        });//endregion

        //region joinmessage
        registerOption("usermessage:joinmessage", "User join message",
                "Sets the join message.\n" +
                        "**Example:** `~>opts usermessage joinmessage Welcome $(event.user.name) to the $(event.guild.name) server! Hope you have a great time`",
                "Sets the join message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.usermessage_joinmessage.no_message", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            String joinMessage = String.join(" ", args);
            guildData.setJoinMessage(joinMessage);
            dbGuild.save();
            ctx.sendLocalized("options.usermessage_joinmessage.success", EmoteReference.CORRECT, joinMessage);
        });//endregion
        addOptionAlias("usermessage:joinmessage", "joinmessage");


        //region joinmessage
        registerOption("usermessage:resetjoinmessage", "Reset join message",
                "Resets the join message", "Resets the join message.", (ctx, args) -> {
                    DBGuild dbGuild = ctx.getDBGuild();
                    GuildData guildData = dbGuild.getData();

                    guildData.setJoinMessage(null);
                    dbGuild.save();
                    ctx.sendLocalized("options.usermessage_joinmessage_reset.success", EmoteReference.CORRECT);
                });//endregion
        addOptionAlias("usermessage:resetjoinmessage", "resetjoinmessage");


        //region leavemessage
        registerOption("usermessage:leavemessage", "User leave message",
                "Sets the leave message.\n" +
                        "**Example:** `~>opts leavemessage Sad to see you depart, $(event.user.name)`",
                "Sets the leave message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.usermessage_leavemessage.no_message", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            String leaveMessage = String.join(" ", args);
            guildData.setLeaveMessage(leaveMessage);
            dbGuild.save();
            ctx.sendLocalized("options.usermessage_leavemessage.success", EmoteReference.CORRECT, leaveMessage);
        });//endregion
        addOptionAlias("usermessage:leavemessage", "leavemessage");

        //region joinmessage
        registerOption("usermessage:resetleavemessage", "Reset leave message",
                "Resets the leave message","Resets the leave message.", (ctx, args) -> {
                    DBGuild dbGuild = ctx.getDBGuild();
                    GuildData guildData = dbGuild.getData();

                    guildData.setLeaveMessage(null);
                    dbGuild.save();
                    ctx.sendLocalized("options.usermessage_leavemessage_reset.success", EmoteReference.CORRECT);
                });//endregion
        addOptionAlias("usermessage:resetleavemessage", "resetleavemessage");


        registerOption("usermessage:joinmessages:add", "Join Message extra messages add", "Adds a new join message\n" +
                "**Example**: `~>opts usermessage joinmessages add hi`", "Adds a new join message", ((ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.usermessage_joinmessages_add.no_message", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            String message = String.join(" ", args);

            guildData.getExtraJoinMessages().add(message);
            dbGuild.save();

            ctx.sendLocalized("options.usermessage_joinmessage_add.success", EmoteReference.CORRECT, message);
        }));

        registerOption("usermessage:joinmessages:remove", "Join Message extra messages remove", "Removes a join message\n" +
                "**Example**: `~>opts usermessage joinmessages remove 0`", "Removes a join message", ((ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.usermessage_joinmessages_remove.no_message", EmoteReference.ERROR);
                return;
            }

            try {
                DBGuild dbGuild = ctx.getDBGuild();
                GuildData guildData = dbGuild.getData();
                int index;
                try {
                    index = Integer.parseInt(args[0]);
                } catch (NumberFormatException ex) {
                    ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR2);
                    return;
                }

                String old = guildData.getExtraJoinMessages().get(index);
                guildData.getExtraJoinMessages().remove(index);
                dbGuild.save();

                ctx.sendLocalized("options.usermessage_joinmessage_remove.success", EmoteReference.CORRECT, old, index);
            } catch (ArrayIndexOutOfBoundsException ex) {
                ctx.sendLocalized("options.usermessage_joinmessage_remove.wrong_index", EmoteReference.ERROR);
            }
        }));

        registerOption("usermessage:joinmessages:clear", "Join Message extra messages clear", "Clears all extra join messages\n" +
                "**Example**: `~>opts usermessage joinmessages clear`", "Clears all extra join messages", ((ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            dbGuild.getData().getExtraJoinMessages().clear();
            dbGuild.save();

            ctx.sendLocalized("options.usermessage_joinmessage_clear.success", EmoteReference.CORRECT);
        }));

        registerOption("usermessage:joinmessages:list", "Join Message extra messages list", "Lists all extra join messages\n" +
                "**Example**: `~>opts usermessage joinmessages list`", "Lists all extra join messages", ((ctx, args) -> {
            StringBuilder builder = new StringBuilder();
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData data = dbGuild.getData();

            if (data.getExtraJoinMessages().isEmpty()) {
                ctx.sendLocalized("options.usermessage_joinmessage_list.no_extras", EmoteReference.ERROR);
                return;
            }

            if (data.getJoinMessage() != null) {
                builder.append("M: ").append(data.getJoinMessage()).append("\n\n");
            }

            AtomicInteger index = new AtomicInteger();
            for (String s : data.getExtraJoinMessages()) {
                builder.append(index.getAndIncrement()).append(".- ").append(s).append("\n");
            }

            List<String> m = DiscordUtils.divideString(builder);
            List<String> messages = new LinkedList<>();
            var lang = ctx.getLanguageContext();
            boolean hasReactionPerms = ctx.hasReactionPerms();
            for (String s1 : m) {
                messages.add(String.format(lang.get("options.usermessage_joinmessage_list.header"),
                        hasReactionPerms ? lang.get("general.text_menu") + " " : lang.get("general.arrow_react"), String.format("```prolog\n%s```", s1)));
            }

            if (hasReactionPerms) {
                DiscordUtils.list(ctx.getEvent(), 45, false, messages);
            } else {
                DiscordUtils.listText(ctx.getEvent(), 45, false, messages);
            }
        }));

        registerOption("usermessage:leavemessages:add", "Leave Message extra messages add", "Adds a new leave message\n" +
                "**Example**: `~>opts usermessage leavemessages add hi`", "Adds a new leave message", ((ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.usermessage_leavemessages_add.no_message", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            String message = String.join(" ", args);

            guildData.getExtraLeaveMessages().add(message);
            dbGuild.save();
            ctx.sendLocalized("options.usermessage_leavemessage_add.success", EmoteReference.CORRECT, message);
        }));

        registerOption("usermessage:leavemessages:remove", "Leave Message extra messages remove", "Removes a leave message\n" +
                "**Example**: `~>opts usermessage leavemessages remove 0`", "Removes a leave message", ((ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.usermessage_leavemessages_remove.no_message", EmoteReference.ERROR);
                return;
            }

            try {
                DBGuild dbGuild = ctx.getDBGuild();
                GuildData guildData = dbGuild.getData();
                int index;
                try {
                    index = Integer.parseInt(args[0]);
                } catch (NumberFormatException ex) {
                    ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR2);
                    return;
                }

                String old = guildData.getExtraLeaveMessages().get(index);

                guildData.getExtraLeaveMessages().remove(index);
                dbGuild.save();

                ctx.sendLocalized("options.usermessage_leavemessage_remove.success", EmoteReference.CORRECT, old, index);
            } catch (ArrayIndexOutOfBoundsException ae) {
                ctx.sendLocalized("options.usermessage_leavemessage_remove.wrong_index", EmoteReference.ERROR);
            }
        }));

        registerOption("usermessage:leavemessages:clear", "Leave Message extra messages clear", "Clears all extra leave messages\n" +
                "**Example**: `~>opts usermessage leavemessages clear`", "Clears all extra leave messages", ((ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            dbGuild.getData().getExtraLeaveMessages().clear();
            dbGuild.save();

            ctx.sendLocalized("options.usermessage_leavemessage_clear.success", EmoteReference.CORRECT);
        }));

        registerOption("usermessage:leavemessages:list", "Leave Message extra messages list", "Lists all extra leave messages\n" +
                "**Example**: `~>opts usermessage leavemessages list`", "Lists all extra leave messages", ((ctx, args) -> {
            StringBuilder builder = new StringBuilder();
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData data = dbGuild.getData();

            if (data.getExtraLeaveMessages().isEmpty()) {
                ctx.sendLocalized("options.usermessage_leavemessage_list.no_extras", EmoteReference.ERROR);
                return;
            }

            if (data.getLeaveMessage() != null) {
                builder.append("M: ").append(data.getLeaveMessage()).append("\n\n");
            }

            AtomicInteger index = new AtomicInteger();
            for (String s : data.getExtraLeaveMessages()) {
                builder.append(index.getAndIncrement()).append(".- ").append(s).append("\n");
            }

            List<String> m = DiscordUtils.divideString(builder);
            List<String> messages = new LinkedList<>();
            var lang = ctx.getLanguageContext();

            boolean hasReactionPerms = ctx.hasReactionPerms();
            for (String s1 : m) {
                messages.add(String.format(lang.get("options.usermessage_leavemessage_list.header"),
                        hasReactionPerms ? lang.get("general.text_menu") + " " : lang.get("general.arrow_react"), String.format("```prolog\n%s```", s1)));
            }

            if (hasReactionPerms) {
                DiscordUtils.list(ctx.getEvent(), 45, false, messages);
            } else {
                DiscordUtils.listText(ctx.getEvent(), 45, false, messages);
            }
        }));

        //endregion
        //region autoroles
        //region add
        registerOption("autoroles:add", "Autoroles add",
                "Adds a role to the `~>iam` list.\n" +
                        "You need the name of the iam and the name of the role. If the role contains spaces wrap it in quotation marks.\n" +
                        "**Example:** `~>opts autoroles add member Member`, `~>opts autoroles add wew \"A role with spaces on its name\"`",
                "Adds an auto-assignable role to the iam lists.", (ctx, args) -> {
            if (args.length < 2) {
                ctx.sendLocalized("options.autoroles_add.no_args", EmoteReference.ERROR);
                return;
            }

            String roleName = args[1];

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            List<Role> roleList = ctx.getGuild().getRolesByName(roleName, true);
            if (roleList.size() == 0) {
                ctx.sendLocalized("options.autoroles_add.no_role_found", EmoteReference.ERROR);
            } else if (roleList.size() == 1) {
                Role role = roleList.get(0);

                if (!ctx.getMember().canInteract(role)) {
                    ctx.sendLocalized("options.autoroles_add.hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getSelfMember().canInteract(role)) {
                    ctx.sendLocalized("options.autoroles_add.self_hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                guildData.getAutoroles().put(args[0], role.getId());
                dbGuild.saveAsync();
                ctx.sendLocalized("options.autoroles_add.success", EmoteReference.OK, args[0], role.getName());
            } else {
                DiscordUtils.selectList(ctx.getEvent(), roleList, role -> String.format("%s (ID: %s)  | Position: %s", role.getName(),
                        role.getId(), role.getPosition()), s -> optsCmd.baseEmbed(ctx.getEvent(), "Select the Role:").setDescription(s).build(),
                        role -> {
                            if (!ctx.getMember().canInteract(role)) {
                                ctx.sendLocalized("options.autoroles_add.hierarchy_conflict", EmoteReference.ERROR);
                                return;
                            }

                            if (!ctx.getSelfMember().canInteract(role)) {
                                ctx.sendLocalized("options.autoroles_add.self_hierarchy_conflict", EmoteReference.ERROR);
                                return;
                            }

                            guildData.getAutoroles().put(args[0], role.getId());
                            dbGuild.saveAsync();
                            ctx.sendLocalized("options.autoroles_add.success", EmoteReference.OK, args[0], role.getName());
                        });
                }
        });

        //region remove
        registerOption("autoroles:remove", "Autoroles remove",
                "Removes a role from the `~>iam` list.\n" +
                        "You need the name of the iam.\n" +
                        "**Example:** `~>opts autoroles remove iamname`",
                "Removes an auto-assignable role from iam.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.autoroles_add.no_args", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            HashMap<String, String> autoroles = guildData.getAutoroles();
            if (autoroles.containsKey(args[0])) {
                autoroles.remove(args[0]);
                dbGuild.saveAsync();
                ctx.sendLocalized("options.autoroles_remove.success", EmoteReference.OK, args[0]);
            } else {
                ctx.sendLocalized("options.autoroles_remove.not_found", EmoteReference.ERROR);
            }
        });//endregion

        //region clear
        registerOption("autoroles:clear", "Autoroles clear",
                "Removes all autoroles.",
                "Removes all autoroles.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            dbGuild.getData().getAutoroles().clear();
            dbGuild.saveAsync();
            ctx.sendLocalized("options.autoroles_clear.success", EmoteReference.CORRECT);
        }); //endregion

        registerOption("autoroles:category:add", "Adds a category to autoroles",
                "Adds a category to autoroles. Useful for organizing",
                "Adds a category to autoroles.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.autoroles_category_add.no_args", EmoteReference.ERROR);
                return;
            }

            String category = args[0];
            String autorole = null;
            if (args.length > 1) {
                autorole = args[1];
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            Map<String, List<String>> categories = guildData.getAutoroleCategories();
            if (categories.containsKey(category) && autorole == null) {
                ctx.sendLocalized("options.autoroles_category_add.already_exists", EmoteReference.ERROR);
                return;
            }

            categories.computeIfAbsent(category, (a) -> new ArrayList<>());

            if (autorole != null) {
                if (guildData.getAutoroles().containsKey(autorole)) {
                    categories.get(category).add(autorole);
                    dbGuild.save();
                    ctx.sendLocalized("options.autoroles_category_add.success", EmoteReference.CORRECT, category, autorole);
                } else {
                    ctx.sendLocalized("options.autoroles_category_add.no_role", EmoteReference.ERROR, autorole);
                }
                return;
            }

            dbGuild.save();
            ctx.sendLocalized("options.autoroles_category_add.success_new", EmoteReference.CORRECT, category);
        });

        registerOption("autoroles:category:remove", "Removes a category from autoroles",
                "Removes a category from autoroles. Useful for organizing",
                "Removes a category from autoroles.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.autoroles_category_remove.no_args", EmoteReference.ERROR);
                return;
            }

            String category = args[0];
            String autorole = null;
            if (args.length > 1) {
                autorole = args[1];
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            Map<String, List<String>> categories = guildData.getAutoroleCategories();
            if (!categories.containsKey(category)) {
                ctx.sendLocalized("options.autoroles_category_add.no_category", EmoteReference.ERROR, category);
                return;
            }

            if (autorole != null) {
                categories.get(category).remove(autorole);
                dbGuild.save();
                ctx.sendLocalized("options.autoroles_category_remove.success", EmoteReference.CORRECT, category, autorole);
                return;
            }

            categories.remove(category);
            dbGuild.save();
            ctx.sendLocalized("options.autoroles_category_remove.success_new", EmoteReference.CORRECT, category);
        });

        //region custom
        registerOption("admincustom", "Admin custom commands",
                "Locks custom commands to admin-only.\n" +
                        "Example: `~>opts admincustom true`",
                "Locks custom commands to admin-only.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.admincustom.no_args", EmoteReference.ERROR);
                return;
            }

            String action = args[0];
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            var lang = ctx.getLanguageContext();

            try {
                guildData.setCustomAdminLockNew(Boolean.parseBoolean(action));
                dbGuild.save();
                String toSend = String.format("%s%s", EmoteReference.CORRECT, Boolean.parseBoolean(action) ? lang.get("options.admincustom.admin_only") : lang.get("options.admincustom.everyone"));
                ctx.send(toSend);
            } catch (Exception ex) {
                ctx.sendLocalized("options.admincustom.not_bool", EmoteReference.ERROR);
            }
        });
        //endregion

        registerOption("timedisplay:set", "Time display set", "Toggles between 12h and 24h time display.\n" +
                "Example: `~>opts timedisplay 24h`", "Toggles between 12h and 24h time display.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (args.length == 0) {
                ctx.sendLocalized("options.timedisplay_set.no_mode_specified", EmoteReference.ERROR);
                return;
            }

            String mode = args[0];

            switch (mode) {
                case "12h" -> {
                    ctx.sendLocalized("options.timedisplay_set.12h", EmoteReference.CORRECT);
                    guildData.setTimeDisplay(1);
                    dbGuild.save();
                }
                case "24h" -> {
                    ctx.sendLocalized("options.timedisplay_set.24h", EmoteReference.CORRECT);
                    guildData.setTimeDisplay(0);
                    dbGuild.save();
                }
                default -> ctx.sendLocalized("options.timedisplay_set.invalid", EmoteReference.ERROR);
            }
        });

        registerOption("server:role:disallow", "Role disallow", "Disallows all users with a role from executing commands.\n" +
                        "You need to provide the name of the role to disallow from mantaro.\n" +
                        "Example: `~>opts server role disallow bad`, `~>opts server role disallow \"No commands\"`",
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

        registerOption("server:role:allow", "Role allow", "Allows all users with a role from executing commands.\n" +
                        "You need to provide the name of the role to allow from mantaro. Has to be already disabled.\n" +
                        "Example: `~>opts server role allow bad`, `~>opts server role allow \"No commands\"`",
                "Allows all users with a role from executing commands (Has to be already disabled)", (ctx, args) -> {
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

        registerOption("server:ignorebots:autoroles:toggle",
                "Bot autorole ignore", "Toggles between ignoring bots on autorole assign and not.", (ctx) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            boolean ignore = guildData.isIgnoreBotsAutoRole();
            guildData.setIgnoreBotsAutoRole(!ignore);
            dbGuild.saveAsync();

            ctx.sendLocalized("options.server_ignorebots_autoroles_toggle.success", EmoteReference.CORRECT, guildData.isIgnoreBotsAutoRole());
        });

        registerOption("server:ignorebots:joinleave:toggle",
                "Bot join/leave ignore", "Toggles between ignoring bots on join/leave message.", (ctx) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            boolean ignore = guildData.isIgnoreBotsWelcomeMessage();
            guildData.setIgnoreBotsWelcomeMessage(!ignore);
            dbGuild.saveAsync();

            ctx.sendLocalized("options.server_ignorebots_joinleave_toggle.success", EmoteReference.CORRECT, guildData.isIgnoreBotsWelcomeMessage());
        });

        registerOption("birthday:message:set", "Birthday message", "Sets the message to display on a new birthday",
                "Sets the birthday message", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.birthday_message_set.no_message", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            String birthdayMessage = String.join(" ", args);
            guildData.setBirthdayMessage(birthdayMessage);
            dbGuild.saveAsync();
            ctx.sendLocalized("options.birthday_message_set.success", EmoteReference.CORRECT, birthdayMessage);
        });

        registerOption("birthday:message:clear", "Birthday message clear", "Clears the message to display on a new birthday",
                "Clears the message to display on birthday", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            guildData.setBirthdayMessage(null);
            dbGuild.saveAsync();

            ctx.sendLocalized("options.birthday_message_clear.success", EmoteReference.CORRECT);
        });

        //region joinmessage
        registerOption("modlog:blacklistwords:add", "Modlog Word Blacklist add",
                "Adds a word to the modlog word blacklist (won't add any messages with that word). Can contain spaces.\n" +
                        "**Example:** `~>opts modlog blacklistwords add mood`",
                "Sets the join message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.modlog_blacklistwords_add.no_word", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (guildData.getModLogBlacklistWords().size() > 20) {
                ctx.sendLocalized("options.modlog_blacklistwords_add.too_many", EmoteReference.ERROR);
                return;
            }

            String word = String.join(" ", args);
            guildData.getModLogBlacklistWords().add(word);
            dbGuild.save();
            ctx.sendLocalized("options.modlog_blacklistwords_add.success", EmoteReference.CORRECT, word);
        });//endregion

        //region joinmessage
        registerOption("modlog:blacklistwords:remove", "Modlog Word Blacklist remove",
                "Removes a word from the modlog word blacklist. Can contain spaces\n" +
                        "**Example:** `~>opts modlog blacklistwords remove mood`",
                "Sets the join message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.modlog_blacklistwords_add.no_word", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            String word = String.join(" ", args);

            if (!guildData.getModLogBlacklistWords().contains(word)) {
                ctx.sendLocalized("options.modlog_blacklistwords_remove.not_in", EmoteReference.ERROR, word);
                return;
            }

            guildData.getModLogBlacklistWords().remove(word);
            dbGuild.save();
            ctx.sendLocalized("options.modlog_blacklistwords_remove.success", EmoteReference.CORRECT, word);
        });//endregion

        //region editmessage
        registerOption("logs:editmessage", "Edit log message",
                "Sets the edit message.\n" +
                        "**Example:** `~>opts logs editmessage [$(hour)] Message (ID: $(event.message.id)) created by **$(event.user.tag)** in channel **$(event.channel.name)** was modified.\n```diff\n-$(old)\n+$(new)````",
                "Sets the edit message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.logs_editmessage.no_message", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (args[0].equals("reset")) {
                guildData.setEditMessageLog(null);
                dbGuild.save();
                ctx.sendLocalized("options.logs_editmessage.reset_success", EmoteReference.CORRECT);
                return;
            }

            String editMessage = String.join(" ", args);
            guildData.setEditMessageLog(editMessage);
            dbGuild.save();
            ctx.sendLocalized("options.logs_editmessage.success", EmoteReference.CORRECT, editMessage);
        });//endregion
        addOptionAlias("logs:editmessage", "editmessage");

        //region deletemessage
        registerOption("logs:deletemessage", "Delete log message",
                "Sets the delete message.\n" +
                        "**Example:** `~>opts logs deletemessage [$(hour)] Message (ID: $(event.message.id)) created by **$(event.user.tag)** (ID: $(event.user.id)) in channel **$(event.channel.name)** was deleted.```diff\n-$(content)``` `",
                "Sets the delete message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.logs_deletemessage.no_message", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (args[0].equals("reset")) {
                guildData.setDeleteMessageLog(null);
                dbGuild.save();
                ctx.sendLocalized("options.logs_deletemessage.reset_success", EmoteReference.CORRECT);
                return;
            }

            String deleteMessage = String.join(" ", args);
            guildData.setDeleteMessageLog(deleteMessage);
            dbGuild.save();
            ctx.sendLocalized("options.logs_deletemessage.success", EmoteReference.CORRECT, deleteMessage);
        });//endregion
        addOptionAlias("logs:deletemessage", "deletemessage");

        //region banmessage
        registerOption("logs:banmessage", "Ban log message",
                "Sets the ban message.\n" +
                        "**Example:** `~>opts logs banmessage [$(hour)] $(event.user.tag) just got banned.`",
                "Sets the ban message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.logs_banmessage.no_message", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (args[0].equals("reset")) {
                guildData.setBannedMemberLog(null);
                dbGuild.save();
                ctx.sendLocalized("options.logs_banmessage.reset_success", EmoteReference.CORRECT);
                return;
            }

            String banMessage = String.join(" ", args);
            guildData.setBannedMemberLog(banMessage);
            dbGuild.save();
            ctx.sendLocalized("options.logs_banmessage.success", EmoteReference.CORRECT, banMessage);
        });//endregion
        addOptionAlias("logs:banmessage", "banmessage");

        //region ubbanmessage
        registerOption("logs:unbanmessage", "Unban log message",
                "Sets the unban message.\n" +
                        "**Example:** `~>opts logs unbanmessage [$(hour)] $(event.user.tag) just got unbanned.`",
                "Sets the unban message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.logs_unbanmessage.no_message", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (args[0].equals("reset")) {
                guildData.setUnbannedMemberLog(null);
                dbGuild.save();
                ctx.sendLocalized("options.logs_unbanmessage.reset_success", EmoteReference.CORRECT);
                return;
            }

            String unbanMessage = String.join(" ", args);
            guildData.setUnbannedMemberLog(unbanMessage);
            dbGuild.save();
            ctx.sendLocalized("options.logs_unbanmessage.success", EmoteReference.CORRECT, unbanMessage);
        });//endregion
        addOptionAlias("logs:unbanmessage", "unbanmessage");

        registerOption("commands:showdisablewarning", "Show disable warning", "Toggles on/off the disabled command warning.",
                "Toggles on/off the disabled command warning.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            guildData.setCommandWarningDisplay(!guildData.isCommandWarningDisplay()); //lombok names are amusing
            dbGuild.save();
            ctx.sendLocalized("options.showdisablewarning.success", EmoteReference.CORRECT, guildData.isCommandWarningDisplay());
        });

        registerOption("commands:birthdayblacklist:add", "Add someone to the birthday blacklist", "Adds a person to the birthday blacklist",
                "Add someone to the birthday blacklist", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            if (args.length == 0) {
                ctx.sendLocalized("options.birthdayblacklist.no_args", EmoteReference.ERROR);
                return;
            }

            String content = String.join(" ", args);
            ctx.findMember(content, ctx.getMessage()).onSuccess(members -> {
                Member member = CustomFinderUtil.findMemberDefault(content, members, ctx, ctx.getMember());
                if (member == null)
                    return;

                guildData.getBirthdayBlockedIds().add(member.getId());
                ctx.sendLocalized("options.birthdayblacklist.add.success", EmoteReference.CORRECT, member.getEffectiveName(), member.getId());
            });
        });

        registerOption("commands:birthdayblacklist:remove", "Removes someone to the birthday blacklist", "Removes a person from the birthday blacklist",
                "Remove someone to the birthday blacklist", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            if (args.length == 0) {
                ctx.sendLocalized("options.birthdayblacklist.no_args", EmoteReference.ERROR);
                return;
            }

            String content = String.join(" ", args);
            ctx.findMember(content, ctx.getMessage()).onSuccess(members -> {
                Member member = CustomFinderUtil.findMemberDefault(content, members, ctx, ctx.getMember());
                if (member == null)
                    return;

                guildData.getBirthdayBlockedIds().remove(member.getId());
                ctx.sendLocalized("options.birthdayblacklist.remove.success", EmoteReference.CORRECT, member.getEffectiveName(), member.getId());
            });
        });

        registerOption("commands:lobby:disable", "Disables game multiple and lobby.", "Disables game multiple and lobby.",
                "Disables game multiple and lobby.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            guildData.setGameMultipleDisabled(true);
            dbGuild.save();

            ctx.sendLocalized("options.lobby.disable.success", EmoteReference.CORRECT);
        });

        registerOption("commands:lobby:enable", "Enables game multiple and lobby.", "Enables game multiple and lobby.",
                "Enables game multiple and lobby.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (!guildData.isGameMultipleDisabled()) {
                ctx.sendLocalized("options.lobby.enable.already_enabled", EmoteReference.CORRECT);
                return;
            }

            guildData.setGameMultipleDisabled(false);
            dbGuild.save();

            ctx.sendLocalized("options.lobby.enable.success", EmoteReference.CORRECT);
        });

        registerOption("djrole:set", "Set a custom DJ role",
                "Sets a custom DJ role. This role will be used to control music." +
                        "**Example:** `~>opts djrole set DJ`, `~>opts djrole set \"Magic Role\"`",
                "Sets the DJ role.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.djrole_set.no_role", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            Consumer<Role> consumer = (role) -> {
                guildData.setDjRoleId(role.getId());
                dbGuild.saveAsync();
                ctx.sendLocalized("options.djrole_set.success", EmoteReference.CORRECT, role.getName(), role.getPosition());
            };

            Role role = FinderUtils.findRoleSelect(ctx.getEvent(), String.join(" ", args), consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });

        registerOption("djrole:reset", "Resets the DJ role",
                "Resets the DJ role", "Resets the DJ role.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            guildData.setDjRoleId(null);
            dbGuild.saveAsync();
            ctx.sendLocalized("options.djrole_reset.success", EmoteReference.CORRECT);
        });

    }

    @Override
    public String description() {
        return "Guild Configuration";
    }
}
