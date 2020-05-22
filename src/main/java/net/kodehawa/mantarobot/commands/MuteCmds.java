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

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.MantaroObj;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.core.Option;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.utils.Pair;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Module
@SuppressWarnings("unused")
public class MuteCmds {
    private static final Pattern timePattern = Pattern.compile("[(\\d+)((?:h(?:our(?:s)?)?)|(?:m(?:in(?:ute(?:s)?)?)?)|(?:s(?:ec(?:ond(?:s)?)?)?))]+");
    private static final Pattern muteTimePattern = Pattern.compile("-time [(\\d+)((?:h(?:our(?:s)?)?)|(?:m(?:in(?:ute(?:s)?)?)?)|(?:s(?:ec(?:ond(?:s)?)?)?))]+");

    @Subscribe
    public void mute(CommandRegistry registry) {
        Command mute = registry.register("mute", new SimpleCommand(Category.MODERATION) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (args.length == 0) {
                    ctx.sendLocalized("commands.mute.no_users", EmoteReference.ERROR);
                    return;
                }

                String affected = args[0];

                if (!(ctx.getMember().hasPermission(Permission.KICK_MEMBERS) || ctx.getMember().hasPermission(Permission.BAN_MEMBERS))) {
                    ctx.sendLocalized("commands.mute.no_permissions", EmoteReference.ERROR);
                    return;
                }

                ManagedDatabase db = ctx.db();
                DBGuild dbGuild = ctx.getDBGuild();
                GuildData guildData = dbGuild.getData();
                String reason = "Not specified";
                Map<String, String> opts = StringUtils.parse(args);

                if (guildData.getMutedRole() == null) {
                    ctx.sendLocalized("commands.mute.no_mute_role", EmoteReference.ERROR);
                    return;
                }

                Role mutedRole = ctx.getGuild().getRoleById(guildData.getMutedRole());
                if (mutedRole == null) {
                    ctx.sendLocalized("commands.mute.null_mute_role", EmoteReference.ERROR);
                    return;
                }

                if (args.length > 1) {
                    reason = StringUtils.splitArgs(content, 2)[1];
                }

                if (!ctx.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
                    ctx.sendLocalized("commands.mute.no_manage_roles", EmoteReference.ERROR);
                    return;
                }

                reason = Utils.mentionPattern.matcher(reason).replaceAll("");
                //Regex from: Fabricio20
                final String finalReason = muteTimePattern.matcher(reason).replaceAll("");

                MantaroObj data = db.getMantaroData();
                Member member = Utils.findMember(ctx.getEvent(), ctx.getMember(), affected);
                if (member == null)
                    return;

                User user = member.getUser();
                long time = guildData.getSetModTimeout() > 0 ? System.currentTimeMillis() + guildData.getSetModTimeout() : 0L;

                if (opts.containsKey("time")) {
                    if (opts.get("time") == null || opts.get("time").isEmpty()) {
                        ctx.sendLocalized("commands.mute.time_not_specified", EmoteReference.WARNING);
                        return;
                    }

                    time = System.currentTimeMillis() + Utils.parseTime(opts.get("time"));

                    if (time > System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10)) {
                        ctx.sendLocalized("commands.mute.time_too_long", EmoteReference.ERROR);
                        return;
                    }

                    if (time < 0) {
                        ctx.sendLocalized("commands.mute.negative_time_notice", EmoteReference.ERROR);
                        return;
                    }

                    data.getMutes().put(user.getIdLong(), Pair.of(ctx.getGuild().getId(), time));
                    data.save();
                    dbGuild.save();
                } else {
                    if (time > 0) {
                        if (time > System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10)) {
                            ctx.sendLocalized("commands.mute.default_time_too_long", EmoteReference.ERROR);
                            return;
                        }

                        data.getMutes().put(user.getIdLong(), Pair.of(ctx.getGuild().getId(), time));
                        data.save();
                        dbGuild.save();
                    } else {
                        ctx.sendLocalized("commands.mute.no_time", EmoteReference.ERROR);
                        return;
                    }
                }

                if (member.getRoles().contains(mutedRole)) {
                    ctx.sendLocalized("commands.mute.already_muted", EmoteReference.WARNING);
                    return;
                }

                if (!ctx.getSelfMember().canInteract(member)) {
                    ctx.sendLocalized("commands.mute.self_hierarchy_error", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getMember().canInteract(member)) {
                    ctx.sendLocalized("commands.mute.user_hierarchy_error", EmoteReference.ERROR);
                    return;
                }

                ctx.getGuild().addRoleToMember(member, mutedRole)
                        .reason(String.format("Muted by %#s for %s: %s", ctx.getAuthor(), Utils.formatDuration(time - System.currentTimeMillis()), finalReason))
                        .queue();

                ctx.sendLocalized("commands.mute.success", EmoteReference.CORRECT, member.getEffectiveName(), Utils.formatDuration(time - System.currentTimeMillis()));

                dbGuild.getData().setCases(dbGuild.getData().getCases() + 1);
                dbGuild.saveAsync();
                ModLog.log(ctx.getMember(), user, finalReason, ctx.getChannel().getName(), ModLog.ModAction.MUTE, dbGuild.getData().getCases());
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Mutes the specified users.")
                        .setUsage("`~>mute <@user> [reason] [-time <time>]`")
                        .addParameter("@user", "The users to mute. Needs to be mentions (pings)")
                        .addParameter("reason", "The mute reason. This is optional.")
                        .addParameter("-time", "The time to mute an user for. For example `~>mute @Natan#1289 wew, nice -time 1m20s` will mute Natan for 1 minute and 20 seconds.")
                        .build();
            }
        });

        mute.addOption("defaultmutetimeout:set", new Option("Default mute timeout",
                "Sets the default mute timeout for ~>mute.\n" +
                        "This command will set the timeout of ~>mute to a fixed value **unless you specify another time in the command**\n" +
                        "**Example:** `~>opts defaultmutetimeout set 1m20s`\n" +
                        "**Considerations:** Time is in 1m20s or 1h10m3s format, for example.", OptionType.GUILD)
                .setActionLang((event, args, lang) -> {
                    if (args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.defaultmutetimeout_set.not_specified"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if (!timePattern.matcher(args[0]).matches()) {
                        event.getChannel().sendMessageFormat(lang.get("options.defaultmutetimeout_set.wrong_format"), EmoteReference.ERROR).queue();
                        return;
                    }

                    long timeoutToSet = Utils.parseTime(args[0]);

                    long time = System.currentTimeMillis() + timeoutToSet;

                    if (time > System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10)) {
                        event.getChannel().sendMessageFormat(lang.get("options.defaultmutetimeout_set.too_long"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if (time < 0) {
                        event.getChannel().sendMessage(lang.get("options.defaultmutetimeout_set.negative_notice")).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setSetModTimeout(timeoutToSet);
                    dbGuild.save();

                    event.getChannel().sendMessageFormat(lang.get("options.defaultmutetimeout_set.success"), EmoteReference.CORRECT, args[0], timeoutToSet).queue();
                }).setShortDescription("Sets the default timeout for the ~>mute command"));


        mute.addOption("defaultmutetimeout:reset", new Option("Default mute timeout reset",
                "Resets the default mute timeout which was set previously with `defaultmusictimeout set`", OptionType.GUILD)
                .setActionLang((event, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    guildData.setSetModTimeout(0L);
                    dbGuild.save();

                    event.getChannel().sendMessageFormat(lang.get("options.defaultmutetimeout_reset.success"), EmoteReference.CORRECT).queue();
                }).setShortDescription("Resets the default mute timeout."));

        mute.addOption("muterole:set", new Option("Mute role set",
                "Sets this guilds mute role to apply on the ~>mute command.\n" +
                        "To use this command you need to specify a role name. *In case the name contains spaces, the name should" +
                        " be wrapped in quotation marks", OptionType.COMMAND)
                .setActionLang((event, args, lang) -> {
                    if (args.length < 1) {
                        event.getChannel().sendMessageFormat(lang.get("options.muterole_set.no_role"), EmoteReference.ERROR).queue();
                        return;
                    }

                    String roleName = String.join(" ", args);
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    Consumer<Role> consumer = (role) -> {
                        guildData.setMutedRole(role.getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessageFormat(lang.get("options.muterole_set.success"), EmoteReference.OK, roleName).queue();
                    };

                    Role role = Utils.findRoleSelect(event, roleName, consumer);

                    if (role != null) {
                        consumer.accept(role);
                    }
                }).setShortDescription("Sets this guilds mute role to apply on the ~>mute command"));

        mute.addOption("muterole:unbind", new Option("Mute Role unbind", "Resets the current value set for the mute role", OptionType.GENERAL)
                .setActionLang((event, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setMutedRole(null);
                    dbGuild.saveAsync();
                    event.getChannel().sendMessageFormat(lang.get("options.muterole_unbind.success"), EmoteReference.OK).queue();
                }).setShortDescription("Resets the current value set for the mute role."));
    }

    @Subscribe
    public void unmute(CommandRegistry commandRegistry) {
        commandRegistry.register("unmute", new SimpleCommand(Category.MODERATION) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!ctx.getMember().hasPermission(Permission.KICK_MEMBERS) || !ctx.getMember().hasPermission(Permission.KICK_MEMBERS)) {
                    ctx.sendLocalized("commands.unmute.no_permissions", EmoteReference.ERROR);
                    return;
                }

                ManagedDatabase db = MantaroData.db();
                DBGuild dbGuild = ctx.getDBGuild();
                GuildData guildData = dbGuild.getData();
                String reason = "Not specified";

                if (guildData.getMutedRole() == null) {
                    ctx.sendLocalized("commands.mute.no_mute_role", EmoteReference.ERROR);
                    return;
                }

                Role mutedRole = ctx.getGuild().getRoleById(guildData.getMutedRole());

                if (mutedRole == null) {
                    ctx.sendLocalized("commands.mute.null_mute_role", EmoteReference.ERROR);
                    return;
                }

                if (args.length > 1) {
                    reason = StringUtils.splitArgs(content, 2)[1];
                }

                List<Member> mentionedMembers = ctx.getMessage().getMentionedMembers();
                if (mentionedMembers.isEmpty()) {
                    ctx.sendLocalized("commands.unmute.no_mentions", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
                    ctx.sendLocalized("commands.mute.no_manage_roles", EmoteReference.ERROR);
                    return;
                }

                final String finalReason = Utils.mentionPattern.matcher(reason).replaceAll("");

                mentionedMembers.forEach(member -> {
                    User user = member.getUser();

                    guildData.getMutedTimelyUsers().remove(user.getIdLong());
                    if (!ctx.getSelfMember().canInteract(member)) {
                        ctx.sendLocalized("commands.mute.self_hierarchy_error", EmoteReference.ERROR);
                        return;
                    }

                    if (!ctx.getMember().canInteract(member)) {
                        ctx.sendLocalized("commands.mute.user_hierarchy_error", EmoteReference.ERROR);
                        return;
                    }

                    if (member.getRoles().contains(mutedRole)) {
                        ctx.getGuild().removeRoleFromMember(member, mutedRole)
                                .reason(String.format("Unmuted by %#s: %s", ctx.getAuthor(), finalReason))
                                .queue();

                        ctx.sendLocalized("commands.unmute.success", EmoteReference.CORRECT, user.getName());

                        dbGuild.getData().setCases(dbGuild.getData().getCases() + 1);
                        dbGuild.saveAsync();
                        ModLog.log(ctx.getMember(), user, finalReason, "none", ModLog.ModAction.UNMUTE, dbGuild.getData().getCases());
                    } else {
                        ctx.sendLocalized("commands.unmute.no_role_assigned", EmoteReference.ERROR);
                    }
                });
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Un-mutes the specified users.")
                        .setUsage("`~>unmute <@user> [reason]`")
                        .addParameter("@user", "The users to un-mute. Needs to be mentions (pings)")
                        .addParameter("reason", "The reason for the un-mute. This is optional.")
                        .build();
            }
        });
    }
}
