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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Module
@SuppressWarnings("unused")
public class ModerationCmds {
    private static final Logger log = LoggerFactory.getLogger(ModerationCmds.class);

    @Subscribe
    public void softban(CommandRegistry cr) {
        cr.register("softban", new SimpleCommand(Category.MODERATION) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                Guild guild = ctx.getGuild();
                User author = ctx.getAuthor();

                Message receivedMessage = ctx.getMessage();
                String reason = content;

                if (ctx.getMentionedUsers().isEmpty()) {
                    ctx.sendLocalized("commands.softban.no_users", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getMember().hasPermission(Permission.BAN_MEMBERS)) {
                    ctx.sendLocalized("commands.softban.no_permission", EmoteReference.ERROR);
                    return;
                }

                Member selfMember = ctx.getSelfMember();

                if (!selfMember.hasPermission(Permission.BAN_MEMBERS)) {
                    ctx.sendLocalized("commands.softban.no_permission_self", EmoteReference.ERROR2);
                    return;
                }

                if (args.length > 1) {
                    reason = StringUtils.splitArgs(content, 2)[1];
                }

                if (reason.isEmpty()) {
                    reason = "Reason not specified";
                }

                final String finalReason = String.format("Softbanned by %#s: %s", author, reason);

                Member member = ctx.getMentionedMembers().get(0);
                User user = member.getUser();

                if (!ctx.getMember().canInteract(member)) {
                    ctx.sendLocalized("commands.softban.hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                if (author.getId().equals(user.getId())) {
                    ctx.sendLocalized("commands.softban.yourself_note", EmoteReference.ERROR);
                    return;
                }

                //If one of them is in a higher hierarchy than the bot, cannot ban.
                if (!selfMember.canInteract(member)) {
                    ctx.sendLocalized("commands.softban.self_hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                final DBGuild db = ctx.getDBGuild();
                var languageContext = ctx.getLanguageContext();

                guild.ban(member, 7).reason(finalReason).queue(
                        success -> {
                            user.openPrivateChannel()
                                    .flatMap(privateChannel ->
                                            privateChannel.sendMessage(String.format("%sYou were **softbanned** by %s#%s for reason %s on server **%s**.",
                                            EmoteReference.MEGA, author.getName(), author.getDiscriminator(), finalReason, ctx.getGuild().getName()))
                                    ).queue();

                            db.getData().setCases(db.getData().getCases() + 1);
                            db.saveAsync();

                            ctx.sendLocalized("commands.softban.success", EmoteReference.ZAP, languageContext.get("general.mod_quotes"), user.getName());
                            guild.unban(user).reason(finalReason).queue(aVoid -> {
                            }, error -> {
                                if (error instanceof PermissionException) {
                                    ctx.sendLocalized("commands.softban.error", EmoteReference.ERROR,
                                            user.getName(), ((PermissionException) error).getPermission()
                                    );
                                } else {
                                    ctx.sendLocalized("commands.softban.unknown_error", EmoteReference.ERROR,
                                            user.getName(), error.getClass().getSimpleName(), error.getMessage()
                                    );

                                    log.warn("Unexpected error while softbanning someone.", error);
                                }
                            });

                            ModLog.log(ctx.getMember(), user, finalReason, ctx.getChannel().getName(), ModLog.ModAction.KICK, db.getData().getCases());
                            TextChannelGround.of(ctx.getEvent()).dropItemWithChance(2, 2);
                        },
                        error -> {
                            if (error instanceof PermissionException) {
                                ctx.sendLocalized("commands.softban.error", EmoteReference.ERROR,
                                        user.getName(), ((PermissionException) error).getPermission()
                                );
                            } else {
                                ctx.sendLocalized("commands.softban.unknown_error", EmoteReference.ERROR,
                                        user.getName(), error.getClass().getSimpleName(), error.getMessage()
                                );

                                log.warn("Unexpected error while softbanning someone.", error);
                            }
                        });

            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Softban the mentioned user and clears their messages from the past week. (You need the Ban Members permission, so does the bot)♥n" +
                                "A soft ban is a ban & instant unban, usually useful to kick and clear messages.")
                        .setUsage("`~>softban <@user> [reason]`")
                        .addParameter("@user", "The user to softban.")
                        .addParameter("reason", "The reason of the softban. This is optional.")
                        .build();
            }
        });
    }

    @Subscribe
    public void ban(CommandRegistry cr) {
        cr.register("ban", new SimpleCommand(Category.MODERATION) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                Guild guild = ctx.getGuild();
                User author = ctx.getAuthor();

                Message receivedMessage = ctx.getMessage();
                String reason = content;
                List<Member> mentionedMembers = ctx.getMentionedMembers();

                if (mentionedMembers.isEmpty()) {
                    ctx.sendLocalized("commands.ban.no_users", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getMember().hasPermission(Permission.BAN_MEMBERS)) {
                    ctx.sendLocalized("commands.ban.no_permission", EmoteReference.ERROR);
                    return;
                }

                Member selfMember = ctx.getSelfMember();

                if (!selfMember.hasPermission(Permission.BAN_MEMBERS)) {
                    ctx.sendLocalized("commands.ban.no_permission_self", EmoteReference.ERROR);
                    return;
                }

                if (args.length > 1) {
                    reason = StringUtils.splitArgs(content, 2)[1];
                }

                if (reason.isEmpty()) {
                    reason = "Reason not specified";
                }

                final String finalReason = String.format("Banned by %#s: %s", author, reason);
                final var languageContext = ctx.getLanguageContext();

                for (Member member : mentionedMembers) {
                    User user = member.getUser();

                    if (!ctx.getMember().canInteract(member)) {
                        ctx.sendLocalized("commands.ban.hierarchy_conflict", EmoteReference.ERROR, EmoteReference.SMILE);
                        return;
                    }

                    if (author.getId().equals(user.getId())) {
                        ctx.sendLocalized("commands.ban.yourself_note", EmoteReference.ERROR);
                        return;
                    }

                    if (!guild.getSelfMember().canInteract(member)) {
                        ctx.sendStrippedLocalized("commands.ban.self_hierarchy_conflict", EmoteReference.ERROR, user.getName());
                        return;
                    }

                    final DBGuild db = MantaroData.db().getGuild(ctx.getGuild());

                    guild.ban(member, 7).reason(finalReason).queue(
                            success -> user.openPrivateChannel().queue(privateChannel -> {
                                if (!user.isBot()) {
                                    privateChannel.sendMessage(String.format("%sYou were **banned** by %s#%s on server **%s**. Reason: %s.",
                                            EmoteReference.MEGA, author.getName(), author.getDiscriminator(), ctx.getGuild().getName(), finalReason)).queue();
                                }

                                db.getData().setCases(db.getData().getCases() + 1);
                                db.saveAsync();

                                if (mentionedMembers.size() == 1)
                                    ctx.sendLocalized("commands.ban.success", EmoteReference.ZAP, languageContext.get("general.mod_quotes"), user.getName());

                                ModLog.log(ctx.getMember(), user, finalReason, ctx.getChannel().getName(), ModLog.ModAction.BAN, db.getData().getCases());
                                TextChannelGround.of(ctx.getEvent()).dropItemWithChance(1, 2);
                            }),
                            error ->
                            {
                                if (error instanceof PermissionException) {
                                    ctx.sendLocalized("commands.ban.error", EmoteReference.ERROR, user.getName(), ((PermissionException) error).getPermission());
                                } else {
                                    ctx.sendLocalized("commands.ban.unknown_error", EmoteReference.ERROR, user.getName());
                                    log.warn("Encountered an unexpected error while trying to ban someone.", error);
                                }
                            });
                }

                if (mentionedMembers.size() > 1) {
                    ctx.sendLocalized("commands.ban.success_multiple", EmoteReference.ZAP, languageContext.get("general.mod_quotes"), mentionedMembers.size());
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Bans the mentioned user.")
                        .setUsage("`~>ban <@user> [reason]`")
                        .addParameter("@user", "The user to ban.")
                        .addParameter("reason", "The reason of the ban. This is optional.")
                        .build();
            }
        });
    }

    @Subscribe
    public void kick(CommandRegistry cr) {
        cr.register("kick", new SimpleCommand(Category.MODERATION) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                Guild guild = ctx.getGuild();
                User author = ctx.getAuthor();

                Message receivedMessage = ctx.getMessage();
                String reason = content;

                if (args.length == 0) {
                    ctx.sendLocalized("commands.kick.no_users", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getMember().hasPermission(Permission.KICK_MEMBERS)) {
                    ctx.sendLocalized("commands.kick.no_permission", EmoteReference.ERROR2);
                    return;
                }

                Member selfMember = guild.getSelfMember();

                if (!selfMember.hasPermission(Permission.KICK_MEMBERS)) {
                    ctx.sendLocalized("commands.kick.no_permission_self", EmoteReference.ERROR2);
                    return;
                }

                if (args.length > 1) {
                    reason = StringUtils.splitArgs(content, 2)[1];
                }

                if (reason.isEmpty()) {
                    reason = "Reason not specified";
                }

                final String finalReason = String.format("Kicked by %#s: %s", ctx.getAuthor(), reason);
                Member member = Utils.findMember(ctx.getEvent(), ctx.getMember(), args[0]);
                if (member == null)
                    return;

                User user = member.getUser();

                if (!ctx.getMember().canInteract(member)) {
                    ctx.sendLocalized("commands.kick.hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                if (ctx.getAuthor().getId().equals(user.getId())) {
                    ctx.sendLocalized("commands.kick.yourself_note", EmoteReference.ERROR);
                    return;
                }

                //If one of them is in a higher hierarchy than the bot, cannot kick.
                if (!selfMember.canInteract(member)) {
                    ctx.sendLocalized("commands.kick.self_hierarchy_conflict", EmoteReference.ERROR2, user.getName());
                    return;
                }
                final DBGuild db = MantaroData.db().getGuild(ctx.getGuild());

                guild.kick(member).reason(finalReason).queue(
                        success -> {
                            if (!user.isBot()) {
                                user.openPrivateChannel()
                                        .flatMap(privateChannel ->
                                                privateChannel.sendMessage(String.format("%sYou were **kicked** by %s#%s with reason: %s on server **%s**.",
                                                EmoteReference.MEGA, ctx.getAuthor().getName(), ctx.getAuthor().getDiscriminator(), finalReason, ctx.getGuild().getName()))
                                        ).queue();
                            }

                            db.getData().setCases(db.getData().getCases() + 1);
                            db.saveAsync();

                            ctx.sendLocalized("commands.kick.success", EmoteReference.ZAP, ctx.getLanguageContext().get("general.mod_quotes"), user.getName());
                            ModLog.log(ctx.getMember(), user, finalReason, ctx.getChannel().getName(), ModLog.ModAction.KICK, db.getData().getCases());
                            TextChannelGround.of(ctx.getEvent()).dropItemWithChance(2, 2);
                        },
                        error -> {
                            if (error instanceof PermissionException) {
                                ctx.sendLocalized("commands.kick.error", EmoteReference.ERROR, user.getName(), ((PermissionException) error).getPermission().getName());
                            } else {
                                ctx.sendLocalized("commands.kick.unknown_error", EmoteReference.ERROR, user.getName());
                                log.warn("Unexpected error while kicking someone.", error);
                            }
                        }
                );
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Kicks the mentioned user.")
                        .setUsage("`~>kick <@user> [reason]`")
                        .addParameter("@user", "The user to kick.")
                        .addParameter("reason", "The reason of the kick. This is optional.")
                        .build();
            }
        });
    }
}
