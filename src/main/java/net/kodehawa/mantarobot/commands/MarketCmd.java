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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;
import net.kodehawa.mantarobot.commands.currency.item.special.Broken;
import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Breakable;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.attributes.Attribute;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Axe;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.FishRod;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Pickaxe;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Wrench;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
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
    private static final Random random = new Random();
    private static final IncreasingRateLimiter buyRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(4)
            .cooldown(3, TimeUnit.SECONDS)
            .maxCooldown(10, TimeUnit.SECONDS)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("buy")
            .premiumAware(true)
            .build();

    private static final IncreasingRateLimiter sellRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(4)
            .cooldown(3, TimeUnit.SECONDS)
            .maxCooldown(10, TimeUnit.SECONDS)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("sell")
            .premiumAware(true)
            .build();

    private static final IncreasingRateLimiter dumpRatelimit = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(4)
            .cooldown(3, TimeUnit.SECONDS)
            .maxCooldown(10, TimeUnit.SECONDS)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("dump")
            .premiumAware(true)
            .build();

    private static final IncreasingRateLimiter marketRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(4)
            .cooldown(3, TimeUnit.SECONDS)
            .maxCooldown(10, TimeUnit.SECONDS)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("market")
            .premiumAware(true)
            .build();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Market.class);
        cr.registerSlash(Buy.class);
        cr.registerSlash(Sell.class);
        cr.registerSlash(Dump.class);
    }

    @Name("market")
    @Description("The hub for all market commands.")
    @Category(CommandCategory.CURRENCY)
    @Help(description = "The hub for all market commands.")
    public static class Market extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Name("show")
        @Description("List current items for buying and selling.")
        @Category(CommandCategory.CURRENCY)
        @Help(description = "List current items for buying and selling.")
        public static class Show extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                showMarket(ctx, item -> true);
            }
        }

        @Name("pet")
        @Description("List current pet items.")
        @Category(CommandCategory.CURRENCY)
        @Help(description = "List current pet items.")
        public static class Pet extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                showMarket(ctx, item -> item.getItemType() == ItemType.PET || item.getItemType() == ItemType.PET_FOOD);
            }
        }

        @Name("common")
        @Description("List current common items.")
        @Category(CommandCategory.CURRENCY)
        @Help(description = "List current common items.")
        public static class Common extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                showMarket(ctx, item -> item.getItemType() == ItemType.COMMON || item.getItemType() == ItemType.COLLECTABLE);
            }
        }

        @Name("tools")
        @Description("List current tool items.")
        @Category(CommandCategory.CURRENCY)
        @Help(description = "List current tool items.")
        public static class Tool extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                showMarket(ctx, item -> item instanceof FishRod || item instanceof Pickaxe || item instanceof Axe || item instanceof Broken);
            }
        }

        @Name("potions")
        @Description("List current potion items.")
        @Category(CommandCategory.CURRENCY)
        @Help(description = "List current potion items.")
        public static class Potions extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                showMarket(ctx, Potion.class::isInstance);
            }
        }

        @Name("buyable")
        @Description("List current buyable items.")
        @Category(CommandCategory.CURRENCY)
        @Help(description = "List current buyable items.")
        public static class Buyable extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                showMarket(ctx, Item::isBuyable);
            }
        }

        @Name("sellable")
        @Description("List current sellable items.")
        @Category(CommandCategory.CURRENCY)
        @Help(description = "List current sellable items.")
        public static class Sellable extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                showMarket(ctx, Item::isSellable);
            }
        }

        @Name("price")
        @Description("Looks up the price of an item.")
        @Category(CommandCategory.CURRENCY)
        @Options({
                @Options.Option(type = OptionType.STRING, name = "item", description = "The item name.", required = true)
        })
        @Help(
                description = "Looks up the price of an item.",
                usage = "`/market price item:<item>`",
                parameters = {
                        @Help.Parameter(name = "item", description = "The item name.")
                }
        )
        public static class Price extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var item = ctx.getOptionAsString("item");
                price(ctx, item);
            }
        }
    }

    @Name("buy")
    @Description("Buys an item.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "item", description = "The item to buy", required = true),
            @Options.Option(type = OptionType.INTEGER, name = "amount", description = "The amount of the item to buy.", maxValue = ItemStack.MAX_STACK_SIZE),
            @Options.Option(type = OptionType.BOOLEAN, name = "max", description = "Buy as many as possible. Makes it so amount is ignored.")
    })
    @Help(
            description = "Buys an item",
            usage = """
                    To buy an item do `/buy amount:<amount> item:<item name>` or `/buy max:true item:<item name>`. The maximum amount is 5000.
                    """,
            parameters = {
                    @Help.Parameter(name = "amount", description = "The amount of the item to buy."),
                    @Help.Parameter(name = "item", description = "The item to buy."),
                    @Help.Parameter(name = "max", description = "Whether to attempt buying the maximum amount currently possible for you. Makes it so amount is ignored.", optional = true)
            }
    )
    public static class Buy extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            buy(ctx, ctx.getOptionAsString("item"), ctx.getOptionAsInteger("amount", 1), ctx.getOptionAsBoolean("max"));
        }
    }

    @Name("sell")
    @Description("Sells an item.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "item", description = "The item to sell", required = true),
            @Options.Option(type = OptionType.INTEGER, name = "amount", description = "The amount of the item to sell.", maxValue = ItemStack.MAX_STACK_SIZE),
            @Options.Option(type = OptionType.BOOLEAN, name = "max", description = "Sell as many as possible. Makes it so amount is ignored.")
    })
    @Help(
            description = "Sells an item",
            usage = """
                    To sell an item do `/sell amount:<amount> item:<item name>` or `/sell max:true item:<item name>`. The maximum amount is the amount of items you have.
                    """,
            parameters = {
                    @Help.Parameter(name = "amount", description = "The amount of the item to sell."),
                    @Help.Parameter(name = "item", description = "The item to sell."),
                    @Help.Parameter(name = "max", description = "Whether to attempt selling as many as currently possible for you. Makes it so amount is ignored.")
            }
    )
    public static class Sell extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            sell(ctx, ctx.getOptionAsString("item"), ctx.getOptionAsInteger("amount", 1), ctx.getOptionAsBoolean("max"));
        }
    }


    @Name("dump")
    @Description("Dumps an item.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "item", description = "The item to dump", required = true),
            @Options.Option(type = OptionType.INTEGER, name = "amount", description = "The amount of the item to dump.", maxValue = ItemStack.MAX_STACK_SIZE),
            @Options.Option(type = OptionType.BOOLEAN, name = "max", description = "Dump as many as possible. Makes it so amount is ignored.")
    })
    @Help(
            description = "Dumps an item",
            usage = """
                    To dump an item do `/dump amount:<amount> item:<item name>`.
                    """,
            parameters = {
                    @Help.Parameter(name = "amount", description = "The amount of the item to dump"),
                    @Help.Parameter(name = "item", description = "The item to dump"),
                    @Help.Parameter(name = "max", description = "Whether to attempt dumping as many as currently possible for you. Makes it so amount is ignored.")
            }
    )
    public static class Dump extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            dump(ctx, ctx.getOptionAsString("item"), ctx.getOptionAsInteger("amount", 1), ctx.getOptionAsBoolean("max"));
        }
    }

    @Subscribe
    public void market(CommandRegistry cr) {
        TreeCommand marketCommand = cr.register("market", new TreeCommand(CommandCategory.CURRENCY) {
            @SuppressWarnings("unused")
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        showMarket(ctx, item -> true);
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
                        .build();
            }
        });

        marketCommand.addSubCommand("pet", new SubCommand() {
            @Override
            public String description() {
                return "List all current pet items.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                showMarket(ctx, item -> item.getItemType() == ItemType.PET || item.getItemType() == ItemType.PET_FOOD);
            }
        });

        marketCommand.addSubCommand("common", new SubCommand() {
            @Override
            public String description() {
                return "List all common items.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                showMarket(ctx, item -> item.getItemType() == ItemType.COMMON || item.getItemType() == ItemType.COLLECTABLE);
            }
        });

        marketCommand.addSubCommand("tools", new SubCommand() {
            @Override
            public String description() {
                return "List all current tools.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                showMarket(ctx, item -> item instanceof FishRod || item instanceof Pickaxe || item instanceof Axe || item instanceof Broken);
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

                price(ctx, content);
            }
        });

        cr.registerAlias("market", "shop");
    }

    @Subscribe
    public void sell(CommandRegistry cr) {
        cr.register("sell", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.market.sell.no_item_amount", EmoteReference.ERROR);
                    return;
                }

                var itemName = content;
                var itemNumber = 1;
                var split = args[0];
                var isMassive = split.matches("^[0-9]*$");
                var isMax = "max".equalsIgnoreCase(split);

                if (isMassive) {
                    try {
                        itemNumber = Math.abs(Integer.parseInt(split));
                        itemName = itemName.replace(args[0], "").trim();
                    } catch (NumberFormatException e) {
                        ctx.sendLocalized("commands.market.sell.invalid", EmoteReference.ERROR);
                        return;
                    }
                }
                if (isMax) {
                    itemName = itemName.replace(args[0], "").trim();
                }

                sell(ctx, itemName, itemNumber, isMax);
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
                        .build();
            }
        });
    }

    @Subscribe
    public void buy(CommandRegistry cr) {
        cr.register("buy", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.market.buy.no_item_amount", EmoteReference.ERROR);
                    return;
                }

                var itemName = content;
                var itemNumber = 1;
                var split = args[0];
                var isMassive = split.matches("^[0-9]*$");
                var isMax = false;
                if (isMassive) {
                    try {
                        itemNumber = Math.abs(Integer.parseInt(split));
                        itemName = itemName.replace(args[0], "").trim();
                    } catch (NumberFormatException e) {
                        ctx.sendLocalized("commands.market.buy.invalid", EmoteReference.ERROR);
                        return;
                    }
                } else {
                    // This is silly but works, people can stop asking about this now :o
                    switch (split) {
                        case "all" -> itemNumber = ItemStack.MAX_STACK_SIZE;
                        case "half" -> itemNumber = ItemStack.MAX_STACK_SIZE / 2;
                        case "quarter" -> itemNumber = ItemStack.MAX_STACK_SIZE / 4;
                        // you might think: isn't "all" and "max", to that I say no
                        // all is MAX_STACK_SIZE, while max changes based on how much the
                        // player can still fit into their inventory
                        case "max" -> isMax = true;
                        default -> {}
                    }

                    if (itemNumber > 1 || isMax) {
                        itemName = itemName.replace(args[0], "").trim();
                    }
                }

                buy(ctx, itemName, itemNumber, isMax);
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
                        .build();
            }
        });
    }

    @Subscribe
    public void dump(CommandRegistry cr) {
        cr.register("dump", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.market.dump.no_item", EmoteReference.ERROR);
                    return;
                }

                var itemName = content;
                var itemNumber = 1;
                var split = itemName.split(" ")[0];
                var isMassive = split.matches("^[0-9]*$");
                var isMax = "max".equalsIgnoreCase(split);
                if (isMassive) {
                    try {
                        itemNumber = Math.abs(Integer.parseInt(itemName.split(" ")[0]));
                        itemName = itemName.replace(args[0], "").trim();
                    } catch (NumberFormatException e) {
                        ctx.sendLocalized("commands.market.dump.invalid", EmoteReference.ERROR);
                        return;
                    }
                }
                if (isMax) {
                    itemName = itemName.replace(args[0], "").trim();
                }

                dump(ctx, itemName, itemNumber, isMax);
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
                        .build();
            }
        });
    }

    private static void price(IContext ctx, String itemString) {
        var item = ItemHelper.fromAnyNoId(itemString, ctx.getLanguageContext()).orElse(null);
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

    private static void dump(IContext ctx, String itemName, int itemNumber, boolean isMax) {
        if (itemNumber < 1) {
            ctx.sendLocalized("commands.market.dump.invalid", EmoteReference.ERROR);
            return;
        }

        var item = ItemHelper.fromAnyNoId(itemName, ctx.getLanguageContext()).orElse(null);

        if (item == null) {
            ctx.sendLocalized("commands.market.dump.non_existent", EmoteReference.ERROR);
            return;
        }

        var player = ctx.getPlayer();
        if (!player.containsItem(item)) {
            ctx.sendLocalized("commands.market.dump.player_no_item", EmoteReference.ERROR);
            return;
        }

        var playerCount = player.getItemAmount(item);

        if (isMax) {
            itemNumber = Math.max(1, playerCount);
        }

        if (playerCount < itemNumber) {
            ctx.sendLocalized("commands.market.dump.more_items_than_player", EmoteReference.ERROR);
            return;
        }

        if (dumpRatelimit != null && !RatelimitUtils.ratelimit(dumpRatelimit, ctx, false)) {
            return;
        }

        player.processItem(item, -itemNumber);
        if (itemNumber > 4000) {
            player.addBadgeIfAbsent(Badge.WASTER);
        }

        player.updateAllChanged();
        ctx.sendLocalized("commands.market.dump.success", EmoteReference.CORRECT, itemNumber, item.getEmoji(), item.getName());
    }

    private static void sell(IContext ctx, String item, int amount, boolean isMax) {
        if (amount < 1) {
            ctx.sendLocalized("commands.market.sell.invalid", EmoteReference.ERROR);
            return;
        }

        var player = ctx.getPlayer();
        try {
            if (item.isEmpty()) {
                ctx.sendLocalized("commands.market.sell.no_item",  EmoteReference.ERROR);
                return;
            }

            var finalName = item.replace("\"", "").trim();
            var toSell = ItemHelper.fromAnyNoId(finalName, ctx.getLanguageContext()).orElse(null);
            if (toSell == null) {
                ctx.sendLocalized("commands.market.sell.non_existent", EmoteReference.ERROR);
                return;
            }

            if (!toSell.isSellable()) {
                ctx.sendLocalized("commands.market.sell.no_sell_price", EmoteReference.ERROR);
                return;
            }

            var playerCount = player.getItemAmount(toSell);
            if (isMax) {
                amount = Math.max(1, playerCount);
            }

            if (playerCount < 1) {
                ctx.sendLocalized("commands.market.sell.no_item_player", EmoteReference.STOP);
                return;
            }

            if (playerCount < amount) {
                ctx.sendLocalized("commands.market.sell.more_items_than_player", EmoteReference.ERROR);
                return;
            }

            if (sellRatelimiter != null && !RatelimitUtils.ratelimit(sellRatelimiter, ctx, false)) {
                return;
            }

            var many = amount * -1;
            var money = Math.round((toSell.getValue() * 0.9)) * Math.abs(many);
            player.processItem(toSell, many);

            player.addMoney(money);
            player.marketUsed(player.getMarketUsed() + 1);
            player.updateAllChanged();
            ctx.sendLocalized("commands.market.sell.success", EmoteReference.CORRECT, Math.abs(many), toSell.getName(), money);
        } catch (Exception e) {
            ctx.send(EmoteReference.ERROR + ctx.getLanguageContext().get("general.invalid_syntax"));
        }
    }

    private static void buy(IContext ctx, String itemName, int itemNumber, boolean isMax) {
        var languageContext = ctx.getLanguageContext();
        var player = ctx.getPlayer();

        if (itemNumber < 1) {
            ctx.sendLocalized("commands.market.buy.invalid", EmoteReference.ERROR);
            return;
        }

        final var itemToBuy = ItemHelper.fromAnyNoId(itemName.replace("\"", ""), ctx.getLanguageContext())
                .orElse(null);

        if (itemName.trim().isEmpty()) {
            ctx.sendLocalized("commands.market.buy.no_item", EmoteReference.ERROR);
            return;
        }

        if (itemToBuy == null) {
            ctx.sendLocalized("commands.market.buy.non_existent", EmoteReference.ERROR);
            return;
        }

        try {
            if (!itemToBuy.isBuyable() || itemToBuy.isPetOnly()) {
                ctx.sendLocalized("commands.market.buy.no_buy_price", EmoteReference.ERROR);
                return;
            }

            var price = itemToBuy.getValue();
            var playerCount = player.getItemAmount(itemToBuy);

            if (isMax) {
                itemNumber = (int) Math.max(1, Math.min(ItemStack.MAX_STACK_SIZE - playerCount, player.getNewMoney() / price));
            }

            if (!player.fitsItemAmount(itemToBuy, itemNumber)) {
                ctx.sendLocalized("commands.market.buy.item_limit_reached", EmoteReference.ERROR);
                return;
            }

            if (buyRatelimiter != null && !RatelimitUtils.ratelimit(buyRatelimiter, ctx, false)) {
                return;
            }

            var value = price * itemNumber;
            var removedMoney = player.removeMoney(value);
            if (removedMoney) {
                player.processItem(itemToBuy, itemNumber);
                player.addBadgeIfAbsent(Badge.BUYER);
                player.marketUsed(player.getMarketUsed() + 1);
                player.updateAllChanged();

                var playerMoney = player.getCurrentMoney();
                var message = "commands.market.buy.success";
                if (itemToBuy instanceof Breakable) {
                    message = "commands.market.buy.success_breakable";
                }

                if (itemToBuy instanceof Potion) {
                    message = "commands.market.buy.success_potion";
                }

                var warn = "";
                if (itemToBuy instanceof Attribute attribute && !(itemToBuy instanceof Wrench) &&
                        attribute.getTier() == 1 && random.nextFloat() <= 0.20 && player.getLevel() <= 5) {
                    warn += "\n" + EmoteReference.WRENCH.toHeaderString() + languageContext.get("commands.market.buy.success_breakable_upgrade") + "\n";
                }

                ctx.sendLocalized(message, warn + EmoteReference.OK, itemNumber, itemToBuy.getEmoji(), value, playerMoney);
            } else {
                ctx.sendLocalized("commands.market.buy.not_enough_money", EmoteReference.STOP, player.getCurrentMoney(), value);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.send(EmoteReference.ERROR + languageContext.get("general.invalid_syntax"));
        }
    }

    private static void showMarket(IContext ctx, Predicate<? super Item> predicate) {
        if (!RatelimitUtils.ratelimit(marketRatelimiter, ctx, false)) {
            return;
        }

        // Slash does not need Embed Links permissions
        if (!ctx.getGuild().getSelfMember().hasPermission(ctx.getChannel(), Permission.MESSAGE_EMBED_LINKS) && ctx instanceof Context) {
            ctx.sendLocalized("general.missing_embed_permissions");
            return;
        }

        var player = ctx.getPlayer();
        if (player.isLocked()) {
            ctx.send(EmoteReference.ERROR + "You cannot access the market now.");
            return;
        }

        var languageContext = ctx.getLanguageContext();
        var embed = new EmbedBuilder()
                .setDescription(languageContext.get("commands.market.header"))
                .setThumbnail("https://apiv2.mantaro.site/image/common/cart.png");

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
        embed.setColor(Color.MAGENTA).setAuthor("Mantaro's Market", null, ctx.getAuthor().getEffectiveAvatarUrl())
                .setDescription(String.format(languageContext.get("general.buy_sell_paged_react"),
                        String.format(languageContext.get("general.reaction_timeout"), 200) + "\n")
                        + (user.isPremium() ? "" : languageContext.get("general.sellout")) + languageContext.get("commands.market.reference")
        );

        DiscordUtils.listButtons(ctx.getUtilsContext(), 200, embed, splitFields);
    }
}
