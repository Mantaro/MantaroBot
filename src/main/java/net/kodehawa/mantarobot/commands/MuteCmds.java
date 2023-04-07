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

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Defer;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Module
public class MuteCmds {
    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Mute.class);
        cr.registerSlash(UnMute.class);
    }

    @Description("Times out a user.")
    @Defer
    @Category(CommandCategory.MODERATION)
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to mute.", required = true),
            @Options.Option(type = OptionType.STRING, name = "time", description = "The amount of time to mute the user for."),
            @Options.Option(type = OptionType.STRING, name = "reason", description = "The reason of the mute.")
    })
    @Help(description = "Mutes the specified user.", usage = "`/mute user:<user> time:<time> reason:[reason]`", parameters = {
            @Help.Parameter(name = "user", description = "The user to mute."),
            @Help.Parameter(
                    name = "time", description = """
                    The amount of time to mute the user for. For example 1m20s.
                    The format is, for example, 1h20m10s for 1 hour, 20 minutes and 10 seconds.
                    If not specified, this uses the default mute timeout set for this server, if set.
                    """
            ),
            @Help.Parameter(name = "reason", description = "The reason of the mute. This will show in the logs, if enabled.", optional = true)
    })
    // This does the same as the built-in /timeout, though?
    public static class Mute extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var dbGuild = ctx.getDBGuild();
            var reason = ctx.getOptionAsString("reason", "");
            var placeholderReason = "Not specified.";
            var user = ctx.getOptionAsUser("user");
            if (user == null) {
                ctx.reply("general.slash_member_lookup_failure", EmoteReference.ERROR);
                return;
            }

            var time = dbGuild.getSetModTimeout() > 0 ? dbGuild.getSetModTimeout() : 0L;

            var maybeTime = ctx.getOptionAsString("time");
            if (maybeTime != null) {
                try {
                    time = Math.abs(Utils.parseTime(maybeTime));
                } catch (Exception e) {
                    ctx.reply("commands.mute.time_too_long", EmoteReference.ERROR);
                    return;
                }
            }

            if (!ctx.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
                ctx.reply("commands.mute.no_permissions", EmoteReference.ERROR);
                return;
            }

            if (!ctx.getSelfMember().hasPermission(Permission.MODERATE_MEMBERS)) {
                ctx.reply("commands.mute.no_moderate_members", EmoteReference.ERROR);
                return;
            }

            if (time == 0) {
                ctx.reply("commands.mute.time_not_specified_generic", EmoteReference.ERROR);
                return;
            }

            if (time > TimeUnit.DAYS.toMillis(28)) {
                ctx.reply("commands.mute.time_too_long", EmoteReference.ERROR);
                return;
            }

            var member = ctx.getGuild().getMember(user);
            // Just in case I guess...
            if (member == null) {
                ctx.reply("commands.mute.no_users", EmoteReference.ERROR);
                return;
            }

            if (!ctx.getSelfMember().canInteract(member)) {
                ctx.reply("commands.mute.self_hierarchy_error", EmoteReference.ERROR);
                return;
            }

            if (!ctx.getMember().canInteract(member)) {
                ctx.reply("commands.mute.user_hierarchy_error", EmoteReference.ERROR);
                return;
            }

            if (member.hasPermission(Permission.ADMINISTRATOR)) {
                ctx.reply("commands.mute.user_hierarchy_error", EmoteReference.ERROR);
                return;
            }

            if (member.isTimedOut()) {
                ctx.reply("commands.mute.already_muted", EmoteReference.WARNING);
                return;
            }

            var logReason = reason.isEmpty() ? placeholderReason : reason;
            member.timeoutFor(Duration.ofMillis(time))
                    .reason(String.format(
                            "Muted by %#s for %s: %s", ctx.getAuthor(), Utils.formatDuration(ctx.getLanguageContext(), time), logReason)
                    ).queue();

            if (reason.isEmpty()) {
                ctx.reply("commands.mute.success", EmoteReference.CORRECT, member.getEffectiveName(), Utils.formatDuration(ctx.getLanguageContext(), time));
            } else {
                ctx.reply("commands.mute.success_reason", EmoteReference.CORRECT,
                        member.getEffectiveName(), Utils.formatDuration(ctx.getLanguageContext(), time), reason
                );
            }

            dbGuild.setCases(dbGuild.getCases() + 1);
            dbGuild.save();
            ModLog.log(
                    ctx.getMember(), user, logReason, ctx.getChannel().getName(), ModLog.ModAction.MUTE, dbGuild.getCases()
            );
        }
    }

    @Description("Removes the timeout from a user.")
    @Defer
    @Category(CommandCategory.MODERATION)
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to remove the timeout from.", required = true),
            @Options.Option(type = OptionType.STRING, name = "reason", description = "The reason for it.")
    })
    @Help(description = "Removes the timeout from a user.", usage = "`/unmute user:<user> reason:[reason]`", parameters = {
            @Help.Parameter(name = "user", description = "The user to unmute."),
            @Help.Parameter(name = "reason", description = "The reason of the unmute. This will show in the logs, if enabled.", optional = true)
    })
    public static class UnMute extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var dbGuild = ctx.getDBGuild();
            var reason = ctx.getOptionAsString("reason", "");
            var user = ctx.getOptionAsUser("user");
            if (user == null) {
                ctx.reply("general.slash_member_lookup_failure", EmoteReference.ERROR);
                return;
            }

            var placeholderReason = "Not specified";
            var logReason = reason.isEmpty() ? placeholderReason : reason;

            if (!ctx.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
                ctx.reply("commands.unmute.no_permissions", EmoteReference.ERROR);
                return;
            }

            if (!ctx.getSelfMember().hasPermission(Permission.MODERATE_MEMBERS)) {
                ctx.reply("commands.mute.no_moderate_members", EmoteReference.ERROR);
                return;
            }

            var member = ctx.getGuild().getMember(user);
            if (member == null) {
                ctx.reply("commands.unmute.no_mentions", EmoteReference.ERROR);
                return;
            }

            if (!ctx.getSelfMember().canInteract(member)) {
                ctx.reply("commands.mute.self_hierarchy_error", EmoteReference.ERROR);
                return;
            }

            if (!ctx.getMember().canInteract(member)) {
                ctx.reply("commands.mute.user_hierarchy_error", EmoteReference.ERROR);
                return;
            }

            if (member.isTimedOut()) {
                member.removeTimeout()
                        .reason(String.format("Unmuted by %#s: %s", ctx.getAuthor(), logReason))
                        .queue();

                ctx.reply("commands.unmute.success", EmoteReference.CORRECT, user.getName());

                ModLog.log(ctx.getMember(), user, logReason, "none", ModLog.ModAction.UNMUTE, dbGuild.getCases());
                dbGuild.setCases(dbGuild.getCases() + 1);
                dbGuild.save();
            } else {
                ctx.reply("commands.unmute.not_muted", EmoteReference.ERROR);
            }
        }
    }
}
