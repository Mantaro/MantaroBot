/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.*;
import net.kodehawa.mantarobot.commands.currency.item.special.Pickaxe;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.SeasonalPlayerData;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.UnifiedPlayer;
import net.kodehawa.mantarobot.commands.utils.RoundedMetricPrefixFormat;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.*;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kodehawa.mantarobot.utils.Utils.handleDefaultIncreasingRatelimit;

/**
 * Basically part of CurrencyCmds, but only the money commands.
 */
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
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();

                if (args.length > 0 && event.getMessage().getMentionedUsers().isEmpty() && args[0].equalsIgnoreCase("-check")) {
                    long rl = rateLimiter.getRemaniningCooldown(event.getAuthor());

                    channel.sendMessageFormat(languageContext.get("commands.daily.check"), EmoteReference.TALKING,
                            (rl) > 0 ? Utils.getHumanizedTime(rl) : languageContext.get("commands.daily.about_now")
                    ).queue();
                    return;
                }

                long money = 150L;
                User mentionedUser = null;
                List<User> mentioned = event.getMessage().getMentionedUsers();

                if (!mentioned.isEmpty())
                    mentionedUser = event.getMessage().getMentionedUsers().get(0);

                if (mentionedUser != null && mentionedUser.isBot()) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "daily.errors.bot"), EmoteReference.ERROR).queue();
                    return;
                }

                UnifiedPlayer unifiedPlayer = UnifiedPlayer.of((mentionedUser != null ? mentionedUser : event.getAuthor()), getConfig().getCurrentSeason());
                Player player = unifiedPlayer.getPlayer();

                if (player.isLocked()) {
                    channel.sendMessage(EmoteReference.ERROR + (mentionedUser != null ? languageContext.withRoot("commands", "daily.errors.receipt_locked") : languageContext.withRoot("commands", "daily.errors.own_locked"))).queue();
                    return;
                }

                if (!handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, languageContext, false))
                    return;

                PlayerData playerData = player.getData();
                String streak;
                String crate = "";

                String playerId = player.getUserId();

                if (playerId.equals(event.getAuthor().getId())) {
                    if (System.currentTimeMillis() - playerData.getLastDailyAt() < TimeUnit.HOURS.toMillis(50)) {
                        playerData.setDailyStreak(playerData.getDailyStreak() + 1);
                        streak = String.format(languageContext.withRoot("commands", "daily.streak.up"), playerData.getDailyStreak());
                    } else {
                        if (playerData.getDailyStreak() == 0) {
                            streak = languageContext.withRoot("commands", "daily.streak.first_time");
                        } else {
                            streak = String.format(languageContext.withRoot("commands", "daily.streak.lost_streak"), playerData.getDailyStreak());
                        }

                        playerData.setDailyStreak(1);
                    }

                    if (playerData.getDailyStreak() > 5) {
                        int bonus = 150;
                        if (playerData.getDailyStreak() > 15)
                            bonus += Math.min(700, Math.floor(150 * playerData.getDailyStreak() / 15D));

                        streak += String.format(languageContext.withRoot("commands", "daily.streak.bonus"), bonus);
                        money += bonus;
                    }

                    if (playerData.getDailyStreak() > 10) {
                        playerData.addBadgeIfAbsent(Badge.CLAIMER);
                    }

                    if (playerData.getDailyStreak() > 100) {
                        playerData.addBadgeIfAbsent(Badge.BIG_CLAIMER);
                    }

                    if (playerData.getDailyStreak() > 10 && playerData.getDailyStreak() % 20 == 0 && player.getInventory().getAmount(Items.LOOT_CRATE) < 5000) {
                        player.getInventory().process(new ItemStack(Items.LOOT_CRATE, 1));
                        crate = "\n" + languageContext.get("commands.daily.crate");
                    }
                } else {
                    Player authorPlayer = MantaroData.db().getPlayer(event.getAuthor());
                    PlayerData authorPlayerData = authorPlayer.getData();

                    if (System.currentTimeMillis() - authorPlayerData.getLastDailyAt() < TimeUnit.HOURS.toMillis(50)) {
                        authorPlayerData.setDailyStreak(authorPlayerData.getDailyStreak() + 1);
                        streak = String.format(languageContext.withRoot("commands", "daily.streak.given.up"), authorPlayerData.getDailyStreak());
                    } else {
                        if (authorPlayerData.getDailyStreak() == 0) {
                            streak = languageContext.withRoot("commands", "daily.streak.first_time");
                        } else {
                            streak = String.format(languageContext.withRoot("commands", "daily.streak.lost_streak"), authorPlayerData.getDailyStreak());
                        }

                        authorPlayerData.setDailyStreak(1);
                    }

                    if (authorPlayerData.getDailyStreak() > 5) {
                        int bonus = 150;

                        if (authorPlayerData.getDailyStreak() > 15)
                            bonus += Math.min(1700, Math.floor(150 * authorPlayerData.getDailyStreak() / 10D));

                        streak += String.format(languageContext.withRoot("commands", "daily.streak.given.bonus"), (mentionedUser == null ? "You" : mentionedUser.getName()), bonus);
                        money += bonus;
                    }

                    if (authorPlayerData.getDailyStreak() > 10) {
                        authorPlayerData.addBadgeIfAbsent(Badge.CLAIMER);
                    }

                    if (authorPlayerData.getDailyStreak() > 100) {
                        authorPlayerData.addBadgeIfAbsent(Badge.BIG_CLAIMER);
                    }

                    if (authorPlayerData.getDailyStreak() > 10 && authorPlayerData.getDailyStreak() % 20 == 0 && authorPlayer.getInventory().getAmount(Items.LOOT_CRATE) < 5000) {
                        authorPlayer.getInventory().process(new ItemStack(Items.LOOT_CRATE, 1));
                        crate = "\n" + languageContext.get("commands.daily.crate");
                    }

                    authorPlayerData.setLastDailyAt(System.currentTimeMillis());
                    authorPlayer.save();
                }

                String sellout = random.nextBoolean() ? "" : languageContext.get("commands.daily.sellout");

                if (mentionedUser != null && !mentionedUser.getId().equals(event.getAuthor().getId())) {
                    money = money + r.nextInt(90);

                    DBUser user = MantaroData.db().getUser(event.getAuthor());
                    UserData userData = user.getData();

                    DBUser mentionedDBUser = MantaroData.db().getUser(mentionedUser.getId());
                    UserData mentionedUserData = user.getData();

                    //Marriage status + inventory ring
                    Marriage marriage = userData.getMarriage();
                    if(marriage != null && mentionedUser.getId().equals(marriage.getOtherPlayer(event.getAuthor().getId())) && player.getInventory().containsItem(Items.RING)) {
                        money = money + Math.max(10, r.nextInt(100));
                    }

                    //Mutual waifu status.
                    if (userData.getWaifus().containsKey(mentionedUser.getId()) && mentionedUserData.getWaifus().containsKey(event.getAuthor().getId())) {
                        money = money + Math.max(5, r.nextInt(70));
                    }

                    unifiedPlayer.addMoney(money);
                    playerData.setLastDailyAt(System.currentTimeMillis());
                    unifiedPlayer.save();

                    channel.sendMessageFormat(languageContext.withRoot("commands", "daily.given_credits") + crate, EmoteReference.CORRECT, money, mentionedUser.getName(), streak, sellout).queue();
                    return;
                }

                unifiedPlayer.addMoney(money);
                //Player object comes from UnifiedPlayer, so it should update here too.
                playerData.setLastDailyAt(System.currentTimeMillis());
                unifiedPlayer.save();

                channel.sendMessageFormat(languageContext.withRoot("commands", "daily.credits") + crate, EmoteReference.CORRECT, money, streak, sellout).queue();
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

            SecureRandom r = new SecureRandom();

            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                final TextChannel channel = event.getChannel();

                Player player = MantaroData.db().getPlayer(event.getAuthor());

                if (player.getMoney() <= 0) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "gamble.no_credits"), EmoteReference.SAD).queue();
                    return;
                }

                if (player.getMoney() > GAMBLE_ABSOLUTE_MAX_MONEY) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "gamble.too_much_money"), EmoteReference.ERROR2, GAMBLE_ABSOLUTE_MAX_MONEY).queue();
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
                    channel.sendMessageFormat(languageContext.withRoot("commands", "gamble.invalid_money_or_modifier"), EmoteReference.ERROR2).queue();
                    return;
                } catch (UnsupportedOperationException e) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "gamble.not_enough_money"), EmoteReference.ERROR2).queue();
                    return;
                } catch (ParseException e) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "gamble.invalid_percentage"), EmoteReference.ERROR2).queue();
                    return;
                }

                if (i < 100) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "gamble.too_little"), EmoteReference.ERROR2).queue();
                    return;
                }

                if (i > GAMBLE_MAX_MONEY) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "gamble.too_much"), EmoteReference.ERROR2, GAMBLE_MAX_MONEY).queue();
                    return;
                }

                //Handle ratelimits after all of the exceptions/error messages could've been thrown already.
                if (!handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, languageContext))
                    return;

                User user = event.getAuthor();
                long gains = (long) (i * multiplier);
                gains = Math.round(gains * 0.45);

                final int finalLuck = luck;
                final long finalGains = gains;

                if (i >= 60000000) {
                    player.setLocked(true);
                    player.save();
                    channel.sendMessageFormat(languageContext.withRoot("commands", "gamble.confirmation_message"), EmoteReference.WARNING, i).queue();
                    InteractiveOperations.create(channel, event.getAuthor().getIdLong(), 30, new InteractiveOperation() {
                        @Override
                        public int run(GuildMessageReceivedEvent e) {
                            if (e.getAuthor().getId().equals(user.getId())) {
                                if (e.getMessage().getContentRaw().equalsIgnoreCase("yes")) {
                                    proceedGamble(event, languageContext, player, finalLuck, random, i, finalGains, i);
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
                            channel.sendMessageFormat(languageContext.get("general.operation_timed_out"), EmoteReference.ERROR2).queue();
                            player.setLocked(false);
                            player.saveAsync();
                        }
                    });
                    return;
                }

                proceedGamble(event, languageContext, player, luck, random, i, gains, i);
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
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                UnifiedPlayer unifiedPlayer = UnifiedPlayer.of(event.getAuthor(), getConfig().getCurrentSeason());

                Player player = unifiedPlayer.getPlayer();
                DBUser dbUser = MantaroData.db().getUser(event.getAuthor());
                TextChannel channel = event.getChannel();
                Member member = event.getMember();

                if (player.isLocked()) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "loot.player_locked"), EmoteReference.ERROR).queue();
                    return;
                }

                if (!handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, languageContext, false))
                    return;

                LocalDate today = LocalDate.now(zoneId);
                LocalDate eventStart = today.withMonth(Month.DECEMBER.getValue()).withDayOfMonth(23);
                LocalDate eventStop = eventStart.plusDays(3); //Up to the 25th
                TextChannelGround ground = TextChannelGround.of(event);

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
                    String overflow;

                    if (player.getInventory().merge(loot))
                        overflow = languageContext.withRoot("commands", "loot.item_overflow");
                    else
                        overflow = "";

                    if (moneyFound != 0) {
                        if (unifiedPlayer.addMoney(moneyFound)) {
                            channel.sendMessageFormat(languageContext.withRoot("commands", "loot.with_item.found"), EmoteReference.POPPER, s, moneyFound, overflow).queue();
                        } else {
                            channel.sendMessageFormat(languageContext.withRoot("commands", "loot.with_item.found_but_overflow"), EmoteReference.POPPER, s, moneyFound, overflow).queue();
                        }
                    } else {
                        channel.sendMessageFormat(languageContext.withRoot("commands", "loot.with_item.found_only_item_but_overflow"), EmoteReference.MEGA, s, overflow).queue();
                    }

                } else {
                    if (moneyFound != 0) {
                        if (unifiedPlayer.addMoney(moneyFound)) {
                            channel.sendMessageFormat(languageContext.withRoot("commands", "loot.without_item.found"), EmoteReference.POPPER, moneyFound).queue();
                        } else {
                            channel.sendMessageFormat(languageContext.withRoot("commands", "loot.without_item.found_but_overflow"), EmoteReference.POPPER, moneyFound).queue();
                        }
                    } else {
                        int dust = dbUser.getData().increaseDustLevel(r.nextInt(2));
                        String msg = String.format(languageContext.withRoot("commands", "loot.dust"), dust);
                        dbUser.save();

                        if (r.nextInt(100) > 93) {
                            msg += languageContext.withRoot("commands", "loot.easter");
                        }

                        channel.sendMessage(EmoteReference.SAD + msg).queue();
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
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                Map<String, String> t = getArguments(content);
                content = Utils.replaceArguments(t, content, "season", "s").trim();
                boolean isSeasonal = t.containsKey("season") || t.containsKey("s");

                ManagedDatabase db = MantaroData.db();
                User user = event.getAuthor();
                boolean isExternal = false;

                List<Member> found = FinderUtil.findMembers(content, event.getGuild());
                if (found.isEmpty() && !content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("general.search_no_result"), EmoteReference.ERROR).queue();
                    return;
                }

                if (found.size() > 1 && !content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("general.too_many_users_found"),
                            EmoteReference.THINKING, found.stream()
                                    .limit(10)
                                    .map(m -> m.getUser().getName() + "#" + m.getUser().getDiscriminator())
                                    .collect(Collectors.joining(", "))).queue();
                    return;
                }

                if (found.size() == 1) {
                    user = found.get(0).getUser();
                    isExternal = true;
                }

                if (user.isBot()) {
                    channel.sendMessageFormat(languageContext.get("commands.balance.bot_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                long balance = isSeasonal ? db.getPlayerForSeason(user, getConfig().getCurrentSeason()).getMoney() : db.getPlayer(user).getMoney();

                channel.sendMessage(EmoteReference.DIAMOND + (isExternal ?
                        String.format(languageContext.withRoot("commands", "balance.external_balance"), user.getName(), balance) :
                        String.format(languageContext.withRoot("commands", "balance.own_balance"), balance))).queue();
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
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Map<String, String> opts = getArguments(args);
                TextChannel channel = event.getChannel();

                long money = 50;
                int slotsChance = 25; //25% raw chance of winning, completely random chance of winning on the other random iteration
                boolean isWin = false;
                boolean coinSelect = false;
                int amountN = 1;

                final ManagedDatabase db = MantaroData.db();
                Player player = db.getPlayer(event.getAuthor());
                PlayerStats stats = db.getPlayerStats(event.getMember());
                SeasonPlayer seasonalPlayer = null; //yes
                boolean season = false;

                if (opts.containsKey("season")) {
                    season = true;
                    seasonalPlayer = db.getPlayerForSeason(event.getAuthor(), getConfig().getCurrentSeason());
                }

                if (opts.containsKey("useticket")) {
                    coinSelect = true;
                }

                Inventory playerInventory = season ? seasonalPlayer.getInventory() : player.getInventory();

                if (opts.containsKey("amount") && opts.get("amount") != null) {
                    if (!coinSelect) {
                        channel.sendMessageFormat(languageContext.withRoot("commands", "slots.errors.amount_not_ticket"), EmoteReference.ERROR).queue();
                        return;
                    }

                    String amount = opts.get("amount");

                    if (amount.isEmpty()) {
                        channel.sendMessageFormat(languageContext.withRoot("commands", "slots.errors.no_amount"), EmoteReference.ERROR).queue();
                        return;
                    }

                    try {
                        amountN = Integer.parseInt(amount);
                    } catch (NumberFormatException e) {
                        channel.sendMessageFormat(languageContext.get("general.invalid_number"), EmoteReference.ERROR).queue();
                    }

                    if (playerInventory.getAmount(Items.SLOT_COIN) < amountN) {
                        channel.sendMessageFormat(languageContext.withRoot("commands", "slots.errors.not_enough_tickets"), EmoteReference.ERROR).queue();
                        return;
                    }

                    money += 58 * amountN;
                }

                if (args.length >= 1 && !coinSelect) {
                    try {
                        Long parsed = new RoundedMetricPrefixFormat().parseObject(args[0], new ParsePosition(0));

                        if (parsed == null) {
                            channel.sendMessageFormat(languageContext.withRoot("commands", "slots.errors.no_valid_amount"), EmoteReference.ERROR).queue();
                            return;
                        }

                        money = Math.abs(parsed);

                        if (money < 25) {
                            channel.sendMessageFormat(languageContext.withRoot("commands", "slots.errors.below_minimum"), EmoteReference.ERROR).queue();
                            return;
                        }

                        if (money > SLOTS_MAX_MONEY) {
                            channel.sendMessageFormat(languageContext.withRoot("commands", "slots.errors.too_much_money"), EmoteReference.WARNING).queue();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        channel.sendMessageFormat(languageContext.get("general.invalid_number"), EmoteReference.ERROR).queue();
                        return;
                    }
                }

                long playerMoney = season ? seasonalPlayer.getMoney() : player.getMoney();

                if (playerMoney < money && !coinSelect) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "slots.errors.not_enough_money"), EmoteReference.SAD).queue();
                    return;
                }

                if (!handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, languageContext))
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
                        channel.sendMessageFormat(languageContext.withRoot("commands", "slots.errors.no_tickets"), EmoteReference.SAD).queue();
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
                    message.append(toSend).append("\n\n").append(String.format(languageContext.withRoot("commands", "slots.win"), gains, money)).append(EmoteReference.POPPER);

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
                channel.sendMessage(message.toString()).queue();
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

    @Subscribe
    public void mine(CommandRegistry cr) {
        cr.register("mine", new SimpleCommand(Category.CURRENCY) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .limit(1)
                    .spamTolerance(2)
                    .cooldown(5, TimeUnit.MINUTES)
                    .maxCooldown(5, TimeUnit.MINUTES)
                    .randomIncrement(false)
                    .premiumAware(true)
                    .pool(MantaroData.getDefaultJedisPool())
                    .prefix("mine")
                    .build();

            final Random r = new Random();

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();

                Map<String, String> t = getArguments(content);
                boolean isSeasonal = t.containsKey("season") || t.containsKey("s");

                final User user = event.getAuthor();
                final ManagedDatabase db = MantaroData.db();

                Player player = db.getPlayer(user);
                PlayerData playerData = player.getData();

                SeasonPlayer seasonalPlayer = db.getPlayerForSeason(user, getConfig().getCurrentSeason());
                SeasonalPlayerData seasonalPlayerData = seasonalPlayer.getData();

                DBUser dbUser = db.getUser(user);
                UserData userData = dbUser.getData();

                Inventory inventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();

                Pickaxe item;
                int equipped = isSeasonal ?
                        seasonalPlayerData.getEquippedItems().of(PlayerEquipment.EquipmentType.PICK) :
                        userData.getEquippedItems().of(PlayerEquipment.EquipmentType.PICK);

                if (equipped == 0) {
                    channel.sendMessageFormat(languageContext.get("commands.mine.not_equipped"), EmoteReference.ERROR).queue();
                    return;
                }

                item = (Pickaxe) Items.fromId(equipped);

                if (!handleDefaultIncreasingRatelimit(rateLimiter, user, event, languageContext, false))
                    return;

                //If it's broken return
                if (Items.handleDurability(event, languageContext, item, player, dbUser, seasonalPlayer, isSeasonal))
                    return;

                long money = Math.max(30, r.nextInt(150)); //30 to 150 credits.

                //Add money buff to higher pickaxes.
                if (item == Items.GEM2_PICKAXE || item == Items.GEM1_PICKAXE)
                    money += r.nextInt(100);
                if (item == Items.GEM5_PICKAXE_2)
                    money += r.nextInt(300);

                boolean waifuHelp = false;
                if (Items.handleEffect(PlayerEquipment.EquipmentType.POTION, userData.getEquippedItems(), Items.WAIFU_PILL, dbUser)) {
                    if (userData.getWaifus().entrySet().stream().anyMatch((w) -> w.getValue() > 10_000_000L)) {
                        money += Math.max(45, random.nextInt(200));
                        waifuHelp = true;
                    }
                }

                String reminder = r.nextInt(6) == 0 ? languageContext.get("commands.mine.reminder") : "";
                String message = String.format(languageContext.get("commands.mine.success") + reminder, item.getEmoji(), money, item.getName());

                boolean hasPotion = Items.handleEffect(PlayerEquipment.EquipmentType.POTION, userData.getEquippedItems(), Items.POTION_HASTE, dbUser);

                //Diamond find
                if (r.nextInt(400) > (hasPotion ? 290 : 350)) {
                    if (inventory.getAmount(Items.DIAMOND) == 5000) {
                        message += "\n" + languageContext.withRoot("commands", "mine.diamond.overflow");
                        money += Items.DIAMOND.getValue() * 0.9;
                    } else {
                        int amount = 1;
                        if (item == Items.GEM2_PICKAXE || item == Items.GEM1_PICKAXE)
                            amount += r.nextInt(2);
                        if (item == Items.GEM5_PICKAXE_2)
                            amount += r.nextInt(4);

                        inventory.process(new ItemStack(Items.DIAMOND, amount));
                        message += "\n" + EmoteReference.DIAMOND + String.format(languageContext.withRoot("commands", "mine.diamond.success"), amount);
                    }

                    playerData.addBadgeIfAbsent(Badge.MINER);
                }

                //Gem find
                if (r.nextInt(400) > (hasPotion ? 278 : 325)) {
                    List<Item> gem = Stream.of(Items.ALL)
                            .filter(i -> i.getItemType() == ItemType.MINE && !i.isHidden() && i.isSellable())
                            .collect(Collectors.toList());

                    //top notch handling for gems, 10/10 implementation -ign
                    ItemStack selectedGem = new ItemStack(gem.get(r.nextInt(gem.size())), Math.max(1, r.nextInt(5)));
                    Item itemGem = selectedGem.getItem();
                    if (inventory.getAmount(itemGem) + selectedGem.getAmount() >= 5000) {
                        message += "\n" + languageContext.withRoot("commands", "mine.gem.overflow");
                        money += itemGem.getValue() * 0.9;
                    } else {
                        inventory.process(selectedGem);
                        message += "\n" + EmoteReference.MEGA + String.format(languageContext.withRoot("commands", "mine.gem.success"), itemGem.getEmoji() + " x" + selectedGem.getAmount());
                    }

                    if (waifuHelp) {
                        message += "\n" + languageContext.get("commands.mine.waifu_help");
                    }

                    playerData.addBadgeIfAbsent(Badge.GEM_FINDER);
                }

                //Sparkle find
                if ((r.nextInt(400) > 395 && item == Items.GEM1_PICKAXE) || (r.nextInt(400) > 390 && (item == Items.GEM2_PICKAXE || item == Items.GEM5_PICKAXE_2))) {
                    Item gem = Items.GEM5_2;
                    if (inventory.getAmount(gem) + 1 >= 5000) {
                        message += "\n" + languageContext.withRoot("commands", "mine.sparkle.overflow");
                        money += gem.getValue() * 0.9;
                    } else {
                        inventory.process(new ItemStack(gem, 1));
                        message += "\n" + EmoteReference.MEGA + String.format(languageContext.withRoot("commands", "mine.sparkle.success"), gem.getEmoji());
                    }

                    playerData.addBadgeIfAbsent(Badge.GEM_FINDER);
                }

                PremiumKey key = db.getPremiumKey(dbUser.getData().getPremiumKey());
                if (r.nextInt(400) > 392) {
                    Item crate = (key != null && key.getDurationDays() > 1) ? Items.MINE_PREMIUM_CRATE : Items.MINE_CRATE;
                    if (inventory.getAmount(crate) + 1 > 5000) {
                        message += "\n" + languageContext.withRoot("commands", "mine.crate.overflow");
                    } else {
                        inventory.process(new ItemStack(crate, 1));
                        message += "\n" + EmoteReference.MEGA + String.format(languageContext.withRoot("commands", "mine.crate.success"), crate.getEmoji(), crate.getName());
                    }
                }

                channel.sendMessage(message).queue();
                if (isSeasonal) {
                    seasonalPlayer.addMoney(money);
                    seasonalPlayer.saveAsync();
                } else {
                    player.addMoney(money);
                }

                //Due to badges.
                player.saveAsync();
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Mines minerals to gain some credits. A bit more lucrative than loot, but needs pickaxes.")
                        .setUsage("`~>mine [pick]` - Mines. You can gain minerals or mineral fragments by mining. This can used later on to cast rods or picks for better chances.")
                        .addParameter("pick", "The pick to use to mine. You can either use the emoji or the full name. " +
                                "This is optional, not specifying it will cause the command to use the default pick or your equipped pick.")
                        .setSeasonal(true)
                        .build();
            }
        });
    }

    private void proceedGamble(GuildMessageReceivedEvent event, I18nContext languageContext, Player player, int luck, Random r, long i, long gains, long bet) {
        PlayerStats stats = MantaroData.db().getPlayerStats(event.getMember());
        TextChannel channel = event.getChannel();
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

                channel.sendMessageFormat(languageContext.withRoot("commands", "gamble.win"), EmoteReference.DICE, gains).queue();
            } else {
                channel.sendMessageFormat(languageContext.withRoot("commands", "gamble.win_overflow"), EmoteReference.DICE, gains).queue();
            }
        } else {
            if (bet == GAMBLE_MAX_MONEY) {
                data.addBadgeIfAbsent(Badge.RISKY_ORDEAL);
            }

            long oldMoney = player.getMoney();
            player.setMoney(Math.max(0, player.getMoney() - i));

            stats.getData().incrementGambleLose();
            channel.sendMessageFormat(languageContext.withRoot("commands", "gamble.lose"), EmoteReference.DICE, (player.getMoney() == 0 ? languageContext.withRoot("commands", "gamble.lose_all") + " " + oldMoney : i), EmoteReference.SAD).queue();
        }

        player.setLocked(false);
        player.saveAsync();
        stats.saveAsync();
    }
}
