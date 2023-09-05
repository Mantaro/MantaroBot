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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Defer;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
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
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.campaign.Campaign;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Module
public class MoneyCmds {
    private static final long DAILY_VALID_PERIOD_MILLIS = MantaroData.config().get().getDailyMaxPeriodMilliseconds();
    private static final IncreasingRateLimiter dailyRateLimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .cooldown(24, TimeUnit.HOURS)
            .maxCooldown(24, TimeUnit.HOURS)
            .randomIncrement(false)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("daily")
            .build();
    private static final IncreasingRateLimiter lootRateLimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(2)
            .cooldown(3, TimeUnit.MINUTES)
            .maxCooldown(3, TimeUnit.MINUTES)
            .randomIncrement(false)
            .premiumAware(true)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("loot")
            .build();

    private static final SecureRandom random = new SecureRandom();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Daily.class);
        cr.registerSlash(Loot.class);
        cr.registerSlash(Balance.class);
    }

    @Name("daily")
    @Defer
    @Description("Gives you some credits per day and a reward for claiming it everyday.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "User to give it to (optional)"),
            @Options.Option(type = OptionType.BOOLEAN, name = "check", description = "Whether to check if your daily is ready (optional)")
    })
    @Help(
            description = """
                    Gives you $150 credits per day (or between 150 and 180 if you transfer it to another person). Maximum amount it can give is ~2000 credits (a bit more for shared dailies)
                    This command gives a reward for claiming it every day (daily streak). You lose the streak if you miss two days in a row.
                    """,
            parameters = {
                    @Help.Parameter(name = "user", description = "The user to give your daily to, without this it gives it to yourself.", optional = true),
                    @Help.Parameter(name = "check", description = "Whether you want to check if you can claim your daily", optional = true)
            },
            usage = "`/daily user:[user] check:[true/false]`"
    )
    public static class Daily extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            daily(ctx, ctx.getOptionAsUser("user"), ctx.getOptionAsBoolean("check"));
        }
    }

    @Name("loot")
    @Defer
    @Description("Loot the current chat for random items.")
    @Category(CommandCategory.CURRENCY)
    @Help(description =
            "Loot the current chat for items, for usage in Mantaro's currency system. " +
                    "You have a random chance of getting collectible items from here."
    )
    public static class Loot extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            loot(ctx);
        }
    }

    @Name("balance")
    @Description("Shows your current balance or another person's balance.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to check the balance of.")
    })
    @Help(
            description = "Shows your current balance or another person's balance",
            usage = "`/balance user:[user]`",
            parameters = { @Help.Parameter(name = "user", description = "The user to check the balance of.", optional = true) }
    )
    public static class Balance extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            balance(ctx, ctx.getOptionAsUser("user"));
        }
    }

    // Old command system start
    @Subscribe
    public void daily(CommandRegistry cr) {
        cr.register("daily", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                final var mentionedUsers = ctx.getMentionedUsers();
                final boolean targetOther = !mentionedUsers.isEmpty();
                User toGive = null;
                if (targetOther) {
                    toGive = mentionedUsers.get(0);
                }

                boolean check = args.length > 0 && ctx.getMentionedUsers().isEmpty() && args[0].equalsIgnoreCase("-check");
                daily(ctx, toGive, check);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Gives you $150 credits per day (or between 150 and 180 if you transfer it to another person). " +
                                "Maximum amount it can give is ~2000 credits (a bit more for shared dailies)\n" +
                                "This command gives a reward for claiming it every day (daily streak)")
                        .setUsage("`~>daily [@user] [-check]`")
                        .addParameterOptional("@user", "The user to give your daily to, without this it gives it to yourself.")
                        .addParameterOptional("-check", "Check the time left for you to be able to claim it.")
                        .build();
            }
        });

        cr.registerAlias("daily", "dailies");
    }

    @Subscribe
    public void loot(CommandRegistry cr) {
        cr.register("loot", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                loot(ctx);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Loot the current chat for items, for usage in Mantaro's currency system. " +
                                "You have a random chance of getting collectible items from here.")
                        .build();
            }
        });
    }


    @Subscribe
    public void balance(CommandRegistry cr) {
        cr.register("balance", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                // Values on lambdas should be final or effectively final part 9999.
                final var finalContent = content;
                ctx.findMember(content, members -> {
                    var user = ctx.getAuthor();
                    boolean isExternal = false;

                    var found = CustomFinderUtil.findMemberDefault(finalContent, members, ctx, ctx.getMember());
                    if (found == null) {
                        return;
                    } else if (!finalContent.isEmpty()) {
                        user = found.getUser();
                        isExternal = true;
                    }

                    balance(ctx, isExternal ? user : null);
                });
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shows your current balance or another person's balance.")
                        .setUsage("`~>balance [@user]`")
                        .addParameter("@user", "The user to check the balance of. This is optional.")
                        .setSeasonal(true)
                        .build();
            }
        });

        cr.registerAlias("balance", "credits");
        cr.registerAlias("balance", "bal");
    }
    // Old command system end

    private static void daily(IContext ctx, User toGive, boolean check) {
        final var languageContext = ctx.getLanguageContext();
        if (check) {
            long rl = dailyRateLimiter.getRemaniningCooldown(ctx.getAuthor());

            ctx.sendLocalized("commands.daily.check", EmoteReference.TALKING,
                    (rl) > 0 ? Utils.formatDuration(ctx.getLanguageContext(), rl) :
                            languageContext.get("commands.daily.about_now")
            );
            return;
        }

        // Determine who gets the money
        var dailyMoney = 350L;

        final var author = ctx.getAuthor();
        var authorPlayer = ctx.getPlayer();
        final var authorDBUser = ctx.getDBUser();
        if (authorPlayer.isLocked()) {
            ctx.sendLocalized("commands.daily.errors.own_locked");
            return;
        }

        Player toAddMoneyTo = ctx.getPlayer(author);
        User otherUser = null;

        boolean targetOther = toGive != null;
        if (targetOther) {
            otherUser = toGive;
            // Bot check mentioned authorDBUser
            if (otherUser.isBot()){
                ctx.sendLocalized("commands.daily.errors.bot", EmoteReference.ERROR);
                return;
            }

            if (otherUser.getIdLong() == author.getIdLong()) {
                ctx.sendLocalized("commands.daily.errors.same_user", EmoteReference.ERROR);
                return;
            }

            var playerOtherUser = ctx.getPlayer(otherUser);
            if (playerOtherUser.isLocked()){
                ctx.sendLocalized("commands.daily.errors.receipt_locked");
                return;
            }

            if (ctx.getMantaroData().getBlackListedUsers().contains(otherUser.getId())) {
                ctx.sendLocalized("commands.transfer.blacklisted_transfer", EmoteReference.ERROR);
                return;
            }

            // Why this is here I have no clue;;;
            dailyMoney += random.nextInt(120);

            var mentionedDBUser = ctx.getDBUser(otherUser);
            // Marriage bonus
            var marriage = authorDBUser.getMarriage();
            if (marriage != null && otherUser.getId().equals(marriage.getOtherPlayer(ctx.getAuthor().getId())) &&
                    playerOtherUser.containsItem(ItemReference.RING)) {
                dailyMoney += Math.max(10, random.nextInt(200));
            }

            // Mutual waifu status.
            if (authorDBUser.containsWaifu(otherUser.getId()) && mentionedDBUser.containsWaifu(author.getId())) {
                dailyMoney +=Math.max(5, random.nextInt(200));
            }

            toAddMoneyTo = ctx.getPlayer(otherUser);
        } else {
            // This is here so you don't overwrite yourself....
            authorPlayer = toAddMoneyTo;
        }

        // Check for rate limit
        if (!RatelimitUtils.ratelimit(dailyRateLimiter, ctx, languageContext.get("commands.daily.ratelimit_message"), false))
            return;

        List<String> returnMessage = new ArrayList<>();
        final long currentTime = System.currentTimeMillis();
        final int amountStreakSavers = authorPlayer.getItemAmount(ItemReference.MAGIC_WATCH);

        // >=0 -> Valid  <0 -> Invalid
        final long currentDailyOffset = DAILY_VALID_PERIOD_MILLIS - (currentTime - authorPlayer.getLastDailyAt()) ;

        long streak = authorPlayer.getDailyStreak();
        // Not expired?
        if (currentDailyOffset + amountStreakSavers * DAILY_VALID_PERIOD_MILLIS >= 0) {
            streak++;
            if (targetOther)
                returnMessage.add(languageContext.get("commands.daily.streak.given.up").formatted(streak));
            else
                returnMessage.add(languageContext.get("commands.daily.streak.up").formatted(streak));
            if (currentDailyOffset < 0){
                int streakSaversUsed = -1 * (int) Math.floor((double) currentDailyOffset / (double) DAILY_VALID_PERIOD_MILLIS);
                authorPlayer.processItem(ItemReference.MAGIC_WATCH, streakSaversUsed * -1);
                returnMessage.add(languageContext.get("commands.daily.streak.watch_used").formatted(
                        streakSaversUsed, streakSaversUsed + 1,
                        amountStreakSavers - streakSaversUsed)
                );
            }
        } else {
            if (streak == 0) {
                returnMessage.add(languageContext.get("commands.daily.streak.first_time"));
            } else {
                if (amountStreakSavers > 0){
                    returnMessage.add(
                            languageContext.get("commands.daily.streak.lost_streak.watch").formatted(streak)
                    );

                    authorPlayer.processItem(ItemReference.MAGIC_WATCH, authorPlayer.getItemAmount(ItemReference.MAGIC_WATCH) * -1);
                } else {
                    returnMessage.add(languageContext.get("commands.daily.streak.lost_streak.normal").formatted(streak));
                }
            }
            streak = 1;
        }

        if (streak > 5) {
            // Bonus money
            int bonus = 250;

            if (streak % 50 == 0){
                authorPlayer.processItem(ItemReference.MAGIC_WATCH,1);
                returnMessage.add(languageContext.get("commands.daily.watch_get"));
            }

            if (streak > 10) {
                authorPlayer.addBadgeIfAbsent(Badge.CLAIMER);

                if (streak % 20 == 0 && authorPlayer.canFitItem(ItemReference.LOOT_CRATE)) {
                    authorPlayer.processItem(ItemReference.LOOT_CRATE, 1);
                    returnMessage.add(languageContext.get("commands.daily.crate"));
                }

                if (streak > 15){
                    bonus += (int) Math.min(targetOther ? 2000 : 1000, Math.floor(200 * streak / (targetOther ? 10D : 15D)));

                    if (streak >= 180) {
                        authorPlayer.addBadgeIfAbsent(Badge.BIG_CLAIMER);
                    }

                    if (streak >= 365) {
                        authorPlayer.addBadgeIfAbsent(Badge.YEARLY_CLAIMER);
                    }

                    if (streak >= 730) {
                        authorPlayer.addBadgeIfAbsent(Badge.BI_YEARLY_CLAIMER);
                    }
                }
            }

            if (targetOther) {
                returnMessage.add(languageContext.get("commands.daily.streak.given.bonus").formatted(otherUser.getName(), bonus));
            } else {
                returnMessage.add(languageContext.get("commands.daily.streak.bonus").formatted(bonus));
            }

            dailyMoney += bonus;
        }

        // If the author is premium, make daily double.
        if (authorDBUser.isPremium()) {
            dailyMoney *=2;
        }

        // Sellout + this is always a day apart, so we can just send campaign.
        if (random.nextBoolean()) {
            returnMessage.add(Campaign.TWITTER.getStringFromCampaign(languageContext, true));
        } else {
            returnMessage.add(Campaign.PREMIUM_DAILY.getStringFromCampaign(languageContext, authorDBUser.isPremium()));
        }

        // Careful not to overwrite yourself ;P
        // Save streak and items
        authorPlayer.lastDailyAt(currentTime);
        authorPlayer.dailyStreak(streak);

        // Critical not to call if author != mentioned because in this case
        // toAdd is the unified player as referenced
        if (targetOther) {
            authorPlayer.updateAllChanged();
        }

        toAddMoneyTo.addMoney(dailyMoney);
        toAddMoneyTo.updateAllChanged();

        // Build Message
        var toSend = new StringBuilder();
        if (targetOther) {
            toSend.append(languageContext.get("commands.daily.given_credits")
                            .formatted(EmoteReference.CORRECT, dailyMoney, ((ctx instanceof SlashContext) ? otherUser.getAsMention() : otherUser.getName()))
            ).append("\n");
        } else {
            toSend.append(languageContext.get("commands.daily.credits").formatted(EmoteReference.CORRECT, dailyMoney)).append("\n");
        }

        for (var string : returnMessage) {
            toSend.append("\n").append(string);
        }

        // Send Message
        ctx.send(toSend.toString());
    }

    private static void loot(IContext ctx) {
        var player = ctx.getPlayer();
        var dbUser = ctx.getDBUser();
        var languageContext = ctx.getLanguageContext();
        if (player.isLocked()) {
            ctx.sendLocalized("commands.loot.player_locked", EmoteReference.ERROR);
            return;
        }

        if (!RatelimitUtils.ratelimit(lootRateLimiter, ctx)) {
            return;
        }

        var today = LocalDate.now(ZoneId.systemDefault());
        var eventStart = today.withMonth(Month.DECEMBER.getValue()).withDayOfMonth(23);
        var eventStop = eventStart.plusDays(3); //Up to the 25th
        var ground = TextChannelGround.of(ctx.getChannel());

        if (today.isEqual(eventStart) || (today.isAfter(eventStart) && today.isBefore(eventStop))) {
            ground.dropItemWithChance(ItemReference.CHRISTMAS_TREE_SPECIAL, 4);
            ground.dropItemWithChance(ItemReference.BELL_SPECIAL, 4);
        }

        if (random.nextInt(100) > 95) {
            ground.dropItem(ItemReference.LOOT_CRATE);
            if (player.addBadgeIfAbsent(Badge.LUCKY)) {
                player.updateAllChanged();
            }
        }

        var loot = ground.collectItems();
        var moneyFound = ground.collectMoney() + Math.max(0, random.nextInt(70));

        // Make the credits minimum 10, instead of... 1
        if (moneyFound != 0) {
            moneyFound = Math.max(10, moneyFound);
        }

        if (dbUser.isPremium() && moneyFound > 0) {
            int extra = (int) (moneyFound * 1.5);
            moneyFound += random.nextInt(extra);
        }

        var extraMessage = "";

        // Sellout
        if (player.shouldSeeCampaign()){
            extraMessage += Campaign.PREMIUM.getStringFromCampaign(languageContext, dbUser.isPremium());
            player.markCampaignAsSeen();
        }


        if (!loot.isEmpty()) {
            var stack = ItemStack.toString(ItemStack.reduce(loot));

            if (player.mergeInventory(loot)) {
                extraMessage += languageContext.withRoot("commands", "loot.item_overflow");
            }

            if (moneyFound != 0) {
                if (player.addMoney(moneyFound)) {
                    ctx.sendLocalized("commands.loot.with_item.found", EmoteReference.POPPER, stack, moneyFound, extraMessage);
                } else {
                    ctx.sendLocalized("commands.loot.with_item.found_but_overflow", EmoteReference.POPPER, stack, moneyFound, extraMessage);
                }
            } else {
                ctx.sendLocalized("commands.loot.with_item.found_only_item_but_overflow", EmoteReference.MEGA, stack, extraMessage);
            }

        } else {
            if (moneyFound != 0) {
                if (player.addMoney(moneyFound)) {
                    ctx.sendLocalized("commands.loot.without_item.found", EmoteReference.POPPER, moneyFound, extraMessage);
                } else {
                    ctx.sendLocalized("commands.loot.without_item.found_but_overflow", EmoteReference.POPPER, moneyFound);
                }
            } else {
                var dust = dbUser.increaseDustLevel(random.nextInt(2));
                var msg = languageContext.withRoot("commands", "loot.dust").formatted(dust);

                dbUser.updateAllChanged();
                if (random.nextInt(100) > 93) {
                    msg += languageContext.withRoot("commands", "loot.easter");
                }

                ctx.send(EmoteReference.SAD + msg);
            }
        }

        player.updateAllChanged();
    }

    private static void balance(IContext ctx, User toCheck) {
        var languageContext = ctx.getLanguageContext();
        var user = ctx.getAuthor();
        boolean isExternal = false;

        if (toCheck != null) {
            user = toCheck;
            isExternal = true;
        }

        if (user.isBot()) {
            ctx.sendLocalized("commands.balance.bot_notice", EmoteReference.ERROR);
            return;
        }

        var player = ctx.getPlayer(user);
        var balance = player.getCurrentMoney();
        var extra = "";

        if (balance < 300 && player.getExperience() < 3400 && !player.isNewPlayerNotice()) {
            extra += languageContext.get("commands.balance.new_player");
            player.newPlayerNotice(true);
            player.updateAllChanged();
        }

        var message = String.format(
                Utils.getLocaleFromLanguage(ctx.getLanguageContext()),
                languageContext.withRoot("commands", "balance.own_balance"),
                balance, extra
        );

        if (isExternal) {
            message = String.format(
                    Utils.getLocaleFromLanguage(ctx.getLanguageContext()),
                    languageContext.withRoot("commands", "balance.external_balance"),
                    user.getName(), balance
            );
        }

        ctx.send(EmoteReference.CREDITCARD + message);
    }
}
