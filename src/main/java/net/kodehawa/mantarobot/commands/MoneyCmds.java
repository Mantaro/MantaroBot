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
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.UnifiedPlayer;
import net.kodehawa.mantarobot.commands.utils.RoundedMetricPrefixFormat;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.RatelimitUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.campaign.Campaign;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;

import java.security.SecureRandom;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Module
public class MoneyCmds {
    private static final SecureRandom random = new SecureRandom();
    private static final int SLOTS_MAX_MONEY = 50_000;
    private static final int TICKETS_MAX_AMOUNT = 50;
    private static final long GAMBLE_ABSOLUTE_MAX_MONEY = Integer.MAX_VALUE;
    private static final long GAMBLE_MAX_MONEY = 10_000;
    private static final long DAILY_VALID_PERIOD_MILLIS = MantaroData.config().get().getDailyMaxPeriodMilliseconds();

    private static final ThreadLocal<NumberFormat> PERCENT_FORMAT = ThreadLocal.withInitial(() -> {
        final NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1); // decimal support
        return format;
    });

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

                if(authorPlayer.isLocked()){
                    ctx.sendLocalized("commands.daily.errors.own_locked");
                    return;
                }

                UnifiedPlayer toAddMoneyTo = UnifiedPlayer.of(author, ctx.getConfig().getCurrentSeason());
                User otherUser = null;

                boolean targetOther = !mentionedUsers.isEmpty();
                if(targetOther){
                    otherUser = mentionedUsers.get(0);
                    // Bot check mentioned authorDBUser
                    if(otherUser.isBot()){
                        ctx.sendLocalized("commands.daily.errors.bot", EmoteReference.ERROR);
                        return;
                    }

                    if(otherUser.getIdLong() == author.getIdLong()) {
                        ctx.sendLocalized("commands.daily.errors.same_user", EmoteReference.ERROR);
                        return;
                    }

                    var playerOtherUser = ctx.getPlayer(otherUser);
                    if(playerOtherUser.isLocked()){
                        ctx.sendLocalized("commands.daily.errors.receipt_locked");
                        return;
                    }

                    // Why this is here I have no clue;;;
                    dailyMoney += r.nextInt(90);

                    var mentionedDBUser = ctx.getDBUser(otherUser.getId());
                    var mentionedUserData = mentionedDBUser.getData();

                    //Marriage bonus
                    var marriage = authorUserData.getMarriage();
                    if(marriage != null && otherUser.getId().equals(marriage.getOtherPlayer(ctx.getAuthor().getId())) &&
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
                if(currentDailyOffset + amountStreaksavers * DAILY_VALID_PERIOD_MILLIS >= 0) {
                    streak++;
                    if(targetOther)
                        returnMessage.add(String.format(languageContext.withRoot("commands","daily.streak.given.up"), streak));
                    else
                        returnMessage.add(String.format(languageContext.withRoot("commands","daily.streak.up"), streak));
                    if(currentDailyOffset < 0){
                        int streakSaversUsed = -1 * (int) Math.floor((double) currentDailyOffset / (double) DAILY_VALID_PERIOD_MILLIS);
                        authorPlayer.getInventory().process(new ItemStack(ItemReference.MAGIC_WATCH, streakSaversUsed * -1));
                        returnMessage.add(String.format(languageContext.withRoot("commands", "daily.streak.watch_used"),
                                streakSaversUsed, streakSaversUsed + 1, amountStreaksavers - streakSaversUsed));
                    }

                } else{
                    if (streak == 0) {
                        returnMessage.add(languageContext.withRoot("commands", "daily.streak.first_time"));
                    } else {
                        if (amountStreaksavers > 0){
                            returnMessage.add(
                                    String.format(languageContext.withRoot("commands", "daily.streak.lost_streak.watch"), streak)
                            );

                            authorPlayer.getInventory().process(
                                    new ItemStack(ItemReference.MAGIC_WATCH, authorPlayer.getInventory().getAmount(ItemReference.MAGIC_WATCH) * -1)
                            );

                        } else{
                            returnMessage.add(String.format(languageContext.withRoot("commands", "daily.streak.lost_streak.normal"), streak));
                        }
                    }
                    streak = 1;
                }

                if (streak > 5) {
                    // Bonus money
                    int bonus = 150;

                    if(streak % 50 == 0){
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

                            if (streak > 100) {
                                authorPlayerData.addBadgeIfAbsent(Badge.BIG_CLAIMER);
                            }
                        }
                    }

                    if(targetOther) {
                        returnMessage.add(String.format(
                                languageContext.withRoot("commands", "daily.streak.given.bonus"), otherUser.getName(), bonus)
                        );
                    } else {
                        returnMessage.add(String.format(
                                languageContext.withRoot("commands", "daily.streak.bonus"), bonus)
                        );
                    }
                    dailyMoney += bonus;
                }

                // If the author is premium, make daily double.
                if(authorDBUser.isPremium()) {
                    dailyMoney *=2;
                }

                // Sellout + this is always a day apart, so we can just send campaign.
                if(r.nextBoolean()) {
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
                if(targetOther) {
                    authorPlayer.save();
                }

                toAddMoneyTo.addMoney(dailyMoney);
                toAddMoneyTo.save();


                // Build Message
                var toSend = new StringBuilder((targetOther ?
                        String.format(languageContext.withRoot("commands", "daily.given_credits"),
                                EmoteReference.CORRECT, dailyMoney, otherUser.getName()) :
                        String.format(languageContext.withRoot("commands", "daily.credits"),
                                EmoteReference.CORRECT, dailyMoney)) + "\n");

                for(var string : returnMessage) {
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
                        .setUsage("`~>daily [@user]`")
                        .addParameter("@user", "The user to give your daily to. This is optional, without this it gives it to yourself.")
                        .build();
            }
        });

        cr.registerAlias("daily", "dailies");
    }

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
                            if (i > player.getCurrentMoney() || i < 0) throw new UnsupportedOperationException();
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

                var user = ctx.getAuthor();
                var gains = (long) (i * multiplier);
                gains = Math.round(gains * 0.45);

                final var finalLuck = luck;
                final var finalGains = gains;

                if (i >= 60000000) {
                    player.setLocked(true);
                    player.save();
                    ctx.sendLocalized("commands.gamble.confirmation_message", EmoteReference.WARNING, i);
                    InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 30, new InteractiveOperation() {
                        @Override
                        public int run(GuildMessageReceivedEvent e) {
                            if (e.getAuthor().getId().equals(user.getId())) {
                                if (e.getMessage().getContentRaw().equalsIgnoreCase("yes")) {
                                    proceedGamble(ctx, player, finalLuck, i, finalGains, i);
                                    return COMPLETED;
                                } else if (e.getMessage().getContentRaw().equalsIgnoreCase("no")) {
                                    e.getChannel().sendMessage(EmoteReference.ZAP + "Cancelled bet.").queue();
                                    player.setLocked(false);
                                    player.saveAsync();
                                    return COMPLETED;
                                }
                            }

                            return IGNORED;
                        }

                        @Override
                        public void onExpire() {
                            ctx.sendLocalized("general.operation_timed_out", EmoteReference.ERROR2);
                            player.setLocked(false);
                            player.saveAsync();
                        }
                    });
                    return;
                }

                proceedGamble(ctx, player, luck, i, gains, i);
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
    public void loot(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(2)
                .cooldown(5, TimeUnit.MINUTES)
                .maxCooldown(5, TimeUnit.MINUTES)
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
                    if (playerData.addBadgeIfAbsent(Badge.LUCKY))
                        player.saveAsync();
                }

                var loot = ground.collectItems();
                var moneyFound = ground.collectMoney() + Math.max(0, random.nextInt(50) - 10);

                if (dbUser.isPremium() && moneyFound > 0) {
                    int extra = (int) (moneyFound * 1.5);
                    moneyFound += random.nextInt(extra);
                }

                var extraMessage = "";

                // Sellout
                if(playerData.shouldSeeCampaign()){
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
                        var msg = String.format(languageContext.withRoot("commands", "loot.dust"), dust);

                        dbUser.save();

                        if (random.nextInt(100) > 93) {
                            msg += languageContext.withRoot("commands", "loot.easter");
                        }

                        ctx.send(EmoteReference.SAD + msg);
                    }
                }


                unifiedPlayer.saveAsync();
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
                    if(found == null) {
                        return;
                    } else if (!finalContent.isEmpty()) {
                        user = found.getUser();
                        isExternal = true;
                    }

                    if (user.isBot()) {
                        ctx.sendLocalized("commands.balance.bot_notice", EmoteReference.ERROR);
                        return;
                    }

                    var balance = isSeasonal ? ctx.getSeasonPlayer(user).getMoney() : ctx.getPlayer(user).getCurrentMoney();

                    ctx.send(EmoteReference.DIAMOND + (isExternal ?
                            String.format(languageContext.withRoot("commands", "balance.external_balance"), user.getName(), balance) :
                            String.format(languageContext.withRoot("commands", "balance.own_balance"), balance))
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

                    if(coinAmount > TICKETS_MAX_AMOUNT) {
                        ctx.sendLocalized("commands.slots.errors.too_many_tickets", EmoteReference.ERROR, TICKETS_MAX_AMOUNT);
                        return;
                    }

                    if (playerInventory.getAmount(ItemReference.SLOT_COIN) < coinAmount) {
                        ctx.sendLocalized("commands.slots.errors.not_enough_tickets", EmoteReference.ERROR);
                        return;
                    }

                    money += 58 * coinAmount;
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
                            seasonalPlayer.saveAsync();
                        else
                            player.saveAsync();

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
                        player.saveAsync();
                    }
                }

                var languageContext = ctx.getLanguageContext();

                var message = new StringBuilder(
                        String.format(languageContext.withRoot("commands", "slots.roll"),
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
                            .append(String.format(languageContext.withRoot("commands", "slots.win"), gains, money))
                            .append(EmoteReference.POPPER);

                    stats.incrementSlotsWins();
                    stats.addSlotsWin(gains);

                    if ((gains + money) > SLOTS_MAX_MONEY) {
                        player.getData().addBadgeIfAbsent(Badge.LUCKY_SEVEN);
                    }

                    if (coinSelect && coinAmount > ItemStack.MAX_STACK_SIZE - random.nextInt(650))
                        player.getData().addBadgeIfAbsent(Badge.SENSELESS_HOARDING);

                    if (season) {
                        seasonalPlayer.addMoney(gains + money);
                        seasonalPlayer.saveAsync();
                    } else {
                        player.addMoney(gains + money);
                        player.saveAsync();
                    }
                } else {
                    stats.getData().incrementSlotsLose();
                    message.append(toSend).append("\n\n").append(
                            String.format(languageContext.withRoot("commands", "slots.lose"), EmoteReference.SAD)
                    );
                }

                stats.saveAsync();

                message.append("\n");
                ctx.send(message.toString());
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Rolls the slot machine. Requires a default of 50 coins to roll.")
                        .setUsage("`~>slots` - Default one, 50 coins.\n" +
                                "`~>slots <credits>` - Puts x credits on the slot machine. " +
                                "You can put a maximum of " + SLOTS_MAX_MONEY + " coins.\n" +
                                "`~>slots -useticket` - Rolls the slot machine with one slot coin.\n" +
                                "You can specify the amount of tickets to use using `-amount` " +
                                "(for example `~>slots -useticket -amount 10`). " +
                                "Using tickets increases your chance by 10%. Maximum amount of tickets allowed is 50.")
                        .build();
            }
        });
    }

    private void proceedGamble(Context ctx, Player player, int luck, long i, long gains, long bet) {
        var stats = MantaroData.db().getPlayerStats(ctx.getMember());
        var data = player.getData();

        if (luck > random.nextInt(140)) {
            if (player.addMoney(gains)) {
                if (gains > Integer.MAX_VALUE / 2) {
                    if (!data.hasBadge(Badge.GAMBLER)) {
                        data.addBadgeIfAbsent(Badge.GAMBLER);
                        player.saveAsync();
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
        player.saveAsync();
        stats.saveAsync();
    }
}
