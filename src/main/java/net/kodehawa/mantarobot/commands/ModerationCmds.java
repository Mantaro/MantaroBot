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

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

@Module
public class ModerationCmds {
    private static final Logger log = LoggerFactory.getLogger(ModerationCmds.class);

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Ban.class);
        cr.registerSlash(SoftBan.class);
        cr.registerSlash(Kick.class);
    }

    @Name("ban")
    @Description("Bans the specified user.")
    @Category(CommandCategory.MODERATION)
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to ban.", required = true),
            @Options.Option(type = OptionType.STRING, name = "reason", description = "The ban reason.")
    })
    @Help(description = "Bans the specified user.", usage = "`/ban <user> [reason]`", parameters = {
            @Help.Parameter(name = "user", description = "The user to ban."),
            @Help.Parameter(name = "reason", description = "The ban reason.", optional = true)
    })
    public static class Ban extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var user = ctx.getOptionAsUser("user");
            var member = ctx.getGuild().getMember(user);
            var reason = ctx.getOptionAsString("reason", "");
            if (reason.isEmpty()) {
                reason = "Reason not specified";
            }

            final var author = ctx.getAuthor();
            final var finalReason = "Banned by %#s: %s".formatted(author, reason);
            final var languageContext = ctx.getLanguageContext();
            final var guild = ctx.getGuild();

            if (!ctx.getMember().canInteract(member)) {
                ctx.replyEphemeral("commands.ban.hierarchy_conflict", EmoteReference.ERROR, EmoteReference.SMILE);
                return;
            }

            if (author.getId().equals(member.getId())) {
                ctx.replyEphemeral("commands.ban.yourself_note", EmoteReference.ERROR);
                return;
            }

            if (!guild.getSelfMember().canInteract(member)) {
                ctx.replyEphemeral("commands.ban.self_hierarchy_conflict", EmoteReference.ERROR, user.getName());
                return;
            }

            final var db = ctx.getDBGuild();

            // DM's before success, because it might be the "c"ast mutual guild.
            user.openPrivateChannel().queue(privateChannel -> {
                if (!user.isBot()) {
                    privateChannel.sendMessage("%sYou were **kicked** by %s with reason: %s on server **%s**.".formatted(
                            EmoteReference.MEGA,
                            author.getAsTag(),
                            finalReason,
                            ctx.getGuild().getName())
                    ).queue();
                }
            });

            guild.ban(member, 7).reason(finalReason).queue(
                success -> {
                    db.getData().setCases(db.getData().getCases() + 1);
                    db.saveUpdating();

                    ctx.reply("commands.ban.success", EmoteReference.ZAP, languageContext.get("general.mod_quotes"), user.getName());
                    ModLog.log(ctx.getMember(), user, finalReason, ctx.getChannel().getName(), ModLog.ModAction.BAN, db.getData().getCases());
                    TextChannelGround.of(ctx.getChannel()).dropItemWithChance(1, 2);
                },
                error ->
                {
                    if (error instanceof PermissionException) {
                        ctx.replyEphemeral("commands.ban.error", EmoteReference.ERROR, user.getName(), ((PermissionException) error).getPermission());
                    } else {
                        ctx.replyEphemeral("commands.ban.unknown_error", EmoteReference.ERROR, user.getName());
                        log.warn("Encountered an unexpected error while trying to ban someone.", error);
                    }
                }
            );
        }

        @Override
        protected Predicate<SlashContext> getPredicate() {
            return ctx -> {
                if (!ctx.getMember().hasPermission(Permission.BAN_MEMBERS)) {
                    ctx.replyEphemeral("commands.ban.no_permission", EmoteReference.ERROR);
                    return false;
                }

                if (!ctx.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
                    ctx.replyEphemeral("commands.ban.no_permission_self", EmoteReference.ERROR);
                    return false;
                }

                return true;
            };
        }
    }

    @Name("kick")
    @Description("Kicks the specified user.")
    @Category(CommandCategory.MODERATION)
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to kick.", required = true),
            @Options.Option(type = OptionType.STRING, name = "reason", description = "The kick reason.")
    })
    @Help(description = "Bans the specified user.", usage = "`/kick <user> [reason]`", parameters = {
            @Help.Parameter(name = "user", description = "The user to kick."),
            @Help.Parameter(name = "reason", description = "The kick reason.", optional = true)
    })
    public static class Kick extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var user = ctx.getOptionAsUser("user");
            var member = ctx.getGuild().getMember(user);
            var reason = ctx.getOptionAsString("reason", "");
            if (reason.isEmpty()) {
                reason = "Reason not specified";
            }

            var author = ctx.getAuthor();
            final var finalReason = "Kicked by %#s: %s".formatted(author, reason);
            if (!ctx.getMember().canInteract(member)) {
                ctx.replyEphemeral("commands.kick.hierarchy_conflict", EmoteReference.ERROR);
                return;
            }

            if (author.getId().equals(user.getId())) {
                ctx.replyEphemeral("commands.kick.yourself_note", EmoteReference.ERROR);
                return;
            }

            //If one of them is in a higher hierarchy than the bot, cannot kick.
            if (!ctx.getSelfMember().canInteract(member)) {
                ctx.replyEphemeral("commands.kick.self_hierarchy_conflict", EmoteReference.ERROR2, user.getName());
                return;
            }

            final var db = ctx.getDBGuild();
            if (!user.isBot()) {
                user.openPrivateChannel()
                        .flatMap(privateChannel ->
                                privateChannel.sendMessage("%sYou were **kicked** by %s with reason: %s on server **%s**.".formatted(
                                        EmoteReference.MEGA,
                                        author.getAsTag(),
                                        finalReason,
                                        ctx.getGuild().getName())
                                )
                        ).queue();
            }

            ctx.getGuild().kick(member).reason(finalReason).queue(
                success -> {
                    db.getData().setCases(db.getData().getCases() + 1);
                    db.saveAsync();

                    ctx.reply("commands.kick.success", EmoteReference.ZAP, ctx.getLanguageContext().get("general.mod_quotes"), user.getName());
                    ModLog.log(ctx.getMember(), user, finalReason, ctx.getChannel().getName(), ModLog.ModAction.KICK, db.getData().getCases());
                    TextChannelGround.of(ctx.getChannel()).dropItemWithChance(2, 2);
                }, error -> {
                    if (error instanceof PermissionException) {
                        ctx.replyEphemeral("commands.kick.error", EmoteReference.ERROR, user.getName(),
                                ((PermissionException) error).getPermission().getName()
                        );
                    } else {
                        ctx.replyEphemeral("commands.kick.unknown_error", EmoteReference.ERROR, user.getName());
                        log.warn("Unexpected error while kicking someone.", error);
                    }
                }
            );
        }

        @Override
        protected Predicate<SlashContext> getPredicate() {
            return ctx -> {
                if (!ctx.getMember().hasPermission(Permission.KICK_MEMBERS)) {
                    ctx.replyEphemeral("commands.kick.no_permission", EmoteReference.ERROR2);
                    return false;
                }

                if (!ctx.getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
                    ctx.replyEphemeral("commands.kick.no_permission_self", EmoteReference.ERROR2);
                    return false;
                }

                return true;
            };
        }
    }
    @Name("softban")
    @Description("Bans and then unbans the specified user.")
    @Category(CommandCategory.MODERATION)
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to soft-ban.", required = true),
            @Options.Option(type = OptionType.STRING, name = "reason", description = "The soft-ban reason.")
    })
    @Help(description = "Bans the specified user.", usage = "`/softban <user> [reason]`", parameters = {
            @Help.Parameter(name = "user", description = "The user to soft-ban."),
            @Help.Parameter(name = "reason", description = "The soft-ban reason.", optional = true)
    })
    public static class SoftBan extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var user = ctx.getOptionAsUser("user");
            var member = ctx.getGuild().getMember(user);
            var author = ctx.getAuthor();
            var reason = ctx.getOptionAsString("reason", "");
            if (reason.isEmpty()) {
                reason = "Reason not specified";
            }

            var finalReason = "Softbanned by %#s: %s".formatted(author, reason);

            if (!ctx.getMember().canInteract(member)) {
                ctx.replyEphemeral("commands.softban.hierarchy_conflict", EmoteReference.ERROR);
                return;
            }

            if (author.getId().equals(user.getId())) {
                ctx.replyEphemeral("commands.softban.yourself_note", EmoteReference.ERROR);
                return;
            }

            //If one of them is in a higher hierarchy than the bot, cannot ban.
            if (!ctx.getSelfMember().canInteract(member)) {
                ctx.sendLocalized("commands.softban.self_hierarchy_conflict", EmoteReference.ERROR, user.getName());
                return;
            }

            var dbGuild = ctx.getDBGuild();
            var languageContext = ctx.getLanguageContext();

            ctx.getGuild().ban(member, 7).reason(finalReason).queue(
                success -> {
                    user.openPrivateChannel()
                            .flatMap(privateChannel ->
                                    privateChannel.sendMessage("%sYou were **softbanned** by %s#%s for reason %s on server **%s**."
                                            .formatted(
                                                    EmoteReference.MEGA,
                                                    author.getName(),
                                                    author.getDiscriminator(),
                                                    finalReason,
                                                    ctx.getGuild().getName()))
                            ).queue();

                    dbGuild.getData().setCases(dbGuild.getData().getCases() + 1);
                    dbGuild.saveAsync();

                    ctx.reply("commands.softban.success", EmoteReference.ZAP, languageContext.get("general.mod_quotes"), user.getName());
                    ctx.getGuild().unban(user).reason(finalReason).queue(__ -> { }, error -> {
                        if (error instanceof PermissionException) {
                            ctx.replyEphemeral("commands.softban.error", EmoteReference.ERROR,
                                    user.getName(), ((PermissionException) error).getPermission()
                            );
                        } else {
                            ctx.replyEphemeral("commands.softban.unknown_error", EmoteReference.ERROR,
                                    user.getName(), error.getClass().getSimpleName(), error.getMessage()
                            );

                            log.warn("Unexpected error while softbanning someone.", error);
                        }
                    });

                    ModLog.log(ctx.getMember(), user, finalReason, ctx.getChannel().getName(), ModLog.ModAction.KICK, dbGuild.getData().getCases());
                    TextChannelGround.of(ctx.getChannel()).dropItemWithChance(2, 2);
                }, error -> {
                    if (error instanceof PermissionException) {
                        ctx.replyEphemeral("commands.softban.error", EmoteReference.ERROR,
                                user.getName(), ((PermissionException) error).getPermission()
                        );
                    } else {
                        ctx.replyEphemeral("commands.softban.unknown_error", EmoteReference.ERROR,
                                user.getName(), error.getClass().getSimpleName(), error.getMessage()
                        );

                        log.warn("Unexpected error while softbanning someone.", error);
                    }
                }
            );
        }

        @Override
        protected Predicate<SlashContext> getPredicate() {
            return ctx -> {
                if (!ctx.getMember().hasPermission(Permission.BAN_MEMBERS)) {
                    ctx.replyEphemeral("commands.softban.no_permission", EmoteReference.ERROR);
                    return false;
                }

                if (!ctx.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
                    ctx.replyEphemeral("commands.softban.no_permission_self", EmoteReference.ERROR2);
                    return false;
                }

                return true;
            };
        }
    }
}
