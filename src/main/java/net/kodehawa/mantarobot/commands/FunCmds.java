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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RPGDice;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Module
public class FunCmds {
    private static final SecureRandom r = new SecureRandom();
    private static final IncreasingRateLimiter rollRateLimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(2)
            .cooldown(5, TimeUnit.SECONDS)
            .maxCooldown(1, TimeUnit.MINUTES)
            .randomIncrement(true)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("roll")
            .build();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(CoinFlip.class);
        cr.registerSlash(RateWaifu.class);
        cr.registerSlash(Roll.class);
        cr.registerSlash(Love.class);
    }

    @Name("coinflip")
    @Description("Flips a coin with a defined number of repetitions.")
    @Category(CommandCategory.FUN)
    @Options({
            @Options.Option(type = OptionType.INTEGER, name = "times", description = "The amount of times to flip the coin.", maxValue = 100)
    })
    @Help(description = "Flips a coin with a defined number of repetitions.", usage = "`/coinflip [times]` - Flips a coin x number of times",
            parameters = {
                @Help.Parameter(name = "times", description = "The amount of times to flip the coin.", optional = true)
            })
    public static class CoinFlip extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var times = ctx.getOptionAsLong("times", 1);
            final int[] heads = {0};
            final int[] tails = {0};

            doTimes((int) times, () -> {
                if (r.nextBoolean()) heads[0]++;
                else tails[0]++;
            });

            ctx.reply("commands.coinflip.success", EmoteReference.PENNY, times, heads[0], tails[0]);
        }
    }

    @Name("ratewaifu")
    @Description("Rates your waifu from zero to 100. Results may vary.")
    @Category(CommandCategory.FUN)
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to rate.", required = true)
    })
    @Help(description = "Rates your waifu from zero to 100. Results may vary.", usage = "/ratewaifu <user>", parameters = {
            @Help.Parameter(name = "user", description = "The user to rate.")
    })
    public static class RateWaifu extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var user = ctx.getOptionAsUser("user").getId();
            var waifuRate = user.chars().sum() % 101;

            //hehe~
            if (user.equalsIgnoreCase("213466096718708737")) {
                waifuRate = 100;
            }

            ctx.reply("commands.ratewaifu.success", EmoteReference.THINKING, waifuRate);
        }
    }

    @Name("roll")
    @Description("Roll a any-sided dice a 1 or more times. (Default is 6-sided)")
    @Category(CommandCategory.FUN)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "times", description = "The amount of times to roll, in d20 format (1d20 is 1 die of 20 sides)")
    })
    @Help(
            description = """
                    Roll a any-sided dice a 1 or more times.
                    By default, this command will roll a 6-sized dice 1 time.
                    """,
            usage = """
                    `/roll [times]`: Rolls a dice of the specified size the specified times.
                    D20 Format: For this, 1d20 would be `~>roll -size 20 -amount 1` or just `1d20` (aka DND format)
                    """,
            parameters = {@Help.Parameter(name = "times", description = "The amount of times to roll, in d20 format")}
    )
    public static class Roll extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            if (!RatelimitUtils.ratelimit(rollRateLimiter, ctx)) {
                return;
            }

            int size = 6;
            int amount = 1;

            var times = ctx.getOptionAsString("times", "");
            if (!times.isBlank()) {
                var d20 = RPGDice.parse(times);
                if (d20 != null) {
                    size = d20.getFaces();
                    amount = d20.getRolls();
                } else {
                    ctx.reply("commands.roll.incorrect_format", EmoteReference.ERROR);
                    return;
                }

                if (amount >= 100) {
                    amount = 100;
                }
            }

            var result = diceRoll(size, amount);
            var sum = result.stream().mapToInt(Integer::intValue).sum();
            if (size == 6 && sum == 6) {
                var player = MantaroData.db().getPlayer(ctx.getAuthor());
                player.getData().addBadgeIfAbsent(Badge.LUCK_BEHIND);
                player.saveUpdating();
            }

            var sumString = result.stream().limit(10).map(Object::toString).collect(Collectors.joining(", "));
            if (result.size() > 10) {
                sumString += ctx.getLanguageContext().get("commands.roll.more_than_10").formatted(sum - 10);
            }
            sumString += " " + ctx.getLanguageContext().get("commands.roll.total").formatted(sum);

            ctx.reply("commands.roll.success",
                    EmoteReference.DICE,
                    amount > 1 ? sumString : sum,
                    ctx.getLanguageContext().get("commands.roll.size").formatted(amount, size)
            );

            TextChannelGround.of(ctx.getChannel()).dropItemWithChance(ItemReference.LOADED_DICE, 5);
        }
    }

    @Name("love")
    @Description("Calculates the love between 2 discord users. Results may vary.")
    @Category(CommandCategory.FUN)
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to compare with.", required = true)
    })
    @Help(description = "Calculates the love between 2 discord users. Results may vary. This is random, for real.", usage = "/love [user]",
    parameters = {
            @Help.Parameter(name = "user", description = "The user to compare with.")
    })
    public static class Love extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var user = ctx.getOptionAsUser("user");
            String result;
            long[] ids = new long[2];
            List<String> listDisplay = new ArrayList<>();
            String toDisplay;

            listDisplay.add("\uD83D\uDC97  %s#%s".formatted(user.getName(), user.getDiscriminator()));
            listDisplay.add("\uD83D\uDC97  %s#%s".formatted(ctx.getAuthor().getName(), ctx.getAuthor().getDiscriminator()));
            toDisplay = String.join("\n", listDisplay);

            ids[0] = ctx.getAuthor().getIdLong();
            ids[1] = user.getIdLong();
            var percentage = (ids[0] == ids[1] ? 101 : r.nextInt(101)); // last value is exclusive, so 101.
            var languageContext = ctx.getLanguageContext();

            final var marriage = ctx.getMarriage(ctx.getDBUser().getData());
            if (marriage != null) {
                final var other = marriage.getOtherPlayer(ctx.getAuthor().getId());
                if (other.equals(user.getId())) {
                    percentage = 100;
                }
            }

            if (percentage < 45) {
                result = languageContext.get("commands.love.not_ideal");
            } else if (percentage < 75) {
                result = languageContext.get("commands.love.decent");
            } else if (percentage < 100) {
                result = languageContext.get("commands.love.nice");
            } else {
                result = languageContext.get("commands.love.perfect");
                if (percentage == 101) {
                    result = languageContext.get("commands.love.yourself_note");
                }
            }

            var loveEmbed = new EmbedBuilder()
                    .setAuthor("\u2764 " + languageContext.get("commands.love.header") + " \u2764", null,
                            ctx.getAuthor().getEffectiveAvatarUrl())
                    .setThumbnail(ctx.getAuthor().getEffectiveAvatarUrl())
                    .setDescription("\n**" + toDisplay + "**\n\n" +
                            percentage + "% **\\|\\|**  " +
                            Utils.bar(percentage, 30) + "  **\\|\\|** \n\n" +
                            "**" + languageContext.get("commands.love.result") + "** " + result
                    ).setColor(ctx.getMember().getColor())
                    .build();

            ctx.reply(loveEmbed);
        }
    }

    private static void doTimes(int times, Runnable runnable) {
        for (int i = 0; i < times; i++) runnable.run();
    }

    private static List<Integer> diceRoll(int size, int amount) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            list.add(r.nextInt(size) + 1);
        }

        return list;
    }
}
