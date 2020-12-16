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
import net.kodehawa.mantarobot.core.CommandRegistry;
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
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.campaign.Campaign;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.Color;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        if (content.trim().isEmpty()) {
                            ctx.sendLocalized("commands.cast.no_item_found", EmoteReference.ERROR);
                            return;
                        }

                        //Argument parsing.
                        var optionalArguments = ctx.getOptionalArguments();
                        content = Utils.replaceArguments(optionalArguments, content, "season", "s").trim();

                        var arguments = StringUtils.advancedSplitArgs(content, -1);

                        var isSeasonal = optionalArguments.containsKey("season") || optionalArguments.containsKey("s");
                        var isMultiple = optionalArguments.containsKey("amount");

                        //Get the necessary entities.
                        var seasonalPlayer = ctx.getSeasonPlayer();
                        var player = ctx.getPlayer();
                        var playerData = player.getData();
                        var user = ctx.getDBUser();
                        var userData = user.getData();

                        //Why
                        var toCast = ItemHelper.fromAnyNoId(arguments[0], ctx.getLanguageContext());
                        Optional<Item> optionalWrench = Optional.empty();

                        if (arguments.length > 1)
                            optionalWrench = ItemHelper.fromAnyNoId(arguments[1], ctx.getLanguageContext());

                        if (toCast.isEmpty()) {
                            ctx.sendLocalized("commands.cast.no_item_found", EmoteReference.ERROR);
                            return;
                        }

                        if (!RatelimitUtils.ratelimit(ratelimiter, ctx)) {
                            return;
                        }

                        var amountSpecified = 1;
                        try {
                            if (isMultiple) {
                                amountSpecified = Math.max(1, Integer.parseInt(optionalArguments.get("amount")));
                            } else if (arguments.length > 2) {
                                amountSpecified = Math.max(1, Integer.parseInt(arguments[2]));
                            } else if (optionalWrench.isEmpty() && arguments.length > 1) {
                                amountSpecified = Math.max(1, Integer.parseInt(arguments[1]));
                            }
                        } catch (Exception ignored) { }

                        var castItem = toCast.get();
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
                        var wrench = (Wrench) wrenchItem;

                        if (amountSpecified > 1 && wrench.getLevel() < 2) {
                            ctx.sendLocalized("commands.cast.low_tier", EmoteReference.ERROR, wrench.getLevel());
                            return;
                        }

                        //Build recipe.
                        Map<Item, Integer> castMap = new HashMap<>();
                        var recipe = castItem.getRecipe();
                        var splitRecipe = recipe.split(";");

                        //How many parenthesis again?
                        var castCost = (long) (((castItem.getValue() / 2) * amountSpecified) * wrench.getMultiplierReduction());

                        var money = isSeasonal ? seasonalPlayer.getMoney() : player.getCurrentMoney();
                        var isItemCastable = castItem instanceof Castable;
                        var wrenchLevelRequired = isItemCastable ? ((Castable) castItem).getCastLevelRequired() : 1;

                        if (money < castCost) {
                            ctx.sendLocalized("commands.cast.not_enough_money", EmoteReference.ERROR, castCost);
                            return;
                        }

                        if (wrench.getLevel() < wrenchLevelRequired) {
                            ctx.sendLocalized("commands.cast.not_enough_wrench_level", EmoteReference.ERROR, wrench.getLevel(), wrenchLevelRequired);
                            return;
                        }

                        var limit = (isItemCastable ? ((Castable) castItem).getMaximumCastAmount() : 5);

                        // Limit is double with sparkle wrench
                        if (wrench == ItemReference.WRENCH_SPARKLE)
                            limit *= 2;

                        if (amountSpecified > limit) {
                            ctx.sendLocalized("commands.cast.too_many_amount", EmoteReference.ERROR, limit, amountSpecified);
                            return;
                        }

                        var playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();

                        if (!playerInventory.containsItem(wrenchItem)) {
                            ctx.sendLocalized("commands.cast.no_tool", EmoteReference.ERROR, ItemReference.WRENCH.getName());
                            return;
                        }

                        var dust = userData.getDustLevel();
                        if (dust > 95) {
                            ctx.sendLocalized("commands.cast.dust", EmoteReference.ERROR, dust);
                            return;
                        }

                        var increment = 0;

                        //build recipe
                        var recipeString = new StringBuilder();
                        for (int i : castItem.getRecipeTypes()) {
                            var item = ItemHelper.fromId(i);
                            var amount = Integer.parseInt(splitRecipe[increment]) * amountSpecified;

                            if (!playerInventory.containsItem(item)) {
                                ctx.sendLocalized("commands.cast.no_item", EmoteReference.ERROR, item.getName());
                                return;
                            }

                            int inventoryAmount = playerInventory.getAmount(item);
                            //Subtract 1 from the usable amount since if your wrench breaks in the process, it causes issues.
                            int usableInventoryAmount = (i == ItemHelper.idOf(wrench)) ? inventoryAmount - 1 : inventoryAmount;
                            if (usableInventoryAmount < amount) {
                                ctx.sendLocalized("commands.cast.not_enough_items",
                                        EmoteReference.ERROR, item.getName(), amount, usableInventoryAmount
                                );

                                if (usableInventoryAmount < inventoryAmount)
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

                        for (var entry : castMap.entrySet()) {
                            var i = entry.getKey();
                            var amount = entry.getValue();
                            playerInventory.process(new ItemStack(i, -amount));
                        }
                        //end of recipe build

                        playerInventory.process(new ItemStack(castItem, amountSpecified));

                        var message = "";

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
                        .setDescription("""
                                Allows you to cast any castable item given you have the necessary elements.
                                Casting requires you to have the necessary materials to cast the item, and it has a cost of `item value / 2`.
                                Cast-able items are only able to be acquired by this command. They're non-buyable items, though you can sell them for a profit.
                                If you specify the item and the wrench, you can use amount without -amount. Example: `~>cast "diamond pickaxe" "sparkle wrench" 10`""")
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
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var castableItems = Arrays.stream(ItemReference.ALL)
                        .sorted(Comparator.comparingInt(i -> i.getItemType().ordinal()))
                        .filter(i -> i.getItemType().isCastable() && i.getRecipeTypes() != null && i.getRecipe() != null)
                        .collect(Collectors.toList());

                List<MessageEmbed.Field> fields = new LinkedList<>();

                EmbedBuilder builder = new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.cast.ls.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setColor(Color.PINK)
                        .setFooter(languageContext.get("general.requested_by").formatted(ctx.getMember().getEffectiveName()), null);

                for (var item : castableItems) {
                    //Build recipe explanation
                    if (item.getRecipe().isEmpty()) {
                        continue;
                    }

                    var recipeAmount = item.getRecipe().split(";");
                    var ai = new AtomicInteger();

                    var recipe = Arrays.stream(item.getRecipeTypes()).mapToObj((i) -> {
                        var recipeItem = ItemHelper.fromId(i);
                        return "%s %sx\u2009*%s*".formatted(
                                recipeItem.getEmoji(),
                                recipeAmount[ai.getAndIncrement()],
                                recipeItem.getName()
                        );
                    }).collect(Collectors.joining(", "));
                    //End of build recipe explanation

                    var castLevel = (item instanceof Castable) ? ((Castable) item).getCastLevelRequired() : 1;
                    fields.add(new MessageEmbed.Field(
                            "%s %s".formatted(item.getEmoji(), item.getName()),
                            "%s\n**%s** %s %s.\n**Recipe: ** %s\n**Wrench Tier: ** %s".formatted(
                                    languageContext.get(item.getDesc()),
                                    languageContext.get("commands.cast.ls.cost"),
                                    item.getValue() / 2,
                                    languageContext.get("commands.gamble.credits"),
                                    recipe, castLevel
                            ), false)
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
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        if (content.isEmpty()) {
                            ctx.sendLocalized("commands.repair.no_item", EmoteReference.ERROR);
                            return;
                        }

                        //Argument parsing.
                        var optionalArguments = ctx.getOptionalArguments();
                        var isSeasonal = optionalArguments.containsKey("season") || optionalArguments.containsKey("s");

                        var args = ctx.getArguments();

                        //Get the necessary entities.
                        var seasonalPlayer = ctx.getSeasonPlayer();
                        var player = ctx.getPlayer();
                        var user = ctx.getDBUser();

                        var itemString = args[0];
                        var item = ItemHelper.fromAnyNoId(itemString, ctx.getLanguageContext()).orElse(null);
                        var playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();
                        var wrench = playerInventory.containsItem(ItemReference.WRENCH_SPARKLE) ?
                                ItemReference.WRENCH_SPARKLE : ItemReference.WRENCH_COMET;

                        if (args.length > 1) {
                            wrench = ItemHelper.fromAnyNoId(args[1], ctx.getLanguageContext()).orElse(null);
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

                        var dust = user.getData().getDustLevel();
                        if (dust > 95) {
                            ctx.sendLocalized("commands.repair.dust", EmoteReference.ERROR, dust);
                            return;
                        }

                        if (!RatelimitUtils.ratelimit(ratelimiter, ctx)) {
                            return;
                        }

                        var brokenItem = (Broken) item;
                        var repairedItem = ItemHelper.fromId(brokenItem.getMainItem());
                        var repairCost = repairedItem.getValue() / 3;

                        var playerMoney = isSeasonal ? seasonalPlayer.getMoney() : player.getCurrentMoney();
                        if (playerMoney < repairCost) {
                            ctx.sendLocalized("commands.repair.not_enough_money", EmoteReference.ERROR, playerMoney, repairCost);
                            return;
                        }

                        Map<Item, Integer> recipeMap = new HashMap<>();
                        var repairRecipe = brokenItem.getRecipe();
                        var splitRecipe = repairRecipe.split(";");
                        var recipeString = new StringBuilder();

                        for (var recipe : splitRecipe) {
                            var split = recipe.split(",");
                            var amount = Integer.parseInt(split[0]);
                            var needed = ItemHelper.fromId(Integer.parseInt(split[1]));

                            if (!playerInventory.containsItem(needed)) {
                                ctx.sendLocalized("commands.repair.no_item_recipe", EmoteReference.ERROR, needed.getName());
                                return;
                            }

                            var inventoryAmount = playerInventory.getAmount(needed);
                            if (inventoryAmount < amount) {
                                ctx.sendLocalized("commands.repair.not_enough_items",
                                        EmoteReference.ERROR, needed.getName(), amount, inventoryAmount
                                );
                                return;
                            }

                            recipeMap.put(needed, amount);
                            recipeString.append(amount).append("x ").append(needed.getName()).append(" ");
                        }

                        for (var entry : recipeMap.entrySet()) {
                            var i = entry.getKey();
                            var amount = entry.getValue();
                            playerInventory.process(new ItemStack(i, -amount));
                        }
                        //end of recipe build

                        playerInventory.process(new ItemStack(brokenItem, -1));
                        playerInventory.process(new ItemStack(repairedItem, 1));

                        var message = "";
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
                        .setDescription("""
                                Allows you to repair any broken item given you have the necessary elements.
                                Repairing requires you to have the necessary materials to cast the item, and it has a cost of `item value / 3`.
                                """)
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
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var repairableItems = Arrays.stream(ItemReference.ALL)
                        .sorted(Comparator.comparingInt(i -> i.getItemType().ordinal()))
                        .filter(Broken.class::isInstance)
                        .map(Broken.class::cast)
                        .collect(Collectors.toList());

                List<MessageEmbed.Field> fields = new LinkedList<>();
                var builder = new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.repair.ls.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setColor(Color.PINK)
                        .setFooter(languageContext.get("general.requested_by").formatted(ctx.getMember().getEffectiveName(), null));

                for (var item : repairableItems) {
                    if (item.getRecipe().isEmpty()) {
                        continue;
                    }

                    var repairRecipe = item.getRecipe();
                    var splitRecipe = repairRecipe.split(";");
                    var recipeString = new StringBuilder();
                    var mainItem = ItemHelper.fromId(item.getMainItem());
                    for (var recipe : splitRecipe) {
                        var split = recipe.split(",");
                        var amount = Integer.parseInt(split[0]);
                        var needed = ItemHelper.fromId(Integer.parseInt(split[1]));

                        recipeString.append(needed.getEmoji())
                                .append(" ")
                                .append(amount).append("x ")
                                .append(" *")
                                .append(needed.getName())
                                .append("*|");
                    }

                    var recipe = String.join(", ", recipeString.toString().split("\\|"));
                    var repairCost = item.getValue() / 3;

                    fields.add(new MessageEmbed.Field("%s %s".formatted(item.getEmoji(), item.getName()),
                            "%s\n**%s** %s %s\n**Recipe: **%s\n**Item: ** %s %s".formatted(
                                    languageContext.get(item.getDesc()),
                                    languageContext.get("commands.repair.ls.cost"),
                                    repairCost, languageContext.get("commands.gamble.credits"),
                                    recipe,
                                    mainItem.getEmoji(), mainItem.getName()
                            ), false)
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
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        if (content.isEmpty()) {
                            ctx.sendLocalized("commands.salvage.no_item", EmoteReference.ERROR);
                            return;
                        }

                        //Argument parsing.
                        final var isSeasonal = ctx.isSeasonal();
                        //Get the necessary entities.
                        final var seasonalPlayer = ctx.getSeasonPlayer();
                        final var player = ctx.getPlayer();
                        final var user = ctx.getDBUser();

                        final var args = ctx.getArguments();
                        final var itemString = args[0];
                        final var item = ItemHelper.fromAnyNoId(itemString, ctx.getLanguageContext()).orElse(null);
                        final var playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();
                        var wrench = playerInventory.containsItem(ItemReference.WRENCH_SPARKLE) ?
                                ItemReference.WRENCH_SPARKLE : ItemReference.WRENCH_COMET;
                        var custom = false;
                        if (args.length > 1) {
                            wrench = ItemHelper.fromAnyNoId(args[1], ctx.getLanguageContext()).orElse(null);
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

                        if (!RatelimitUtils.ratelimit(ratelimiter, ctx)) {
                            return;
                        }

                        final var salvageable = (Salvageable) original;
                        var returns = salvageable.getReturns().stream().map(ItemHelper::fromId).collect(Collectors.toList());

                        if (returns.isEmpty()) {
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
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var broken = Arrays.stream(ItemReference.ALL)
                        .sorted(Comparator.comparingInt(i -> i.getItemType().ordinal()))
                        .filter(Broken.class::isInstance)
                        .map(Broken.class::cast)
                        .collect(Collectors.toList());

                List<MessageEmbed.Field> fields = new LinkedList<>();

                var builder = new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.salvage.ls.header"),
                                null, ctx.getAuthor().getEffectiveAvatarUrl()
                        )
                        .setColor(Color.PINK)
                        .setFooter(languageContext.get("general.requested_by").formatted(ctx.getMember().getEffectiveName()),
                                null
                        );

                for (var item : broken) {
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
                                    ".\n**" + languageContext.get("commands.salvage.ls.return") + "** " + recipeString,
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

                var itemOptional = ItemHelper.fromAnyNoId(content.replace("\"", ""), ctx.getLanguageContext());
                if (itemOptional.isEmpty()) {
                    ctx.sendLocalized("commands.iteminfo.no_item", EmoteReference.ERROR);
                    return;
                }

                var item = itemOptional.get();
                var description = ctx.getLanguageContext().get(item.getDesc());
                var name = item.getTranslatedName();
                var translatedName = item.getTranslatedName().isEmpty() ?
                        item.getName() : ctx.getLanguageContext().get(item.getTranslatedName());

                ctx.sendLocalized("commands.iteminfo.success", EmoteReference.BLUE_SMALL_MARKER,
                        item.getEmoji(), item.getName(),
                        translatedName, item.getItemType(), description
                );
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shows the information of an item")
                        .setUsage("`~>iteminfo <item name>` - Shows the info of an item.")
                        .addParameter("item",
                                "The item name or emoji. If the name contains spaces \"wrap it in quotes\"")
                        .build();
            }
        });
    }
}
