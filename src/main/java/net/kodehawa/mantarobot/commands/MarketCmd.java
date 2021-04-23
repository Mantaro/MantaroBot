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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.kodehawa.mantarobot.commands.currency.item.*;
import net.kodehawa.mantarobot.commands.currency.item.special.*;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Breakable;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.attributes.Attribute;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Axe;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.FishRod;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Pickaxe;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Wrench;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.Color;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Module
public class MarketCmd {
    private final static Random random = new Random();
    private final IncreasingRateLimiter buyRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(4)
            .cooldown(3, TimeUnit.SECONDS)
            .maxCooldown(10, TimeUnit.SECONDS)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("buy")
            .premiumAware(true)
            .build();

    private final IncreasingRateLimiter sellRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(4)
            .cooldown(3, TimeUnit.SECONDS)
            .maxCooldown(10, TimeUnit.SECONDS)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("sell")
            .premiumAware(true)
            .build();

    private final IncreasingRateLimiter dumpRatelimit = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(4)
            .cooldown(3, TimeUnit.SECONDS)
            .maxCooldown(10, TimeUnit.SECONDS)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("dump")
            .premiumAware(true)
            .build();

    @Subscribe
    public void market(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(4)
                .cooldown(3, TimeUnit.SECONDS)
                .maxCooldown(10, TimeUnit.SECONDS)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("market")
                .premiumAware(true)
                .build();


        TreeCommand marketCommand = cr.register("market", new TreeCommand(CommandCategory.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        showMarket(ctx, (item) -> true);
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("List current items for buying and selling. You can check more specific markets below.")
                        .setUsage("""
                                To buy an item do `~>market buy <item>`. It will subtract the value from your money and give you the item.
                                To sell do `~>market sell all` to sell all your items or `~>market sell <item>` to sell the specified item.
                                If the item name contains spaces, "wrap it in quotes".
                                To buy and sell multiple items you need to do `~>market <buy/sell> <amount> <item>`
                                """)
                        .addParameter("item", "The item name or emoji")
                        .setSeasonal(true)
                        .build();
            }
        });

        marketCommand.setPredicate((ctx) -> {
            if (!RatelimitUtils.ratelimit(rateLimiter, ctx, false)) {
                return false;
            }

            if (!ctx.getSelfMember().hasPermission(ctx.getChannel(), Permission.MESSAGE_EMBED_LINKS)) {
                ctx.sendLocalized("general.missing_embed_permissions");
                return false;
            }

            var player = ctx.getPlayer();
            if (player.isLocked()) {
                ctx.send(EmoteReference.ERROR + "You cannot access the market now.");
                return false;
            }

            return true;
        });

        marketCommand.addSubCommand("pet", new SubCommand() {
            @Override
            public String description() {
                return "List all current pet items.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                showMarket(ctx, (item) -> item.getItemType() == ItemType.PET || item.getItemType() == ItemType.PET_FOOD);
            }
        });

        marketCommand.addSubCommand("common", new SubCommand() {
            @Override
            public String description() {
                return "List all common items.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                showMarket(ctx, (item) -> item.getItemType() == ItemType.COMMON || item.getItemType() == ItemType.COLLECTABLE);
            }
        });

        marketCommand.addSubCommand("tools", new SubCommand() {
            @Override
            public String description() {
                return "List all current tools.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                showMarket(ctx, (item) -> item instanceof FishRod || item instanceof Pickaxe || item instanceof Axe || item instanceof Broken);
            }
        });

        marketCommand.addSubCommand("potions", new SubCommand() {
            @Override
            public String description() {
                return "List all current potions.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                showMarket(ctx, Potion.class::isInstance);
            }
        });


        marketCommand.addSubCommand("buyable", new SubCommand() {
            @Override
            public String description() {
                return "List all the buyable items.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                showMarket(ctx, Item::isBuyable);
            }
        });

        marketCommand.addSubCommand("sellable", new SubCommand() {
            @Override
            public String description() {
                return "List all the sellable items.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                showMarket(ctx, Item::isSellable);
            }
        });


        marketCommand.addSubCommand("dump", new SubCommand() {
            @Override
            public String description() {
                return "Use `~>dump` instead.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                dump(ctx, content, ctx.getArguments(), null, languageContext.get("commands.market.dump.replacement"));
            }
        }).createSubCommandAlias("dump", "trash");

        marketCommand.addSubCommand("price", new SubCommand() {
            @Override
            public String description() {
                return "Checks the price of any given item.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.market.price.no_item", EmoteReference.ERROR);
                    return;
                }

                var item = ItemHelper.fromAnyNoId(content, ctx.getLanguageContext()).orElse(null);

                if (item == null) {
                    ctx.sendLocalized("commands.market.price.non_existent", EmoteReference.ERROR);
                    return;
                }

                if (!item.isBuyable() && !item.isSellable()) {
                    ctx.sendLocalized("commands.market.price.no_price", EmoteReference.THINKING);
                    return;
                }

                if (!item.isBuyable()) {
                    ctx.sendLocalized("commands.market.price.collectible", EmoteReference.EYES, Math.round(item.getValue() * 0.9));
                    return;
                }

                ctx.sendLocalized("commands.market.price.success",
                        EmoteReference.MARKET, item.getEmoji() + " ", item.getName(), item.getValue(), Math.round(item.getValue() * 0.9)
                );
            }
        });

        marketCommand.addSubCommand("sell", new SubCommand() {
            @Override
            public String description() {
                return "Use ~>sell instead.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                sell(ctx, content, ctx.getArguments(), null, languageContext.get("commands.market.sell.replacement"));
            }
        });

        marketCommand.addSubCommand("buy", new SubCommand() {
            @Override
            public String description() {
                return "Use ~>buy instead.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                buy(ctx, content, ctx.getArguments(), null, languageContext.get("commands.market.buy.replacement"));
            }
        });

        cr.registerAlias("market", "shop");
    }

    @Subscribe
    public void sell(CommandRegistry cr) {
        cr.register("sell", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                sell(ctx, content, args, sellRatelimiter, "");
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Sells an item.")
                        .setUsage("""
                                To sell an item do `~>sell <item>`.
                                If the item name contains spaces, "wrap it in quotes".
                                To sell multiple items you need to do `~>sell <amount> <item>`
                                """)
                        .addParameter("item", "The item name or emoji")
                        .addParameterOptional("amount", "The amount you want to sell. If you want to sell all of one item, use allof here.")
                        .setSeasonal(true)
                        .build();
            }
        });
    }

    @Subscribe
    public void buy(CommandRegistry cr) {
        cr.register("buy", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                buy(ctx, content, args, buyRatelimiter, "");
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Buys an item.")
                        .setUsage("""
                                To buy an item do `~>buy <item>`. It will subtract the value from your money and give you the item.
                                If the item name contains spaces, "wrap it in quotes".
                                To buy multiple items you need to do `~>buy <amount> <item>`
                                """)
                        .addParameter("item", "The item name or emoji")
                        .addParameterOptional("amount", "The amount you want to buy.")
                        .setSeasonal(true)
                        .build();
            }
        });
    }

    @Subscribe
    public void dump(CommandRegistry cr) {
        cr.register("dump", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context context, String content, String[] args) {
                dump(context, content, args, dumpRatelimit, "");
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Dumps an item.")
                        .setUsage("""
                                To dump an item do `~>dump <item>`. If the item name contains spaces, "wrap it in quotes".
                                To dump multiple items you need to do `~>dump <amount> <item>`
                                """)
                        .addParameter("item", "The item name or emoji")
                        .addParameterOptional("amount", "The amount you want to throw away. If you want to throw all of one item, use allof here.")
                        .setSeasonal(true)
                        .build();
            }
        });
    }

    private void dump(Context ctx, String content, String[] args, IncreasingRateLimiter rateLimiter, String extra) {
        var warn = extra.isEmpty() ? "" : extra + "\n";
        if (content.isEmpty()) {
            ctx.sendLocalized("commands.market.dump.no_item", warn + EmoteReference.ERROR);
            return;
        }

        var arguments = ctx.getOptionalArguments();
        var isSeasonal = ctx.isSeasonal();
        content = Utils.replaceArguments(arguments, content, "season", "s").trim();

        var itemName = content;
        if (args[0].equalsIgnoreCase("allof")) {
            itemName = content.replace("allof", "").trim();
        }

        var itemNumber = 1;
        var isMassive = !itemName.isEmpty() && itemName.split(" ")[0].matches("^[0-9]*$");
        if (isMassive) {
            try {
                itemNumber = Math.abs(Integer.parseInt(itemName.split(" ")[0]));
                itemName = itemName.replace(args[0], "").trim();
            } catch (NumberFormatException e) {
                ctx.sendLocalized("commands.market.dump.invalid", warn + EmoteReference.ERROR);
                return;
            }
        }


        var item = ItemHelper.fromAnyNoId(itemName, ctx.getLanguageContext()).orElse(null);

        if (item == null) {
            ctx.sendLocalized("commands.market.dump.non_existent", warn + EmoteReference.ERROR);
            return;
        }

        var player = ctx.getPlayer();
        var seasonalPlayer = ctx.getSeasonPlayer();
        var playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();

        if (!playerInventory.containsItem(item)) {
            ctx.sendLocalized("commands.market.dump.player_no_item", warn + EmoteReference.ERROR);
            return;
        }

        if (args[0].equalsIgnoreCase("allof")) {
            itemNumber = playerInventory.getAmount(item);
        }

        if (playerInventory.getAmount(item) < itemNumber) {
            ctx.sendLocalized("commands.market.dump.more_items_than_player", warn + EmoteReference.ERROR);
            return;
        }

        if (rateLimiter != null && !RatelimitUtils.ratelimit(rateLimiter, ctx, false)) {
            return;
        }

        playerInventory.process(new ItemStack(item, -itemNumber));

        if (itemNumber > 4000) {
            player.getData().addBadgeIfAbsent(Badge.WASTER);
        }

        if (isSeasonal) {
            seasonalPlayer.saveAsync();
        } else {
            player.saveAsync();
        }

        ctx.sendLocalized("commands.market.dump.success", warn + EmoteReference.CORRECT, itemNumber, item.getEmoji(), item.getName());
    }

    private void sell(Context ctx, String content, String[] args, IncreasingRateLimiter rateLimiter, String extra) {
        var warn = extra.isEmpty() ? "" : extra + "\n";
        if (content.isEmpty()) {
            ctx.sendLocalized("commands.market.sell.no_item_amount", warn + EmoteReference.ERROR);
            return;
        }

        var optionalArguments = ctx.getOptionalArguments();
        content = Utils.replaceArguments(optionalArguments, content, "season", "s").trim();

        var languageContext = ctx.getLanguageContext();
        var player = ctx.getPlayer();
        var seasonalPlayer = ctx.getSeasonPlayer();
        var isSeasonal = ctx.isSeasonal();
        var itemName = content;
        var itemNumber = 1;
        var split = args[0];
        var isMassive = !itemName.isEmpty() && split.matches("^[0-9]*$");

        if (isMassive) {
            try {
                itemNumber = Math.abs(Integer.parseInt(split));
                itemName = itemName.replaceFirst(args[0], "").trim();
            } catch (NumberFormatException e) {
                ctx.sendLocalized("commands.market.sell.invalid", warn + EmoteReference.ERROR);
                return;
            }
        }

        try {
            if (args[0].equals("all") && !isSeasonal) {
                ctx.sendLocalized("commands.market.sell.all.confirmation", warn + EmoteReference.WARNING);
                //Start the operation.
                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 60, e -> {
                    if (!e.getAuthor().getId().equals(ctx.getAuthor().getId())) {
                        return Operation.IGNORED;
                    }

                    String c = e.getMessage().getContentRaw();

                    if (c.equalsIgnoreCase("yes")) {
                        long all = player.getInventory().asList().stream()
                                .filter(item -> item.getItem().isSellable())
                                .mapToLong(value -> Math.round(value.getItem().getValue() * value.getAmount() * 0.9d))
                                .sum();

                        player.getInventory().clearOnlySellables();
                        player.addMoney(all);

                        ctx.sendLocalized("commands.market.sell.all.success", warn + EmoteReference.MONEY, all);
                        player.save();
                        return Operation.COMPLETED;
                    } else if (c.equalsIgnoreCase("no")) {
                        ctx.sendLocalized("commands.market.sell.all.cancelled", warn + EmoteReference.CORRECT);
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });

                return;
            }

            var playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();

            if (args[0].equalsIgnoreCase("allof")) {
                itemName = content.replace("allof", "").trim();
            }

            if (itemName.trim().isEmpty()) {
                ctx.sendLocalized("commands.market.sell.no_item", warn + EmoteReference.ERROR);
                return;
            }

            var finalName = itemName.replace("\"", "").trim();
            var toSell = ItemHelper.fromAnyNoId(finalName, ctx.getLanguageContext()).orElse(null);
            if (toSell == null) {
                ctx.sendLocalized("commands.market.sell.non_existent", warn + EmoteReference.ERROR);
                return;
            }

            if (!toSell.isSellable()) {
                ctx.sendLocalized("commands.market.sell.no_sell_price", warn + EmoteReference.ERROR);
                return;
            }

            if (playerInventory.getAmount(toSell) < 1) {
                ctx.sendLocalized("commands.market.sell.no_item_player", warn + EmoteReference.STOP);
                return;
            }

            if (args[0].equalsIgnoreCase("allof")) {
                itemNumber = playerInventory.getAmount(toSell);
            }

            if (playerInventory.getAmount(toSell) < itemNumber) {
                ctx.sendLocalized("commands.market.sell.more_items_than_player", warn + EmoteReference.ERROR);
                return;
            }

            if (rateLimiter != null && !RatelimitUtils.ratelimit(rateLimiter, ctx, false)) {
                return;
            }

            var many = itemNumber * -1;
            var amount = Math.round((toSell.getValue() * 0.9)) * Math.abs(many);
            playerInventory.process(new ItemStack(toSell, many));

            if (isSeasonal) {
                seasonalPlayer.addMoney(amount);
            } else {
                player.addMoney(amount);
            }

            player.getData().setMarketUsed(player.getData().getMarketUsed() + 1);
            ctx.sendLocalized("commands.market.sell.success", warn + EmoteReference.CORRECT, Math.abs(many), toSell.getName(), amount);
            player.save();

            if (isSeasonal)
                seasonalPlayer.saveAsync();
        } catch (Exception e) {
            ctx.send(warn + EmoteReference.ERROR + languageContext.get("general.invalid_syntax"));
        }
    }

    private void buy(Context ctx, String content, String[] args, IncreasingRateLimiter rateLimiter, String extra) {
        var warn = extra.isEmpty() ? "" : extra + "\n";
        if (content.isEmpty()) {
            ctx.sendLocalized("commands.market.buy.no_item_amount", warn + EmoteReference.ERROR);
            return;
        }

        var optionalArguments = ctx.getOptionalArguments();
        content = Utils.replaceArguments(optionalArguments, content, "season", "s").trim();

        var player = ctx.getPlayer();
        var seasonalPlayer = ctx.getSeasonPlayer();
        var isSeasonal = ctx.isSeasonal();
        var itemName = content;
        var itemNumber = 1;
        var split = args[0];
        var isMassive = !itemName.isEmpty() && split.matches("^[0-9]*$");
        var languageContext = ctx.getLanguageContext();

        if (isMassive) {
            try {
                itemNumber = Math.abs(Integer.parseInt(split));
                itemName = itemName.replace(args[0], "").trim();
            } catch (NumberFormatException e) {
                ctx.sendLocalized("commands.market.buy.invalid", warn + EmoteReference.ERROR);
                return;
            }
        } else {
            // This is silly but works, people can stop asking about this now :o
            if (!itemName.isEmpty()) {
                switch (split) {
                    case "all":
                        itemNumber = ItemStack.MAX_STACK_SIZE;
                        break;
                    case "half":
                        itemNumber = ItemStack.MAX_STACK_SIZE / 2;
                        break;
                    case "quarter":
                        itemNumber = ItemStack.MAX_STACK_SIZE / 4;
                        break;
                    default:
                        break;
                }

                if (itemNumber > 1) {
                    itemName = itemName.replace(args[0], "").trim();
                }
            }
        }

        final var itemToBuy = ItemHelper.fromAnyNoId(itemName.replace("\"", ""), ctx.getLanguageContext())
                .orElse(null);

        if (itemName.trim().isEmpty()) {
            ctx.sendLocalized("commands.market.buy.no_item", warn + EmoteReference.ERROR);
            return;
        }

        if (itemToBuy == null) {
            ctx.sendLocalized("commands.market.buy.non_existent", warn + EmoteReference.ERROR);
            return;
        }

        try {
            if (!itemToBuy.isBuyable() || itemToBuy.isPetOnly()) {
                ctx.sendLocalized("commands.market.buy.no_buy_price", warn + EmoteReference.ERROR);
                return;
            }

            var playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();
            if (playerInventory.getAmount(itemToBuy) + itemNumber > 5000) {
                ctx.sendLocalized("commands.market.buy.item_limit_reached", warn + EmoteReference.ERROR);
                return;
            }

            if (rateLimiter != null && !RatelimitUtils.ratelimit(rateLimiter, ctx, false)) {
                return;
            }

            var value = itemToBuy.getValue() * itemNumber;
            var removedMoney = isSeasonal ? seasonalPlayer.removeMoney(value) : player.removeMoney(value);
            if (removedMoney) {
                playerInventory.process(new ItemStack(itemToBuy, itemNumber));
                player.getData().addBadgeIfAbsent(Badge.BUYER);
                player.getData().setMarketUsed(player.getData().getMarketUsed() + 1);

                //Due to player data being updated here too.
                player.saveAsync();

                if (isSeasonal) {
                    seasonalPlayer.saveAsync();
                }

                var playerMoney = isSeasonal ? seasonalPlayer.getMoney() : player.getCurrentMoney();
                var message = "commands.market.buy.success";
                if (itemToBuy instanceof Breakable) {
                    message = "commands.market.buy.success_breakable";
                }

                if (itemToBuy instanceof Potion) {
                    message = "commands.market.buy.success_potion";
                }

                if (itemToBuy instanceof Attribute && !(itemToBuy instanceof Wrench) &&
                        ((Attribute) itemToBuy).getTier() == 1 && random.nextFloat() <= 0.20 && player.getLevel() <= 5) {
                    warn += EmoteReference.WRENCH.toHeaderString() + languageContext.get("commands.market.buy.success_breakable_upgrade") + "\n";
                }

                ctx.sendLocalized(message, warn + EmoteReference.OK, itemNumber, itemToBuy.getEmoji(), value, playerMoney);
            } else {
                ctx.sendLocalized("commands.market.buy.not_enough_money", warn + EmoteReference.STOP, player.getCurrentMoney(), value);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.send(warn + EmoteReference.ERROR + languageContext.get("general.invalid_syntax"));
        }
    }

    private void showMarket(Context ctx, Predicate<? super Item> predicate) {
        var languageContext = ctx.getLanguageContext();
        var embed = new EmbedBuilder()
                .setDescription(languageContext.get("commands.market.header"))
                .setThumbnail("https://i.imgur.com/GIHXZAH.png");

        List<MessageEmbed.Field> fields = new LinkedList<>();
        Stream.of(ItemReference.ALL)
                .sorted(Comparator.comparingInt(i -> i.getItemType().ordinal()))
                .filter(predicate)
                .filter(item -> !item.isHidden())
                .forEach(item -> {
                    String buyValue = item.isBuyable() ? "$%,d".formatted(item.getValue()) : "N/A";
                    String sellValue = item.isSellable() ? ("$%,d".formatted((int) Math.round(item.getValue() * 0.9))) : "N/A";

                    // I blame discord stripping spaces for this unicode bullshitery
                    fields.add(new MessageEmbed.Field("%s\u2009\u2009\u2009%s".formatted(item.getEmoji(), item.getName()),
                                    (languageContext.getContextLanguage().equals("en_US") ? "" :
                                            " (" + languageContext.get(item.getTranslatedName()) + ")\n") +
                                            languageContext.get(item.getDesc()) + "\n" +
                                            languageContext.get("commands.market.buy_price") + " " + buyValue + "\n" +
                                            languageContext.get("commands.market.sell_price") + " " + sellValue,
                                    false
                            )
                    );
                });

        var user = ctx.getDBUser();

        var splitFields = DiscordUtils.divideFields(4, fields);
        var hasReactionPerms = ctx.hasReactionPerms();
        embed.setColor(Color.MAGENTA).setAuthor("Mantaro's Market", null, ctx.getAuthor().getEffectiveAvatarUrl());

        if (hasReactionPerms) {
            embed.setDescription(String.format(languageContext.get("general.buy_sell_paged_react"),
                    String.format(String.format(languageContext.get("general.reaction_timeout"), 200),
                            EmoteReference.BUY, EmoteReference.SELL)) + "\n"
                    + (user.isPremium() ? "" : languageContext.get("general.sellout")) + languageContext.get("commands.market.reference")
            );

            DiscordUtils.list(ctx.getEvent(), 200, false, embed, splitFields);
        } else {
            embed.setDescription(String.format(languageContext.get("general.buy_sell_paged_text"),
                    String.format(String.format(languageContext.get("general.reaction_timeout"), 200),
                            EmoteReference.BUY, EmoteReference.SELL)) + "\n"
                    + (user.isPremium() ? "" : languageContext.get("general.sellout")) + languageContext.get("commands.market.reference")
            );

            DiscordUtils.listText(ctx.getEvent(), 200, false, embed, splitFields);
        }
    }
}
