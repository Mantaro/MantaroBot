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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.UnifiedPlayer;
import net.kodehawa.mantarobot.commands.utils.RoundedMetricPrefixFormat;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PlayerStats;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;

import java.security.SecureRandom;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.utils.Utils.handleIncreasingRatelimit;

@Module
@SuppressWarnings("unused")
public class MoneyCmds {
    private static final ThreadLocal<NumberFormat> PERCENT_FORMAT = ThreadLocal.withInitial(() -> {
        final NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1); // decimal support
        return format;
    });

    private final Random random = new Random();
    private final int SLOTS_MAX_MONEY = 175_000_000;
    private final long GAMBLE_ABSOLUTE_MAX_MONEY = (long) (Integer.MAX_VALUE) * 5;
    private final long GAMBLE_MAX_MONEY = 275_000_000;
    private final long DAILY_VALID_PERIOD_MILLIS = MantaroData.config().get().dailyMaxPeriodMilliseconds;

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
        cr.register("daily", new SimpleCommand(Category.CURRENCY) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                I18nContext languageContext = ctx.getLanguageContext();

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
                long dailyMoney = 150L;
                List<User> mentionedUsers = ctx.getMentionedUsers();

                User author = ctx.getAuthor();
                Player authorPlayer = ctx.getPlayer();
                PlayerData authorPlayerData = authorPlayer.getData();
                DBUser user = ctx.getDBUser();

                if(authorPlayer.isLocked()){
                    ctx.sendLocalized("commands.daily.errors.own_locked");
                    return;
                }

                UnifiedPlayer toAddMoneyTo = UnifiedPlayer.of(author, ctx.getConfig().getCurrentSeason());
                User otherUser = null;

                boolean targetOther = !mentionedUsers.isEmpty();
                if(targetOther){
                    otherUser = mentionedUsers.get(0);
                    // Bot check mentioned user
                    if(otherUser.isBot()){
                        ctx.sendLocalized("commands.daily.errors.bot", EmoteReference.ERROR);
                        return;
                    }

                    if(otherUser.getIdLong() == author.getIdLong()) {
                        ctx.sendLocalized("commands.daily.errors.same_user", EmoteReference.ERROR);
                        return;
                    }

                    Player playerOtherUser = ctx.getPlayer(otherUser);
                    if(playerOtherUser.isLocked()){
                        ctx.sendLocalized("commands.daily.errors.receipt_locked");
                        return;
                    }

                    // Why this is here I have no clue;;;
                    dailyMoney += r.nextInt(90);

                    UserData userData = user.getData();

                    DBUser mentionedDBUser = ctx.getDBUser(otherUser.getId());
                    UserData mentionedUserData = user.getData();

                    //Marriage bonus
                    Marriage marriage = userData.getMarriage();
                    if(marriage != null && otherUser.getId().equals(marriage.getOtherPlayer(ctx.getAuthor().getId())) &&
                            playerOtherUser.getInventory().containsItem(Items.RING)) {
                        dailyMoney += Math.max(10, r.nextInt(100));
                    }

                    //Mutual waifu status.
                    if (userData.getWaifus().containsKey(playerOtherUser.getId()) && mentionedUserData.getWaifus().containsKey(author.getId())) {
                        dailyMoney +=Math.max(5, r.nextInt(70));
                    }

                    toAddMoneyTo = UnifiedPlayer.of(otherUser, ctx.getConfig().getCurrentSeason());


                } else{
                    // This is here so you dont overwrite yourself....
                    authorPlayer = toAddMoneyTo.getPlayer();
                    authorPlayerData = authorPlayer.getData();
                }

                // Check for rate limit
                if (!Utils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx.getEvent(), ctx.getLanguageContext(), false))
                    return;

                List<String> returnMessage = new ArrayList<>();
                long currentTime = System.currentTimeMillis();
                int amountStreaksavers = authorPlayer.getInventory().getAmount(Items.MAGIC_WATCH);
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
                        authorPlayer.getInventory().process(new ItemStack(Items.MAGIC_WATCH, streakSaversUsed * -1));
                        returnMessage.add(String.format(languageContext.withRoot("commands", "daily.streak.watch_used"),
                                streakSaversUsed, streakSaversUsed + 1, amountStreaksavers - streakSaversUsed));
                    }

                } else{
                    if (streak == 0) {
                        returnMessage.add(languageContext.withRoot("commands", "daily.streak.first_time"));
                    } else {
                        if (amountStreaksavers > 0){
                            returnMessage.add(String.format(languageContext.withRoot("commands", "daily.streak.lost_streak.watch"), streak));
                            authorPlayer.getInventory().process(new ItemStack(Items.MAGIC_WATCH, authorPlayer.getInventory().getAmount(Items.MAGIC_WATCH) * -1));
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
                        authorPlayer.getInventory().process(new ItemStack(Items.MAGIC_WATCH,1));
                        returnMessage.add(languageContext.get("commands.daily.watch_get"));
                    }

                    if (streak > 10) {
                        authorPlayerData.addBadgeIfAbsent(Badge.CLAIMER);

                        if (streak % 20 == 0 && authorPlayer.getInventory().getAmount(Items.LOOT_CRATE) < 5000) {
                            authorPlayer.getInventory().process(new ItemStack(Items.LOOT_CRATE, 1));
                            returnMessage.add(languageContext.get("commands.daily.crate"));
                        }

                        if (streak > 15){
                            bonus += Math.min(targetOther ? 1700 : 700, Math.floor(150 * streak / (targetOther ? 10D : 15D)));

                            if (streak > 100) {
                                authorPlayerData.addBadgeIfAbsent(Badge.BIG_CLAIMER);
                            }
                        }
                    }
                    // Cleaner using if
                    if(targetOther)
                        returnMessage.add(String.format(languageContext.withRoot("commands", "daily.streak.given.bonus"), otherUser.getName(), bonus));
                    else
                        returnMessage.add(String.format(languageContext.withRoot("commands", "daily.streak.bonus"), bonus));
                    dailyMoney += bonus;

                }
                // Careful not to overwrite yourself ;P
                // Save streak and items
                authorPlayerData.setLastDailyAt(currentTime);
                authorPlayerData.setDailyStreak(streak);
                // Critical not to call if author != mentioned because in this case
                // toAdd is the unified player as referenced
                if(targetOther)
                    authorPlayer.save();
                toAddMoneyTo.addMoney(dailyMoney);
                toAddMoneyTo.save();

                // Sellout
                if(random.nextBoolean()){
                    returnMessage.add(user.isPremium() ? languageContext.get("commands.daily.sellout.already_premium") :
                            languageContext.get("commands.daily.sellout.get_premium"));
                }
                // Build Message
                StringBuilder toSend = new StringBuilder((targetOther ?
                        String.format(languageContext.withRoot("commands", "daily.given_credits"),
                                EmoteReference.CORRECT, dailyMoney, otherUser.getName()) :
                        String.format(languageContext.withRoot("commands", "daily.credits"),
                                EmoteReference.CORRECT, dailyMoney)) + "\n");

                for(String s : returnMessage)
                    toSend.append("\n").append(s);

                // Send Message
                ctx.send(toSend.toString());

            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Gives you $150 credits per day (or between 150 and 180 if you transfer it to another person). Maximum amount it can give is ~2000 credits (a bit more for shared dailies)\n" +
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
        cr.register("gamble", new SimpleCommand(Category.CURRENCY) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .spamTolerance(3)
                    .limit(1)
                    .cooldown(30, TimeUnit.SECONDS)
                    .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                    .maxCooldown(5, TimeUnit.MINUTES)
                    .pool(MantaroData.getDefaultJedisPool())
                    .prefix("gamble")
                    .premiumAware(true)
                    .build();

            final SecureRandom r = new SecureRandom();

            @Override
            public void call(Context ctx, String content, String[] args) {
                Player player = ctx.getPlayer();

                if (player.getMoney() <= 0) {
                    ctx.sendLocalized("commands.gamble.no_credits", EmoteReference.SAD);
                    return;
                }

                if (player.getMoney() > GAMBLE_ABSOLUTE_MAX_MONEY) {
                    ctx.sendLocalized("commands.gamble.too_much_money", EmoteReference.ERROR2, GAMBLE_ABSOLUTE_MAX_MONEY);
                    return;
                }

                double multiplier;
                long i;
                int luck;
                try {
                    switch (content) {
                        case "all":
                        case "everything":
                            i = player.getMoney();
                            multiplier = 1.3d + (r.nextInt(1350) / 1000d);
                            luck = 19 + (int) (multiplier * 13) + r.nextInt(18);
                            break;
                        case "half":
                            i = player.getMoney() == 1 ? 1 : player.getMoney() / 2;
                            multiplier = 1.2d + (r.nextInt(1350) / 1000d);
                            luck = 18 + (int) (multiplier * 13) + r.nextInt(18);
                            break;
                        case "quarter":
                            i = player.getMoney() == 1 ? 1 : player.getMoney() / 4;
                            multiplier = 1.1d + (r.nextInt(1250) / 1000d);
                            luck = 18 + (int) (multiplier * 12) + r.nextInt(18);
                            break;
                        default:
                            i = content.endsWith("%")
                                    ? Math.round(PERCENT_FORMAT.get().parse(content).doubleValue() * player.getMoney())
                                    : new RoundedMetricPrefixFormat().parseObject(content, new ParsePosition(0));
                            if (i > player.getMoney() || i < 0) throw new UnsupportedOperationException();
                            multiplier = 1.1d + (i / ((double) player.getMoney()) * r.nextInt(1300) / 1000d);
                            luck = 17 + (int) (multiplier * 13) + r.nextInt(12);
                            break;
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
                if (!handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx))
                    return;

                User user = ctx.getAuthor();
                long gains = (long) (i * multiplier);
                gains = Math.round(gains * 0.45);

                final int finalLuck = luck;
                final long finalGains = gains;

                if (i >= 60000000) {
                    player.setLocked(true);
                    player.save();
                    ctx.sendLocalized("commands.gamble.confirmation_message", EmoteReference.WARNING, i);
                    InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 30, new InteractiveOperation() {
                        @Override
                        public int run(GuildMessageReceivedEvent e) {
                            if (e.getAuthor().getId().equals(user.getId())) {
                                if (e.getMessage().getContentRaw().equalsIgnoreCase("yes")) {
                                    proceedGamble(ctx, player, finalLuck, random, i, finalGains, i);
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

                proceedGamble(ctx, player, luck, random, i, gains, i);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Gambles your money away. It's like Vegas, but without real money and without the impending doom. Kinda.")
                        .setUsage("`~>gamble <all/half/quarter>` or `~>gamble <amount>` or `~>gamble <percentage>`")
                        .addParameter("amount", "How much money you want to gamble. You can also express this on K or M (100K is 100000, 1M is 1000000, 100M is well, you know how it goes from here)")
                        .addParameter("all/half/quarter", "How much of your money you want to gamble, but if you're too lazy to type the number (half = 50% of all of your money)")
                        .addParameter("percentage", "The percentage of money you want to gamble. Works anywhere from 1% to 100%.")
                        .build();
            }
        });
    }

    @Subscribe
    public void loot(CommandRegistry cr) {
        cr.register("loot", new SimpleCommand(Category.CURRENCY) {
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
            final Random r = new Random();

            @Override
            public void call(Context ctx, String content, String[] args) {
                UnifiedPlayer unifiedPlayer = UnifiedPlayer.of(ctx.getAuthor(), ctx.getConfig().getCurrentSeason());

                Player player = unifiedPlayer.getPlayer();
                DBUser dbUser = ctx.getDBUser();
                Member member = ctx.getMember();
                I18nContext languageContext = ctx.getLanguageContext();

                if (player.isLocked()) {
                    ctx.sendLocalized("commands.loot.player_locked", EmoteReference.ERROR);
                    return;
                }

                if (!Utils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx.getEvent(), languageContext, false))
                    return;

                LocalDate today = LocalDate.now(zoneId);
                LocalDate eventStart = today.withMonth(Month.DECEMBER.getValue()).withDayOfMonth(23);
                LocalDate eventStop = eventStart.plusDays(3); //Up to the 25th
                TextChannelGround ground = TextChannelGround.of(ctx.getEvent());

                if (today.isEqual(eventStart) || (today.isAfter(eventStart) && today.isBefore(eventStop))) {
                    ground.dropItemWithChance(Items.CHRISTMAS_TREE_SPECIAL, 4);
                    ground.dropItemWithChance(Items.BELL_SPECIAL, 4);
                }

                if (r.nextInt(100) == 0) { //1 in 100 chance of it dropping a loot crate.
                    ground.dropItem(Items.LOOT_CRATE);
                    if (player.getData().addBadgeIfAbsent(Badge.LUCKY))
                        player.saveAsync();
                }

                List<ItemStack> loot = ground.collectItems();
                int moneyFound = ground.collectMoney() + Math.max(0, r.nextInt(50) - 10);

                if (MantaroData.db().getUser(member).isPremium() && moneyFound > 0) {
                    moneyFound = moneyFound + random.nextInt(moneyFound);
                }

                if (!loot.isEmpty()) {
                    String s = ItemStack.toString(ItemStack.reduce(loot));
                    String overflow = "";

                    if (player.getInventory().merge(loot))
                        overflow = languageContext.withRoot("commands", "loot.item_overflow");

                    if (moneyFound != 0) {
                        if (unifiedPlayer.addMoney(moneyFound)) {
                            ctx.sendLocalized("commands.loot.with_item.found", EmoteReference.POPPER, s, moneyFound, overflow);
                        } else {
                            ctx.sendLocalized("commands.loot.with_item.found_but_overflow", EmoteReference.POPPER, s, moneyFound, overflow);
                        }
                    } else {
                        ctx.sendLocalized("commands.loot.with_item.found_only_item_but_overflow", EmoteReference.MEGA, s, overflow);
                    }

                } else {
                    if (moneyFound != 0) {
                        if (unifiedPlayer.addMoney(moneyFound)) {
                            ctx.sendLocalized("commands.loot.without_item.found", EmoteReference.POPPER, moneyFound);
                        } else {
                            ctx.sendLocalized("commands.loot.without_item.found_but_overflow", EmoteReference.POPPER, moneyFound);
                        }
                    } else {
                        int dust = dbUser.getData().increaseDustLevel(r.nextInt(2));
                        String msg = String.format(languageContext.withRoot("commands", "loot.dust"), dust);
                        dbUser.save();

                        if (r.nextInt(100) > 93) {
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
        cr.register("balance", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                Map<String, String> t = ctx.getOptionalArguments();
                content = Utils.replaceArguments(t, content, "season", "s").trim();
                boolean isSeasonal = t.containsKey("season") || t.containsKey("s");
                I18nContext languageContext = ctx.getLanguageContext();

                User user = ctx.getAuthor();
                boolean isExternal = false;

                if(!content.isEmpty()) {
                    Member found = Utils.findMember(ctx.getEvent(), languageContext, content);

                    if (found != null) {
                        user = found.getUser();
                        isExternal = true;
                    } else {
                        return;
                    }
                }

                if (user.isBot()) {
                    ctx.sendLocalized("commands.balance.bot_notice", EmoteReference.ERROR);
                    return;
                }

                long balance = isSeasonal ? ctx.getSeasonPlayer(user).getMoney() : ctx.getPlayer(user).getMoney();

                ctx.send(EmoteReference.DIAMOND + (isExternal ?
                        String.format(languageContext.withRoot("commands", "balance.external_balance"), user.getName(), balance) :
                        String.format(languageContext.withRoot("commands", "balance.own_balance"), balance))
                );
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
                .cooldown(35, TimeUnit.SECONDS)
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

        cr.register("slots", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                Map<String, String> opts = ctx.getOptionalArguments();

                long money = 50;
                int slotsChance = 25; //25% raw chance of winning, completely random chance of winning on the other random iteration
                boolean isWin = false;
                boolean coinSelect = false;
                int amountN = 1;

                Player player = ctx.getPlayer();
                PlayerStats stats = ctx.db().getPlayerStats(ctx.getAuthor());
                SeasonPlayer seasonalPlayer = null; //yes
                boolean season = false;

                if (opts.containsKey("season")) {
                    season = true;
                    seasonalPlayer = ctx.getSeasonPlayer();
                }

                if (opts.containsKey("useticket"))
                    coinSelect = true;

                Inventory playerInventory = season ? seasonalPlayer.getInventory() : player.getInventory();

                if (opts.containsKey("amount") && opts.get("amount") != null) {
                    if (!coinSelect) {
                        ctx.sendLocalized("commands.slots.errors.amount_not_ticket", EmoteReference.ERROR);
                        return;
                    }

                    String amount = opts.get("amount");

                    if (amount.isEmpty()) {
                        ctx.sendLocalized("commands.slots.errors.no_amount", EmoteReference.ERROR);
                        return;
                    }

                    try {
                        amountN = Integer.parseInt(amount);
                    } catch (NumberFormatException e) {
                        ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR);
                        return;
                    }

                    if (playerInventory.getAmount(Items.SLOT_COIN) < amountN) {
                        ctx.sendLocalized("commands.slots.errors.not_enough_tickets", EmoteReference.ERROR);
                        return;
                    }

                    money += 58 * amountN;
                }

                if (args.length >= 1 && !coinSelect) {
                    try {
                        Long parsed = new RoundedMetricPrefixFormat().parseObject(args[0], new ParsePosition(0));

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

                long playerMoney = season ? seasonalPlayer.getMoney() : player.getMoney();

                if (playerMoney < money && !coinSelect) {
                    ctx.sendLocalized("commands.slots.errors.not_enough_money", EmoteReference.SAD);
                    return;
                }

                if (!handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx))
                    return;

                if (coinSelect) {
                    if (playerInventory.containsItem(Items.SLOT_COIN)) {
                        playerInventory.process(new ItemStack(Items.SLOT_COIN, -amountN));
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

                I18nContext languageContext = ctx.getLanguageContext();

                StringBuilder message = new StringBuilder(String.format(languageContext.withRoot("commands", "slots.roll"), EmoteReference.DICE, coinSelect ? amountN + " " + languageContext.get("commands.slots.tickets") : money + " " + languageContext.get("commands.slots.credits")));
                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < 9; i++) {
                    if (i > 1 && i % 3 == 0) {
                        builder.append("\n");
                    }

                    builder.append(emotes[random.nextInt(emotes.length)]);
                }

                String toSend = builder.toString();
                int gains = 0;
                String[] rows = toSend.split("\\r?\\n");

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

                    if (coinSelect && amountN > ItemStack.MAX_STACK_SIZE - random.nextInt(650))
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
                    message.append(toSend).append("\n\n").append(String.format(languageContext.withRoot("commands", "slots.lose"), EmoteReference.SAD));
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
                                "`~>slots <credits>` - Puts x credits on the slot machine. You can put a maximum of " + SLOTS_MAX_MONEY + " coins.\n" +
                                "`~>slots -useticket` - Rolls the slot machine with one slot coin.\n" +
                                "You can specify the amount of tickets to use using `-amount` (for example `~>slots -useticket -amount 10`)")
                        .build();
            }
        });
    }

    private void proceedGamble(Context ctx, Player player, int luck, Random r, long i, long gains, long bet) {
        PlayerStats stats = MantaroData.db().getPlayerStats(ctx.getMember());
        PlayerData data = player.getData();

        if (luck > r.nextInt(140)) {
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

            long oldMoney = player.getMoney();
            player.setMoney(Math.max(0, player.getMoney() - i));

            stats.getData().incrementGambleLose();
            ctx.sendLocalized("commands.gamble.lose", EmoteReference.DICE,
                    (player.getMoney() == 0 ? ctx.getLanguageContext().get("commands.gamble.lose_all") + " " + oldMoney : i),
                    EmoteReference.SAD
            );
        }

        player.setLocked(false);
        player.saveAsync();
        stats.saveAsync();
    }
}
