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
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.commands.utils.RoundedMetricPrefixFormat;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.security.SecureRandom;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Module
public class GambleCmds {
    private static final int SLOTS_MAX_MONEY = 50_000;
    private static final int TICKETS_MAX_AMOUNT = 50;
    private static final long GAMBLE_ABSOLUTE_MAX_MONEY = Integer.MAX_VALUE;
    private static final long GAMBLE_MAX_MONEY = 10_000;

    private static final ThreadLocal<NumberFormat> PERCENT_FORMAT = ThreadLocal.withInitial(() -> {
        final NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1); // decimal support
        return format;
    });

    @Subscribe
    public void gamble(CommandRegistry cr) {
        cr.register("gamble", new SimpleCommand(CommandCategory.CURRENCY) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .spamTolerance(3)
                    .limit(1)
                    .cooldown(2, TimeUnit.MINUTES)
                    .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                    .maxCooldown(10, TimeUnit.MINUTES)
                    .pool(MantaroData.getDefaultJedisPool())
                    .prefix("gamble")
                    .premiumAware(true)
                    .build();

            final SecureRandom secureRandom = new SecureRandom();

            @Override
            public void call(Context ctx, String content, String[] args) {
                var player = ctx.getPlayer();

                if (player.getCurrentMoney() <= 0) {
                    ctx.sendLocalized("commands.gamble.no_credits", EmoteReference.SAD);
                    return;
                }

                if (player.getCurrentMoney() > GAMBLE_ABSOLUTE_MAX_MONEY) {
                    ctx.sendLocalized("commands.gamble.too_much_money", EmoteReference.ERROR2, GAMBLE_ABSOLUTE_MAX_MONEY);
                    return;
                }

                double multiplier;
                long i;
                int luck;
                try {
                    switch (content) {
                        case "all", "everything" -> {
                            i = player.getCurrentMoney();
                            multiplier = 1.3d + (secureRandom.nextInt(1350) / 1000d);
                            luck = 19 + (int) (multiplier * 13) + secureRandom.nextInt(18);
                        }
                        case "half" -> {
                            i = player.getCurrentMoney() == 1 ? 1 : player.getCurrentMoney() / 2;

                            multiplier = 1.2d + (secureRandom.nextInt(1350) / 1000d);
                            luck = 18 + (int) (multiplier * 13) + secureRandom.nextInt(18);
                        }
                        case "quarter" -> {
                            i = player.getCurrentMoney() == 1 ? 1 : player.getCurrentMoney() / 4;

                            multiplier = 1.1d + (secureRandom.nextInt(1250) / 1000d);
                            luck = 18 + (int) (multiplier * 12) + secureRandom.nextInt(18);
                        }
                        default -> {
                            i = content.endsWith("%")
                                    ? Math.round(PERCENT_FORMAT.get().parse(content).doubleValue() * player.getCurrentMoney())
                                    : new RoundedMetricPrefixFormat().parseObject(content, new ParsePosition(0));

                            if (i > player.getCurrentMoney() || i < 0) {
                                throw new UnsupportedOperationException();
                            }

                            multiplier = 1.1d + (i / ((double) player.getCurrentMoney()) * secureRandom.nextInt(1300) / 1000d);
                            luck = 17 + (int) (multiplier * 13) + secureRandom.nextInt(12);
                        }
                    }
                } catch (NumberFormatException | NullPointerException e) {
                    ctx.sendLocalized("commands.gamble.invalid_money_or_modifier", EmoteReference.ERROR);
                    return;
                } catch (UnsupportedOperationException e) {
                    ctx.sendLocalized("commands.gamble.not_enough_money", EmoteReference.ERROR2);
                    return;
                } catch (ParseException e) {
                    ctx.sendLocalized("commands.gamble.invalid_percentage", EmoteReference.ERROR2);
                    return;
                }

                if (i < 100) {
                    ctx.sendLocalized("commands.gamble.too_little", EmoteReference.ERROR2);
                    return;
                }

                if (i > GAMBLE_MAX_MONEY) {
                    ctx.sendLocalized("commands.gamble.too_much", EmoteReference.ERROR2, GAMBLE_MAX_MONEY);
                    return;
                }

                //Handle ratelimits after all of the exceptions/error messages could've been thrown already.
                if (!RatelimitUtils.ratelimit(rateLimiter, ctx)) {
                    return;
                }

                var gains = (long) (i * multiplier);
                gains = Math.round(gains * 0.45);
                // Get the player again, to make sure the entry is not stale.
                proceedGamble(ctx, ctx.getPlayer(), luck, i, gains, i);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Gambles your money away. It's like Vegas, but without real money and without the impending doom. Kinda.")
                        .setUsage("`~>gamble <all/half/quarter>` or `~>gamble <amount>` or `~>gamble <percentage>`")
                        .addParameter("amount", "How much money you want to gamble. " +
                                "You can also express this on K (10k is 10000, for example). The maximum amount you can gamble at once is " + GAMBLE_MAX_MONEY + " credits.")
                        .addParameter("all/half/quarter",
                                "How much of your money you want to gamble, but if you're too lazy to type the number (half = 50% of all of your money)")
                        .addParameter("percentage", "The percentage of money you want to gamble. Works anywhere from 1% to 100%.")
                        .build();
            }
        });
    }

    @Subscribe
    public void slots(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .spamTolerance(4)
                .limit(1)
                .cooldown(1, TimeUnit.MINUTES)
                .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                .maxCooldown(5, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .premiumAware(true)
                .prefix("slots")
                .build();

        String[] emotes = {"\uD83C\uDF52", "\uD83D\uDCB0", "\uD83D\uDCB2", "\uD83E\uDD55", "\uD83C\uDF7F", "\uD83C\uDF75", "\uD83C\uDFB6"};
        Random random = new SecureRandom();
        List<String> winCombinations = new ArrayList<>();

        for (String emote : emotes) {
            winCombinations.add(emote + emote + emote);
        }

        cr.register("slots", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var opts = ctx.getOptionalArguments();

                var money = 50L;
                var slotsChance = 25; //25% raw chance of winning, completely random chance of winning on the other random iteration
                var isWin = false;
                var coinSelect = false;
                var coinAmount = 1;

                var player = ctx.getPlayer();
                var stats = ctx.db().getPlayerStats(ctx.getAuthor());

                SeasonPlayer seasonalPlayer = null; //yes
                var season = false;

                if (opts.containsKey("season")) {
                    season = true;
                    seasonalPlayer = ctx.getSeasonPlayer();
                }

                if (opts.containsKey("useticket")) {
                    coinSelect = true;
                }

                var playerInventory = season ? seasonalPlayer.getInventory() : player.getInventory();

                if (opts.containsKey("amount") && opts.get("amount") != null) {
                    if (!coinSelect) {
                        ctx.sendLocalized("commands.slots.errors.amount_not_ticket", EmoteReference.ERROR);
                        return;
                    }

                    var amount = opts.get("amount");

                    if (amount.isEmpty()) {
                        ctx.sendLocalized("commands.slots.errors.no_amount", EmoteReference.ERROR);
                        return;
                    }

                    try {
                        coinAmount = Integer.parseInt(amount);
                    } catch (NumberFormatException e) {
                        ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR);
                        return;
                    }

                    if (coinAmount > TICKETS_MAX_AMOUNT) {
                        ctx.sendLocalized("commands.slots.errors.too_many_tickets", EmoteReference.ERROR, TICKETS_MAX_AMOUNT);
                        return;
                    }

                    if (playerInventory.getAmount(ItemReference.SLOT_COIN) < coinAmount) {
                        ctx.sendLocalized("commands.slots.errors.not_enough_tickets", EmoteReference.ERROR);
                        return;
                    }

                    money += 58L * coinAmount;
                }

                if (args.length >= 1 && !coinSelect) {
                    try {
                        var parsed = new RoundedMetricPrefixFormat().parseObject(args[0], new ParsePosition(0));

                        if (parsed == null) {
                            ctx.sendLocalized("commands.slots.errors.no_valid_amount", EmoteReference.ERROR);
                            return;
                        }

                        money = Math.abs(parsed);

                        if (money < 25) {
                            ctx.sendLocalized("commands.slots.errors.below_minimum", EmoteReference.ERROR);
                            return;
                        }

                        if (money > SLOTS_MAX_MONEY) {
                            ctx.sendLocalized("commands.slots.errors.too_much_money", EmoteReference.WARNING);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR);
                        return;
                    }
                }

                var playerMoney = season ? seasonalPlayer.getMoney() : player.getCurrentMoney();

                if (playerMoney < money && !coinSelect) {
                    ctx.sendLocalized("commands.slots.errors.not_enough_money", EmoteReference.SAD);
                    return;
                }

                if (!RatelimitUtils.ratelimit(rateLimiter, ctx)) {
                    return;
                }

                if (coinSelect) {
                    if (playerInventory.containsItem(ItemReference.SLOT_COIN)) {
                        playerInventory.process(new ItemStack(ItemReference.SLOT_COIN, -coinAmount));
                        if (season)
                            seasonalPlayer.save();
                        else
                            player.save();

                        slotsChance = slotsChance + 10;
                    } else {
                        ctx.sendLocalized("commands.slots.errors.no_tickets", EmoteReference.SAD);
                        return;
                    }
                } else {
                    if (season) {
                        seasonalPlayer.removeMoney(money);
                        seasonalPlayer.saveAsync();
                    } else {
                        player.removeMoney(money);
                        player.saveUpdating();
                    }
                }

                var languageContext = ctx.getLanguageContext();

                var message = new StringBuilder(
                        languageContext.withRoot("commands", "slots.roll").formatted(
                                EmoteReference.DICE, coinSelect ? coinAmount + " " +
                                        languageContext.get("commands.slots.tickets") : money + " " +
                                        languageContext.get("commands.slots.credits"))
                );

                var builder = new StringBuilder();


                for (int i = 0; i < 9; i++) {
                    if (i > 1 && i % 3 == 0) {
                        builder.append("\n");
                    }

                    builder.append(emotes[random.nextInt(emotes.length)]);
                }

                var toSend = builder.toString();
                var gains = 0;
                var rows = toSend.split("\\r?\\n");

                if (random.nextInt(100) < slotsChance) {
                    rows[1] = winCombinations.get(random.nextInt(winCombinations.size()));
                }

                if (winCombinations.contains(rows[1])) {
                    isWin = true;
                    gains = random.nextInt((int) Math.round(money * 1.76)) + 16;
                }

                rows[1] = rows[1] + " \u2b05";
                toSend = String.join("\n", rows);

                if (isWin) {
                    message.append(toSend).append("\n\n")
                            .append(languageContext.withRoot("commands", "slots.win")
                                    .formatted(gains, money))
                            .append(EmoteReference.POPPER);

                    stats.incrementSlotsWins();
                    stats.addSlotsWin(gains);

                    if ((gains + money) > SLOTS_MAX_MONEY) {
                        player.getData().addBadgeIfAbsent(Badge.LUCKY_SEVEN);
                    }

                    if (coinSelect && coinAmount > 45) {
                        player.getData().addBadgeIfAbsent(Badge.SENSELESS_HOARDING);
                    }

                    if (season) {
                        seasonalPlayer.addMoney(gains + money);
                        seasonalPlayer.saveUpdating();
                    } else {
                        player.addMoney(gains + money);
                        player.saveUpdating();
                    }
                } else {
                    stats.getData().incrementSlotsLose();
                    message.append(toSend).append("\n\n").append(
                            languageContext.withRoot("commands", "slots.lose").formatted(EmoteReference.SAD)
                    );
                }

                stats.saveUpdating();

                message.append("\n");
                ctx.send(message.toString());
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Rolls the slot machine. Requires a default of 50 coins to roll.")
                        .setUsage(
                                """
                                `~>slots` - Default one, 50 coins.
                                `~>slots <credits>` - Puts x credits on the slot machine. You can put a maximum of 50,000 coins.
                                `~>slots -useticket` - Rolls the slot machine with one slot coin.
                                You can specify the amount of tickets to use using `-amount` (for example `~>slots -useticket -amount 10`).
                                Using tickets increases your chance by 10%. Maximum amount of tickets allowed is 50.
                                """
                        ).build();
            }
        });
    }

    private void proceedGamble(Context ctx, Player player, int luck, long i, long gains, long bet) {
        var stats = MantaroData.db().getPlayerStats(ctx.getMember());
        var data = player.getData();
        final SecureRandom random = new SecureRandom();

        if (luck > random.nextInt(140)) {
            if (player.addMoney(gains)) {
                if (gains >= 4_950L) {
                    if (!data.hasBadge(Badge.GAMBLER)) {
                        data.addBadgeIfAbsent(Badge.GAMBLER);
                        player.saveUpdating();
                    }
                }

                stats.incrementGambleWins();
                stats.addGambleWin(gains);

                ctx.sendLocalized("commands.gamble.win", EmoteReference.DICE, gains);
            } else {
                ctx.sendLocalized("commands.gamble.win_overflow", EmoteReference.DICE, gains);
            }
        } else {
            if (bet == GAMBLE_MAX_MONEY) {
                data.addBadgeIfAbsent(Badge.RISKY_ORDEAL);
            }

            var oldMoney = player.getCurrentMoney();
            player.setCurrentMoney(Math.max(0, player.getCurrentMoney() - i));

            stats.getData().incrementGambleLose();
            ctx.sendLocalized("commands.gamble.lose", EmoteReference.DICE,
                    (player.getCurrentMoney() == 0 ? ctx.getLanguageContext().get("commands.gamble.lose_all") + " " + oldMoney : i),
                    EmoteReference.SAD
            );
        }

        player.setLocked(false);
        player.saveUpdating();
        stats.saveUpdating();
    }
}
