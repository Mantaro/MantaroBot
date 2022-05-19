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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Module
public class TransferCmds {
    private static final int TRANSFER_LIMIT = 500_000;
    private static final IncreasingRateLimiter transferRatelimiter = new IncreasingRateLimiter.Builder()
            .spamTolerance(2)
            .limit(1)
            .cooldown(45, TimeUnit.SECONDS)
            .cooldownPenaltyIncrease(10, TimeUnit.SECONDS)
            .maxCooldown(10, TimeUnit.MINUTES)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("transfer")
            .build();
    private static final RateLimiter partyRateLimiter = new RateLimiter(TimeUnit.MINUTES, 10);
    private static final IncreasingRateLimiter itemTransferRatelimiter = new IncreasingRateLimiter.Builder()
            .spamTolerance(2)
            .limit(1)
            .cooldown(15, TimeUnit.SECONDS)
            .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
            .maxCooldown(20, TimeUnit.MINUTES)
            .pool(MantaroData.getDefaultJedisPool())
            .premiumAware(true)
            .prefix("itemtransfer")
            .build();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Transfer.class);
    }

    @Description("Transfers money from you to another player.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to transfer money to.", required = true),
            @Options.Option(type = OptionType.INTEGER, name = "money", description = "The amount to transfer.", minValue = 100, maxValue = TRANSFER_LIMIT, required = true),
    })
    @Help(
            description = """
                    Transfers money from you to another player.
                    The maximum amount you can transfer at once is 500000 credits.
                    Current tax rate is 8%%.
                    """,
            usage = "`/transfer <@user> <money>` - Transfers money to x player.",
            parameters = {
                    @Help.Parameter(name = "user", description = "The user to transfer money to."),
                    @Help.Parameter(name = "money", description = "The amount to transfer to the user.")
            }
    )
    public static class Transfer extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            ctx.defer();
            var giveTo = ctx.getOptionAsUser("user");
            if (giveTo.equals(ctx.getAuthor())) {
                ctx.reply("commands.transfer.transfer_yourself_note", EmoteReference.THINKING);
                return;
            }

            if (giveTo.isBot()) {
                ctx.reply("commands.transfer.bot_notice", EmoteReference.ERROR);
                return;
            }

            Predicate<User> oldEnough = (u -> u.getTimeCreated().isBefore(OffsetDateTime.now().minus(14, ChronoUnit.DAYS)));
            if (!oldEnough.test(ctx.getAuthor())) {
                ctx.reply("commands.transfer.new_account_notice_yourself", EmoteReference.ERROR);
                return;
            }

            if (!oldEnough.test(giveTo)) {
                ctx.reply("commands.transfer.new_account_notice_other", EmoteReference.ERROR);
                return;
            }

            if (ctx.isUserBlacklisted(giveTo.getId())) {
                ctx.reply("commands.transfer.blacklisted_transfer", EmoteReference.ERROR);
                return;
            }

            var toSend = Math.abs(ctx.getOptionAsLong("money"));
            // Keep? Slash should take care of it.
            if (toSend > TRANSFER_LIMIT) {
                ctx.reply("commands.transfer.over_transfer_limit", EmoteReference.ERROR, TRANSFER_LIMIT);
                return;
            }

            final var transferPlayer = ctx.getPlayer();
            final var toTransfer = ctx.getPlayer(giveTo);

            if (transferPlayer.isLocked()) {
                ctx.reply("commands.transfer.own_locked_notice", EmoteReference.ERROR);
                return;
            }

            if (transferPlayer.getCurrentMoney() < toSend) {
                ctx.reply("commands.transfer.no_money_notice", EmoteReference.ERROR);
                return;
            }

            if (toTransfer.isLocked()) {
                ctx.reply("commands.transfer.receipt_locked_notice", EmoteReference.ERROR);
                return;
            }

            if (!RatelimitUtils.ratelimit(transferRatelimiter, ctx))
                return;

            var partyKey = ctx.getAuthor().getId() + ":" + giveTo.getId();
            if (!partyRateLimiter.process(partyKey)) {
                ctx.reply(EmoteReference.STOPWATCH +
                                ctx.getLanguageContext().get("commands.transfer.party").formatted(giveTo.getName()) +
                                " (Ratelimited)\n **You'll be able to transfer to this user again in " +
                                Utils.formatDuration(ctx.getLanguageContext(), partyRateLimiter.tryAgainIn(partyKey)) + ".**"
                );

                RatelimitUtils.ratelimitedUsers.computeIfAbsent(ctx.getAuthor().getIdLong(), __ -> new AtomicInteger()).incrementAndGet();
                return;
            }

            var amountTransfer = Math.round(toSend * 0.92);
            if (toTransfer.addMoney(amountTransfer)) {
                transferPlayer.removeMoney(toSend);
                transferPlayer.saveUpdating();

                ctx.reply("commands.transfer.success", EmoteReference.CORRECT, toSend, amountTransfer, giveTo.getName());
                toTransfer.saveUpdating();
                transferRatelimiter.limit(toTransfer.getUserId());
            } else {
                ctx.reply("commands.transfer.receipt_overflow_notice", EmoteReference.ERROR);
            }

        }
    }

    @Description("Transfers items from you to another player.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to transfer money to.", required = true),
            @Options.Option(type = OptionType.INTEGER, name = "amount", description = "The amount of the item to transfer. Defaults to 1.", maxValue = 5000),
            @Options.Option(type = OptionType.STRING, name = "item", description = "The item to transfer.", required = true),
    })
    @Help(
            description = "Transfers items from you to another player.",
            usage = "`/transferitems <user> [amount] <item>`",
            parameters = {
                    @Help.Parameter(name = "user", description = "The user to transfer money to. Needs to be in the server you're running the command in."),
                    @Help.Parameter(name = "amount", description = "The amount of the item to transfer. If not specified, this is 1. Maximum is 5000.", optional = true),
                    @Help.Parameter(name = "item", description = "The item to transfer. Can be a shorten name.")
            }
    )
    public static class TransferItems extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            ctx.defer();
            var giveTo = ctx.getOptionAsUser("user");
            if (ctx.getAuthor().getId().equals(giveTo.getId())) {
                ctx.reply("commands.itemtransfer.transfer_yourself_note", EmoteReference.ERROR);
                return;
            }

            if (giveTo.isBot()) {
                ctx.reply("commands.itemtransfer.bot_notice", EmoteReference.ERROR);
                return;
            }

            Predicate<User> oldEnough = (u -> u.getTimeCreated().isBefore(OffsetDateTime.now().minus(14, ChronoUnit.DAYS)));
            if (!oldEnough.test(ctx.getAuthor())) {
                ctx.reply("commands.transfer.new_account_notice_yourself", EmoteReference.ERROR);
                return;
            }

            if (!oldEnough.test(giveTo)) {
                ctx.reply("commands.transfer.new_account_notice_other", EmoteReference.ERROR);
                return;
            }

            if (ctx.isUserBlacklisted(giveTo.getId())) {
                ctx.reply("commands.transfer.blacklisted_transfer", EmoteReference.ERROR);
                return;
            }

            if (!RatelimitUtils.ratelimit(itemTransferRatelimiter, ctx)) {
                return;
            }

            var itemString = ctx.getOptionAsString("item");
            var item = ItemHelper.fromAnyNoId(itemString, ctx.getLanguageContext()).orElse(null);
            if (item == null) {
                ctx.reply("general.item_lookup.not_found");
                return;
            }


            if (item == ItemReference.CLAIM_KEY) {
                ctx.reply("general.item_lookup.claim_key");
                return;
            }

            final var player = ctx.getPlayer();
            final var giveToPlayer = ctx.getPlayer(giveTo);

            if (player.isLocked()) {
                ctx.reply("commands.itemtransfer.locked_notice", EmoteReference.ERROR);
                return;
            }

            var amount = (int) Math.abs(ctx.getOptionAsLong("amount", 1));
            if (amount == 1) {
                if (player.getInventory().containsItem(item)) {
                    if (item.isHidden()) {
                        ctx.reply("commands.itemtransfer.hidden_item", EmoteReference.ERROR);
                        return;
                    }

                    if (giveToPlayer.getInventory().getAmount(item) >= 5000) {
                        ctx.reply("commands.itemtransfer.overflow", EmoteReference.ERROR);
                        return;
                    }

                    player.getInventory().process(new ItemStack(item, -1));
                    giveToPlayer.getInventory().process(new ItemStack(item, 1));
                    ctx.reply("commands.itemtransfer.success",
                            EmoteReference.OK, ctx.getMember().getEffectiveName(), 1,
                            item.getName(), giveTo.getName()
                    );
                } else {
                    ctx.reply("commands.itemtransfer.multiple_items_error", EmoteReference.ERROR);
                }

                player.save();
                giveToPlayer.save();
                return;
            }

            try {
                if (player.getInventory().containsItem(item) && player.getInventory().getAmount(item) >= amount) {
                    if (item.isHidden()) {
                        ctx.reply("commands.itemtransfer.hidden_item", EmoteReference.ERROR);
                        return;
                    }

                    if (giveToPlayer.getInventory().getAmount(item) + amount > 5000) {
                        ctx.reply("commands.itemtransfer.overflow_after", EmoteReference.ERROR);
                        return;
                    }

                    player.getInventory().process(new ItemStack(item, amount * -1));
                    giveToPlayer.getInventory().process(new ItemStack(item, amount));

                    ctx.reply("commands.itemtransfer.success", EmoteReference.OK,
                            ctx.getMember().getEffectiveName(), amount, item.getName(), giveTo.getName()
                    );
                } else {
                    ctx.reply("commands.itemtransfer.error", EmoteReference.ERROR);
                }
            } catch (NumberFormatException nfe) {
                ctx.reply(String.format(ctx.getLanguageContext().get("general.invalid_number") + " " +
                        ctx.getLanguageContext().get("general.space_notice"), EmoteReference.ERROR)
                );
            }

            player.save();
            giveToPlayer.save();
        }
    }
}
