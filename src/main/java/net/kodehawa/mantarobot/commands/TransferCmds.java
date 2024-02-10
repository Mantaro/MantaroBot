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
import net.kodehawa.mantarobot.core.command.meta.Module;
import net.kodehawa.mantarobot.core.command.helpers.CommandCategory;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final IncreasingRateLimiter partyRateLimiter = new IncreasingRateLimiter.Builder()
            .spamTolerance(2)
            .limit(1)
            .cooldown(10, TimeUnit.MINUTES)
            .randomIncrement(false)
            .pool(MantaroData.getDefaultJedisPool())
            .premiumAware(false)
            .prefix("transferparty")
            .build();

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
        cr.registerSlash(TransferItems.class);
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
            usage = "`/transfer user:<user> money:<amount>` - Transfers money to x player.",
            parameters = {
                    @Help.Parameter(name = "user", description = "The user to transfer money to."),
                    @Help.Parameter(name = "money", description = "The amount to transfer to the user.")
            }
    )
    public static class Transfer extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var giveTo = ctx.getOptionAsUser("user");
            if (giveTo == null) {
                ctx.reply("general.slash_member_lookup_failure", EmoteReference.ERROR);
                return;
            }

            if (giveTo.equals(ctx.getAuthor())) {
                ctx.reply("commands.transfer.transfer_yourself_note", EmoteReference.THINKING);
                return;
            }

            if (giveTo.isBot()) {
                ctx.reply("commands.transfer.bot_notice", EmoteReference.ERROR);
                return;
            }

            if (!Utils.isAccountOldEnough(ctx.getAuthor(), 14, ChronoUnit.DAYS)) {
                ctx.reply("commands.transfer.new_account_notice_yourself", EmoteReference.ERROR);
                return;
            }

            if (!Utils.isAccountOldEnough(giveTo, 14, ChronoUnit.DAYS)) {
                ctx.reply("commands.transfer.new_account_notice_other", EmoteReference.ERROR);
                return;
            }

            if (ctx.isUserBlacklisted(giveTo.getId())) {
                ctx.reply("commands.transfer.blacklisted_transfer", EmoteReference.ERROR);
                return;
            }

            var toSend = ctx.getOptionAsLong("money", 1);
            if (toSend < 100) {
                ctx.reply("commands.transfer.too_little", EmoteReference.ERROR);
                return;
            }

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
            var rl = partyRateLimiter.limit(partyKey);
            if (rl.getTriesLeft() < 1) {
                ctx.reply(EmoteReference.STOPWATCH +
                        ctx.getLanguageContext().get("commands.transfer.party").formatted(giveTo.getName()) +
                        " (Ratelimited)\n **You'll be able to transfer to this user again in " +
                        Utils.formatDuration(ctx.getLanguageContext(), rl.getCooldown()) + ".**"
                );

                RatelimitUtils.ratelimitedUsers.computeIfAbsent(ctx.getAuthor().getIdLong(), __ -> new AtomicInteger()).incrementAndGet();
                return;
            }

            var amountTransfer = Math.round(toSend * 0.92);
            if (toTransfer.addMoney(amountTransfer)) {
                transferPlayer.removeMoney(toSend);
                transferPlayer.updateAllChanged();

                toTransfer.updateAllChanged();
                transferRatelimiter.limit(toTransfer.getId());
                ctx.reply("commands.transfer.success", EmoteReference.CORRECT, toSend, amountTransfer, giveTo.getAsMention());
            } else {
                ctx.reply("commands.transfer.receipt_overflow_notice", EmoteReference.ERROR);
            }
        }
    }

    @Description("Transfers items from you to another player.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to transfer money to.", required = true),
            @Options.Option(type = OptionType.STRING, name = "item", description = "The item to transfer.", required = true),
            @Options.Option(type = OptionType.INTEGER, name = "amount", description = "The amount of the item to transfer. Defaults to 1.", maxValue = ItemStack.MAX_STACK_SIZE),
            @Options.Option(type = OptionType.BOOLEAN, name = "max", description = "Transfer as many as possible. Makes it so amount is ignored.")
    })
    @Help(
            description = "Transfers items from you to another player.",
            usage = "`/transferitems user:<user> item:<item name> amount:[amount]`",
            parameters = {
                    @Help.Parameter(name = "user", description = "The user to transfer money to. Needs to be in the server you're running the command in."),
                    @Help.Parameter(name = "item", description = "The item to transfer. Can be a shorten name."),
                    @Help.Parameter(name = "amount", description = "The amount of the item to transfer. If not specified, this is 1. Maximum is 5000.", optional = true),
                    @Help.Parameter(name = "max", description = "Whether to attempt transferring the maximum amount currently possible for you and them. Makes it so amount is ignored.", optional = true),
            }
    )
    public static class TransferItems extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var giveTo = ctx.getOptionAsUser("user");
            if (giveTo == null) {
                ctx.reply("general.slash_member_lookup_failure", EmoteReference.ERROR);
                return;
            }

            if (ctx.getAuthor().getId().equals(giveTo.getId())) {
                ctx.reply("commands.itemtransfer.transfer_yourself_note", EmoteReference.ERROR);
                return;
            }

            if (giveTo.isBot()) {
                ctx.reply("commands.itemtransfer.bot_notice", EmoteReference.ERROR);
                return;
            }

            if (!Utils.isAccountOldEnough(ctx.getAuthor(), 14, ChronoUnit.DAYS)) {
                ctx.reply("commands.transfer.new_account_notice_yourself", EmoteReference.ERROR);
                return;
            }

            if (!Utils.isAccountOldEnough(giveTo, 14, ChronoUnit.DAYS)) {
                ctx.reply("commands.transfer.new_account_notice_other", EmoteReference.ERROR);
                return;
            }

            if (ctx.isUserBlacklisted(giveTo.getId())) {
                ctx.reply("commands.transfer.blacklisted_transfer", EmoteReference.ERROR);
                return;
            }

            var itemString = ctx.getOptionAsString("item");
            var item = ItemHelper.fromAnyNoId(itemString, ctx.getLanguageContext()).orElse(null);
            if (item == null) {
                ctx.replyLocalized("general.item_lookup.not_found");
                return;
            }

            if (item == ItemReference.CLAIM_KEY) {
                ctx.replyLocalized("general.item_lookup.claim_key");
                return;
            }

            final var player = ctx.getPlayer();
            final var giveToPlayer = ctx.getPlayer(giveTo);
            if (player.isLocked()) {
                ctx.reply("commands.itemtransfer.locked_notice", EmoteReference.ERROR);
                return;
            }

            if (giveToPlayer.isLocked()) {
                ctx.reply("commands.itemtransfer.locked_notice_other", EmoteReference.ERROR);
                return;
            }

            if (!RatelimitUtils.ratelimit(itemTransferRatelimiter, ctx)) {
                return;
            }

            var amount = ctx.getOptionAsInteger("amount", 1);
            var isMax = ctx.getOptionAsBoolean("max");

            var playerCount = player.getItemAmount(item);
            var giveToCount = giveToPlayer.getItemAmount(item);
            if (isMax) {
                amount = Math.max(1, Math.min(playerCount, ItemStack.MAX_STACK_SIZE - giveToCount));
            }

            if (playerCount >= amount) {
                if (item.isHidden()) {
                    ctx.reply("commands.itemtransfer.hidden_item", EmoteReference.ERROR);
                    return;
                }

                if (!giveToPlayer.fitsItemAmount(item, amount)) {
                    ctx.reply("commands.itemtransfer.overflow_after", EmoteReference.ERROR);
                    return;
                }

                player.processItem(item, amount * -1);
                giveToPlayer.processItem(item, amount);

                player.updateAllChanged();
                giveToPlayer.updateAllChanged();
                ctx.reply("commands.itemtransfer.success", EmoteReference.OK,
                        ctx.getMember().getEffectiveName(), amount, item.getName(), giveTo.getAsMention()
                );
            } else {
                ctx.reply("commands.itemtransfer.error", EmoteReference.ERROR);
            }
        }
    }
}
