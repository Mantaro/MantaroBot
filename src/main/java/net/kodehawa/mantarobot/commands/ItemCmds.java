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
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.special.Broken;
import net.kodehawa.mantarobot.commands.currency.item.special.Wrench;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Castable;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Salvageable;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.campaign.Campaign;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;

import java.awt.*;
import java.security.SecureRandom;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.RatelimitUtils.handleIncreasingRatelimit;

@Module
public class ItemCmds {
    @Subscribe
    public void cast(CommandRegistry cr) {
        final IncreasingRateLimiter ratelimiter = new IncreasingRateLimiter.Builder()
                .spamTolerance(3)
                .limit(1)
                .cooldown(5, TimeUnit.SECONDS)
                .cooldownPenaltyIncrease(2, TimeUnit.SECONDS)
                .maxCooldown(2, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("cast")
                .build();

        final SecureRandom random = new SecureRandom();

        TreeCommand castCommand = cr.register("cast", new TreeCommand(CommandCategory.CURRENCY) {
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
                        PlayerData playerData = player.getData();
                        DBUser user = ctx.getDBUser();
                        UserData userData = user.getData();

                        //Why
                        Optional<Item> toCast = ItemHelper.fromAnyNoId(arguments[0]);
                        Optional<Item> optionalWrench = Optional.empty();

                        if (arguments.length > 1)
                            optionalWrench = ItemHelper.fromAnyNoId(arguments[1]);

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
                        // This is a good way of getting if it's castable,
                        // since implementing an interface wouldn't cut it (some rods aren't castable, for example)
                        if (!castItem.getItemType().isCastable()) {
                            ctx.sendLocalized("commands.cast.item_not_cast", EmoteReference.ERROR);
                            return;
                        }

                        if (castItem.getRecipe() == null || castItem.getRecipe().isEmpty()) {
                            ctx.sendLocalized("commands.cast.item_not_cast", EmoteReference.ERROR);
                            return;
                        }

                        Item wrenchItem = optionalWrench.orElse(ItemReference.WRENCH);

                        if (!(wrenchItem instanceof Wrench)) {
                            wrenchItem = ItemReference.WRENCH;
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

                        long money = isSeasonal ? seasonalPlayer.getMoney() : player.getCurrentMoney();
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

                        // Limit is double with sparkle wrench
                        if(wrench == ItemReference.WRENCH_SPARKLE)
                            limit *= 2;

                        if (amountSpecified > limit) {
                            ctx.sendLocalized("commands.cast.too_many_amount", EmoteReference.ERROR, limit, amountSpecified);
                            return;
                        }

                        Inventory playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();

                        if (!playerInventory.containsItem(wrenchItem)) {
                            ctx.sendLocalized("commands.cast.no_tool", EmoteReference.ERROR, ItemReference.WRENCH.getName());
                            return;
                        }

                        int dust = userData.getDustLevel();
                        if (dust > 95) {
                            ctx.sendLocalized("commands.cast.dust", EmoteReference.ERROR, dust);
                            return;
                        }

                        int increment = 0;
                        //build recipe
                        StringBuilder recipeString = new StringBuilder();
                        for (int i : castItem.getRecipeTypes()) {
                            Item item = ItemHelper.fromId(i);
                            int amount = Integer.parseInt(splitRecipe[increment]) * amountSpecified;

                            if (!playerInventory.containsItem(item)) {
                                ctx.sendLocalized("commands.cast.no_item", EmoteReference.ERROR, item.getName());
                                return;
                            }

                            int inventoryAmount = playerInventory.getAmount(item);
                            //Subtract 1 from the usable amount since if your wrench breaks in the process, it causes issues.
                            int usableInventoryAmount = (i == ItemHelper.idOf(wrench)) ? inventoryAmount - 1 : inventoryAmount;
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

                        if (playerData.shouldSeeCampaign()) {
                            message += Campaign.PREMIUM.getStringFromCampaign(ctx.getLanguageContext(), user.isPremium());
                            playerData.markCampaignAsSeen();
                        }

                        //The higher the chance, the lower it's the chance to break. Yes, I know.
                        if (random.nextInt(100) > wrench.getChance()) {
                            playerInventory.process(new ItemStack(wrenchItem, -1));
                            message += ctx.getLanguageContext().get("commands.cast.item_broke");
                        }

                        userData.increaseDustLevel(3);
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
                                "Cast-able items are only able to be acquired by this command. " +
                                "They're non-buyable items, though you can sell them for a profit.\n" +
                                "If you specify the item and the wrench, you can use amount without -amount. " +
                                "Example: `~>cast \"diamond pickaxe\" \"sparkle wrench\" 10`")
                        .setUsage("`~>cast <item> [wrench] [-amount <amount>]` - Casts the item you provide.")
                        .addParameter("item",
                                "The item name or emoji. If the name contains spaces \"wrap it in quotes\"")
                        .addParameterOptional("wrench",
                                "The wrench name or emoji. If the name contains spaces \"wrap it in quotes\"")
                        .addParameterOptional("amount",
                                "The amount of items you want to cast. Depends on your wrench, maximum of 10.")
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
                List<Item> castableItems = Arrays.stream(ItemReference.ALL)
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
                        Item recipeItem = ItemHelper.fromId(i);
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

                DiscordUtils.sendPaginatedEmbed(ctx, builder, DiscordUtils.divideFields(4, fields), languageContext.get("commands.cast.ls.desc"));
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
        TreeCommand rp = registry.register("repair", new TreeCommand(CommandCategory.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String commandName, String content) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        if (content.isEmpty()) {
                            ctx.sendLocalized("commands.repair.no_item", EmoteReference.ERROR);
                            return;
                        }

                        //Argument parsing.
                        Map<String, String> t = ctx.getOptionalArguments();
                        boolean isSeasonal = t.containsKey("season") || t.containsKey("s");

                        String[] args = ctx.getArguments();

                        //Get the necessary entities.
                        SeasonPlayer seasonalPlayer = ctx.getSeasonPlayer();
                        Player player = ctx.getPlayer();
                        DBUser user = ctx.getDBUser();

                        String itemString = args[0];
                        Item item = ItemHelper.fromAnyNoId(itemString).orElse(null);
                        Inventory playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();
                        Item wrench = playerInventory.containsItem(ItemReference.WRENCH_SPARKLE) ?
                                ItemReference.WRENCH_SPARKLE : ItemReference.WRENCH_COMET;

                        if (args.length > 1) {
                            wrench = ItemHelper.fromAnyNoId(args[1]).orElse(null);
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
                        Item repairedItem = ItemHelper.fromId(brokenItem.getMainItem());
                        long repairCost = repairedItem.getValue() / 3;

                        long playerMoney = isSeasonal ? seasonalPlayer.getMoney() : player.getCurrentMoney();
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
                            Item needed = ItemHelper.fromId(Integer.parseInt(split[1]));

                            if (!playerInventory.containsItem(needed)) {
                                ctx.sendLocalized("commands.repair.no_item_recipe", EmoteReference.ERROR, needed.getName());
                                return;
                            }

                            int inventoryAmount = playerInventory.getAmount(needed);
                            if (inventoryAmount < amount) {
                                ctx.sendLocalized("commands.repair.not_enough_items",
                                        EmoteReference.ERROR, needed.getName(), amount, inventoryAmount
                                );
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
                                "Repairing requires you to have the necessary materials to cast the item, " +
                                "and it has a cost of `item value / 3`.\n")
                        .setUsage("`~>repair <item>` - Repairs a broken item.")
                        .addParameter("item",
                                "The item name or emoji. If the name contains spaces \"wrap it in quotes\"")
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
                List<Broken> repairableItems = Arrays.stream(ItemReference.ALL)
                        .filter(Broken.class::isInstance)
                        .map(Broken.class::cast)
                        .collect(Collectors.toList());

                var languageContext = ctx.getLanguageContext();
                List<MessageEmbed.Field> fields = new LinkedList<>();
                EmbedBuilder builder = new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.repair.ls.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setColor(Color.PINK)
                        .setFooter(
                                String.format(
                                        languageContext.get("general.requested_by"), ctx.getMember().getEffectiveName()
                                ), null
                        );

                for (Broken item : repairableItems) {
                    //Build recipe explanation
                    if (item.getRecipe().isEmpty())
                        continue;

                    String repairRecipe = item.getRecipe();
                    String[] splitRecipe = repairRecipe.split(";");
                    StringBuilder recipeString = new StringBuilder();
                    Item mainItem = ItemHelper.fromId(item.getMainItem());
                    for (String s : splitRecipe) {
                        String[] split = s.split(",");
                        int amount = Integer.parseInt(split[0]);
                        Item needed = ItemHelper.fromId(Integer.parseInt(split[1]));
                        recipeString.append(amount).append("x ")
                                .append(needed.getEmoji())
                                .append(" *")
                                .append(needed.getName())
                                .append("*|");
                    }

                    //End of build recipe explanation
                    //This is still, but if it works it works.
                    String recipe = String.join(", ", recipeString.toString().split("\\|"));
                    long repairCost = item.getValue() / 3;

                    fields.add(new MessageEmbed.Field(item.getEmoji() + " " + item.getName(),
                            languageContext.get(item.getDesc()) + "\n**" + languageContext.get("commands.repair.ls.cost") + "**" +
                                    repairCost + " " + languageContext.get("commands.gamble.credits") +
                                    ".\n**Recipe: **" + recipe + "\n**Item: **" + mainItem.getEmoji() + " " + mainItem.getName(),
                            false)
                    );
                }

                DiscordUtils.sendPaginatedEmbed(ctx, builder, DiscordUtils.divideFields(4, fields), languageContext.get("commands.repair.ls.desc"));
            }
        }).createSubCommandAlias("ls", "list").createSubCommandAlias("ls", "is");
    }

    @Subscribe
    public void salvage(CommandRegistry cr) {
        final var random = new SecureRandom();
        final var ratelimiter = new IncreasingRateLimiter.Builder()
                .spamTolerance(3)
                .limit(1)
                .cooldown(5, TimeUnit.SECONDS)
                .cooldownPenaltyIncrease(2, TimeUnit.SECONDS)
                .maxCooldown(2, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("repair")
                .build();

        TreeCommand sv = cr.register("salvage", new TreeCommand(CommandCategory.CURRENCY) {
            @Override
            public Command defaultTrigger(Context context, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        if (content.isEmpty()) {
                            ctx.sendLocalized("commands.salvage.no_item", EmoteReference.ERROR);
                            return;
                        }

                        //Argument parsing.
                        boolean isSeasonal = ctx.isSeasonal();
                        //Get the necessary entities.
                        final var seasonalPlayer = ctx.getSeasonPlayer();
                        final var player = ctx.getPlayer();
                        final var user = ctx.getDBUser();

                        final var args = ctx.getArguments();
                        final var itemString = args[0];
                        final var item = ItemHelper.fromAnyNoId(itemString).orElse(null);
                        final var playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();
                        var wrench = playerInventory.containsItem(ItemReference.WRENCH_SPARKLE) ?
                                ItemReference.WRENCH_SPARKLE : ItemReference.WRENCH_COMET;
                        var custom = false;
                        if (args.length > 1) {
                            wrench = ItemHelper.fromAnyNoId(args[1]).orElse(null);
                            custom = true;
                        }

                        if (item == null) {
                            ctx.sendLocalized("commands.salvage.no_item_found", EmoteReference.ERROR);
                            return;
                        }

                        if (!(item instanceof Broken)) {
                            ctx.sendLocalized("commands.salvage.not_broken", EmoteReference.ERROR, item.getName());
                            return;
                        }

                        final var broken = (Broken) item;
                        final var original = ItemHelper.fromId(broken.getMainItem());

                        if (!(original instanceof Salvageable)) {
                            ctx.sendLocalized("commands.salvage.cant_salvage", EmoteReference.ERROR, item.getName());
                            return;
                        }

                        if (!(wrench instanceof Wrench)) {
                            ctx.sendLocalized("commands.salvage.not_wrench", EmoteReference.ERROR);
                            return;
                        }

                        if (((Wrench) wrench).getLevel() < 2) {
                            ctx.sendLocalized("commands.salvage.not_enough_level", EmoteReference.ERROR);
                            return;
                        }

                        if (!playerInventory.containsItem(item)) {
                            ctx.sendLocalized("commands.salvage.no_main_item", EmoteReference.ERROR);
                            return;
                        }

                        if (!playerInventory.containsItem(wrench) && custom) {
                            ctx.sendLocalized("commands.salvage.no_tool", EmoteReference.ERROR, wrench.getName());
                            return;
                        }

                        if (!playerInventory.containsItem(wrench) && !custom) {
                            ctx.sendLocalized("commands.salvage.no_tool_default", EmoteReference.ERROR, wrench.getName());
                            return;
                        }

                        int dust = user.getData().getDustLevel();
                        if (dust > 95) {
                            ctx.sendLocalized("commands.salvage.dust", EmoteReference.ERROR, dust);
                            return;
                        }

                        if (!handleIncreasingRatelimit(ratelimiter, ctx.getAuthor(), ctx))
                            return;

                        final var salvageable = (Salvageable) original;
                        List<Item> returns = salvageable.getReturns().stream().map(ItemHelper::fromId).collect(Collectors.toList());

                        if(returns.isEmpty()) {
                            ctx.sendLocalized("commands.salvage.no_returnables", EmoteReference.SAD);
                            return;
                        }

                        var message = "";
                        if (random.nextInt(100) > ((Wrench) wrench).getChance()) {
                            playerInventory.process(new ItemStack(wrench, -1));
                            message += ctx.getLanguageContext().get("commands.repair.item_broke");
                        }

                        var salvageCost = item.getValue() / 3;
                        var toReturn = returns.get(random.nextInt(returns.size()));
                        playerInventory.process(new ItemStack(toReturn, 1));
                        playerInventory.process(new ItemStack(broken, -1));

                        user.getData().increaseDustLevel(3);
                        user.save();

                        if (isSeasonal) {
                            seasonalPlayer.removeMoney(salvageCost);
                            seasonalPlayer.save();
                        } else {
                            player.removeMoney(salvageCost);
                            player.save();
                        }

                        ctx.sendLocalized("commands.salvage.success", EmoteReference.POPPER, item.getName(), toReturn, salvageCost, message);
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Salvages an item. Useful when you can't repair it but wanna get something back. " +
                                "The cost is 1/3rd of the item price.")
                        .setUsage("`~>salvage <item> [wrench]` - Salvages an item.")
                        .addParameter("item",
                                "The item name or emoji. If the name contains spaces \"wrap it in quotes\"")
                        .addParameterOptional("wrench", "The wrench to use.")
                        .build();
            }
        });

        sv.addSubCommand("ls", new SubCommand() {
            @Override
            public String description() {
                return "Lists all of the salvage-able items";
            }

            @Override
            protected void call(Context ctx, String content) {
                var broken = Arrays.stream(ItemReference.ALL)
                        .filter(Broken.class::isInstance)
                        .map(Broken.class::cast)
                        .collect(Collectors.toList());

                var languageContext = ctx.getLanguageContext();
                List<MessageEmbed.Field> fields = new LinkedList<>();
                EmbedBuilder builder = new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.salvage.ls.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setColor(Color.PINK)
                        .setFooter(String.format(languageContext.get("general.requested_by"), ctx.getMember().getEffectiveName()), null);

                for (Broken item : broken) {
                    var mainItem = item.getItem();
                    if (!(mainItem instanceof Salvageable)) {
                        continue;
                    }

                    var salvageable = (Salvageable) mainItem;

                    if (salvageable.getReturns().isEmpty())
                        continue;

                    var recipeString = new StringBuilder();
                    var returns = salvageable.getReturns();
                    var salvageCost = mainItem.getValue() / 3;
                    for(int id : returns) {
                        Item rt = ItemHelper.fromId(id);
                        recipeString.append(rt.getEmoji())
                                .append(" ")
                                .append(rt.getName())
                                .append(" ");
                    }

                    fields.add(new MessageEmbed.Field(item.getEmoji() + " " + item.getName(),
                            languageContext.get(item.getDesc()) + "\n**" +
                                    languageContext.get("commands.salvage.ls.cost") + "**" +
                                    salvageCost + " " + languageContext.get("commands.gamble.credits") +
                                    ".\n**" + languageContext.get("commands.salvage.ls.return") + "** " +
                                    recipeString.toString(),
                            false)
                    );
                }

                DiscordUtils.sendPaginatedEmbed(ctx, builder, DiscordUtils.divideFields(4, fields), languageContext.get("commands.salvage.ls.desc"));
            }
        }).createSubCommandAlias("ls", "list").createSubCommandAlias("ls", "is");
    }

    @Subscribe
    public void iteminfo(CommandRegistry registry) {
        registry.register("iteminfo", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.iteminfo.no_content", EmoteReference.ERROR);
                    return;
                }

                Optional<Item> itemOptional = ItemHelper.fromAnyNoId(content.replace("\"", ""));

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
