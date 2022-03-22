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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.utils.RoundedMetricPrefixFormat;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PlayerStats;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.security.SecureRandom;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Module
public class GambleCmds {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int SLOTS_MAX_MONEY = 50_000;
    private static final int TICKETS_MAX_AMOUNT = 100; // Technically ~8,000 credits.
    private static final long GAMBLE_ABSOLUTE_MAX_MONEY = Integer.MAX_VALUE;
    private static final long GAMBLE_MAX_MONEY = 10_000;

    private static final ThreadLocal<NumberFormat> PERCENT_FORMAT = ThreadLocal.withInitial(() -> {
        final NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1); // decimal support
        return format;
    });

    private static final IncreasingRateLimiter gambleRatelimiter = new IncreasingRateLimiter.Builder()
            .spamTolerance(3)
            .limit(1)
            .cooldown(2, TimeUnit.MINUTES)
            .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
            .maxCooldown(10, TimeUnit.MINUTES)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("gamble")
            .premiumAware(true)
            .build();
    private static final IncreasingRateLimiter slotsRatelimiter = new IncreasingRateLimiter.Builder()
            .spamTolerance(4)
            .limit(1)
            .cooldown(1, TimeUnit.MINUTES)
            .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
            .maxCooldown(5, TimeUnit.MINUTES)
            .pool(MantaroData.getDefaultJedisPool())
            .premiumAware(true)
            .prefix("slots")
            .build();

    private static final String[] emotes = {"\uD83C\uDF52", "\uD83D\uDCB0", "\uD83D\uDCB2", "\uD83E\uDD55", "\uD83C\uDF7F", "\uD83C\uDF75", "\uD83C\uDFB6"};
    private static final List<String> winCombinations = new ArrayList<>();

    static {
        for (String emote : emotes) {
            winCombinations.add(emote + emote + emote);
        }
    }

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Gamble.class);
        cr.registerSlash(Slots.class);
    }

    @Name("gamble")
    @Description("Gambles your money away. It's like Vegas, but without the impending doom.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "amount", description = "The amount to gamble. Can be a number or all, half, quarter or a percentage.", required = true)
    })
    @Help(
            description = "Gambles your money away. It's like Vegas, but without real money and without the impending doom. Kinda.",
            usage = "/gamble [amount] - amount can be all, half, quarter, an amount of money, or a percentage of your money.",
            parameters = {
                    // Can't use a text block here, because it doesn't let me use String#formatted on annotation values?
                    @Help.Parameter(
                            name = "amount",
                            description = "How much money you want to gamble. This can be either all (all your money), half, quarter, a percentage or an amount of money.\n" +
                                    "You can also express this on K (10k is 10000, for example). The maximum amount you can gamble at once is" + GAMBLE_MAX_MONEY + "credits."
                    )
            }
    )
    public static class Gamble extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            doGamble(ctx, ctx.getPlayer(), ctx.getOptionAsString("amount"));
        }
    }

    @Name("slots")
    @Description("Rolls the slot machine.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "credits", description = "The amount of credits to put. Default is 50 if not specifed."),
            @Options.Option(type = OptionType.BOOLEAN, name = "useticket", description = "Whether to use a ticket. False by default"),
            @Options.Option(type = OptionType.INTEGER, name = "ticketamount", description = "The amount of tickets to put. Only works if credits isn't specified.")
    })
    @Help(
            description = """
                        Rolls the slot machine. Requires a default of 50 credits to roll.
                        To win, you need to hit all 3 emojis of the same type on the middle row.
                        You can gain anywhere from ~15% to 175% of the money you put in. This is what you can *gain*, if you win you won't lose what you put in.
                        """,
            usage = """
                    `/slots` - Default one, 50 credits.
                    `/slots [credits]` - Puts x credits on the slot machine. You can put a maximum of 50,000 credits.
                    /slots [useticket] [ticketamount]` - Rolls the slot machine with the specified slot coins.
                    Using tickets increases your chance by 6 to 12%. Maximum amount of tickets allowed is 100.
                    """,
            parameters = {
                    @Help.Parameter(name = "credits", description = "The amount of credits to put on the slot machine. You can also express this on K (10k is 10000, for example)", optional = true),
                    @Help.Parameter(name = "useticket", description = "Whether to use a ticket. False by default. If you specify this, the credit amount will be ignored.", optional = true),
                    @Help.Parameter(name = "ticketamount", description = "The amount of credits to put on the slot machine. You can also express this on K (10k is 10000, for example)", optional = true)
            }
    )
    public static class Slots extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var player = ctx.getPlayer();
            var stats = ctx.db().getPlayerStats(ctx.getAuthor());
            var moneyAmount = 50L;

            var money = ctx.getOptionAsString("credits", "50"); //Since it can be expressed in M or K.
            var coinSelect = ctx.getOptionAsBoolean("ticket");
            var ticketAmount = ctx.getOptionAsLong("ticketamount", 1);

            // No need to parse if we aren't gonna use it.
            if (!coinSelect && !money.equals("50")) {
                try {
                    var parsed = new RoundedMetricPrefixFormat().parseObject(money, new ParsePosition(0));
                    if (parsed == null) {
                        ctx.sendLocalized("commands.slots.errors.no_valid_amount", EmoteReference.ERROR);
                        return;
                    }

                    moneyAmount = Math.abs(parsed);
                } catch (NumberFormatException e) {
                    ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR);
                    return;
                }
            }

            slots(ctx, player, stats, moneyAmount, ticketAmount, coinSelect);
        }
    }

    @Subscribe
    public void gamble(CommandRegistry cr) {
        cr.register("gamble", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                doGamble(ctx, ctx.getPlayer(), content);
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

        cr.registerAlias("gamble", "bet");
    }

    @Subscribe
    public void slots(CommandRegistry cr) {
        cr.register("slots", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var opts = ctx.getOptionalArguments();
                var money = 50L;
                var coinSelect = false;
                var coinAmount = 1;

                var player = ctx.getPlayer();
                var stats = ctx.getPlayerStats(ctx.getAuthor());

                var playerInventory = player.getInventory();
                if (opts.containsKey("useticket")) {
                    if (!playerInventory.containsItem(ItemReference.SLOT_COIN)) {
                        ctx.sendLocalized("commands.slots.errors.no_tickets", EmoteReference.SAD);
                        return;
                    }

                    coinSelect = true;
                }

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
                        coinAmount = Math.abs(Integer.parseInt(amount));
                    } catch (NumberFormatException e) {
                        ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR);
                        return;
                    }
                }

                if (args.length >= 1 && !coinSelect) {
                    try {
                        var parsed = new RoundedMetricPrefixFormat().parseObject(args[0], new ParsePosition(0));
                        if (parsed == null) {
                            ctx.sendLocalized("commands.slots.errors.no_valid_amount", EmoteReference.ERROR);
                            return;
                        }

                        money = Math.abs(parsed);
                    } catch (NumberFormatException e) {
                        ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR);
                        return;
                    }
                }

                slots(ctx, player, stats, money, coinAmount, coinSelect);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Rolls the slot machine. Requires a default of 50 credits to roll.
                                To win, you need to hit all 3 emojis of the same type on the middle row.
                                You can gain anywhere from ~15% to 175% of the money you put in. This is what you can *gain*, if you win you won't lose what you put in.
                                """
                        ).setUsage(
                                """
                                `~>slots` - Default one, 50 credits.
                                `~>slots <credits>` - Puts x credits on the slot machine. You can put a maximum of 50,000 credits.
                                `~>slots -useticket` - Rolls the slot machine with one slot coin.
                                You can specify the amount of tickets to use using `-amount` (for example `~>slots -useticket -amount 10`).
                                Using tickets increases your chance by 6 to 12%. Maximum amount of tickets allowed is 100.
                                """
                        ).build();
            }
        });
    }

    private static void slots(IContext ctx, Player player, PlayerStats stats, long money, long coinAmount, boolean ticket) {
        var playerInventory = player.getInventory();
        var slotsChance = 25; //25% raw chance of winning, completely random chance of winning on the other random iteration
        var isWin = false;
        var coinSelect = false;

        if (ticket) {
            if (coinAmount > TICKETS_MAX_AMOUNT) {
                ctx.sendLocalized("commands.slots.errors.too_many_tickets", EmoteReference.ERROR, TICKETS_MAX_AMOUNT);
                return;
            }

            if (!playerInventory.containsItem(ItemReference.SLOT_COIN)) {
                ctx.sendLocalized("commands.slots.errors.no_tickets", EmoteReference.SAD);
                return;
            }

            if (playerInventory.getAmount(ItemReference.SLOT_COIN) < coinAmount) {
                ctx.sendLocalized("commands.slots.errors.not_enough_tickets", EmoteReference.ERROR);
                return;
            }

            coinSelect = true;
        }

        if (money < 25) {
            ctx.sendLocalized("commands.slots.errors.below_minimum", EmoteReference.ERROR);
            return;
        }

        if (money > SLOTS_MAX_MONEY) {
            ctx.sendLocalized("commands.slots.errors.too_much_money", EmoteReference.WARNING);
            return;
        }

        var playerMoney = player.getCurrentMoney();
        if (playerMoney < money && !coinSelect) {
            ctx.sendLocalized("commands.slots.errors.not_enough_money", EmoteReference.SAD);
            return;
        }

        if (!RatelimitUtils.ratelimit(slotsRatelimiter, ctx)) {
            return;
        }

        if (coinSelect) {
            // Substract slot tickets.
            playerInventory.process(new ItemStack(ItemReference.SLOT_COIN, (int) -coinAmount));
            slotsChance = slotsChance + Math.max(6, secureRandom.nextInt(12) + 1);
            money = 70L * coinAmount;
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

            builder.append(emotes[secureRandom.nextInt(emotes.length)]);
        }

        var toSend = builder.toString();
        var gains = 0;
        var rows = toSend.split("\\r?\\n");

        var chance = secureRandom.nextInt(100);
        if (chance < slotsChance) {
            rows[1] = winCombinations.get(secureRandom.nextInt(winCombinations.size()));
        }

        if (winCombinations.contains(rows[1])) {
            isWin = true;
            var maxGains = secureRandom.nextInt((int) Math.round(money * 1.76)) + 16;
            gains = (int) Math.max(money / 6, maxGains);
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

            player.addMoney(gains);
            player.save();
        } else {
            stats.getData().incrementSlotsLose();
            message.append(toSend).append("\n\n").append(
                    languageContext.withRoot("commands", "slots.lose").formatted(EmoteReference.SAD)
            );

            if (!coinSelect) { // We already substracted the gained amount, in slot tickets.
                player.removeMoney(money);
            }

            // We need to save anyway.
            player.save();
        }

        stats.saveUpdating();

        message.append("\n");
        ctx.send(message.toString());
    }

    private static void doGamble(IContext ctx, Player player, String amount) {
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
            switch (amount) {
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
                    i = amount.endsWith("%")
                            ? Math.round(PERCENT_FORMAT.get().parse(amount).doubleValue() * player.getCurrentMoney())
                            : new RoundedMetricPrefixFormat().parseObject(amount, new ParsePosition(0));

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

        // Handle ratelimits after all of the exceptions/error messages could've been thrown already.
        if (!RatelimitUtils.ratelimit(gambleRatelimiter, ctx)) {
            return;
        }

        var gains = (long) (i * multiplier);
        gains = Math.round(gains * 0.45);
        // Get the player again, to make sure the entry is not stale.
        proceedGamble(ctx, ctx.getPlayer(), luck, i, gains, i);
    }


    private static void proceedGamble(IContext ctx, Player player, int luck, long i, long gains, long bet) {
        var stats = MantaroData.db().getPlayerStats(ctx.getMember());
        var data = player.getData();
        final SecureRandom random = new SecureRandom();

        if (luck > random.nextInt(140)) {
            if (player.addMoney(gains)) {
                if (gains >= 4_950L) {
                    if (!data.hasBadge(Badge.GAMBLER)) {
                        data.addBadgeIfAbsent(Badge.GAMBLER);
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
