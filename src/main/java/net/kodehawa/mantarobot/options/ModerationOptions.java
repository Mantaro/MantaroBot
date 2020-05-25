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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Option
public class ModerationOptions extends OptionHandler {
    public ModerationOptions() {
        setType(OptionType.MODERATION);
    }

    @Subscribe
    public void onRegistry(OptionRegistryEvent e) {
        registerOption("localblacklist:add", "Local Blacklist add",
                "Adds someone to the local blacklist.\n" +
                        "You need to mention the user. You can mention multiple users.\n" +
                        "**Example:** `~>opts localblacklist add @user1 @user2`",
                "Adds someone to the local blacklist.", (event, args, lang) -> {

                    List<User> mentioned = event.getMessage().getMentionedUsers();

                    if (mentioned.isEmpty()) {
                        event.getChannel().sendMessageFormat(lang.get("options.localblacklist_add.invalid"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if (mentioned.contains(event.getAuthor())) {
                        event.getChannel().sendMessageFormat(lang.get("options.localblacklist_add.yourself_notice"), EmoteReference.ERROR).queue();
                        return;
                    }

                    Guild guild = event.getGuild();
                    if (mentioned.stream().anyMatch(u -> CommandPermission.ADMIN.test(guild.getMember(u)))) {
                        event.getChannel().sendMessageFormat(lang.get("options.localblacklist_add.admin_notice"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(guild);
                    GuildData guildData = dbGuild.getData();
                    List<String> toBlackList = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());

                    String blacklisted = mentioned.stream().map(user -> user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining(","));

                    guildData.getDisabledUsers().addAll(toBlackList);
                    dbGuild.save();

                    event.getChannel().sendMessageFormat(lang.get("options.localblacklist_add.success"), EmoteReference.CORRECT, blacklisted).queue();
                });

        registerOption("localblacklist:remove", "Local Blacklist remove",
                "Removes someone from the local blacklist.\n" +
                        "You need to mention the user. You can mention multiple users.\n" +
                        "**Example:** `~>opts localblacklist remove @user1 @user2`",
                "Removes someone from the local blacklist.", (event, args, lang) -> {
                    List<User> mentioned = event.getMessage().getMentionedUsers();

                    if (mentioned.isEmpty()) {
                        event.getChannel().sendMessageFormat(lang.get("options.localblacklist_remove.invalid"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    List<String> toUnBlackList = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());
                    String unBlackListed = mentioned.stream().map(user -> user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining(","));

                    guildData.getDisabledUsers().removeAll(toUnBlackList);
                    dbGuild.save();

                    event.getChannel().sendMessageFormat(lang.get("options.localblacklist_remove.success"), EmoteReference.CORRECT, unBlackListed).queue();
                });

        registerOption("logs:enable", "Enable logs",
                "Enables logs. You need to use the channel name.\n" +
                        "**Example:** `~>opts logs enable mod-logs`",
                "Enables logs.", (event, args, lang) -> {
                    if (args.length < 1) {
                        event.getChannel().sendMessageFormat(lang.get("options.logs_enable.no_channel"), EmoteReference.ERROR).queue();
                        return;
                    }

                    String logChannel = args[0];
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    Consumer<TextChannel> consumer = textChannel -> {
                        guildData.setGuildLogChannel(textChannel.getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessage(String.format(lang.get("options.logs_enable.success"),
                                EmoteReference.MEGA, textChannel.getName(), textChannel.getId())
                        ).queue();
                    };

                    TextChannel channel = Utils.findChannelSelect(event, logChannel, consumer);

                    if (channel != null) {
                        consumer.accept(channel);
                    }
                });

        registerOption("logs:exclude", "Exclude log channel.",
                "Excludes a channel from logging. You need to use the channel name, *not* the mention.\n" +
                        "**Example:** `~>opts logs exclude staff`. " +
                        "The `opts logs exclude clearchannels` clears all of the log exclusions, and `opts logs exclude remove <channel>` removes a single channel from the exclusion list.",
                "Excludes a channel from logging.", (event, args, lang) -> {
                    if (args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.logs_exclude.no_args"), EmoteReference.ERROR).queue();
                        return;
                    }
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    if (args[0].equals("clearchannels")) {
                        guildData.getLogExcludedChannels().clear();
                        dbGuild.saveAsync();
                        event.getChannel().sendMessageFormat(lang.get("options.logs_exclude.clearchannels.success"), EmoteReference.OK).queue();
                        return;
                    }

                    if (args[0].equals("remove")) {
                        if (args.length < 2) {
                            event.getChannel().sendMessageFormat(lang.get("options.log_exclude.invalid"), EmoteReference.ERROR).queue();
                            return;
                        }
                        String channel = args[1];

                        Consumer<TextChannel> consumer = textChannel -> {
                            guildData.getLogExcludedChannels().remove(textChannel.getId());
                            dbGuild.saveAsync();
                            event.getChannel().sendMessageFormat(lang.get("options.logs_exclude.remove.success"),
                                    EmoteReference.OK, textChannel.getAsMention()
                            ).queue();
                        };

                        TextChannel ch = Utils.findChannelSelect(event, channel, consumer);

                        if (ch != null) {
                            consumer.accept(ch);
                        }
                        return;
                    }

                    String channel = args[0];
                    Consumer<TextChannel> consumer = textChannel -> {
                        guildData.getLogExcludedChannels().add(textChannel.getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessageFormat(lang.get("options.logs_exclude.success"), EmoteReference.OK, textChannel.getAsMention()).queue();
                    };

                    TextChannel ch = Utils.findChannelSelect(event, channel, consumer);

                    if (ch != null) {
                        consumer.accept(ch);
                    }
                });


        registerOptionShort("logs:disable", "Disable logs",
                "Disables logs.\n" +
                        "**Example:** `~>opts logs disable`",
                "Disables logs.", (GuildMessageReceivedEvent event, I18nContext lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setGuildLogChannel(null);
                    dbGuild.saveAsync();
                    event.getChannel().sendMessageFormat(lang.get("options.logs_disable.success"), EmoteReference.MEGA).queue();
                });
    }

    @Override
    public String description() {
        return null;
    }
}
