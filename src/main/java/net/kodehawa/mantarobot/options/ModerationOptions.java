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

package net.kodehawa.mantarobot.options;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.FinderUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Option
public class ModerationOptions extends OptionHandler {
    private static final Pattern offsetRegex = Pattern.compile("(?:UTC|GMT)[+-][0-9]{1,2}(:[0-9]{1,2})?", Pattern.CASE_INSENSITIVE);
    private static final Pattern timePattern = Pattern.compile("[(\\d+)((?:h(?:our(?:s)?)?)|(?:m(?:in(?:ute(?:s)?)?)?)|(?:s(?:ec(?:ond(?:s)?)?)?))]+");

    public ModerationOptions() {
        setType(OptionType.MODERATION);
    }

    @Subscribe
    public void onRegistry(OptionRegistryEvent e) {
        registerOption("localblacklist:add", "Local Blacklist add", """
                        Adds someone to the local blacklist.
                        You need to mention the user. You can mention multiple users.
                        **Example:** `~>opts localblacklist add @user1 @user2`""",
                "Adds someone to the local blacklist.", (ctx, args) -> {
            List<Member> mentioned = ctx.getMentionedMembers();

            if (mentioned.isEmpty()) {
                ctx.sendLocalized("options.localblacklist_add.invalid", EmoteReference.ERROR);
                return;
            }

            if (mentioned.contains(ctx.getMember())) {
                ctx.sendLocalized("options.localblacklist_add.yourself_notice", EmoteReference.ERROR);
                return;
            }

            if (mentioned.stream().anyMatch(u -> u.getUser().isBot())) {
                ctx.sendLocalized("options.localblacklist_add.bot_notice", EmoteReference.ERROR);
                return;
            }

            if (mentioned.stream().anyMatch(CommandPermission.ADMIN::test)) {
                ctx.sendLocalized("options.localblacklist_add.admin_notice", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            List<String> toBlacklist = mentioned.stream().map(ISnowflake::getId).toList();

            String blacklisted = mentioned.stream()
                    .map(Member::getUser)
                    .map(User::getAsTag)
                    .collect(Collectors.joining(","));

            guildData.getDisabledUsers().addAll(toBlacklist);
            dbGuild.save();

            ctx.sendLocalized("options.localblacklist_add.success", EmoteReference.CORRECT, blacklisted);
        });

        registerOption("localblacklist:remove", "Local Blacklist remove", """
                        Removes someone from the local blacklist.
                        You need to mention the user. You can mention multiple users.
                        **Example:** `~>opts localblacklist remove @user1 @user2`""",
                "Removes someone from the local blacklist.", (ctx, args) -> {
            List<Member> mentioned = ctx.getMentionedMembers();

            if (mentioned.isEmpty()) {
                ctx.sendLocalized("options.localblacklist_remove.invalid", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            List<String> toUnBlacklist = mentioned.stream().map(ISnowflake::getId).toList();
            String unBlacklisted = mentioned.stream()
                    .map(Member::getUser)
                    .map(User::getAsTag)
                    .collect(Collectors.joining(",")
            );

            guildData.getDisabledUsers().removeAll(toUnBlacklist);
            dbGuild.save();

            ctx.sendLocalized("options.localblacklist_remove.success", EmoteReference.CORRECT, unBlacklisted);
        });

        registerOption("logs:enable", "Enable logs",
                "Enables logs. You need to use the channel name.\n" +
                        "**Example:** `~>opts logs enable mod-logs`",
                "Enables logs.", (ctx, args) -> {
            if (args.length < 1) {
                ctx.sendLocalized("options.logs_enable.no_channel", EmoteReference.ERROR);
                return;
            }

            String logChannel = args[0];
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            Consumer<StandardGuildMessageChannel> consumer = textChannel -> {
                guildData.setGuildLogChannel(textChannel.getId());
                dbGuild.saveAsync();
                ctx.sendLocalized("options.logs_enable.success", EmoteReference.MEGA, textChannel.getName(), textChannel.getId());
            };

            var channel = FinderUtils.findChannelSelect(ctx, logChannel, consumer);

            if (channel != null) {
                consumer.accept(channel);
            }
        });

        registerOption("logs:exclude", "Exclude log channel.", """
                Excludes a channel from logging. You need to use the channel name, *not* the mention.
                **Example:** `~>opts logs exclude staff`
                The `opts logs exclude clearchannels` command clears all of the log exclusions, and `opts logs exclude remove <channel>` removes a single channel from the exclusion list.
                """, "Excludes a channel from logging.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.logs_exclude.no_args", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (args[0].equals("clearchannels")) {
                guildData.getLogExcludedChannels().clear();
                dbGuild.saveAsync();
                ctx.sendLocalized("options.logs_exclude.clearchannels.success", EmoteReference.OK);
                return;
            }

            if (args[0].equals("remove")) {
                if (args.length < 2) {
                    ctx.sendLocalized("options.log_exclude.invalid", EmoteReference.ERROR);
                    return;
                }
                String channel = args[1];

                Consumer<StandardGuildMessageChannel> consumer = textChannel -> {
                    guildData.getLogExcludedChannels().remove(textChannel.getId());
                    dbGuild.saveAsync();
                    ctx.sendLocalized("options.logs_exclude.remove.success", EmoteReference.OK, textChannel.getAsMention());
                };

                var ch = FinderUtils.findChannelSelect(ctx, channel, consumer);

                if (ch != null) {
                    consumer.accept(ch);
                }
                return;
            }

            String channel = args[0];
            Consumer<StandardGuildMessageChannel> consumer = textChannel -> {
                guildData.getLogExcludedChannels().add(textChannel.getId());
                dbGuild.saveAsync();
                ctx.sendLocalized("options.logs_exclude.success", EmoteReference.OK, textChannel.getAsMention());
            };

            var ch = FinderUtils.findChannelSelect(ctx, channel, consumer);

            if (ch != null) {
                consumer.accept(ch);
            }
        });

        registerOption("logs:timezone", "Sets the log timeozne", """
                Sets the log timezone.
                For example, `~>opts logs timezone America/Chicago`
                """, "Sets the logs timezone", (ctx, args) -> {
            if (args.length < 1) {
                ctx.sendLocalized("options.logs_timezone.not_specified", EmoteReference.ERROR);
                return;
            }

            var timezone = args[0];
            if (offsetRegex.matcher(timezone).matches()) {
                timezone = timezone.toUpperCase().replace("UTC", "GMT");
            }

            if (!Utils.isValidTimeZone(timezone)) {
                ctx.sendLocalized("options.logs_timezone.invalid", EmoteReference.ERROR);
                return;
            }

            var dbGuild = ctx.getDBGuild();
            dbGuild.getData().setLogTimezone(timezone);
            dbGuild.saveUpdating();

            ctx.sendLocalized("options.logs_timezone.success", EmoteReference.CORRECT, timezone);
        });

        registerOption("logs:timezonereset", "Resets the log timezone", "Resets the log timezone", (ctx) -> {
            var dbGuild = ctx.getDBGuild();
            dbGuild.getData().setLogTimezone(null);
            dbGuild.saveUpdating();

            ctx.sendLocalized("options.logs_timezonereset.success", EmoteReference.CORRECT);
        });

        registerOption("logs:disable", "Disable logs",
                "Disables logs.\n**Example:** `~>opts logs disable`", "Disables logs.", (ctx) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setGuildLogChannel(null);
            dbGuild.saveAsync();
            ctx.sendLocalized("options.logs_disable.success", EmoteReference.MEGA);
        });

        registerOption("defaultmutetimeout:set", "Default mute timeout", """
                Sets the default mute timeout for ~>mute.
                This command will set the timeout of ~>mute to a fixed value **unless you specify another time in the command**
                **Example:** `~>opts defaultmutetimeout set 1m20s`
                **Considerations:** Time is in 1m20s or 1h10m3s format, for example.""", "Sets the default mute timeout", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.defaultmutetimeout_set.not_specified", EmoteReference.ERROR);
                return;
            }

            if (!timePattern.matcher(args[0]).matches()) {
                ctx.sendLocalized("options.defaultmutetimeout_set.wrong_format", EmoteReference.ERROR);
                return;
            }

            var timeoutToSet = Utils.parseTime(args[0]);
            var time = System.currentTimeMillis() + timeoutToSet;
            if (time > System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10)) {
                ctx.sendLocalized("options.defaultmutetimeout_set.too_long", EmoteReference.ERROR);
                return;
            }

            if (time < 0) {
                ctx.sendLocalized("options.defaultmutetimeout_set.negative_notice");
                return;
            }

            if (time < 10000) {
                ctx.sendLocalized("commands.defaultmutetimeout_set.too_short", EmoteReference.ERROR);
                return;
            }

            var dbGuild = ctx.getDBGuild();
            var guildData = dbGuild.getData();

            guildData.setSetModTimeout(timeoutToSet);
            dbGuild.save();

            ctx.sendLocalized("options.defaultmutetimeout_set.success", EmoteReference.CORRECT, args[0], timeoutToSet);
        });

        registerOption("defaultmutetimeout:reset", "Default mute timeout reset",
            "Resets the default mute timeout which was set previously with `defaultmusictimeout set`", "Resets the default mute timeout.", ctx -> {
                var dbGuild = ctx.getDBGuild();
                var guildData = dbGuild.getData();

                guildData.setSetModTimeout(0L);
                dbGuild.save();

                ctx.sendLocalized("options.defaultmutetimeout_reset.success", EmoteReference.CORRECT);
        });
    }

    @Override
    public String description() {
        return null;
    }
}
