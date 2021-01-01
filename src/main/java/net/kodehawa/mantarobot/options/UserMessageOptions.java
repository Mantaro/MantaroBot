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
import net.dv8tion.jda.api.entities.TextChannel;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.FinderUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Option
public class UserMessageOptions extends OptionHandler {
    public UserMessageOptions() {
        setType(OptionType.GUILD);
    }

    @Subscribe
    public void onRegistry(OptionRegistryEvent event) {
        registerOption("usermessage:resetchannel", "Reset message channel", """
                Clears the join/leave message channel.
                **Example:** `~>opts usermessage resetchannel`
                """, "Clears the join/leave message channel.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setLogJoinLeaveChannel(null);
            guildData.setLogLeaveChannel(null);
            guildData.setLogJoinChannel(null);
            dbGuild.save();
            ctx.sendLocalized("options.usermessage_resetchannel.success", EmoteReference.CORRECT);
        });
        registerOption("usermessage:resetdata", "Reset join/leave message data", """
                Resets the join/leave message data.
                Example:** `~>opts usermessage resetdata`
                """, "Resets the join/leave message data.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setLeaveMessage(null);
            guildData.setJoinMessage(null);
            guildData.setLogJoinLeaveChannel(null);

            dbGuild.save();
            ctx.sendLocalized("options.usermessage_resetdata.success", EmoteReference.CORRECT);
        });

        registerOption("usermessage:join:channel", "Sets the join message channel", """
                Sets the join channel, you need the channel **name**.
                **Example:** `~>opts usermessage join channel join-magic`
                You can reset it by doing `~>opts usermessage join resetchannel`
                """, "Sets the join message channel", (ctx, args) -> {
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


        registerOption("usermessage:leave:channel", "Sets the leave message channel", """
                Sets the leave channel, you need the channel **name**.
                You can reset it by doing `~>opts usermessage leave resetchannel` 
                """, "Sets the leave message channel", (ctx, args) -> {
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

        registerOption("usermessage:channel", "Set message channel", """
                Sets the join/leave message channel. You need the channel **name**.
                **Example:** `~>opts usermessage channel join-magic`
                Warning: if you set this, you cannot set individual join/leave channels unless you reset the channel.
                """, "Sets the join/leave message channel.", (ctx, args) -> {
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
        });

        registerOption("usermessage:joinmessage", "User join message", """
                Sets the join message.
                **Example:** `~>opts usermessage joinmessage Welcome $(event.user.name) to the $(event.guild.name) server! Hope you have a great time`
                """, "Sets the join message.", (ctx, args) -> {
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
        });
        addOptionAlias("usermessage:joinmessage", "joinmessage");

        registerOption("usermessage:resetjoinmessage", "Reset join message",
                "Resets the join message", "Resets the join message.", (ctx, args) -> {
                    DBGuild dbGuild = ctx.getDBGuild();
                    GuildData guildData = dbGuild.getData();

                    guildData.setJoinMessage(null);
                    dbGuild.save();
                    ctx.sendLocalized("options.usermessage_joinmessage_reset.success", EmoteReference.CORRECT);
                });
        addOptionAlias("usermessage:resetjoinmessage", "resetjoinmessage");

        registerOption("usermessage:leavemessage", "User leave message", """
                Sets the leave message.
                **Example:** `~>opts leavemessage Sad to see you depart, $(event.user.name)`
                """, "Sets the leave message.", (ctx, args) -> {
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
        });
        addOptionAlias("usermessage:leavemessage", "leavemessage");

        registerOption("usermessage:resetleavemessage", "Reset leave message",
                "Resets the leave message","Resets the leave message.", (ctx, args) -> {
                    DBGuild dbGuild = ctx.getDBGuild();
                    GuildData guildData = dbGuild.getData();

                    guildData.setLeaveMessage(null);
                    dbGuild.save();
                    ctx.sendLocalized("options.usermessage_leavemessage_reset.success", EmoteReference.CORRECT);
                });
        addOptionAlias("usermessage:resetleavemessage", "resetleavemessage");

        registerOption("usermessage:joinmessages:add", "Join Message extra messages add", """
                Adds a new join message
                **Example**: `~>opts usermessage joinmessages add hi`
                """, "Adds a new join message", ((ctx, args) -> {
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
        addOptionAlias("usermessage:joinmessages:add", "joinmessages:add");

        registerOption("usermessage:joinmessages:remove", "Join Message extra messages remove", """
                Removes a join message.
                **Example**: `~>opts usermessage joinmessages remove 1`
                """, "Removes a join message", ((ctx, args) -> {
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
        addOptionAlias("usermessage:joinmessages:remove", "joinmessages:remove");

        registerOption("usermessage:joinmessages:clear", "Join Message extra messages clear", """
                Clears all extra join messages.
                **Example**: `~>opts usermessage joinmessages clear`"
                """, "Clears all extra join messages", ((ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            dbGuild.getData().getExtraJoinMessages().clear();
            dbGuild.save();

            ctx.sendLocalized("options.usermessage_joinmessage_clear.success", EmoteReference.CORRECT);
        }));
        addOptionAlias("usermessage:joinmessages:clear", "joinmessages:clear");

        registerOption("usermessage:joinmessages:list", "Join Message extra messages list", """
                Lists all extra join messages
                **Example**: `~>opts usermessage joinmessages list`", 
                """, "Lists all extra join messages", ((ctx, args) -> {
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

        registerOption("usermessage:leavemessages:add", "Leave Message extra messages add", """
                Adds a new leave message
                **Example**: `~>opts usermessage leavemessages add hi`
                """, "Adds a new leave message", ((ctx, args) -> {
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

        registerOption("usermessage:leavemessages:remove", "Leave Message extra messages remove", """
                Removes a leave message.
                **Example**: `~>opts usermessage leavemessages remove 0`
                """, "Removes a leave message", ((ctx, args) -> {
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

        registerOption("usermessage:leavemessages:clear", "Leave Message extra messages clear", """
                Clears all extra leave messages
                **Example**: `~>opts usermessage leavemessages clear`
                """, "Clears all extra leave messages", ((ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            dbGuild.getData().getExtraLeaveMessages().clear();
            dbGuild.save();

            ctx.sendLocalized("options.usermessage_leavemessage_clear.success", EmoteReference.CORRECT);
        }));

        registerOption("usermessage:leavemessages:list", "Leave Message extra messages list", """
                Lists all extra leave messages
                **Example**: `~>opts usermessage leavemessages list`
                """, "Lists all extra leave messages", ((ctx, args) -> {
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
    }

    @Override
    public String description() {
        return "Guild User Message (Join/Leave) Configuration";
    }
}
