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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.utils.RoundedMetricPrefixFormat;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.text.ParsePosition;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Module
public class TransferCmds {
    private static final int TRANSFER_LIMIT = 500_000;

    @Subscribe
    public void transfer(CommandRegistry cr) {
        cr.register("transfer", new SimpleCommand(CommandCategory.CURRENCY) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .spamTolerance(2)
                    .limit(1)
                    .cooldown(45, TimeUnit.SECONDS)
                    .cooldownPenaltyIncrease(10, TimeUnit.SECONDS)
                    .maxCooldown(10, TimeUnit.MINUTES)
                    .pool(MantaroData.getDefaultJedisPool())
                    .prefix("transfer")
                    .build();

            //this still uses a normal RL
            final RateLimiter partyRateLimiter = new RateLimiter(TimeUnit.MINUTES, 10);

            @Override
            public void call(Context ctx, String content, String[] args) {
                if (ctx.getMentionedUsers().isEmpty()) {
                    ctx.sendLocalized("general.mention_user_required", EmoteReference.ERROR);
                    return;
                }

                final var giveTo = ctx.getMentionedUsers().get(0);

                if (giveTo.equals(ctx.getAuthor())) {
                    ctx.sendLocalized("commands.transfer.transfer_yourself_note", EmoteReference.THINKING);
                    return;
                }

                if (giveTo.isBot()) {
                    ctx.sendLocalized("commands.transfer.bot_notice", EmoteReference.ERROR);
                    return;
                }

                Predicate<User> oldEnough = (u -> u.getTimeCreated().isBefore(OffsetDateTime.now().minus(7, ChronoUnit.DAYS)));
                if (!oldEnough.test(ctx.getAuthor())) {
                    ctx.sendLocalized("commands.transfer.new_account_notice_yourself", EmoteReference.ERROR);
                    return;
                }

                if (!oldEnough.test(giveTo)) {
                    ctx.sendLocalized("commands.transfer.new_account_notice_other", EmoteReference.ERROR);
                    return;
                }

                if (!RatelimitUtils.ratelimit(rateLimiter, ctx))
                    return;


                var toSend = 0L; // = 0 at the start

                try {
                    //Convert negative values to absolute.
                    toSend = Math.abs(new RoundedMetricPrefixFormat().parseObject(args[1], new ParsePosition(0)));
                } catch (Exception e) {
                    ctx.sendLocalized("commands.transfer.no_amount", EmoteReference.ERROR);
                    return;
                }

                if (toSend == 0) {
                    ctx.sendLocalized("commands.transfer.no_money_specified_notice", EmoteReference.ERROR);
                    return;
                }

                if (ItemHelper.fromAnyNoId(args[1], ctx.getLanguageContext()).isPresent()) {
                    ctx.sendLocalized("commands.transfer.item_transfer", EmoteReference.ERROR);
                    return;
                }

                if (toSend > TRANSFER_LIMIT) {
                    ctx.sendLocalized("commands.transfer.over_transfer_limit", EmoteReference.ERROR, TRANSFER_LIMIT);
                    return;
                }

                final var transferPlayer = ctx.getPlayer();
                final var toTransfer = ctx.getPlayer(giveTo);

                if (transferPlayer.isLocked()) {
                    ctx.sendLocalized("commands.transfer.own_locked_notice", EmoteReference.ERROR);
                    return;
                }

                if (transferPlayer.getCurrentMoney() < toSend) {
                    ctx.sendLocalized("commands.transfer.no_money_notice", EmoteReference.ERROR);
                    return;
                }

                if (toTransfer.isLocked()) {
                    ctx.sendLocalized("commands.transfer.receipt_locked_notice", EmoteReference.ERROR);
                    return;
                }

                var partyKey = ctx.getAuthor().getId() + ":" + giveTo.getId();
                if (!partyRateLimiter.process(partyKey)) {
                    ctx.getChannel().sendMessage(
                            EmoteReference.STOPWATCH +
                                    ctx.getLanguageContext().get("commands.transfer.party").formatted(giveTo.getName()) +
                                    " (Ratelimited)\n **You'll be able to transfer to this user again in " +
                                    Utils.formatDuration(partyRateLimiter.tryAgainIn(partyKey)) + ".**"
                    ).queue();

                    RatelimitUtils.ratelimitedUsers.computeIfAbsent(ctx.getAuthor().getIdLong(), __ -> new AtomicInteger()).incrementAndGet();
                    return;
                }

                var amountTransfer = Math.round(toSend * 0.92);

                if (toTransfer.addMoney(amountTransfer)) {
                    transferPlayer.removeMoney(toSend);
                    transferPlayer.saveUpdating();

                    ctx.sendLocalized("commands.transfer.success", EmoteReference.CORRECT, toSend, amountTransfer, giveTo.getName());
                    toTransfer.saveUpdating();
                    rateLimiter.limit(toTransfer.getUserId());
                } else {
                    ctx.sendLocalized("commands.transfer.receipt_overflow_notice", EmoteReference.ERROR);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Transfers money from you to another player.
                                The maximum amount you can transfer at once is %s credits.
                                Current tax rate is 8%%.
                                """.formatted(TRANSFER_LIMIT)
                        )
                        .setUsage("`~>transfer <@user> <money>` - Transfers money to x player")
                        .addParameter("@user", "The user to send the money to. You have to mention (ping) the user.")
                        .addParameter("money", "How much money to transfer.")
                        .build();
            }
        });
    }

    @Subscribe
    public void transferItems(CommandRegistry cr) {
        cr.register("itemtransfer", new SimpleCommand(CommandCategory.CURRENCY) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .spamTolerance(2)
                    .limit(1)
                    .cooldown(15, TimeUnit.SECONDS)
                    .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                    .maxCooldown(20, TimeUnit.MINUTES)
                    .pool(MantaroData.getDefaultJedisPool())
                    .premiumAware(true)
                    .prefix("itemtransfer")
                    .build();

            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (args.length < 2) {
                    ctx.sendLocalized("commands.itemtransfer.no_item_mention", EmoteReference.ERROR);
                    return;
                }

                final var mentionedMembers = ctx.getMentionedMembers();

                if (mentionedMembers.isEmpty()) {
                    ctx.sendLocalized("general.mention_user_required", EmoteReference.ERROR);
                    return;
                }

                var giveTo = mentionedMembers.get(0);

                if (ctx.getAuthor().getId().equals(giveTo.getId())) {
                    ctx.sendLocalized("commands.itemtransfer.transfer_yourself_note", EmoteReference.ERROR);
                    return;
                }

                if (giveTo.getUser().isBot()) {
                    ctx.sendLocalized("commands.itemtransfer.bot_notice", EmoteReference.ERROR);
                    return;
                }

                Predicate<User> oldEnough = (u -> u.getTimeCreated().isBefore(OffsetDateTime.now().minus(7, ChronoUnit.DAYS)));
                if (!oldEnough.test(ctx.getAuthor())) {
                    ctx.sendLocalized("commands.transfer.new_account_notice_yourself", EmoteReference.ERROR);
                    return;
                }

                if (!oldEnough.test(giveTo.getUser())) {
                    ctx.sendLocalized("commands.transfer.new_account_notice_other", EmoteReference.ERROR);
                    return;
                }

                if (!RatelimitUtils.ratelimit(rateLimiter, ctx)) {
                    return;
                }

                var item = ItemHelper.fromAnyNoId(args[1], ctx.getLanguageContext()).orElse(null);
                if (item == null) {
                    item = ItemHelper.fromAnyNoId(args[0], ctx.getLanguageContext()).orElse(null);
                    if (item == null) {
                        ctx.sendLocalized("general.item_lookup.no_item_emoji");
                        return;
                    }
                }

                if (item == ItemReference.CLAIM_KEY) {
                    ctx.sendLocalized("general.item_lookup.claim_key");
                    return;
                }

                final var player = ctx.getPlayer();
                final var giveToPlayer = ctx.getPlayer(giveTo);

                if (player.isLocked()) {
                    ctx.sendLocalized("commands.itemtransfer.locked_notice", EmoteReference.ERROR);
                    return;
                }

                if (args.length == 2) {
                    if (player.getInventory().containsItem(item)) {
                        if (item.isHidden()) {
                            ctx.sendLocalized("commands.itemtransfer.hidden_item", EmoteReference.ERROR);
                            return;
                        }

                        if (giveToPlayer.getInventory().getAmount(item) >= 5000) {
                            ctx.sendLocalized("commands.itemtransfer.overflow", EmoteReference.ERROR);
                            return;
                        }

                        player.getInventory().process(new ItemStack(item, -1));
                        giveToPlayer.getInventory().process(new ItemStack(item, 1));
                        ctx.sendStrippedLocalized("commands.itemtransfer.success",
                                EmoteReference.OK, ctx.getMember().getEffectiveName(), 1,
                                item.getName(), giveTo.getEffectiveName()
                        );
                    } else {
                        ctx.sendLocalized("commands.itemtransfer.multiple_items_error", EmoteReference.ERROR);
                    }

                    player.save();
                    giveToPlayer.save();
                    return;
                }

                try {
                    int amount = Math.abs(Integer.parseInt(args[2]));
                    if (player.getInventory().containsItem(item) && player.getInventory().getAmount(item) >= amount) {
                        if (item.isHidden()) {
                            ctx.sendLocalized("commands.itemtransfer.hidden_item", EmoteReference.ERROR);
                            return;
                        }

                        if (giveToPlayer.getInventory().getAmount(item) + amount > 5000) {
                            ctx.sendLocalized("commands.itemtransfer.overflow_after", EmoteReference.ERROR);
                            return;
                        }

                        player.getInventory().process(new ItemStack(item, amount * -1));
                        giveToPlayer.getInventory().process(new ItemStack(item, amount));

                        ctx.sendStrippedLocalized("commands.itemtransfer.success", EmoteReference.OK,
                                ctx.getMember().getEffectiveName(), amount, item.getName(), giveTo.getEffectiveName()
                        );
                    } else {
                        ctx.sendLocalized("commands.itemtransfer.error", EmoteReference.ERROR);
                    }
                } catch (NumberFormatException nfe) {
                    ctx.send(String.format(ctx.getLanguageContext().get("general.invalid_number") + " " +
                            ctx.getLanguageContext().get("general.space_notice"), EmoteReference.ERROR)
                    );
                }

                player.save();
                giveToPlayer.save();
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Transfers items from you to another player.")
                        .setUsage("`~>itemtransfer <@user> <item> <amount>` *or* " +
                                "`~>itemtransfer <item> <@user> <amount>` - Transfers the item to a user.")
                        .addParameter("@user", "User mention or name.")
                        .addParameter("item",
                                "The item emoji or name. If the name contains spaces \"wrap it in quotes\"")
                        .addParameter("amount", "" +
                                "The amount of items you want to transfer. This is optional.")
                        .build();
            }
        });

        cr.registerAlias("itemtransfer", "transferitems");
        cr.registerAlias("itemtransfer", "transferitem");
    }
}
