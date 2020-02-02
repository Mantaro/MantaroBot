/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.options;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
                                                                                  "**Example:** `~>opts language set es_CL`", "Sets the language of this guild", ((event, args) -> {
            if(args.length < 1) {
                event.getChannel().sendMessageFormat(
                        "%1$sYou need to specify the display language that you want the bot to use on this server. (To see avaliable lang codes, use `~>lang`)",
                        EmoteReference.ERROR
                ).queue();
                
                return;
            }
            
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            String language = args[0];
            
            if(!I18n.isValidLanguage(language)) {
                new MessageBuilder().append(String.format("%s`%s` is not a valid language or it's not yet supported by Mantaro.", EmoteReference.ERROR2, language))
                        .stripMentions(event.getJDA())
                        .sendTo(event.getChannel()).queue();
                return;
            }
            
            guildData.setLang(language);
            dbGuild.save();
            event.getChannel().sendMessageFormat("%sSuccessfully set the language of this server to `%s`", EmoteReference.CORRECT, language).queue();
        }));
        //endregion
        //region opts birthday
        registerOption("birthday:test", "Tests if the birthday assigner works.",
                "Tests if the birthday assigner works properly. You need to input an user mention/id/tag to test it with.", "Tests if the birthday assigner works.",
                (event, args, lang) -> {
                    if(args.length < 1) {
                        event.getChannel().sendMessageFormat(lang.get("options.birthday_test.no_user"), EmoteReference.ERROR2).queue();
                        return;
                    }
                    
                    Member m = Utils.findMember(event, event.getMember(), String.join(" ", args));
                    if(m == null)
                        return;
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    TextChannel birthdayChannel = guildData.getBirthdayChannel() == null ? null : event.getGuild().getTextChannelById(guildData.getBirthdayChannel());
                    Role birthdayRole = guildData.getBirthdayRole() == null ? null : event.getGuild().getRoleById(guildData.getBirthdayRole());
                    
                    if(birthdayChannel == null) {
                        event.getChannel().sendMessageFormat(lang.get("options.birthday_test.no_bd_channel"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    if(birthdayRole == null) {
                        event.getChannel().sendMessageFormat(lang.get("options.birthday_test.no_bd_role"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    if(!birthdayChannel.canTalk()) {
                        event.getChannel().sendMessageFormat(lang.get("options.birthday_test.no_talk_permission"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    if(!event.getGuild().getSelfMember().canInteract(birthdayRole)) {
                        event.getChannel().sendMessageFormat(lang.get("options.birthday_test.cannot_interact"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    User user = m.getUser();
                    String message = String.format("%s**%s is a year older now! Wish them a happy birthday.** :tada: (test)", EmoteReference.POPPER, m.getEffectiveName());
                    if(dbGuild.getData().getBirthdayMessage() != null) {
                        message = dbGuild.getData().getBirthdayMessage().replace("$(user)", m.getEffectiveName())
                                          .replace("$(usermention)", m.getAsMention());
                    }
                    
                    //Value used in lambda... blabla :c
                    final String finalMessage = message;
                    
                    event.getGuild().addRoleToMember(m, birthdayRole).queue(success ->
                                                                                    new MessageBuilder(finalMessage).stripMentions(event.getJDA()).sendTo(birthdayChannel).queue(s ->
                                                                                                                                                                                         event.getChannel().sendMessageFormat(lang.get("options.birthday_test.success"),
                                                                                                                                                                                                 EmoteReference.CORRECT, birthdayChannel.getName(), user.getName(), birthdayRole.getName()
                                                                                                                                                                                         ).queue(), error ->
                                                                                                                                                                                                            event.getChannel().sendMessageFormat(lang.get("options.birthday_test.error"),
                                                                                                                                                                                                                    EmoteReference.CORRECT, birthdayChannel.getName(), user.getName(), birthdayRole.getName()
                                                                                                                                                                                                            ).queue())
                    );
                });
        
        registerOption("birthday:enable", "Birthday Monitoring enable",
                "Enables birthday monitoring. You need the channel **name** and the role name (it assigns that role on birthday)\n" +
                        "**Example:** `~>opts birthday enable general Birthday`, `~>opts birthday enable general \"Happy Birthday\"`",
                "Enables birthday monitoring.", (event, args, lang) -> {
                    if(args.length < 2) {
                        event.getChannel().sendMessageFormat(lang.get("options.birthday_enable.no_args"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    try {
                        String channel = args[0];
                        String role = args[1];
                        
                        TextChannel channelObj = Utils.findChannel(event, channel);
                        if(channelObj == null)
                            return;
                        
                        String channelId = channelObj.getId();
                        
                        Role roleObj = event.getGuild().getRolesByName(role.replace(channelId, ""), true).get(0);
                        
                        if(roleObj.isPublicRole()) {
                            event.getChannel().sendMessageFormat(lang.get("options.birthday_enable.public_role"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        if(guildData.getGuildAutoRole() != null && roleObj.getId().equals(guildData.getGuildAutoRole())) {
                            event.getChannel().sendMessageFormat(lang.get("options.birthday_enable.autorole"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        event.getChannel().sendMessageFormat(
                                String.join("\n", lang.get("options.birthday_enable.warning"),
                                        lang.get("options.birthday_enable.warning_1"),
                                        lang.get("options.birthday_enable.warning_2"),
                                        lang.get("options.birthday_enable.warning_3"),
                                        lang.get("options.birthday_enable.warning_4")), EmoteReference.WARNING, roleObj.getName()
                        ).queue();
                        InteractiveOperations.create(event.getChannel(), event.getAuthor().getIdLong(), 45, interactiveEvent -> {
                            String content = interactiveEvent.getMessage().getContentRaw();
                            if(content.equalsIgnoreCase("yes")) {
                                String roleId = roleObj.getId();
                                guildData.setBirthdayChannel(channelId);
                                guildData.setBirthdayRole(roleId);
                                dbGuild.saveAsync();
                                event.getChannel().sendMessageFormat(lang.get("options.birthday_enable.success"), EmoteReference.MEGA,
                                        channelObj.getName(), channelId, role, roleId
                                ).queue();
                                return Operation.COMPLETED;
                            } else if(content.equalsIgnoreCase("no")) {
                                interactiveEvent.getChannel().sendMessageFormat(lang.get("general.cancelled"), EmoteReference.CORRECT).queue();
                                return Operation.COMPLETED;
                            }
                            
                            return Operation.IGNORED;
                        });
                        
                    } catch(IndexOutOfBoundsException ex1) {
                        event.getChannel().sendMessageFormat(lang.get("options.birthday_enable.error_channel_1") + "\n" + lang.get("options.birthday_enable.error_channel_2"),
                                EmoteReference.ERROR
                        ).queue();
                    } catch(Exception ex) {
                        event.getChannel().sendMessage(lang.get("general.invalid_syntax") + "\nCheck https://github.com/Mantaro/MantaroBot/wiki/Configuration for more information.").queue();
                    }
                });
        
        registerOption("birthday:disable", "Birthday disable", "Disables birthday monitoring.", (event, lang) -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            guildData.setBirthdayChannel(null);
            guildData.setBirthdayRole(null);
            dbGuild.saveAsync();
            event.getChannel().sendMessageFormat(lang.get("options.birthday_disable.success"), EmoteReference.MEGA).queue();
        });
        //endregion
        
        //region prefix
        //region set
        registerOption("prefix:set", "Prefix set",
                "Sets the server prefix.\n" +
                        "**Example:** `~>opts prefix set .`",
                "Sets the server prefix.", (event, args, lang) -> {
                    if(args.length < 1) {
                        event.getChannel().sendMessageFormat(lang.get("options.prefix_set.no_prefix"), EmoteReference.ERROR).queue();
                        return;
                    }
                    String prefix = args[0];
                    
                    if(prefix.length() > 50) {
                        event.getChannel().sendMessageFormat(lang.get("options.prefix_set.too_long"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    if(prefix.isEmpty()) {
                        event.getChannel().sendMessageFormat(lang.get("options.prefix_set.empty_prefix"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setGuildCustomPrefix(prefix);
                    dbGuild.save();
                    
                    new MessageBuilder().append(String.format(lang.get("options.prefix_set.success"), EmoteReference.MEGA, prefix))
                            .stripMentions(event.getJDA())
                            .sendTo(event.getChannel()).queue();
                });//endregion
        
        //region clear
        registerOption("prefix:clear", "Prefix clear",
                "Clear the server prefix.\n" +
                        "**Example:** `~>opts prefix clear`", (event, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setGuildCustomPrefix(null);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.prefix_clear.success"), EmoteReference.MEGA).queue();
                });//endregion
        // endregion
        
        //region autorole
        //region set
        registerOption("autorole:set", "Autorole set",
                "Sets the server autorole. This means every user who joins will get this role. **You need to use the role name, if it contains spaces" +
                        " you need to wrap it in quotation marks**\n" +
                        "**Example:** `~>opts autorole set Member`, `~>opts autorole set \"Magic Role\"`",
                "Sets the server autorole.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.autorole_set.no_role"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    Consumer<Role> consumer = (role) -> {
                        if(!event.getMember().canInteract(role)) {
                            event.getChannel().sendMessageFormat(lang.get("options.autorole_set.hierarchy_conflict"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        if(!event.getGuild().getSelfMember().canInteract(role)) {
                            event.getChannel().sendMessageFormat(lang.get("options.autorole_set.self_hierarchy_conflict"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        guildData.setGuildAutoRole(role.getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessageFormat(lang.get("options.autorole_set.success"), EmoteReference.CORRECT,
                                role.getName(), role.getPosition()
                        ).queue();
                    };
                    
                    Role role = Utils.findRoleSelect(event, String.join(" ", args), consumer);
                    
                    if(role != null) {
                        consumer.accept(role);
                    }
                });//endregion
        
        //region unbind
        registerOption("autorole:unbind", "Autorole clear",
                "Clear the server autorole.\n" +
                        "**Example:** `~>opts autorole unbind`",
                "Resets the servers autorole.", (event, args, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setGuildAutoRole(null);
                    dbGuild.saveAsync();
                    event.getChannel().sendMessageFormat(lang.get("options.autorole_unbind.success"), EmoteReference.OK).queue();
                });//endregion
        //endregion
        
        //region usermessage
        //region resetchannel
        registerOption("usermessage:resetchannel", "Reset message channel",
                "Clears the join/leave message channel.\n" +
                        "**Example:** `~>opts usermessage resetchannel`",
                "Clears the join/leave message channel.", (event, args, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setLogJoinLeaveChannel(null);
                    guildData.setLogLeaveChannel(null);
                    guildData.setLogJoinChannel(null);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.usermessage_resetchannel.success"), EmoteReference.CORRECT).queue();
                });//endregion
        
        //region resetdata
        registerOption("usermessage:resetdata", "Reset join/leave message data",
                "Resets the join/leave message data.\n" +
                        "**Example:** `~>opts usermessage resetdata`",
                "Resets the join/leave message data.", (event, args, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setLeaveMessage(null);
                    guildData.setJoinMessage(null);
                    guildData.setLogJoinLeaveChannel(null);
                    
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.usermessage_resetdata.success"), EmoteReference.CORRECT).queue();
                });
        //endregion
        
        //region channel
        
        registerOption("usermessage:join:channel", "Sets the join message channel", "Sets the join channel, you need the channel **name**\n" +
                                                                                            "**Example:** `~>opts usermessage join channel join-magic`\n" +
                                                                                            "You can reset it by doing `~>opts usermessage join resetchannel`", "Sets the join message channel", (event, args, lang) -> {
            if(args.length == 0) {
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_join_channel.no_channel"), EmoteReference.ERROR).queue();
                return;
            }
            
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            String channelName = args[0];
            Consumer<TextChannel> consumer = tc -> {
                guildData.setLogJoinChannel(tc.getId());
                dbGuild.saveAsync();
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_join_channel.success"), EmoteReference.OK, tc.getAsMention()).queue();
            };
            
            TextChannel channel = Utils.findChannelSelect(event, channelName, consumer);
            
            if(channel != null) {
                consumer.accept(channel);
            }
        });
        addOptionAlias("usermessage:join:channel", "joinchannel");
        
        registerOption("usermessage:join:resetchannel", "Resets the join message channel", "Resets the join message channel", (event, lang) -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            guildData.setLogJoinChannel(null);
            dbGuild.saveAsync();
            event.getChannel().sendMessageFormat(lang.get("options.usermessage_join_resetchannel.success"), EmoteReference.CORRECT).queue();
        });
        addOptionAlias("usermessage:join:resetchannel", "resetjoinchannel");
        
        
        registerOption("usermessage:leave:channel", "Sets the leave message channel", "Sets the leave channel, you need the channel **name**\n" +
                                                                                              "**Example:** `~>opts usermessage leave channel leave-magic`\n" +
                                                                                              "You can reset it by doing `~>opts usermessage leave resetchannel`", "Sets the leave message channel", (event, args, lang) -> {
            if(args.length == 0) {
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_leave_channel.no_channel"), EmoteReference.ERROR).queue();
                return;
            }
            
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            String channelName = args[0];
            
            Consumer<TextChannel> consumer = tc -> {
                guildData.setLogLeaveChannel(tc.getId());
                dbGuild.saveAsync();
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_leave_channel.success"), EmoteReference.CORRECT, tc.getAsMention()).queue();
            };
            
            TextChannel channel = Utils.findChannelSelect(event, channelName, consumer);
            
            if(channel != null) {
                consumer.accept(channel);
            }
        });
        addOptionAlias("usermessage:leave:channel", "leavechannel");
        
        registerOption("usermessage:leave:resetchannel", "Resets the leave message channel", "Resets the leave message channel", (event, lang) -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            guildData.setLogLeaveChannel(null);
            dbGuild.saveAsync();
            event.getChannel().sendMessageFormat(lang.get("options.usermessage_leave_resetchannel.success"), EmoteReference.CORRECT).queue();
        });
        addOptionAlias("usermessage:join:resetchannel", "resetleavechannel");
        
        registerOption("usermessage:channel", "Set message channel",
                "Sets the join/leave message channel. You need the channel **name**\n" +
                        "**Example:** `~>opts usermessage channel join-magic`\n" +
                        "Warning: if you set this, you cannot set individual join/leave channels unless you reset the channel.",
                "Sets the join/leave message channel.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.usermessage_channel.no_channel"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    String channelName = args[0];
                    
                    Consumer<TextChannel> consumer = textChannel -> {
                        guildData.setLogJoinLeaveChannel(textChannel.getId());
                        dbGuild.save();
                        event.getChannel().sendMessageFormat(lang.get("options.usermessage_channel.success"), EmoteReference.OK, textChannel.getAsMention()).queue();
                    };
                    
                    TextChannel channel = Utils.findChannelSelect(event, channelName, consumer);
                    
                    if(channel != null) {
                        consumer.accept(channel);
                    }
                });//endregion
        
        //region joinmessage
        registerOption("usermessage:joinmessage", "User join message",
                "Sets the join message.\n" +
                        "**Example:** `~>opts usermessage joinmessage Welcome $(event.user.name) to the $(event.guild.name) server! Hope you have a great time`",
                "Sets the join message.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.usermessage_joinmessage.no_message"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    String joinMessage = String.join(" ", args);
                    guildData.setJoinMessage(joinMessage);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.usermessage_joinmessage.success"), EmoteReference.CORRECT, joinMessage).queue();
                });//endregion
        addOptionAlias("usermessage:joinmessage", "joinmessage");
        
        //region leavemessage
        registerOption("usermessage:leavemessage", "User leave message",
                "Sets the leave message.\n" +
                        "**Example:** `~>opts usermessage leavemessage Sad to see you depart, $(event.user.name)`",
                "Sets the leave message.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.usermessage_leavemessage.no_message"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    String leaveMessage = String.join(" ", args);
                    guildData.setLeaveMessage(leaveMessage);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.usermessage_leavemessage.success"), EmoteReference.CORRECT, leaveMessage).queue();
                });//endregion
        addOptionAlias("usermessage:leavemessage", "leavemessage");
        
        registerOption("usermessage:joinmessages:add", "Join Message extra messages add", "Adds a new join message\n" +
                                                                                                  "**Example**: `~>opts usermessage joinmessages add hi`", "Adds a new join message", ((event, args, lang) -> {
            if(args.length == 0) {
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_joinmessages_add.no_message"), EmoteReference.ERROR).queue();
                return;
            }
            
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            String message = String.join(" ", args);
            
            guildData.getExtraJoinMessages().add(message);
            dbGuild.save();
            
            event.getChannel().sendMessageFormat(lang.get("options.usermessage_joinmessage_add.success"), EmoteReference.CORRECT, message).queue();
        }));
        
        registerOption("usermessage:joinmessages:remove", "Join Message extra messages remove", "Removes a join message\n" +
                                                                                                        "**Example**: `~>opts usermessage joinmessages remove 0`", "Removes a join message", ((event, args, lang) -> {
            if(args.length == 0) {
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_joinmessages_remove.no_message"), EmoteReference.ERROR).queue();
                return;
            }
            
            try {
                DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                GuildData guildData = dbGuild.getData();
                int index;
                try {
                    index = Integer.parseInt(args[0]);
                } catch(NumberFormatException ex) {
                    event.getChannel().sendMessageFormat(lang.get("general.invalid_number"), EmoteReference.ERROR2).queue();
                    return;
                }
                
                String old = guildData.getExtraJoinMessages().get(index);
                
                guildData.getExtraJoinMessages().remove(index);
                dbGuild.save();
                
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_joinmessage_remove.success"), EmoteReference.CORRECT, old, index).queue();
            } catch(ArrayIndexOutOfBoundsException ex) {
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_joinmessage_remove.wrong_index"), EmoteReference.ERROR).queue();
            }
        }));
        
        registerOption("usermessage:joinmessages:clear", "Join Message extra messages clear", "Clears all extra join messages\n" +
                                                                                                      "**Example**: `~>opts usermessage joinmessages clear`", "Clears all extra join messages", ((event, args, lang) -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            dbGuild.getData().getExtraJoinMessages().clear();
            dbGuild.save();
            
            event.getChannel().sendMessageFormat(lang.get("options.usermessage_joinmessage_clear.success"), EmoteReference.CORRECT).queue();
        }));
        
        registerOption("usermessage:joinmessages:list", "Join Message extra messages list", "Lists all extra join messages\n" +
                                                                                                    "**Example**: `~>opts usermessage joinmessages list`", "Lists all extra join messages", ((event, args, lang) -> {
            StringBuilder builder = new StringBuilder();
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData data = dbGuild.getData();
            
            if(data.getExtraJoinMessages().isEmpty()) {
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_joinmessage_list.no_extras"), EmoteReference.ERROR).queue();
                return;
            }
            
            if(data.getJoinMessage() != null) {
                builder.append("M: ").append(data.getJoinMessage()).append("\n\n");
            }
            
            AtomicInteger index = new AtomicInteger();
            for(String s : data.getExtraJoinMessages()) {
                builder.append(index.getAndIncrement()).append(".- ").append(s).append("\n");
            }
            
            List<String> m = DiscordUtils.divideString(builder);
            List<String> messages = new LinkedList<>();
            boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);
            for(String s1 : m) {
                messages.add(String.format(lang.get("options.usermessage_joinmessage_list.header"),
                        hasReactionPerms ? lang.get("general.text_menu") + " " : lang.get("general.arrow_react"), String.format("```prolog\n%s```", s1)));
            }
            
            if(hasReactionPerms) {
                DiscordUtils.list(event, 45, false, messages);
            } else {
                DiscordUtils.listText(event, 45, false, messages);
            }
        }));
        
        registerOption("usermessage:leavemessages:add", "Leave Message extra messages add", "Adds a new leave message\n" +
                                                                                                    "**Example**: `~>opts usermessage leavemessages add hi`", "Adds a new leave message", ((event, args, lang) -> {
            if(args.length == 0) {
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_leavemessages_add.no_message"), EmoteReference.ERROR).queue();
                return;
            }
            
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            String message = String.join(" ", args);
            
            guildData.getExtraLeaveMessages().add(message);
            dbGuild.save();
            
            event.getChannel().sendMessageFormat(lang.get("options.usermessage_leavemessage_add.success"), EmoteReference.CORRECT, message).queue();
        }));
        
        registerOption("usermessage:leavemessages:remove", "Leave Message extra messages remove", "Removes a leave message\n" +
                                                                                                          "**Example**: `~>opts usermessage leavemessages remove 0`", "Removes a leave message", ((event, args, lang) -> {
            if(args.length == 0) {
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_leavemessages_remove.no_message"), EmoteReference.ERROR).queue();
                return;
            }
            
            try {
                DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                GuildData guildData = dbGuild.getData();
                int index;
                try {
                    index = Integer.parseInt(args[0]);
                } catch(NumberFormatException ex) {
                    event.getChannel().sendMessageFormat(lang.get("general.invalid_number"), EmoteReference.ERROR2).queue();
                    return;
                }
                
                String old = guildData.getExtraLeaveMessages().get(index);
                
                guildData.getExtraLeaveMessages().remove(index);
                dbGuild.save();
                
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_leavemessage_remove.success"), EmoteReference.CORRECT, old, index).queue();
            } catch(ArrayIndexOutOfBoundsException ae) {
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_leavemessage_remove.wrong_index"), EmoteReference.ERROR).queue();
            }
        }));
        
        registerOption("usermessage:leavemessages:clear", "Leave Message extra messages clear", "Clears all extra leave messages\n" +
                                                                                                        "**Example**: `~>opts usermessage leavemessages clear`", "Clears all extra leave messages", ((event, args, lang) -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            dbGuild.getData().getExtraLeaveMessages().clear();
            dbGuild.save();
            
            event.getChannel().sendMessageFormat(lang.get("options.usermessage_leavemessage_clear.success"), EmoteReference.CORRECT).queue();
            
        }));
        
        registerOption("usermessage:leavemessages:list", "Leave Message extra messages list", "Lists all extra leave messages\n" +
                                                                                                      "**Example**: `~>opts usermessage leavemessages list`", "Lists all extra leave messages", ((event, args, lang) -> {
            StringBuilder builder = new StringBuilder();
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData data = dbGuild.getData();
            
            if(data.getExtraLeaveMessages().isEmpty()) {
                event.getChannel().sendMessageFormat(lang.get("options.usermessage_leavemessage_list.no_extras"), EmoteReference.ERROR).queue();
                return;
            }
            
            if(data.getLeaveMessage() != null) {
                builder.append("M: ").append(data.getJoinMessage()).append("\n\n");
            }
            
            AtomicInteger index = new AtomicInteger();
            for(String s : data.getExtraLeaveMessages()) {
                builder.append(index.getAndIncrement()).append(".- ").append(s).append("\n");
            }
            
            List<String> m = DiscordUtils.divideString(builder);
            List<String> messages = new LinkedList<>();
            boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);
            for(String s1 : m) {
                messages.add(String.format(lang.get("options.usermessage_leavemessage_list.header"),
                        hasReactionPerms ? lang.get("general.text_menu") + " " : lang.get("general.arrow_react"), String.format("```prolog\n%s```", s1)));
            }
            
            if(hasReactionPerms) {
                DiscordUtils.list(event, 45, false, messages);
            } else {
                DiscordUtils.listText(event, 45, false, messages);
            }
        }));
        //endregion
        //region autoroles
        //region add
        registerOption("autoroles:add", "Autoroles add",
                "Adds a role to the `~>iam` list.\n" +
                        "You need the name of the iam and the name of the role. If the role contains spaces wrap it in quotation marks.\n" +
                        "**Example:** `~>opts autoroles add member Member`, `~>opts autoroles add wew \"A role with spaces on its name\"`",
                "Adds an auto-assignable role to the iam lists.", (event, args, lang) -> {
                    if(args.length < 2) {
                        event.getChannel().sendMessageFormat(lang.get("options.autoroles_add.no_args"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    String roleName = args[1];
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    List<Role> roleList = event.getGuild().getRolesByName(roleName, true);
                    if(roleList.size() == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.autoroles_add.no_role_found"), EmoteReference.ERROR).queue();
                    } else if(roleList.size() == 1) {
                        Role role = roleList.get(0);
                        
                        if(!event.getMember().canInteract(role)) {
                            event.getChannel().sendMessageFormat(lang.get("options.autoroles_add.hierarchy_conflict"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        if(!event.getGuild().getSelfMember().canInteract(role)) {
                            event.getChannel().sendMessageFormat(lang.get("options.autoroles_add.self_hierarchy_conflict"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        guildData.getAutoroles().put(args[0], role.getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessageFormat(lang.get("options.autoroles_add.success"), EmoteReference.OK, args[0], role.getName()).queue();
                    } else {
                        DiscordUtils.selectList(event, roleList, role -> String.format("%s (ID: %s)  | Position: %s", role.getName(),
                                role.getId(), role.getPosition()), s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Role:")
                                                                                        .setDescription(s).build(),
                                role -> {
                                    if(!event.getMember().canInteract(role)) {
                                        event.getChannel().sendMessageFormat(lang.get("options.autoroles_add.hierarchy_conflict"), EmoteReference.ERROR).queue();
                                        return;
                                    }
                                    
                                    if(!event.getGuild().getSelfMember().canInteract(role)) {
                                        event.getChannel().sendMessageFormat(lang.get("options.autoroles_add.self_hierarchy_conflict"), EmoteReference.ERROR).queue();
                                        return;
                                    }
                                    
                                    guildData.getAutoroles().put(args[0], role.getId());
                                    dbGuild.saveAsync();
                                    event.getChannel().sendMessageFormat(lang.get("options.autoroles_add.success"), EmoteReference.OK, args[0], role.getName()).queue();
                                });
                    }
                });
        
        //region remove
        registerOption("autoroles:remove", "Autoroles remove",
                "Removes a role from the `~>iam` list.\n" +
                        "You need the name of the iam.\n" +
                        "**Example:** `~>opts autoroles remove iamname`",
                "Removes an auto-assignable role from iam.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.autoroles_add.no_args"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    HashMap<String, String> autoroles = guildData.getAutoroles();
                    if(autoroles.containsKey(args[0])) {
                        autoroles.remove(args[0]);
                        dbGuild.saveAsync();
                        event.getChannel().sendMessageFormat(lang.get("options.autoroles_remove.success"), EmoteReference.OK, args[0]).queue();
                    } else {
                        event.getChannel().sendMessageFormat(lang.get("options.autoroles_remove.not_found"), EmoteReference.ERROR).queue();
                    }
                });//endregion
        
        //region clear
        registerOption("autoroles:clear", "Autoroles clear",
                "Removes all autoroles.",
                "Removes all autoroles.", (event, args, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    dbGuild.getData().getAutoroles().clear();
                    dbGuild.saveAsync();
                    event.getChannel().sendMessageFormat(lang.get("options.autoroles_clear.success"), EmoteReference.CORRECT).queue();
                }
        ); //endregion
        
        registerOption("autoroles:category:add", "Adds a category to autoroles",
                "Adds a category to autoroles. Useful for organizing",
                "Adds a category to autoroles.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.autoroles_category_add.no_args"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    String category = args[0];
                    String autorole = null;
                    if(args.length > 1) {
                        autorole = args[1];
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    Map<String, List<String>> categories = guildData.getAutoroleCategories();
                    if(categories.containsKey(category) && autorole == null) {
                        event.getChannel().sendMessageFormat(lang.get("options.autoroles_category_add.already_exists"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    categories.computeIfAbsent(category, (a) -> new ArrayList<>());
                    
                    if(autorole != null) {
                        if(guildData.getAutoroles().containsKey(autorole)) {
                            categories.get(category).add(autorole);
                            dbGuild.save();
                            event.getChannel().sendMessageFormat(lang.get("options.autoroles_category_add.success"), EmoteReference.CORRECT, category, autorole).queue();
                        } else {
                            event.getChannel().sendMessageFormat(lang.get("options.autoroles_category_add.no_role"), EmoteReference.ERROR, autorole).queue();
                        }
                        return;
                    }
                    
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.autoroles_category_add.success_new"), EmoteReference.CORRECT, category).queue();
                });
        
        registerOption("autoroles:category:remove", "Removes a category from autoroles",
                "Removes a category from autoroles. Useful for organizing",
                "Removes a category from autoroles.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.autoroles_category_remove.no_args"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    String category = args[0];
                    String autorole = null;
                    if(args.length > 1) {
                        autorole = args[1];
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    Map<String, List<String>> categories = guildData.getAutoroleCategories();
                    if(!categories.containsKey(category)) {
                        event.getChannel().sendMessageFormat(lang.get("options.autoroles_category_add.no_category"), EmoteReference.ERROR, category).queue();
                        return;
                    }
                    
                    if(autorole != null) {
                        categories.get(category).remove(autorole);
                        dbGuild.save();
                        event.getChannel().sendMessageFormat(lang.get("options.autoroles_category_remove.success"), EmoteReference.CORRECT, category, autorole).queue();
                        return;
                    }
                    
                    categories.remove(category);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.autoroles_category_remove.success_new"), EmoteReference.CORRECT, category).queue();
                });
        
        //region custom
        registerOption("admincustom", "Admin custom commands",
                "Locks custom commands to admin-only.\n" +
                        "Example: `~>opts admincustom true`",
                "Locks custom commands to admin-only.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.admincustom.no_args"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    String action = args[0];
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    try {
                        guildData.setCustomAdminLockNew(Boolean.parseBoolean(action));
                        dbGuild.save();
                        String toSend = String.format("%s%s", EmoteReference.CORRECT, Boolean.parseBoolean(action) ? lang.get("options.admincustom.admin_only") : lang.get("options.admincustom.everyone"));
                        event.getChannel().sendMessage(toSend).queue();
                    } catch(Exception ex) {
                        event.getChannel().sendMessageFormat(lang.get("options.admincustom.not_bool"), EmoteReference.ERROR).queue();
                    }
                });
        //endregion
        
        registerOption("timedisplay:set", "Time display set", "Toggles between 12h and 24h time display.\n" +
                                                                      "Example: `~>opts timedisplay 24h`", "Toggles between 12h and 24h time display.", (event, args, lang) -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            
            if(args.length == 0) {
                event.getChannel().sendMessageFormat(lang.get("options.timedisplay_set.no_mode_specified"), EmoteReference.ERROR).queue();
                return;
            }
            
            String mode = args[0];
            
            switch(mode) {
                case "12h":
                    event.getChannel().sendMessageFormat(lang.get("options.timedisplay_set.12h"), EmoteReference.CORRECT).queue();
                    guildData.setTimeDisplay(1);
                    dbGuild.save();
                    break;
                case "24h":
                    event.getChannel().sendMessageFormat(lang.get("options.timedisplay_set.24h"), EmoteReference.CORRECT).queue();
                    guildData.setTimeDisplay(0);
                    dbGuild.save();
                    break;
                default:
                    event.getChannel().sendMessageFormat(lang.get("options.timedisplay_set.invalid"), EmoteReference.ERROR).queue();
                    break;
            }
        });
        
        registerOption("server:role:disallow", "Role disallow", "Disallows all users with a role from executing commands.\n" +
                                                                        "You need to provide the name of the role to disallow from mantaro.\n" +
                                                                        "Example: `~>opts server role disallow bad`, `~>opts server role disallow \"No commands\"`",
                "Disallows all users with a role from executing commands.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.server_role_disallow.no_name"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    String roleName = String.join(" ", args);
                    
                    Consumer<Role> consumer = (role) -> {
                        guildData.getDisabledRoles().add(role.getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessageFormat(lang.get("options.server_role_disallow.success"), EmoteReference.CORRECT, role.getName()).queue();
                    };
                    
                    Role role = Utils.findRoleSelect(event, roleName, consumer);
                    
                    if(role != null && role.isPublicRole()) {
                        event.getChannel().sendMessageFormat(lang.get("options.server_role_disallow.public_role"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    if(role != null) {
                        consumer.accept(role);
                    }
                });
        
        registerOption("server:role:allow", "Role allow", "Allows all users with a role from executing commands.\n" +
                                                                  "You need to provide the name of the role to allow from mantaro. Has to be already disabled.\n" +
                                                                  "Example: `~>opts server role allow bad`, `~>opts server role allow \"No commands\"`",
                "Allows all users with a role from executing commands (Has to be already disabled)", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.server_role_allow.no_name"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    String roleName = String.join(" ", args);
                    
                    Consumer<Role> consumer = (role) -> {
                        if(!guildData.getDisabledRoles().contains(role.getId())) {
                            event.getChannel().sendMessageFormat(lang.get("options.server_role_allow.not_disabled"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        guildData.getDisabledRoles().remove(role.getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessageFormat(lang.get("options.server_role_allow.success"), EmoteReference.CORRECT, role.getName()).queue();
                    };
                    
                    Role role = Utils.findRoleSelect(event, roleName, consumer);
                    
                    if(role != null) {
                        consumer.accept(role);
                    }
                });
        
        registerOption("server:ignorebots:autoroles:toggle",
                "Bot autorole ignore", "Toggles between ignoring bots on autorole assign and not.", (event, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    boolean ignore = guildData.isIgnoreBotsAutoRole();
                    guildData.setIgnoreBotsAutoRole(!ignore);
                    dbGuild.saveAsync();
                    
                    event.getChannel().sendMessageFormat(lang.get("options.server_ignorebots_autoroles_toggle.success"), EmoteReference.CORRECT, guildData.isIgnoreBotsAutoRole()).queue();
                });
        
        registerOption("server:ignorebots:joinleave:toggle",
                "Bot join/leave ignore", "Toggles between ignoring bots on join/leave message.", (event, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    boolean ignore = guildData.isIgnoreBotsWelcomeMessage();
                    guildData.setIgnoreBotsWelcomeMessage(!ignore);
                    dbGuild.saveAsync();
                    
                    event.getChannel().sendMessageFormat(lang.get("options.server_ignorebots_joinleave_toggle.success"), EmoteReference.CORRECT, guildData.isIgnoreBotsWelcomeMessage()).queue();
                });
        
        registerOption("levelupmessages:toggle", "Level-up toggle",
                "Toggles level up messages, remember that after this you have to set thee channel and the message!", (event, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    boolean ignore = guildData.isEnabledLevelUpMessages();
                    guildData.setEnabledLevelUpMessages(!ignore);
                    dbGuild.saveAsync();
                    
                    event.getChannel().sendMessageFormat(lang.get("options.levelupmessages_toggle.success"), EmoteReference.CORRECT, guildData.isEnabledLevelUpMessages()).queue();
                });
        
        registerOption("levelupmessages:message:set", "Level-up message", "Sets the message to display on level up",
                "Sets the level up message", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.levelupmessages_message_set.no_message"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    String levelUpMessage = String.join(" ", args);
                    guildData.setLevelUpMessage(levelUpMessage);
                    dbGuild.saveAsync();
                    event.getChannel().sendMessageFormat(lang.get("options.levelupmessages_message_set.success"), EmoteReference.CORRECT, levelUpMessage).queue();
                });
        
        registerOption("levelupmessages:message:clear", "Level-up message clear", "Clears the message to display on level up",
                "Clears the message to display on level up", (event, args, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    guildData.setLevelUpMessage(null);
                    dbGuild.saveAsync();
                    
                    event.getChannel().sendMessageFormat(lang.get("options.levelupmessages_message_clear.success"), EmoteReference.CORRECT).queue();
                });
        
        registerOption("levelupmessages:channel:set", "Level-up message channel",
                "Sets the channel to display level up messages", "Sets the channel to display level up messages",
                (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.levelupmessages_channel_set.no_channel"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    String channelName = args[0];
                    
                    Consumer<TextChannel> consumer = textChannel -> {
                        guildData.setLevelUpChannel(textChannel.getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessageFormat(lang.get("options.levelupmessages_channel_set.success"), EmoteReference.OK, textChannel.getAsMention()).queue();
                    };
                    
                    TextChannel channel = Utils.findChannelSelect(event, channelName, consumer);
                    
                    if(channel != null) {
                        consumer.accept(channel);
                    }
                });
        
        registerOption("birthday:message:set", "Birthday message", "Sets the message to display on a new birthday",
                "Sets the birthday message", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.birthday_message_set.no_message"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    String birthdayMessage = String.join(" ", args);
                    guildData.setBirthdayMessage(birthdayMessage);
                    dbGuild.saveAsync();
                    event.getChannel().sendMessageFormat(lang.get("options.birthday_message_set.success"), EmoteReference.CORRECT, birthdayMessage).queue();
                });
        
        registerOption("birthday:message:clear", "Birthday message clear", "Clears the message to display on a new birthday",
                "Clears the message to display on birthday", (event, args, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    guildData.setBirthdayMessage(null);
                    dbGuild.saveAsync();
                    
                    event.getChannel().sendMessageFormat(lang.get("options.birthday_message_clear.success"), EmoteReference.CORRECT).queue();
                });
        
        //region joinmessage
        registerOption("modlog:blacklistwords:add", "Modlog Word Blacklist add",
                "Adds a word to the modlog word blacklist (won't add any messages with that word). Can contain spaces.\n" +
                        "**Example:** `~>opts modlog blacklistwords add mood`",
                "Sets the join message.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.modlog_blacklistwords_add.no_word"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    if(guildData.getModLogBlacklistWords().size() > 20) {
                        event.getChannel().sendMessageFormat(lang.get("options.modlog_blacklistwords_add.too_many"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    String word = String.join(" ", args);
                    guildData.getModLogBlacklistWords().add(word);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.modlog_blacklistwords_add.success"), EmoteReference.CORRECT, word).queue();
                });//endregion
        
        //region joinmessage
        registerOption("modlog:blacklistwords:remove", "Modlog Word Blacklist remove",
                "Removes a word from the modlog word blacklist. Can contain spaces\n" +
                        "**Example:** `~>opts modlog blacklistwords remove mood`",
                "Sets the join message.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.modlog_blacklistwords_add.no_word"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    String word = String.join(" ", args);
                    
                    if(!guildData.getModLogBlacklistWords().contains(word)) {
                        event.getChannel().sendMessageFormat(lang.get("options.modlog_blacklistwords_remove.not_in"), EmoteReference.ERROR, word).queue();
                        return;
                    }
                    
                    guildData.getModLogBlacklistWords().remove(word);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.modlog_blacklistwords_remove.success"), EmoteReference.CORRECT, word).queue();
                });//endregion
        
        //region editmessage
        registerOption("logs:editmessage", "Edit log message",
                "Sets the edit message.\n" +
                        "**Example:** `~>opts logs editmessage [$(hour)] Message (ID: $(event.message.id)) created by **$(event.user.tag)** in channel **$(event.channel.name)** was modified.\n```diff\n-$(old)\n+$(new)````",
                "Sets the edit message.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.logs_editmessage.no_message"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    String editMessage = String.join(" ", args);
                    guildData.setEditMessageLog(editMessage);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.logs_editmessage.success"), EmoteReference.CORRECT, editMessage).queue();
                });//endregion
        addOptionAlias("logs:editmessage", "editmessage");
        
        //region deletemessage
        registerOption("logs:deletemessage", "Delete log message",
                "Sets the delete message.\n" +
                        "**Example:** `~>opts logs deletemessage [$(hour)] Message (ID: $(event.message.id)) created by **$(event.user.tag)** (ID: $(event.user.id)) in channel **$(event.channel.name)** was deleted.```diff\n-$(content)``` `",
                "Sets the delete message.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.logs_deletemessage.no_message"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    String deleteMessage = String.join(" ", args);
                    guildData.setDeleteMessageLog(deleteMessage);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.logs_deletemessage.success"), EmoteReference.CORRECT, deleteMessage).queue();
                });//endregion
        addOptionAlias("logs:deletemessage", "deletemessage");
        
        //region banmessage
        registerOption("logs:banmessage", "Ban log message",
                "Sets the ban message.\n" +
                        "**Example:** `~>opts logs banmessage [$(hour)] $(event.user.tag) just got banned.`",
                "Sets the ban message.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.logs_banmessage.no_message"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    String banMessage = String.join(" ", args);
                    guildData.setBannedMemberLog(banMessage);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.logs_banmessage.success"), EmoteReference.CORRECT, banMessage).queue();
                });//endregion
        addOptionAlias("logs:banmessage", "banmessage");
        
        //region ubbanmessage
        registerOption("logs:unbanmessage", "Unban log message",
                "Sets the unban message.\n" +
                        "**Example:** `~>opts logs unbanmessage [$(hour)] $(event.user.tag) just got unbanned.`",
                "Sets the unban message.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.logs_unbanmessage.no_message"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    String unbanMessage = String.join(" ", args);
                    guildData.setUnbannedMemberLog(unbanMessage);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.logs_unbanmessage.success"), EmoteReference.CORRECT, unbanMessage).queue();
                });//endregion
        addOptionAlias("logs:unbanmessage", "unbanmessage");
        
        
        //TODO:
        //bannedMemberLog;
        //unbannedMemberLog;
        
        registerOption("commands:showdisablewarning", "Show disable warning", "Toggles on/off the disabled command warning.",
                "Toggles on/off the disabled command warning.", (event, args, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    guildData.setCommandWarningDisplay(!guildData.isCommandWarningDisplay()); //lombok names are amusing
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.showdisablewarning.success"), EmoteReference.CORRECT, guildData.isCommandWarningDisplay()).queue();
                });
    }
    
    @Override
    public String description() {
        return "Guild Configuration";
    }
}
