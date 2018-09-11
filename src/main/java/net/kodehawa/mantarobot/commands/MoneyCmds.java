/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.texts.StringUtils;
import com.google.common.eventbus.Subscribe;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.utils.RoundedMetricPrefixFormat;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PlayerStats;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;

import java.security.SecureRandom;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kodehawa.mantarobot.utils.Utils.handleDefaultRatelimit;

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
        final RateLimiter rateLimiter = new RateLimiter(TimeUnit.HOURS, 24);
        Random r = new Random();
        cr.register("daily", new SimpleCommand(Category.CURRENCY) {
            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(args.length > 0 && event.getMessage().getMentionedUsers().isEmpty() && args[0].equalsIgnoreCase("-check")) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.daily.check"), EmoteReference.TALKING,
                            (rateLimiter.tryAgainIn(event.getAuthor()) > 0 ?
                                    Utils.getHumanizedTime(rateLimiter.tryAgainIn(event.getAuthor())) : languageContext.get("commands.daily.about_now"))
                    ).queue();
                    return;
                }

                long money = 150L;
                User mentionedUser = null;
                List<User> mentioned = event.getMessage().getMentionedUsers();

                if(!mentioned.isEmpty())
                    mentionedUser = event.getMessage().getMentionedUsers().get(0);

                if(mentionedUser != null && mentionedUser.isBot()) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "daily.errors.bot"), EmoteReference.ERROR).queue();
                    return;
                }

                Player player = mentionedUser != null ? MantaroData.db().getPlayer(event.getGuild().getMember(mentionedUser)) : MantaroData.db().getPlayer(event.getMember());

                if(player.isLocked()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + (mentionedUser != null ? languageContext.withRoot("commands", "daily.errors.receipt_locked") : languageContext.withRoot("commands", "daily.errors.own_locked"))).queue();
                    return;
                }

                if(!handleDefaultRatelimit(rateLimiter, event.getAuthor(), event)) return;

                PlayerData playerData = player.getData();
                String streak;

                String playerId = player.getUserId();

                if(playerId.equals(event.getAuthor().getId())) {
                    if(System.currentTimeMillis() - playerData.getLastDailyAt() < TimeUnit.HOURS.toMillis(50)) {
                        playerData.setDailyStreak(playerData.getDailyStreak() + 1);
                        streak = String.format(languageContext.withRoot("commands", "daily.streak.up"), playerData.getDailyStreak());
                    } else {
                        if(playerData.getDailyStreak() == 0) {
                            streak = languageContext.withRoot("commands", "daily.streak.first_time");
                        } else {
                            streak = String.format(languageContext.withRoot("commands", "daily.streak.lost_streak"), playerData.getDailyStreak());
                        }

                        playerData.setDailyStreak(1);
                    }

                    if(playerData.getDailyStreak() > 5) {
                        int bonus = 150;
                        if(playerData.getDailyStreak() > 15)
                            bonus += Math.min(700, Math.floor(150 * playerData.getDailyStreak() / 15));

                        streak += String.format(languageContext.withRoot("commands", "daily.streak.bonus"), bonus);
                        money += bonus;
                    }

                    if(playerData.getDailyStreak() > 10) {
                        playerData.addBadgeIfAbsent(Badge.CLAIMER);
                    }

                    if(playerData.getDailyStreak() > 100) {
                        playerData.addBadgeIfAbsent(Badge.BIG_CLAIMER);
                    }
                } else {
                    Player authorPlayer = MantaroData.db().getPlayer(event.getAuthor());
                    PlayerData authorPlayerData = authorPlayer.getData();

                    if(System.currentTimeMillis() - authorPlayerData.getLastDailyAt() < TimeUnit.HOURS.toMillis(50)) {
                        authorPlayerData.setDailyStreak(authorPlayerData.getDailyStreak() + 1);
                        streak = String.format(languageContext.withRoot("commands", "daily.streak.given.up"), authorPlayerData.getDailyStreak());
                    } else {
                        if(authorPlayerData.getDailyStreak() == 0) {
                            streak = languageContext.withRoot("commands", "daily.streak.first_time");
                        } else {
                            streak = String.format(languageContext.withRoot("commands", "daily.streak.lost_streak"), authorPlayerData.getDailyStreak());
                        }

                        authorPlayerData.setDailyStreak(1);
                    }

                    if(authorPlayerData.getDailyStreak() > 5) {
                        int bonus = 150;

                        if(authorPlayerData.getDailyStreak() > 15)
                            bonus += Math.min(700, Math.floor(150 * authorPlayerData.getDailyStreak() / 15));

                        streak += String.format(languageContext.withRoot("commands", "daily.streak.given.bonus"), (mentionedUser == null ? "You" : mentionedUser.getName()), bonus);
                        money += bonus;
                    }

                    if(authorPlayerData.getDailyStreak() > 10) {
                        authorPlayerData.addBadgeIfAbsent(Badge.CLAIMER);
                    }

                    if(authorPlayerData.getDailyStreak() > 100) {
                        authorPlayerData.addBadgeIfAbsent(Badge.BIG_CLAIMER);
                    }

                    authorPlayerData.setLastDailyAt(System.currentTimeMillis());
                    authorPlayer.save();
                }

                if(mentionedUser != null && !mentionedUser.getId().equals(event.getAuthor().getId())) {
                    money = money + r.nextInt(90);

                    DBUser user = MantaroData.db().getUser(event.getAuthor());
                    UserData userData = user.getData();

                    DBUser mentionedDBUser = MantaroData.db().getUser(mentionedUser.getId());
                    UserData mentionedUserData = user.getData();

                    //so many parenthesis is this lisp
                    if((userData.getMarriage() != null && mentionedUser.getId().equals(userData.getMarriage().getOtherPlayer(event.getAuthor().getId())))
                            || (mentionedUser.getId().equals(player.getData().getMarriedWith()) && userData.getMarriage() == null)) {
                        if(player.getInventory().containsItem(Items.RING)) {
                            money = money + Math.max(10, r.nextInt(100));
                        }
                    }

                    //Mutual waifu status.
                    if(userData.getWaifus().containsKey(mentionedUser.getId()) && mentionedUserData.getWaifus().containsKey(event.getAuthor().getId())) {
                        money = money + Math.max(5, r.nextInt(70));
                    }

                    player.addMoney(money);
                    playerData.setLastDailyAt(System.currentTimeMillis());
                    player.save();

                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "daily.given_credits"), EmoteReference.CORRECT, money, mentionedUser.getName(), streak).queue();
                    return;
                }

                player.addMoney(money);
                playerData.setLastDailyAt(System.currentTimeMillis());
                player.save();

                event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "daily.credits"), EmoteReference.CORRECT, money, streak).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Daily command")
                        .setDescription("**Gives you $150 credits per day (or between 150 and 180 if you transfer it to another person)**.\n" +
                                "This command gives a reward for claiming it every day.")
                        .build();
            }
        });

        cr.registerAlias("daily", "dailies");
    }

    @Subscribe
    public void gamble(CommandRegistry cr) {
        cr.register("gamble", new SimpleCommand(Category.CURRENCY) {
            final RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS, 35, true);
            SecureRandom r = new SecureRandom();

            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Player player = MantaroData.db().getPlayer(event.getMember());

                if(player.getMoney() <= 0) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "gamble.no_credits"), EmoteReference.SAD).queue();
                    return;
                }

                if(player.getMoney() > GAMBLE_ABSOLUTE_MAX_MONEY) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "gamble.too_much_money"), EmoteReference.ERROR2, GAMBLE_ABSOLUTE_MAX_MONEY).queue();
                    return;
                }

                if(!handleDefaultRatelimit(rateLimiter, event.getAuthor(), event))
                    return;

                double multiplier;
                long i;
                int luck;
                try {
                    switch(content) {
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
                            if(i > player.getMoney() || i < 0) throw new UnsupportedOperationException();
                            multiplier = 1.1d + (i / player.getMoney() * r.nextInt(1300) / 1000d);
                            luck = 17 + (int) (multiplier * 13) + r.nextInt(12);
                            break;
                    }
                } catch(NumberFormatException | NullPointerException e) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "gamble.invalid_money_or_modifier"), EmoteReference.ERROR2).queue();
                    return;
                } catch(UnsupportedOperationException e) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "gamble.not_enough_money"), EmoteReference.ERROR2).queue();
                    return;
                } catch(ParseException e) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "gamble.invalid_percentage"), EmoteReference.ERROR2).queue();
                    return;
                }

                if(i < 100) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "gamble.too_little"), EmoteReference.ERROR2).queue();
                    return;
                }

                if(i > GAMBLE_MAX_MONEY) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "gamble.too_much"), EmoteReference.ERROR2, GAMBLE_MAX_MONEY).queue();
                    return;
                }

                User user = event.getAuthor();
                long gains = (long) (i * multiplier);
                gains = Math.round(gains * 0.45);

                final int finalLuck = luck;
                final long finalGains = gains;

                if(i >= 60000000) {
                    player.setLocked(true);
                    player.save();
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "gamble.confirmation_message"), EmoteReference.WARNING, i).queue();
                    InteractiveOperations.create(event.getChannel(), event.getAuthor().getIdLong(),30, new InteractiveOperation() {
                        @Override
                        public int run(GuildMessageReceivedEvent e) {
                            if(e.getAuthor().getId().equals(user.getId())) {
                                if(e.getMessage().getContentRaw().equalsIgnoreCase("yes")) {
                                    proceedGamble(event, languageContext, player, finalLuck, random, i, finalGains);
                                    return COMPLETED;
                                } else if(e.getMessage().getContentRaw().equalsIgnoreCase("no")) {
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
                            event.getChannel().sendMessageFormat(languageContext.get("general.operation_timed_out"), EmoteReference.ERROR2).queue();
                            player.setLocked(false);
                            player.saveAsync();
                        }
                    });
                    return;
                }

                proceedGamble(event, languageContext, player, luck, random, i, gains);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Gamble command")
                        .setDescription("Gambles your money")
                        .addField("Usage", "~>gamble <all/half/quarter> or ~>gamble <amount>\n" +
                                "You can also use percentages now, for example `~>gamble 35%`", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void loot(CommandRegistry cr) {
        cr.register("loot", new SimpleCommand(Category.CURRENCY) {
            final RateLimiter rateLimiter = new RateLimiter(TimeUnit.MINUTES, 5, true);
            final ZoneId zoneId = ZoneId.systemDefault();
            final Random r = new Random();

            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Player player = MantaroData.db().getPlayer(event.getMember());
                TextChannel channel = event.getChannel();

                if(player.isLocked()) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "loot.player_locked"), EmoteReference.ERROR).queue();
                    return;
                }

                if(!handleDefaultRatelimit(rateLimiter, event.getAuthor(), event))
                    return;

                LocalDate today = LocalDate.now(zoneId);
                LocalDate eventStart = today.withMonth(Month.DECEMBER.getValue()).withDayOfMonth(23);
                LocalDate eventStop = eventStart.plusDays(3); //Up to the 25th
                TextChannelGround ground = TextChannelGround.of(event);

                if(today.isEqual(eventStart) || (today.isAfter(eventStart) && today.isBefore(eventStop))) {
                    ground.dropItemWithChance(Items.CHRISTMAS_TREE_SPECIAL, 4);
                    ground.dropItemWithChance(Items.BELL_SPECIAL, 4);
                }

                if(r.nextInt(100) == 0) { //1 in 100 chance of it dropping a loot crate.
                    ground.dropItem(Items.LOOT_CRATE);
                    if(player.getData().addBadgeIfAbsent(Badge.LUCKY)) player.saveAsync();
                }

                List<ItemStack> loot = ground.collectItems();
                int moneyFound = ground.collectMoney() + Math.max(0, r.nextInt(50) - 10);

                if(MantaroData.db().getUser(event.getMember()).isPremium() && moneyFound > 0) {
                    moneyFound = moneyFound + random.nextInt(moneyFound);
                }


                if(!loot.isEmpty()) {
                    String s = ItemStack.toString(ItemStack.reduce(loot));
                    String overflow;

                    if(player.getInventory().merge(loot))
                        overflow = languageContext.withRoot("commands", "loot.item_overflow");
                    else
                        overflow = "";

                    if(moneyFound != 0) {
                        if(player.addMoney(moneyFound)) {
                            channel.sendMessageFormat(languageContext.withRoot("commands", "loot.with_item.found"),
                                    EmoteReference.POPPER, s, moneyFound, overflow).queue();
                        } else {
                            channel.sendMessageFormat(languageContext.withRoot("commands", "loot.with_item.found_but_overflow"),
                                    EmoteReference.POPPER, s, moneyFound, overflow).queue();
                        }
                    } else {
                        channel.sendMessageFormat(languageContext.withRoot("commands", "loot.with_item.found_only_item_but_overflow"), EmoteReference.MEGA, s, overflow).queue();
                    }

                } else {
                    if(moneyFound != 0) {
                        if(player.addMoney(moneyFound)) {
                            channel.sendMessageFormat(languageContext.withRoot("commands", "loot.without_item.found"), EmoteReference.POPPER, moneyFound).queue();
                        } else {
                            channel.sendMessageFormat(languageContext.withRoot("commands", "loot.without_item.found_but_overflow"), EmoteReference.POPPER, moneyFound).queue();
                        }
                    } else {
                        String msg = languageContext.withRoot("commands", "loot.dust");

                        if(r.nextInt(100) > 93) {
                            msg += languageContext.withRoot("commands", "loot.easter");
                        }

                        channel.sendMessage(EmoteReference.SAD + msg).queue();
                    }
                }

                player.saveAsync();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Loot command")
                        .setDescription("**Loot the current chat for items, for usage in Mantaro's currency system.**\n"
                                + "Currently, there are ``" + Items.ALL.length + "`` items available in chance," +
                                "in which you have a `random chance` of getting one or more.")
                        .addField("Note", "The channel ground is limited to 25 items per stack.", false)
                        .addField("Usage", "~>loot", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void balance(CommandRegistry cr) {
        cr.register("balance", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                User user = event.getAuthor();
                boolean isExternal = false;
                List<Member> found = FinderUtil.findMembers(content, event.getGuild());
                if(found.isEmpty() && !content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("general.search_no_result"), EmoteReference.ERROR).queue();
                    return;
                }

                if(found.size() > 1 && !content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("general.too_many_users_found"),
                            EmoteReference.THINKING, found.stream()
                                    .map(m -> m.getUser().getName() + "#" + m.getUser().getDiscriminator())
                                    .collect(Collectors.joining(", "))).queue();
                    return;
                }

                if(found.size() == 1) {
                    user = found.get(0).getUser();
                    isExternal = true;
                }

                if(user.isBot()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.balance.bot_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                long balance = MantaroData.db().getPlayer(user).getMoney();

                event.getChannel().sendMessage(EmoteReference.DIAMOND + (isExternal ?
                        String.format(languageContext.withRoot("commands", "balance.external_balance"), user.getName(), balance) :
                        String.format(languageContext.withRoot("commands", "balance.own_balance"), balance))).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return baseEmbed(event, "Balance command")
                        .setDescription("**Shows your current balance or another person's balance.**")
                        .build();
            }
        });

        cr.registerAlias("balance", "credits");
        cr.registerAlias("balance", "bal");
    }

    @Subscribe
    public void slots(CommandRegistry cr) {
        RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS, 35);
        String[] emotes = {"\uD83C\uDF52", "\uD83D\uDCB0", "\uD83D\uDCB2", "\uD83E\uDD55", "\uD83C\uDF7F", "\uD83C\uDF75", "\uD83C\uDFB6"};
        Random random = new SecureRandom();
        List<String> winCombinations = new ArrayList<>();

        for(String emote : emotes) {
            winCombinations.add(emote + emote + emote);
        }

        cr.register("slots", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Map<String, Optional<String>> opts = StringUtils.parse(args);

                long money = 50;
                int slotsChance = 25; //25% raw chance of winning, completely random chance of winning on the other random iteration
                boolean isWin = false;
                boolean coinSelect = false;
                int amountN = 1;

                final ManagedDatabase db = MantaroData.db();
                Player player = db.getPlayer(event.getAuthor());
                PlayerStats stats = db.getPlayerStats(event.getMember());


                if(opts.containsKey("useticket")) {
                    coinSelect = true;
                }

                if(opts.containsKey("amount") && opts.get("amount").isPresent()) {
                    if(!coinSelect) {
                        event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "slots.errors.amount_not_ticket"), EmoteReference.ERROR).queue();
                        return;
                    }

                    String amount = opts.get("amount").get();

                    if(amount.isEmpty()) {
                        event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "slots.errors.no_amount"), EmoteReference.ERROR).queue();
                        return;
                    }

                    try {
                        amountN = Integer.parseInt(amount);
                    } catch (NumberFormatException e) {
                        event.getChannel().sendMessageFormat(languageContext.get("general.invalid_number"), EmoteReference.ERROR).queue();
                    }

                   if(player.getInventory().getAmount(Items.SLOT_COIN) < amountN) {
                        event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "slots.errors.not_enough_tickets"), EmoteReference.ERROR).queue();
                        return;
                   }

                   money += 58 * amountN;
                }

                if(args.length == 1 && !coinSelect) {
                    try {
                        Long parsed = new RoundedMetricPrefixFormat().parseObject(args[0], new ParsePosition(0));

                        if(parsed == null) {
                            event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "slots.errors.no_valid_amount"), EmoteReference.ERROR).queue();
                            return;
                        }

                        money = Math.abs(parsed);

                        if(money < 25) {
                            event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "slots.errors.below_minimum"), EmoteReference.ERROR).queue();
                            return;
                        }

                        if(money > SLOTS_MAX_MONEY) {
                            event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "slots.errors.too_much_money"), EmoteReference.WARNING).queue();
                            return;
                        }
                    } catch(NumberFormatException e) {
                        event.getChannel().sendMessageFormat(languageContext.get("general.invalid_number"), EmoteReference.ERROR).queue();
                        return;
                    }
                }


                if(player.getMoney() < money && !coinSelect) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "slots.errors.not_enough_money"), EmoteReference.SAD).queue();
                    return;
                }

                if(!handleDefaultRatelimit(rateLimiter, event.getAuthor(), event)) return;

                if(coinSelect) {
                    if(player.getInventory().containsItem(Items.SLOT_COIN)) {
                        player.getInventory().process(new ItemStack(Items.SLOT_COIN, -amountN));
                        player.saveAsync();
                        slotsChance = slotsChance + 10;
                    } else {
                        event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "slots.errors.no_tickets"), EmoteReference.SAD).queue();
                        return;
                    }
                } else {
                    player.removeMoney(money);
                    player.saveAsync();
                }

                StringBuilder message = new StringBuilder(String.format(languageContext.withRoot("commands", "slots.roll"), EmoteReference.DICE, coinSelect ? amountN + " " + languageContext.get("commands.slots.tickets") : money + " " + languageContext.get("commands.slots.credits")));
                StringBuilder builder = new StringBuilder();

                for(int i = 0; i < 9; i++) {
                    if(i > 1 && i % 3 == 0) {
                        builder.append("\n");
                    }

                    builder.append(emotes[random.nextInt(emotes.length)]);
                }

                String toSend = builder.toString();
                int gains = 0;
                String[] rows = toSend.split("\\r?\\n");

                if(random.nextInt(100) < slotsChance) {
                    rows[1] = winCombinations.get(random.nextInt(winCombinations.size()));
                }

                if(winCombinations.contains(rows[1])) {
                    isWin = true;
                    gains = random.nextInt((int) Math.round(money * 1.76)) + 16;
                }

                rows[1] = rows[1] + " \u2b05";
                toSend = String.join("\n", rows);

                if(isWin) {
                    message.append(toSend).append("\n\n").append(String.format(languageContext.withRoot("commands", "slots.win"), gains, money)).append(EmoteReference.POPPER);
                    player.addMoney(gains + money);

                    stats.incrementSlotsWins();
                    stats.addSlotsWin(gains);

                    if((gains + money) > SLOTS_MAX_MONEY) {
                        player.getData().addBadgeIfAbsent(Badge.LUCKY_SEVEN);
                    }

                    player.saveAsync();
                } else {
                    stats.getData().incrementSlotsLose();
                    message.append(toSend).append("\n\n").append(String.format(languageContext.withRoot("commands", "slots.lose"), EmoteReference.SAD));
                }

                stats.saveAsync();

                message.append("\n");
                event.getChannel().sendMessage(message.toString()).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Slots Command")
                        .setDescription("**Rolls the slot machine. Requires a default of 50 coins to roll.**")
                        .addField("Considerations", "You can gain a maximum of put credits * 1.76 coins from it.\n" +
                                "You can use the `-useticket` argument to use a slot ticket (slightly bigger chance)", false)
                        .addField("Usage", "`~>slots` - Default one, 50 coins.\n" +
                                "`~>slots <credits>` - Puts x credits on the slot machine. Max of " + SLOTS_MAX_MONEY + " coins.\n" +
                                "`~>slots -useticket` - Rolls the slot machine with one slot coin.\n" +
                                "You can specify the amount of tickets to use using `-amount` (for example `~>slots -useticket -amount 10`)", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void mine(CommandRegistry cr) {
        cr.register("mine", new SimpleCommand(Category.CURRENCY) {
            final RateLimiter rateLimiter = new RateLimiter(TimeUnit.MINUTES, 5, false);
            final Random r = new Random();

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                final User user = event.getAuthor();
                final ManagedDatabase db = MantaroData.db();

                Player player = db.getPlayer(user);
                Inventory inventory = player.getInventory();
                UserData userData = db.getUser(user).getData();
                Item item = Items.BROM_PICKAXE; //default pick

                Optional<Item> itemOpt = Items.fromAnyNoId(content);
                //why is the item optional present when there's no content?
                if(itemOpt.isPresent() && !content.isEmpty()) {
                    Item temp = itemOpt.get();
                    if(temp.getItemType() != ItemType.CAST_MINE) {
                        event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "mine.not_suitable"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if(!inventory.containsItem(temp)) {
                        event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "mine.not_in_inventory"), EmoteReference.ERROR, temp).queue();
                    } else {
                        item = temp;
                    }
                }

                if(!inventory.containsItem(item)) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "mine.no_pick"), EmoteReference.ERROR).queue();
                    return;
                }

                if(!handleDefaultRatelimit(rateLimiter, user, event))
                    return;

                if(!item.getAction().test(event, languageContext))
                    return;

                long money = Math.max(30, r.nextInt(150)); //30 to 150 credits.

                boolean waifuHelp = false;
                if(Items.handlePotion(Items.WAIFU_PILL, 5, player)) {
                    if(userData.getWaifus().entrySet().stream().anyMatch((w) -> w.getValue() > 10_000_000L)) {
                        money += Math.max(45, random.nextInt(200));
                        waifuHelp = true;
                    }
                }

                String message = String.format(languageContext.get("commands.mine.success"), EmoteReference.PICK, money, item.getName());

                if(r.nextInt(400) > (Items.handlePotion(Items.POTION_HASTE, 2, player) ? 290 : 350)) {
                    if(inventory.getAmount(Items.DIAMOND) == 5000) {
                        message += "\n" + languageContext.withRoot("commands", "mine.diamond.overflow");
                        money += Items.DIAMOND.getValue() * 0.9;
                    } else {
                        inventory.process(new ItemStack(Items.DIAMOND, 1));
                        message += "\n" + EmoteReference.DIAMOND + languageContext.withRoot("commands", "mine.diamond.success");
                    }

                    player.getData().addBadgeIfAbsent(Badge.MINER);
                }

                if(r.nextInt(400) > (Items.handlePotion(Items.POTION_HASTE, 2, player) ? 311 : 371)) {
                    List<Item> gem = Stream.of(Items.ALL)
                            .filter(i -> i.getItemType() == ItemType.MINE && !i.isHidden() && i.isSellable())
                            .collect(Collectors.toList());

                    //top notch handling for gems, 10/10 implementation -ign
                    ItemStack selectedGem = new ItemStack(gem.get(r.nextInt(gem.size())), Math.max(1, r.nextInt(5)));
                    Item itemGem = selectedGem.getItem();
                    if(inventory.getAmount(itemGem) + selectedGem.getAmount() >= 5000) {
                        message += "\n" + languageContext.withRoot("commands", "mine.gem.overflow");
                        money += itemGem.getValue() * 0.9;
                    } else {
                        inventory.process(selectedGem);
                        message += "\n" + EmoteReference.MEGA + String.format(languageContext.withRoot("commands", "mine.gem.success"), itemGem.getEmoji() + " x" + selectedGem.getAmount());
                    }

                    if(waifuHelp) {
                        message += "\n" + languageContext.get("commands.mine.waifu_help");
                    }

                    player.getData().addBadgeIfAbsent(Badge.GEM_FINDER);
                }

                event.getChannel().sendMessage(message).queue();
                player.addMoney(money);
                player.saveAsync();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Mine command")
                        .setDescription("**Mines minerals to gain some credits. A bit more lucrative than loot, but needs pickaxes.**\n" +
                                "Has a random chance of finding diamonds.")
                        .build();
            }
        });
    }

    private void proceedGamble(GuildMessageReceivedEvent event, I18nContext languageContext, Player player, int luck, Random r, long i, long gains) {
        PlayerStats stats = MantaroData.db().getPlayerStats(event.getMember());

        if(luck > r.nextInt(140)) {
            if(player.addMoney(gains)) {
                if(gains > Integer.MAX_VALUE) {
                    if(!player.getData().hasBadge(Badge.GAMBLER)) {
                        player.getData().addBadgeIfAbsent(Badge.GAMBLER);
                        player.saveAsync();
                    }
                }

                stats.incrementGambleWins();
                stats.addGambleWin(gains);

                event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "gamble.win"), EmoteReference.DICE, gains).queue();
            } else {
                event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "gamble.win_overflow"), EmoteReference.DICE, gains).queue();
            }
        } else {
            long oldMoney = player.getMoney();
            player.setMoney(Math.max(0, player.getMoney() - i));

            stats.getData().incrementGambleLose();
            event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "gamble.lose"), EmoteReference.DICE, (player.getMoney() == 0 ? languageContext.withRoot("commands", "gamble.lose_all") + " " + oldMoney : i), EmoteReference.SAD).queue();
        }

        player.setLocked(false);
        player.saveAsync();
        stats.saveAsync();
    }
}
