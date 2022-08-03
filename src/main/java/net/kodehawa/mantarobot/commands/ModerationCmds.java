/*
 * Copyright (C) 2016-2022 David Rubio Escares / Kodehawa
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
        cr.registerSlash(SoftBan.class);
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
        public Predicate<SlashContext> getPredicate() {
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
