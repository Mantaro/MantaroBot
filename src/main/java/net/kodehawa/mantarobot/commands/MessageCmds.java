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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Module
public class MessageCmds {
    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Prune.class);
    }

    @Name("prune")
    @Description("Prunes X amount of messages from a channel. Requires Message Manage permission.")
    @Category(CommandCategory.MODERATION)
    @Options({
            @Options.Option(type = OptionType.INTEGER, name = "amount", description = "The amount of messages to prune", maxValue = 100, minValue = 5, required = true),
            @Options.Option(type = OptionType.BOOLEAN, name = "botonly", description = "Only prune messages from bots"),
            @Options.Option(type = OptionType.BOOLEAN, name = "skippinned", description = "Don't prune pinned messages"),
            @Options.Option(type = OptionType.USER, name = "user", description = "Only prune messages from the specified user")
    })
    @Help(
            description = "Prunes X amount of messages from a channel. Requires Message Manage permission.",
            usage = "`/prune <amount> [user] [bot only] [skip pinned]`",
            parameters = {
                    @Help.Parameter(name = "amount", description = "The amount of messages to prune, from 5 to 100."),
                    @Help.Parameter(name = "user", description = "Only prune from this specific user.", optional = true),
                    @Help.Parameter(name = "botonly", description = "Only prune bot messages.", optional = true),
                    @Help.Parameter(name = "skippinned", description = "Don't prune pinned messages.", optional = true),
            }
    )
    public static class Prune extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var user = ctx.getOptionAsUser("user");
            var amount = ctx.getOptionAsLong("amount", 1);
            var botOnly = ctx.getOptionAsBoolean("botonly");
            var skipPinned = ctx.getOptionAsBoolean("skippinned");
            Predicate<Message> predicate = message -> {
                boolean bool = true;
                if (user != null) {
                    bool = user.getIdLong() == message.getAuthor().getIdLong();
                }

                if (botOnly) {
                    bool = bool && message.getAuthor().isBot();
                }

                if (skipPinned) {
                    bool = bool && message.isPinned();
                }

                return bool;
            };

            ctx.getChannel().getHistory().retrievePast((int) amount)
                    .queue(
                            messageHistory -> getMessageHistory(ctx, messageHistory, (int) amount, predicate),
                            error -> ctx.reply("commands.prune.error_retrieving", EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage())
                    );

        }

        @Override
        public Predicate<SlashContext> getPredicate() {
            return ctx -> {
                if (!ctx.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                    ctx.reply("commands.prune.no_permissions_user", EmoteReference.ERROR);
                    return false;
                }

                if (!ctx.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                    ctx.reply("commands.prune.no_permissions", EmoteReference.ERROR);
                    return false;
                }

                return true;
            };
        }
    }

    private static void getMessageHistory(SlashContext ctx, List<Message> messageHistory, int limit, Predicate<Message> predicate) {
        var stream = messageHistory.stream().filter(predicate);
        if (limit != -1) {
            stream = stream.limit(limit);
        }

        messageHistory = stream.collect(Collectors.toList());
        if (messageHistory.isEmpty()) {
            ctx.reply("commands.prune.no_messages", EmoteReference.ERROR);
            return;
        }

        if (messageHistory.size() < 3) {
            ctx.reply("commands.prune.too_few_messages", EmoteReference.ERROR);
            return;
        }

        prune(ctx, messageHistory);
    }

    private static void prune(SlashContext ctx, List<Message> messageHistory) {
        messageHistory = messageHistory.stream()
                .filter(message -> message.getType().canDelete())
                .filter(message -> !message.getTimeCreated().isBefore(OffsetDateTime.now().minusWeeks(2)))
                .collect(Collectors.toList());

        if (messageHistory.isEmpty()) {
            ctx.reply("commands.prune.messages_too_old", EmoteReference.ERROR);
            return;
        }

        final var size = messageHistory.size();

        if (messageHistory.size() < 3) {
            ctx.reply("commands.prune.too_few_messages", EmoteReference.ERROR);
            return;
        }

        ctx.getChannel().deleteMessages(messageHistory).queue(
                success -> {
                    ctx.reply("commands.prune.success", EmoteReference.PENCIL, size);

                    var db = ctx.getDBGuild();
                    db.getData().setCases(db.getData().getCases() + 1);
                    db.saveAsync();
                    ModLog.log(ctx.getMember(), null, "Pruned Messages",
                            ctx.getChannel().getName(), ModLog.ModAction.PRUNE, db.getData().getCases(), size
                    );
                },
                error -> {
                    if (error instanceof PermissionException pe) {
                        ctx.reply("commands.prune.lack_perms", EmoteReference.ERROR, pe.getPermission());
                    } else {
                        ctx.reply("commands.prune.error_deleting", EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage());
                        error.printStackTrace();
                    }
                }
        );
    }
}
