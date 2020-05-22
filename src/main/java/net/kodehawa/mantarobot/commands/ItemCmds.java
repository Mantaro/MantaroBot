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
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.item.special.Broken;
import net.kodehawa.mantarobot.commands.currency.item.special.Wrench;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Castable;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;

import java.awt.*;
import java.security.SecureRandom;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.Utils.handleIncreasingRatelimit;

@Module
public class ItemCmds {
    @Subscribe
    public void cast(CommandRegistry cr) {
        final IncreasingRateLimiter ratelimiter = new IncreasingRateLimiter.Builder()
                .spamTolerance(3)
                .limit(1)
                .cooldown(10, TimeUnit.SECONDS)
                .cooldownPenaltyIncrease(2, TimeUnit.SECONDS)
                .maxCooldown(2, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("cast")
                .build();

        final SecureRandom random = new SecureRandom();

        TreeCommand castCommand = (TreeCommand) cr.register("cast", new TreeCommand(Category.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        if (content.trim().isEmpty()) {
                            ctx.sendLocalized("commands.cast.no_item_found", EmoteReference.ERROR);
                            return;
                        }

                        //Argument parsing.
                        Map<String, String> t = ctx.getOptionalArguments();
                        content = Utils.replaceArguments(t, content, "season", "s").trim();

                        String[] arguments = StringUtils.advancedSplitArgs(content, -1);

                        boolean isSeasonal = t.containsKey("season") || t.containsKey("s");
                        boolean isMultiple = t.containsKey("amount");

                        //Get the necessary entities.
                        SeasonPlayer seasonalPlayer = ctx.getSeasonPlayer();
                        Player player = ctx.getPlayer();
                        DBUser user = ctx.getDBUser();

                        //Why
                        Optional<Item> toCast = Items.fromAnyNoId(arguments[0]);
                        Optional<Item> optionalWrench = Optional.empty();

                        if (arguments.length > 1)
                            optionalWrench = Items.fromAnyNoId(arguments[1]);

                        if (toCast.isEmpty()) {
                            ctx.sendLocalized("commands.cast.no_item_found", EmoteReference.ERROR);
                            return;
                        }

                        if (!handleIncreasingRatelimit(ratelimiter, ctx.getAuthor(), ctx))
                            return;

                        int amountSpecified = 1;
                        try {
                            if (isMultiple)
                                amountSpecified = Math.max(1, Integer.parseInt(t.get("amount")));
                            else if (arguments.length > 2)
                                amountSpecified = Math.max(1, Integer.parseInt(arguments[2]));
                            else if (optionalWrench.isEmpty() && arguments.length > 1)
                                amountSpecified = Math.max(1, Integer.parseInt(arguments[1]));
                        } catch (Exception ignored) { }

                        Item castItem = toCast.get();
                        //This is a good way of getting if it's castable, since implementing an interface wouldn't cut it (some rods aren't castable, for example)
                        if (!castItem.getItemType().isCastable()) {
                            ctx.sendLocalized("commands.cast.item_not_cast", EmoteReference.ERROR);
                            return;
                        }

                        if (castItem.getRecipe() == null || castItem.getRecipe().isEmpty()) {
                            ctx.sendLocalized("commands.cast.item_not_cast", EmoteReference.ERROR);
                            return;
                        }

                        Item wrenchItem = optionalWrench.orElse(Items.WRENCH);

                        if (!(wrenchItem instanceof Wrench)) {
                            wrenchItem = Items.WRENCH;
                        }

                        //How many steps until this again?
                        Wrench wrench = (Wrench) wrenchItem;

                        if (amountSpecified > 1 && wrench.getLevel() < 2) {
                            ctx.sendLocalized("commands.cast.low_tier", EmoteReference.ERROR, wrench.getLevel());
                            return;
                        }

                        //Build recipe.
                        Map<Item, Integer> castMap = new HashMap<>();
                        String recipe = castItem.getRecipe();
                        String[] splitRecipe = recipe.split(";");

                        //How many parenthesis again?
                        long castCost = (long) (((castItem.getValue() / 2) * amountSpecified) * wrench.getMultiplierReduction());

                        long money = isSeasonal ? seasonalPlayer.getMoney() : player.getMoney();
                        boolean isItemCastable = castItem instanceof Castable;
                        int wrenchLevelRequired = isItemCastable ? ((Castable) castItem).getCastLevelRequired() : 1;

                        if (money < castCost) {
                            ctx.sendLocalized("commands.cast.not_enough_money", EmoteReference.ERROR, castCost);
                            return;
                        }

                        if (wrench.getLevel() < wrenchLevelRequired) {
                            ctx.sendLocalized("commands.cast.not_enough_wrench_level", EmoteReference.ERROR, wrench.getLevel(), wrenchLevelRequired);
                            return;
                        }

                        int limit = (isItemCastable ? ((Castable) castItem).getMaximumCastAmount() : 5);
                        if (amountSpecified > limit) {
                            ctx.sendLocalized("commands.cast.too_many_amount", EmoteReference.ERROR, limit, amountSpecified);
                            return;
                        }

                        Inventory playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();

                        if (!playerInventory.containsItem(wrenchItem)) {
                            ctx.sendLocalized("commands.cast.no_tool", EmoteReference.ERROR, Items.WRENCH.getName());
                            return;
                        }

                        int dust = user.getData().getDustLevel();
                        if (dust > 95) {
                            ctx.sendLocalized("commands.cast.dust", EmoteReference.ERROR, dust);
                            return;
                        }

                        int increment = 0;
                        //build recipe
                        StringBuilder recipeString = new StringBuilder();
                        for (int i : castItem.getRecipeTypes()) {
                            Item item = Items.fromId(i);
                            int amount = Integer.parseInt(splitRecipe[increment]) * amountSpecified;

                            if (!playerInventory.containsItem(item)) {
                                ctx.sendLocalized("commands.cast.no_item", EmoteReference.ERROR, item.getName());
                                return;
                            }

                            int inventoryAmount = playerInventory.getAmount(item);
                            //Subtract 1 from the usable amount since if your wrench breaks in the process, it causes issues.
                            int usableInventoryAmount = (i == Items.idOf(wrench)) ? inventoryAmount - 1 : inventoryAmount;
                            if (usableInventoryAmount < amount) {
                                ctx.sendLocalized("commands.cast.not_enough_items", EmoteReference.ERROR, item.getName(), amount, usableInventoryAmount);

                                if(usableInventoryAmount < inventoryAmount)
                                    ctx.sendLocalized("commands.cast.wrench_multiple_use");

                                return;
                            }

                            castMap.put(item, amount);
                            recipeString.append(amount).append("x ").append(item.getName()).append(" ");
                            increment++;
                        }

                        if (playerInventory.getAmount(castItem) + amountSpecified > 5000) {
                            ctx.sendLocalized("commands.cast.too_many", EmoteReference.ERROR);
                            return;
                        }

                        for (Map.Entry<Item, Integer> entry : castMap.entrySet()) {
                            Item i = entry.getKey();
                            int amount = entry.getValue();
                            playerInventory.process(new ItemStack(i, -amount));
                        }
                        //end of recipe build

                        playerInventory.process(new ItemStack(castItem, amountSpecified));

                        String message = "";
                        //The higher the chance, the lower it's the chance to break. Yes, I know.
                        if (random.nextInt(100) > wrench.getChance()) {
                            playerInventory.process(new ItemStack(wrenchItem, -1));
                            message += ctx.getLanguageContext().get("commands.cast.item_broke");
                        }

                        user.getData().increaseDustLevel(3);
                        user.save();

                        if (isSeasonal) {
                            seasonalPlayer.removeMoney(castCost);
                            seasonalPlayer.save();
                        } else {
                            player.removeMoney(castCost);
                            player.save();
                        }

                        ctx.sendFormat(ctx.getLanguageContext().get("commands.cast.success") + "\n" + message,
                                EmoteReference.WRENCH, castItem.getEmoji(), castItem.getName(), castCost, recipeString.toString().trim()
                        );
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Allows you to cast any castable item given you have the necessary elements.\n" +
                                "Casting requires you to have the necessary materials to cast the item, and it has a cost of `item value / 2`.\n" +
                                "Cast-able items are only able to be acquired by this command. They're non-buyable items, though you can sell them for a profit.\n" +
                                "If you specify the item and the wrench, you can use amount without -amount. Example: `~>cast \"diamond pickaxe\" \"sparkle wrench\" 10`")
                        .setUsage("`~>cast <item> [wrench] [-amount <amount>]` - Casts the item you provide.")
                        .addParameter("item", "The item name or emoji. If the name contains spaces \"wrap it in quotes\"")
                        .addParameterOptional("wrench", "The wrench name or emoji. If the name contains spaces \"wrap it in quotes\"")
                        .addParameterOptional("amount", "The amount of items you want to cast. Depends on your wrench, maximum of 10.")
                        .build();
            }
        });

        castCommand.addSubCommand("ls", new SubCommand() {
            @Override
            public String description() {
                return "Lists all of the cast-able items";
            }

            @Override
            protected void call(Context ctx, String content) {
                List<Item> castableItems = Arrays.stream(Items.ALL)
                        .filter(i -> i.getItemType().isCastable() && i.getRecipeTypes() != null && i.getRecipe() != null)
                        .collect(Collectors.toList());

                var languageContext = ctx.getLanguageContext();
                List<MessageEmbed.Field> fields = new LinkedList<>();
                EmbedBuilder builder = new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.cast.ls.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setColor(Color.PINK)
                        .setFooter(String.format(languageContext.get("general.requested_by"), ctx.getMember().getEffectiveName()), null);

                for (Item item : castableItems) {
                    //Build recipe explanation
                    if (item.getRecipe().isEmpty())
                        continue;

                    String[] recipeAmount = item.getRecipe().split(";");
                    AtomicInteger ai = new AtomicInteger();

                    String recipe = Arrays.stream(item.getRecipeTypes()).mapToObj((i) -> {
                        Item recipeItem = Items.fromId(i);
                        return recipeItem.getEmoji() + " " + recipeAmount[ai.getAndIncrement()] + "x" + "\u2009*" + recipeItem.getName() + "*";
                    }).collect(Collectors.joining(", "));
                    //End of build recipe explanation

                    int castLevel = (item instanceof Castable) ? ((Castable) item).getCastLevelRequired() : 1;
                    fields.add(new MessageEmbed.Field(item.getEmoji() + " " + item.getName(),
                            languageContext.get(item.getDesc()) + "\n**" + languageContext.get("commands.cast.ls.cost") + "**" +
                                    item.getValue() / 2 + " " + languageContext.get("commands.gamble.credits") + ".\n**Recipe: **" + recipe +
                                    "\n**Wrench Tier: **" + castLevel + ".",
                            false)
                    );
                }

                List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(4, fields);
                if (ctx.hasReactionPerms()) {
                    builder.setDescription(String.format(languageContext.get("general.buy_sell_paged_react"), splitFields.size(), "\n" + EmoteReference.TALKING + languageContext.get("commands.cast.ls.desc")));
                    DiscordUtils.list(ctx.getEvent(), 45, false, builder, splitFields);
                } else {
                    builder.setDescription(String.format(languageContext.get("general.buy_sell_paged_text"), splitFields.size(), "\n" + EmoteReference.TALKING + languageContext.get("commands.cast.ls.desc")));
                    DiscordUtils.listText(ctx.getEvent(), 45, false, builder, splitFields);
                }
            }
        });

        castCommand.createSubCommandAlias("ls", "list");
        castCommand.createSubCommandAlias("ls", "Is"); //people, smh.
    }

    @Subscribe
    public void repair(CommandRegistry registry) {
        final IncreasingRateLimiter ratelimiter = new IncreasingRateLimiter.Builder()
                .spamTolerance(3)
                .limit(1)
                .cooldown(5, TimeUnit.SECONDS)
                .cooldownPenaltyIncrease(2, TimeUnit.SECONDS)
                .maxCooldown(2, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("repair")
                .build();

        final SecureRandom random = new SecureRandom();

        //The contents of this command are -mostly- taken from the cast command, so they'll look similar.
        TreeCommand rp = (TreeCommand) registry.register("repair", new TreeCommand(Category.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String commandName, String content) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        if (content.isEmpty()) {
                            ctx.sendLocalized("commands.repair.no_item", EmoteReference.ERROR);
                            return;
                        }

                        ManagedDatabase db = MantaroData.db();

                        //Argument parsing.
                        Map<String, String> t = ctx.getOptionalArguments();
                        boolean isSeasonal = t.containsKey("season") || t.containsKey("s");

                        content = Utils.replaceArguments(t, content, "season", "s");
                        String[] args = ctx.getArguments();

                        //Get the necessary entities.
                        SeasonPlayer seasonalPlayer = ctx.getSeasonPlayer();
                        Player player = ctx.getPlayer();
                        DBUser user = ctx.getDBUser();

                        String itemString = args[0];
                        Item item = Items.fromAnyNoId(itemString).orElse(null);
                        Inventory playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();
                        Item wrench = playerInventory.containsItem(Items.WRENCH_SPARKLE) ? Items.WRENCH_SPARKLE : Items.WRENCH_COMET;

                        if (args.length > 1) {
                            wrench = Items.fromAnyNoId(args[1]).orElse(null);
                        }

                        if (item == null) {
                            ctx.sendLocalized("commands.repair.no_item_found", EmoteReference.ERROR);
                            return;
                        }

                        if (!(item instanceof Broken)) {
                            ctx.sendLocalized("commands.repair.cant_repair", EmoteReference.ERROR, item.getName());
                            return;
                        }

                        if (!(wrench instanceof Wrench)) {
                            ctx.sendLocalized("commands.repair.not_wrench", EmoteReference.ERROR);
                            return;
                        }

                        if (((Wrench) wrench).getLevel() < 2) {
                            ctx.sendLocalized("commands.repair.not_enough_level", EmoteReference.ERROR);
                            return;
                        }

                        if (!playerInventory.containsItem(item)) {
                            ctx.sendLocalized("commands.repair.no_main_item", EmoteReference.ERROR);
                            return;
                        }

                        if (!playerInventory.containsItem(wrench)) {
                            ctx.sendLocalized("commands.repair.no_tool", EmoteReference.ERROR, wrench.getName());
                            return;
                        }

                        int dust = user.getData().getDustLevel();
                        if (dust > 95) {
                            ctx.sendLocalized("commands.repair.dust", EmoteReference.ERROR, dust);
                            return;
                        }

                        if (!handleIncreasingRatelimit(ratelimiter, ctx.getAuthor(), ctx))
                            return;

                        Broken brokenItem = (Broken) item;
                        Item repairedItem = Items.fromId(brokenItem.getMainItem());
                        long repairCost = repairedItem.getValue() / 3;

                        long playerMoney = isSeasonal ? seasonalPlayer.getMoney() : player.getMoney();
                        if (playerMoney < repairCost) {
                            ctx.sendLocalized("commands.repair.not_enough_money", EmoteReference.ERROR, playerMoney, repairCost);
                            return;
                        }

                        Map<Item, Integer> recipeMap = new HashMap<>();
                        String repairRecipe = brokenItem.getRecipe();
                        String[] splitRecipe = repairRecipe.split(";");
                        StringBuilder recipeString = new StringBuilder();

                        for (String s : splitRecipe) {
                            String[] split = s.split(",");
                            int amount = Integer.parseInt(split[0]);
                            Item needed = Items.fromId(Integer.parseInt(split[1]));

                            if (!playerInventory.containsItem(needed)) {
                                ctx.sendLocalized("commands.repair.no_item_recipe", EmoteReference.ERROR, needed.getName());
                                return;
                            }

                            int inventoryAmount = playerInventory.getAmount(needed);
                            if (inventoryAmount < amount) {
                                ctx.sendLocalized("commands.repair.not_enough_items", EmoteReference.ERROR, needed.getName(), amount, inventoryAmount);
                                return;
                            }

                            recipeMap.put(needed, amount);
                            recipeString.append(amount).append("x ").append(needed.getName()).append(" ");
                        }

                        for (Map.Entry<Item, Integer> entry : recipeMap.entrySet()) {
                            Item i = entry.getKey();
                            int amount = entry.getValue();
                            playerInventory.process(new ItemStack(i, -amount));
                        }
                        //end of recipe build

                        playerInventory.process(new ItemStack(brokenItem, -1));
                        playerInventory.process(new ItemStack(repairedItem, 1));

                        String message = "";
                        //The higher the chance, the lower it's the chance to break. Yes, I know.
                        if (random.nextInt(100) > ((Wrench) wrench).getChance()) {
                            playerInventory.process(new ItemStack(wrench, -1));
                            message += ctx.getLanguageContext().get("commands.repair.item_broke");
                        }

                        user.getData().increaseDustLevel(3);
                        user.save();

                        if (isSeasonal) {
                            seasonalPlayer.removeMoney(repairCost);
                            seasonalPlayer.save();
                        } else {
                            player.removeMoney(repairCost);
                            player.save();
                        }

                        ctx.sendFormat(ctx.getLanguageContext().get("commands.repair.success") + "\n" + message,
                                EmoteReference.WRENCH, brokenItem.getEmoji(), brokenItem.getName(),
                                repairedItem.getEmoji(), repairedItem.getName(), repairCost,
                                recipeString.toString().trim()
                        );
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Allows you to repair any broken item given you have the necessary elements.\n" +
                                "Repairing requires you to have the necessary materials to cast the item, and it has a cost of `item value / 3`.\n")
                        .setUsage("`~>repair <item>` - Repairs a broken item.")
                        .addParameter("item", "The item name or emoji. If the name contains spaces \"wrap it in quotes\"")
                        .build();
            }
        });

        rp.addSubCommand("ls", new SubCommand() {
            @Override
            public String description() {
                return "Lists all of the repair-able items";
            }

            @Override
            protected void call(Context ctx, String content) {
                List<Broken> repairableItems = Arrays.stream(Items.ALL)
                        .filter(Broken.class::isInstance)
                        .map(Broken.class::cast)
                        .collect(Collectors.toList());

                var languageContext = ctx.getLanguageContext();
                List<MessageEmbed.Field> fields = new LinkedList<>();
                EmbedBuilder builder = new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.repair.ls.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setColor(Color.PINK)
                        .setFooter(String.format(languageContext.get("general.requested_by"), ctx.getMember().getEffectiveName()), null);

                for (Broken item : repairableItems) {
                    //Build recipe explanation
                    if (item.getRecipe().isEmpty())
                        continue;

                    String repairRecipe = item.getRecipe();
                    String[] splitRecipe = repairRecipe.split(";");
                    StringBuilder recipeString = new StringBuilder();
                    Item mainItem = Items.fromId(item.getMainItem());
                    for (String s : splitRecipe) {
                        String[] split = s.split(",");
                        int amount = Integer.parseInt(split[0]);
                        Item needed = Items.fromId(Integer.parseInt(split[1]));
                        recipeString.append(amount).append("x ").append(needed.getEmoji()).append(" *").append(needed.getName()).append("*|");
                    }

                    //End of build recipe explanation
                    //This is still, but if it works it works.
                    String recipe = String.join(", ", recipeString.toString().split("\\|"));
                    long repairCost = item.getValue() / 3;

                    fields.add(new MessageEmbed.Field(item.getEmoji() + " " + item.getName(),
                            languageContext.get(item.getDesc()) + "\n**" + languageContext.get("commands.repair.ls.cost") + "**" +
                                    repairCost + " " + languageContext.get("commands.gamble.credits") + ".\n**Recipe: **" + recipe + "\n**Item: **" + mainItem.getEmoji() + " " + mainItem.getName(), true));
                }

                List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(4, fields);
                if (ctx.hasReactionPerms()) {
                    builder.setDescription(String.format(languageContext.get("general.buy_sell_paged_react"), splitFields.size(), "\n" + EmoteReference.TALKING + languageContext.get("commands.repair.ls.desc")));
                    DiscordUtils.list(ctx.getEvent(), 45, false, builder, splitFields);
                } else {
                    builder.setDescription(String.format(languageContext.get("general.buy_sell_paged_text"), splitFields.size(), "\n" + EmoteReference.TALKING + languageContext.get("commands.repair.ls.desc")));
                    DiscordUtils.listText(ctx.getEvent(), 45, false, builder, splitFields);
                }
            }
        }).createSubCommandAlias("ls", "list");
    }

    @Subscribe
    public void iteminfo(CommandRegistry registry) {
        registry.register("iteminfo", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.iteminfo.no_content", EmoteReference.ERROR);
                    return;
                }

                Optional<Item> itemOptional = Items.fromAnyNoId(content.replace("\"", ""));

                if (itemOptional.isEmpty()) {
                    ctx.sendLocalized("commands.iteminfo.no_item", EmoteReference.ERROR);
                    return;
                }

                Item item = itemOptional.get();
                String description = ctx.getLanguageContext().get(item.getDesc());
                ctx.sendLocalized("commands.iteminfo.success", EmoteReference.BLUE_SMALL_MARKER,
                        item.getEmoji(), item.getName(),
                        ctx.getLanguageContext().get(item.getTranslatedName()),
                        item.getItemType(), description
                );
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shows the information of an item")
                        .setUsage("`~>iteminfo <item name>` - Shows the info of an item.")
                        .addParameter("item", "The item name or emoji. If the name contains spaces \"wrap it in quotes\"")
                        .build();
            }
        });
    }
}
