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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.UnifiedPlayer;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
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
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Module
public class MoneyCmds {
    private static final long DAILY_VALID_PERIOD_MILLIS = MantaroData.config().get().getDailyMaxPeriodMilliseconds();

    @Subscribe
    public void daily(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .cooldown(24, TimeUnit.HOURS)
                .maxCooldown(24, TimeUnit.HOURS)
                .randomIncrement(false)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("daily")
                .build();

        Random r = new Random();
        cr.register("daily", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                final var languageContext = ctx.getLanguageContext();

                //155
                //Args: Check -check for duration
                if (args.length > 0 && ctx.getMentionedUsers().isEmpty() && args[0].equalsIgnoreCase("-check")) {
                    long rl = rateLimiter.getRemaniningCooldown(ctx.getAuthor());

                    ctx.sendLocalized("commands.daily.check", EmoteReference.TALKING,
                            (rl) > 0 ? Utils.formatDuration(rl) :
                                    languageContext.get("commands.daily.about_now")
                    );
                    return;
                }

                // Determine who gets the money
                var dailyMoney = 150L;
                final var mentionedUsers = ctx.getMentionedUsers();

                final var author = ctx.getAuthor();
                var authorPlayer = ctx.getPlayer();
                var authorPlayerData = authorPlayer.getData();
                final var authorDBUser = ctx.getDBUser();
                final var authorUserData = authorDBUser.getData();

                if (authorPlayer.isLocked()){
                    ctx.sendLocalized("commands.daily.errors.own_locked");
                    return;
                }

                UnifiedPlayer toAddMoneyTo = UnifiedPlayer.of(author, ctx.getConfig().getCurrentSeason());
                User otherUser = null;

                boolean targetOther = !mentionedUsers.isEmpty();
                if (targetOther){
                    otherUser = mentionedUsers.get(0);
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

                    // Why this is here I have no clue;;;
                    dailyMoney += r.nextInt(90);

                    var mentionedDBUser = ctx.getDBUser(otherUser.getId());
                    var mentionedUserData = mentionedDBUser.getData();

                    //Marriage bonus
                    var marriage = authorUserData.getMarriage();
                    if (marriage != null && otherUser.getId().equals(marriage.getOtherPlayer(ctx.getAuthor().getId())) &&
                            playerOtherUser.getInventory().containsItem(ItemReference.RING)) {
                        dailyMoney += Math.max(10, r.nextInt(100));
                    }

                    //Mutual waifu status.
                    if (authorUserData.getWaifus().containsKey(otherUser.getId()) && mentionedUserData.getWaifus().containsKey(author.getId())) {
                        dailyMoney +=Math.max(5, r.nextInt(100));
                    }

                    toAddMoneyTo = UnifiedPlayer.of(otherUser, ctx.getConfig().getCurrentSeason());


                } else{
                    // This is here so you dont overwrite yourself....
                    authorPlayer = toAddMoneyTo.getPlayer();
                    authorPlayerData = authorPlayer.getData();
                }

                // Check for rate limit
                if (!RatelimitUtils.ratelimit(rateLimiter, ctx, false))
                    return;

                List<String> returnMessage = new ArrayList<>();
                long currentTime = System.currentTimeMillis();
                int amountStreaksavers = authorPlayer.getInventory().getAmount(ItemReference.MAGIC_WATCH);
                // >=0 -> Valid  <0 -> Invalid
                long currentDailyOffset = DAILY_VALID_PERIOD_MILLIS - (currentTime - authorPlayerData.getLastDailyAt()) ;

                long streak = authorPlayerData.getDailyStreak();

                // Not expired?
                if (currentDailyOffset + amountStreaksavers * DAILY_VALID_PERIOD_MILLIS >= 0) {
                    streak++;
                    if (targetOther)
                        returnMessage.add(languageContext.withRoot("commands","daily.streak.given.up").formatted(streak));
                    else
                        returnMessage.add(languageContext.withRoot("commands","daily.streak.up").formatted(streak));
                    if (currentDailyOffset < 0){
                        int streakSaversUsed = -1 * (int) Math.floor((double) currentDailyOffset / (double) DAILY_VALID_PERIOD_MILLIS);
                        authorPlayer.getInventory().process(new ItemStack(ItemReference.MAGIC_WATCH, streakSaversUsed * -1));
                        returnMessage.add(languageContext.withRoot("commands", "daily.streak.watch_used").formatted(
                                streakSaversUsed, streakSaversUsed + 1,
                                amountStreaksavers - streakSaversUsed)
                        );
                    }

                } else{
                    if (streak == 0) {
                        returnMessage.add(languageContext.withRoot("commands", "daily.streak.first_time"));
                    } else {
                        if (amountStreaksavers > 0){
                            returnMessage.add(
                                    languageContext.withRoot("commands", "daily.streak.lost_streak.watch").formatted(streak)
                            );

                            authorPlayer.getInventory().process(
                                    new ItemStack(ItemReference.MAGIC_WATCH, authorPlayer.getInventory().getAmount(ItemReference.MAGIC_WATCH) * -1)
                            );

                        } else{
                            returnMessage.add(languageContext.withRoot("commands", "daily.streak.lost_streak.normal").formatted(streak));
                        }
                    }
                    streak = 1;
                }

                if (streak > 5) {
                    // Bonus money
                    int bonus = 150;

                    if (streak % 50 == 0){
                        authorPlayer.getInventory().process(new ItemStack(ItemReference.MAGIC_WATCH,1));
                        returnMessage.add(languageContext.get("commands.daily.watch_get"));
                    }

                    if (streak > 10) {
                        authorPlayerData.addBadgeIfAbsent(Badge.CLAIMER);

                        if (streak % 20 == 0 && authorPlayer.getInventory().getAmount(ItemReference.LOOT_CRATE) < 5000) {
                            authorPlayer.getInventory().process(new ItemStack(ItemReference.LOOT_CRATE, 1));
                            returnMessage.add(languageContext.get("commands.daily.crate"));
                        }

                        if (streak > 15){
                            bonus += Math.min(targetOther ? 1700 : 700, Math.floor(150 * streak / (targetOther ? 10D : 15D)));

                            if (streak >= 180) {
                                authorPlayerData.addBadgeIfAbsent(Badge.BIG_CLAIMER);
                            }

                            if (streak >= 365) {
                                authorPlayerData.addBadgeIfAbsent(Badge.YEARLY_CLAIMER);
                            }

                            if (streak >= 730) {
                                authorPlayerData.addBadgeIfAbsent(Badge.BI_YEARLY_CLAIMER);
                            }
                        }
                    }

                    if (targetOther) {
                        returnMessage.add(
                                languageContext.withRoot("commands", "daily.streak.given.bonus").formatted(otherUser.getName(), bonus)
                        );
                    } else {
                        returnMessage.add(
                                languageContext.withRoot("commands", "daily.streak.bonus").formatted(bonus)
                        );
                    }
                    dailyMoney += bonus;
                }

                // If the author is premium, make daily double.
                if (authorDBUser.isPremium()) {
                    dailyMoney *=2;
                }

                // Sellout + this is always a day apart, so we can just send campaign.
                if (r.nextBoolean()) {
                    returnMessage.add(Campaign.TWITTER.getStringFromCampaign(languageContext, true));
                } else {
                    returnMessage.add(Campaign.PREMIUM_DAILY.getStringFromCampaign(languageContext, authorDBUser.isPremium()));
                }

                // Careful not to overwrite yourself ;P
                // Save streak and items
                authorPlayerData.setLastDailyAt(currentTime);
                authorPlayerData.setDailyStreak(streak);

                // Critical not to call if author != mentioned because in this case
                // toAdd is the unified player as referenced
                if (targetOther) {
                    authorPlayer.save();
                }

                toAddMoneyTo.addMoney(dailyMoney);
                toAddMoneyTo.saveUpdating();


                // Build Message
                var toSend = new StringBuilder((targetOther ?
                        languageContext.withRoot("commands", "daily.given_credits")
                                .formatted(EmoteReference.CORRECT, dailyMoney, otherUser.getName()) :
                        languageContext.withRoot("commands", "daily.credits").formatted(
                                EmoteReference.CORRECT, dailyMoney)) +
                        "\n"
                );

                for (var string : returnMessage) {
                    toSend.append("\n").append(string);
                }

                // Send Message
                ctx.send(toSend.toString());

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
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(2)
                .cooldown(3, TimeUnit.MINUTES)
                .maxCooldown(3, TimeUnit.MINUTES)
                .randomIncrement(false)
                .premiumAware(true)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("loot")
                .build();

        final ZoneId zoneId = ZoneId.systemDefault();
        final SecureRandom random = new SecureRandom();

        cr.register("loot", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                var unifiedPlayer = UnifiedPlayer.of(ctx.getAuthor(), ctx.getConfig().getCurrentSeason());

                var player = unifiedPlayer.getPlayer();
                var playerData = player.getData();
                var dbUser = ctx.getDBUser();
                var languageContext = ctx.getLanguageContext();

                if (player.isLocked()) {
                    ctx.sendLocalized("commands.loot.player_locked", EmoteReference.ERROR);
                    return;
                }

                if (!RatelimitUtils.ratelimit(rateLimiter, ctx, false)) {
                    return;
                }

                var today = LocalDate.now(zoneId);
                var eventStart = today.withMonth(Month.DECEMBER.getValue()).withDayOfMonth(23);
                var eventStop = eventStart.plusDays(3); //Up to the 25th
                var ground = TextChannelGround.of(ctx.getEvent());

                if (today.isEqual(eventStart) || (today.isAfter(eventStart) && today.isBefore(eventStop))) {
                    ground.dropItemWithChance(ItemReference.CHRISTMAS_TREE_SPECIAL, 4);
                    ground.dropItemWithChance(ItemReference.BELL_SPECIAL, 4);
                }

                if (random.nextInt(100) > 95) {
                    ground.dropItem(ItemReference.LOOT_CRATE);
                    if (playerData.addBadgeIfAbsent(Badge.LUCKY)) {
                        player.saveUpdating();
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
                if (playerData.shouldSeeCampaign()){
                    extraMessage += Campaign.PREMIUM.getStringFromCampaign(languageContext, dbUser.isPremium());
                    playerData.markCampaignAsSeen();
                }


                if (!loot.isEmpty()) {
                    var stack = ItemStack.toString(ItemStack.reduce(loot));

                    if (player.getInventory().merge(loot))
                        extraMessage += languageContext.withRoot("commands", "loot.item_overflow");

                    if (moneyFound != 0) {
                        if (unifiedPlayer.addMoney(moneyFound)) {
                            ctx.sendLocalized("commands.loot.with_item.found", EmoteReference.POPPER, stack, moneyFound, extraMessage);
                        } else {
                            ctx.sendLocalized("commands.loot.with_item.found_but_overflow", EmoteReference.POPPER, stack, moneyFound, extraMessage);
                        }
                    } else {
                        ctx.sendLocalized("commands.loot.with_item.found_only_item_but_overflow", EmoteReference.MEGA, stack, extraMessage);
                    }

                } else {
                    if (moneyFound != 0) {
                        if (unifiedPlayer.addMoney(moneyFound)) {
                            ctx.sendLocalized("commands.loot.without_item.found", EmoteReference.POPPER, moneyFound, extraMessage);
                        } else {
                            ctx.sendLocalized("commands.loot.without_item.found_but_overflow", EmoteReference.POPPER, moneyFound);
                        }
                    } else {
                        var dust = dbUser.getData().increaseDustLevel(random.nextInt(2));
                        var msg = languageContext.withRoot("commands", "loot.dust").formatted(dust);

                        dbUser.save();

                        if (random.nextInt(100) > 93) {
                            msg += languageContext.withRoot("commands", "loot.easter");
                        }

                        ctx.send(EmoteReference.SAD + msg);
                    }
                }


                unifiedPlayer.saveUpdating();
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
                var optionalArguments = ctx.getOptionalArguments();
                content = Utils.replaceArguments(optionalArguments, content, "season", "s").trim();
                var isSeasonal = optionalArguments.containsKey("season") || optionalArguments.containsKey("s");
                var languageContext = ctx.getLanguageContext();

                // Values on lambdas should be final or effectively final part 9999.
                final var finalContent = content;
                ctx.findMember(content, ctx.getMessage()).onSuccess(members -> {
                    var user = ctx.getAuthor();
                    boolean isExternal = false;

                    var found = CustomFinderUtil.findMemberDefault(finalContent, members, ctx, ctx.getMember());
                    if (found == null) {
                        return;
                    } else if (!finalContent.isEmpty()) {
                        user = found.getUser();
                        isExternal = true;
                    }

                    if (user.isBot()) {
                        ctx.sendLocalized("commands.balance.bot_notice", EmoteReference.ERROR);
                        return;
                    }

                    var player = ctx.getPlayer(user);
                    var playerData = player.getData();

                    var balance = isSeasonal ? ctx.getSeasonPlayer(user).getMoney() : player.getCurrentMoney();
                    var extra = "";

                    if (!playerData.isResetWarning() && !ctx.getConfig().isPremiumBot() && ctx.getPlayer().getOldMoney() > 10_000) {
                        extra = languageContext.get("commands.balance.reset_notice");
                        playerData.setResetWarning(true);
                        player.saveUpdating();
                    }

                    ctx.send(EmoteReference.DIAMOND + (isExternal ?
                            languageContext.withRoot("commands", "balance.external_balance").formatted(user.getName(), balance) :
                            languageContext.withRoot("commands", "balance.own_balance").formatted(balance, extra))
                    );

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
}
