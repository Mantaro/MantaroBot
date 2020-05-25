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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static net.kodehawa.mantarobot.utils.Utils.handleIncreasingRatelimit;

@Module
public class MarketCmd {
    @Subscribe
    public void market(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(2)
                .cooldown(6, TimeUnit.SECONDS)
                .maxCooldown(6, TimeUnit.SECONDS)
                .randomIncrement(true)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("market")
                .premiumAware(true)
                .build();


        TreeCommand marketCommand = (TreeCommand) cr.register("market", new TreeCommand(Category.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        I18nContext languageContext = ctx.getLanguageContext();

                        EmbedBuilder embed = baseEmbed(ctx, languageContext.get("commands.market.header"))
                                .setThumbnail("https://i.imgur.com/GIHXZAH.png");

                        List<MessageEmbed.Field> fields = new LinkedList<>();
                        Stream.of(Items.ALL).forEach(item -> {
                            if (!item.isPetOnly() && !item.isHidden() && item.getItemType() != ItemType.PET) {
                                String buyValue = item.isBuyable() ? String.format("$%d", item.getValue()) : "N/A";
                                String sellValue = item.isSellable() ? String.format("$%d", (int) Math.floor(item.getValue() * 0.9)) : "N/A";

                                fields.add(new MessageEmbed.Field(String.format("%s %s", item.getEmoji(), item.getName()),
                                        (languageContext.getContextLanguage().equals("en_US") ? "" : " (" + languageContext.get(item.getTranslatedName()) + ")\n") +
                                                languageContext.get(item.getDesc()) + "\n" +
                                                languageContext.get("commands.market.buy_price") + " " + buyValue + "\n" +
                                                languageContext.get("commands.market.sell_price") + " " + sellValue,
                                        false
                                        )
                                );
                            }
                        });

                        DBUser user = ctx.getDBUser();

                        List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(4, fields);
                        boolean hasReactionPerms = ctx.hasReactionPerms();

                        if (hasReactionPerms) {
                            embed.setDescription(String.format(languageContext.get("general.buy_sell_paged_react"),
                                    splitFields.size(),
                                    String.format(String.format(languageContext.get("general.reaction_timeout"), 200),
                                            EmoteReference.BUY, EmoteReference.SELL)) + "\n"
                                    + (user.isPremium() ? "" : languageContext.get("general.sellout")) + languageContext.get("commands.market.reference")
                            );

                            DiscordUtils.list(ctx.getEvent(), 200, false, embed, splitFields);
                        } else {
                            embed.setDescription(String.format(languageContext.get("general.buy_sell_paged_text"),
                                    splitFields.size(),
                                    String.format(String.format(languageContext.get("general.reaction_timeout"), 200),
                                            EmoteReference.BUY, EmoteReference.SELL)) + "\n"
                                    + (user.isPremium() ? "" : languageContext.get("general.sellout")) + languageContext.get("commands.market.reference")
                            );

                            DiscordUtils.listText(ctx.getEvent(), 200, false, embed, splitFields);
                        }
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("List current items for buying and selling.")
                        .setUsage("To buy an item do `~>market buy <item>`. It will subtract the value from your money and give you the item.\n" +
                                "To sell do `~>market sell all` to sell all your items or `~>market sell <item>` to sell the specified item.\n" +
                                "If the item name contains spaces, \"wrap it in quotes\".\n" +
                                "To buy and sell multiple items you need to do `~>market <buy/sell> <amount> <item>`\n")
                        .addParameter("item", "The item name or emoji")
                        .setSeasonal(true)
                        .build();
            }
        });

        marketCommand.setPredicate((ctx) -> {
            if (!Utils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx.getEvent(), null, false))
                return false;

            Player player = ctx.getPlayer();
            if (player.isLocked()) {
                ctx.send(EmoteReference.ERROR + "You cannot access the market now.");
                return false;
            }

            return true;
        });

        marketCommand.addSubCommand("dump", new SubCommand() {
            @Override
            public String description() {
                return "Dumps an item. Usage: `~>market dump <item>`";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.market.dump.no_item", EmoteReference.ERROR);
                    return;
                }

                Map<String, String> t = ctx.getOptionalArguments();
                boolean isSeasonal = ctx.isSeasonal();
                content = Utils.replaceArguments(t, content, "season", "s").trim();
                I18nContext languageContext = ctx.getLanguageContext();

                String[] args = content.split(" ");
                String itemName = content;
                int itemNumber = 1;
                boolean isMassive = !itemName.isEmpty() && itemName.split(" ")[0].matches("^[0-9]*$");
                if (isMassive) {
                    try {
                        itemNumber = Math.abs(Integer.parseInt(itemName.split(" ")[0]));
                        itemName = itemName.replace(args[0], "").trim();
                    } catch (NumberFormatException e) {
                        ctx.sendLocalized("commands.market.dump.invalid", EmoteReference.ERROR);
                        return;
                    }
                }

                Item item = Items.fromAny(itemName).orElse(null);

                if (item == null) {
                    ctx.sendLocalized("commands.market.dump.non_existent", EmoteReference.ERROR);
                    return;
                }

                Player player = ctx.getPlayer();
                SeasonPlayer seasonalPlayer = ctx.getSeasonPlayer();

                Inventory playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();

                if (!playerInventory.containsItem(item)) {
                    ctx.sendLocalized("commands.market.dump.player_no_item", EmoteReference.ERROR);
                    return;
                }

                if (playerInventory.getAmount(item) < itemNumber) {
                    ctx.sendLocalized("commands.market.dump.more_items_than_player", EmoteReference.ERROR);
                    return;
                }

                playerInventory.process(new ItemStack(item, -itemNumber));

                if(itemNumber > 4000)
                    player.getData().addBadgeIfAbsent(Badge.WASTER);

                if (isSeasonal)
                    seasonalPlayer.saveAsync();
                else
                    player.saveAsync();

                ctx.sendLocalized("commands.market.dump.success", EmoteReference.CORRECT, itemNumber, item.getEmoji(), item.getName());
            }
        }).createSubCommandAlias("dump", "trash");

        marketCommand.addSubCommand("price", new SubCommand() {
            @Override
            public String description() {
                return "Checks the price of any given item. Usage: `~>market price <item>`";
            }

            @Override
            protected void call(Context ctx, String content) {
                String[] args = content.split(" ");
                String itemName = content.replace(args[0] + " ", "");
                Item item = Items.fromAny(itemName).orElse(null);

                if (item == null) {
                    ctx.sendLocalized("commands.market.price.non_existent", EmoteReference.ERROR);
                    return;
                }

                if (!item.isBuyable() && !item.isSellable()) {
                    ctx.sendLocalized("commands.market.price.no_price", EmoteReference.THINKING);
                    return;
                }

                if (!item.isBuyable()) {
                    ctx.sendLocalized("commands.market.price.collectible", EmoteReference.EYES, (int) (item.getValue() * 0.9));
                    return;
                }

                ctx.sendLocalized("commands.market.price.success",
                        EmoteReference.MARKET, item.getEmoji(), item.getName(), item.getValue(), (int) (item.getValue() * 0.9)
                );
            }
        });

        marketCommand.addSubCommand("sell", new SubCommand() {
            @Override
            public String description() {
                return "Sells an item. Usage: `~>market sell <item>`. You can sell multiple items if you put the amount before the item.\n" +
                        "Use `~>market sell all` to sell all of your items.";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.market.sell.no_item_amount", EmoteReference.ERROR);
                    return;
                }

                Player player = ctx.getPlayer();
                SeasonPlayer seasonalPlayer = ctx.getSeasonPlayer();
                Map<String, String> t = ctx.getOptionalArguments();
                boolean isSeasonal = ctx.isSeasonal();
                content = Utils.replaceArguments(t, content, "season", "s").trim();

                String[] args = content.split(" ");
                String itemName = content;
                int itemNumber = 1;
                String split = args[0];
                boolean isMassive = !itemName.isEmpty() && split.matches("^[0-9]*$");
                if (isMassive) {
                    try {
                        itemNumber = Math.abs(Integer.parseInt(split));
                        itemName = itemName.replace(args[0], "").trim();
                    } catch (NumberFormatException e) {
                        ctx.sendLocalized("commands.market.sell.invalid", EmoteReference.ERROR);
                        return;
                    }
                }

                try {
                    if (args[0].equals("all") && !isSeasonal) {
                        ctx.sendLocalized("commands.market.sell.all.confirmation", EmoteReference.WARNING);
                        //Start the operation.
                        InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 60, e -> {
                            if (!e.getAuthor().getId().equals(ctx.getAuthor().getId())) {
                                return Operation.IGNORED;
                            }

                            String c = e.getMessage().getContentRaw();

                            if (c.equalsIgnoreCase("yes")) {
                                long all = player.getInventory().asList().stream()
                                        .filter(item -> item.getItem().isSellable())
                                        .mapToLong(value -> (long) (value.getItem().getValue() * value.getAmount() * 0.9d))
                                        .sum();

                                player.getInventory().clearOnlySellables();
                                player.addMoney(all);

                                ctx.sendLocalized("commands.market.sell.all.success", EmoteReference.MONEY, all);
                                player.saveAsync();
                                return Operation.COMPLETED;
                            } else if (c.equalsIgnoreCase("no")) {
                                ctx.sendLocalized("commands.market.sell.all.cancelled", EmoteReference.CORRECT);
                                return Operation.COMPLETED;
                            }

                            return Operation.IGNORED;
                        });

                        return;
                    }

                    Inventory playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();
                    Item toSell = Items.fromAny(itemName.replace("\"", "")).orElse(null);

                    if (toSell == null) {
                        ctx.sendLocalized("commands.market.sell.non_existent", EmoteReference.ERROR);
                        return;
                    }

                    if (!toSell.isSellable()) {
                        ctx.sendLocalized("commands.market.sell.no_sell_price", EmoteReference.ERROR);
                        return;
                    }

                    if (playerInventory.getAmount(toSell) < 1) {
                        ctx.sendLocalized("commands.market.sell.no_item_player", EmoteReference.STOP);
                        return;
                    }

                    if (playerInventory.getAmount(toSell) < itemNumber) {
                        ctx.sendLocalized("commands.market.sell.more_items_than_player", EmoteReference.ERROR);
                        return;
                    }

                    int many = itemNumber * -1;
                    long amount = Math.round((toSell.getValue() * 0.9)) * Math.abs(many);
                    playerInventory.process(new ItemStack(toSell, many));
                    if (isSeasonal)
                        seasonalPlayer.addMoney(amount);
                    else
                        player.addMoney(amount);

                    player.getData().setMarketUsed(player.getData().getMarketUsed() + 1);
                    ctx.sendLocalized("commands.market.sell.success", EmoteReference.CORRECT, Math.abs(many), toSell.getName(), amount);
                    player.saveAsync();

                    if (isSeasonal)
                        seasonalPlayer.saveAsync();
                } catch (Exception e) {
                    ctx.send(EmoteReference.ERROR + ctx.getLanguageContext().get("general.invalid_syntax"));
                }
            }
        });

        marketCommand.addSubCommand("buy", new SubCommand() {
            @Override
            public String description() {
                return "Buys an item. Usage: `~>market buy <item>`. You can buy multiple items if you put the amount before the item. " +
                        "You can use all, half and quarter to buy for ex., a quarter of 5000 items.";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.market.buy.no_item_amount", EmoteReference.ERROR);
                    return;
                }

                Player player = ctx.getPlayer();
                SeasonPlayer seasonalPlayer = ctx.getSeasonPlayer();
                Map<String, String> t = ctx.getOptionalArguments();
                boolean isSeasonal = ctx.isSeasonal();
                content = Utils.replaceArguments(t, content, "season", "s").trim();

                String[] args = content.split(" ");
                String itemName = content;
                int itemNumber = 1;
                String split = args[0];
                boolean isMassive = !itemName.isEmpty() && split.matches("^[0-9]*$");
                if (isMassive) {
                    try {
                        itemNumber = Math.abs(Integer.parseInt(split));
                        itemName = itemName.replace(args[0], "").trim();
                    } catch (NumberFormatException e) {
                        ctx.sendLocalized("commands.market.buy.invalid", EmoteReference.ERROR);
                        return;
                    }
                } else {
                    //This is silly but works, people can stop asking about this now :o
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

                        if (itemNumber > 1)
                            itemName = itemName.replace(args[0], "").trim();
                    }
                }

                final Item itemToBuy = Items.fromAnyNoId(itemName.replace("\"", "")).orElse(null);

                if (itemToBuy == null) {
                    ctx.sendLocalized("commands.market.buy.non_existent", EmoteReference.ERROR);
                    return;
                }

                try {
                    if (!itemToBuy.isBuyable() || itemToBuy.isPetOnly()) {
                        ctx.sendLocalized("commands.market.buy.no_buy_price", EmoteReference.ERROR);
                        return;
                    }

                    Inventory playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();
                    ItemStack stack = playerInventory.getStackOf(itemToBuy);
                    if ((stack != null && !stack.canJoin(new ItemStack(itemToBuy, itemNumber))) || itemNumber > 5000) {
                        //assume overflow
                        ctx.sendLocalized("commands.market.buy.item_limit_reached", EmoteReference.ERROR);
                        return;
                    }

                    boolean removedMoney = isSeasonal ? seasonalPlayer.removeMoney(itemToBuy.getValue() * itemNumber) : player.removeMoney(itemToBuy.getValue() * itemNumber);

                    if (removedMoney) {
                        playerInventory.process(new ItemStack(itemToBuy, itemNumber));
                        player.getData().addBadgeIfAbsent(Badge.BUYER);
                        player.getData().setMarketUsed(player.getData().getMarketUsed() + 1);

                        //Due to player data being updated here too.
                        player.saveAsync();

                        if (isSeasonal)
                            seasonalPlayer.saveAsync();

                        long playerMoney = isSeasonal ? seasonalPlayer.getMoney() : player.getMoney();

                        ctx.sendLocalized("commands.market.buy.success",
                                EmoteReference.OK, itemNumber, itemToBuy.getEmoji(), itemToBuy.getValue() * itemNumber, playerMoney
                        );
                    } else {
                        ctx.sendLocalized("commands.market.buy.not_enough_money", EmoteReference.STOP);
                    }
                } catch (Exception e) {
                    ctx.send(EmoteReference.ERROR + ctx.getLanguageContext().get("general.invalid_syntax"));
                }
            }
        });

        cr.registerAlias("market", "shop");
    }
}
