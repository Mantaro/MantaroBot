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
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.kodehawa.mantarobot.commands.currency.item.*;
import net.kodehawa.mantarobot.commands.currency.item.special.Broken;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Castable;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Salvageable;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.attributes.Attribute;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.attributes.Tiered;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Wrench;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
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
import net.kodehawa.mantarobot.db.entities.PlayerStats;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.campaign.Campaign;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.*;
import java.security.SecureRandom;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Module
public class ItemCmds {
    @Subscribe
    public void cast(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .spamTolerance(3)
                .limit(1)
                .cooldown(5, TimeUnit.SECONDS)
                .cooldownPenaltyIncrease(2, TimeUnit.SECONDS)
                .maxCooldown(2, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("cast")
                .build();

        TreeCommand castCommand = cr.register("cast", new TreeCommand(CommandCategory.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        if (content.trim().isEmpty()) {
                            ctx.sendLocalized("commands.cast.no_item_specified", EmoteReference.ERROR);
                            return;
                        }

                        //Argument parsing.
                        var optionalArguments = ctx.getOptionalArguments();
                        var isSeasonal = optionalArguments.containsKey("season") || optionalArguments.containsKey("s");
                        var amountSpecified = 1;

                        // Replace all arguments given on "-argument" sorta stuff.
                        content = Utils.replaceArguments(optionalArguments, content, "amount", "season", "s")
                                .replaceAll("\"", "") // This is because it needed quotes before. Not anymore.
                                .trim();

                        // This is cursed because I wanna keep compatibility with "-amount"
                        var multipleArg = optionalArguments.containsKey("amount");
                        if (multipleArg) {
                            try {
                                var amount = Integer.parseInt(optionalArguments.get("amount"));
                                amountSpecified = Math.max(1, amount);
                                content = content.replaceFirst(String.valueOf(amount), "").trim();
                            } catch (Exception ignored) { } // Well, heck.
                        } else {
                            var arguments = StringUtils.advancedSplitArgs(content, -1);
                            // It's a number
                            if (arguments[0].matches("^\\d{1,2}$")) {
                                try {
                                    amountSpecified = Math.max(1, Integer.parseInt(arguments[0]));
                                    content = content.replaceFirst(arguments[0], "").trim();
                                } catch (Exception ignored) { } // This shouldn't fail?
                            }
                        }

                        // Get the necessary entities.
                        var seasonalPlayer = ctx.getSeasonPlayer();
                        var player = ctx.getPlayer();
                        var playerData = player.getData();
                        var user = ctx.getDBUser();
                        var userData = user.getData();
                        var wrench = userData.getEquippedItems().of(PlayerEquipment.EquipmentType.WRENCH);
                        if (wrench == 0) {
                            ctx.sendLocalized("commands.cast.not_equipped", EmoteReference.ERROR);
                            return;
                        }

                        var wrenchItem = (Wrench) ItemHelper.fromId(wrench);
                        var toCast = ItemHelper.fromAnyNoId(content, ctx.getLanguageContext());
                        if (toCast.isEmpty()) {
                            ctx.sendLocalized("commands.cast.no_item_found", EmoteReference.ERROR);
                            return;
                        }

                        if (!RatelimitUtils.ratelimit(rateLimiter, ctx)) {
                            return;
                        }

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

                        if (amountSpecified > 1 && wrenchItem.getLevel() < 2) {
                            ctx.sendLocalized("commands.cast.low_tier", EmoteReference.ERROR, wrenchItem.getLevel());
                            return;
                        }

                        // Build recipe.
                        Map<Item, Integer> castMap = new HashMap<>();
                        var recipe = castItem.getRecipe();
                        var splitRecipe = recipe.split(";");

                        // How many parenthesis again?
                        var castCost = (long) (((castItem.getValue() / 2) * amountSpecified) * wrenchItem.getMultiplierReduction());

                        var money = isSeasonal ? seasonalPlayer.getMoney() : player.getCurrentMoney();
                        var isItemCastable = castItem instanceof Castable;
                        var wrenchLevelRequired = isItemCastable ? ((Castable) castItem).getCastLevelRequired() : 1;

                        if (money < castCost) {
                            ctx.sendLocalized("commands.cast.not_enough_money", EmoteReference.ERROR, castCost);
                            return;
                        }

                        if (wrenchItem.getLevel() < wrenchLevelRequired) {
                            ctx.sendLocalized("commands.cast.not_enough_wrench_level", EmoteReference.ERROR, wrenchItem.getLevel(), wrenchLevelRequired);
                            return;
                        }

                        var limit = (isItemCastable ? ((Castable) castItem).getMaximumCastAmount() : 5);
                        if (wrenchItem.getTier() >= 4)
                            limit *= 2;

                        if (amountSpecified > limit) {
                            ctx.sendLocalized("commands.cast.too_many_amount", EmoteReference.ERROR, limit, amountSpecified);
                            return;
                        }

                        var playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();
                        var dust = userData.getDustLevel();
                        if (dust > 95) {
                            ctx.sendLocalized("commands.cast.dust", EmoteReference.ERROR, dust);
                            return;
                        }

                        // build recipe
                        var increment = 0;
                        var recipeString = new StringBuilder();
                        for (int i : castItem.getRecipeTypes()) {
                            var item = ItemHelper.fromId(i);
                            var amount = Integer.parseInt(splitRecipe[increment]) * amountSpecified;

                            if (!playerInventory.containsItem(item)) {
                                ctx.sendLocalized("commands.cast.no_item", EmoteReference.ERROR, item.getName(), amount);
                                return;
                            }

                            int inventoryAmount = playerInventory.getAmount(item);
                            if (inventoryAmount < amount) {
                                ctx.sendLocalized("commands.cast.not_enough_items",
                                        EmoteReference.ERROR, item.getName(), castItem.getName(), amount, inventoryAmount
                                );
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
                        // end of recipe build

                        playerInventory.process(new ItemStack(castItem, amountSpecified));

                        if (castItem == ItemReference.HELLFIRE_PICK)
                            playerData.addBadgeIfAbsent(Badge.HOT_MINER);
                        if (castItem == ItemReference.HELLFIRE_ROD)
                            playerData.addBadgeIfAbsent(Badge.HOT_FISHER);
                        if (castItem == ItemReference.HELLFIRE_AXE)
                            playerData.addBadgeIfAbsent(Badge.HOT_CHOPPER);

                        var message = "";
                        if (playerData.shouldSeeCampaign()) {
                            message += Campaign.PREMIUM.getStringFromCampaign(ctx.getLanguageContext(), user.isPremium());
                            playerData.markCampaignAsSeen();
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

                        PlayerStats stats = ctx.getPlayerStats();
                        stats.incrementCraftedItems(amountSpecified);
                        stats.saveUpdating();

                        ItemHelper.handleItemDurability(wrenchItem, ctx, player, user, seasonalPlayer, "commands.cast.autoequip.success", isSeasonal);
                        ctx.sendFormat(ctx.getLanguageContext().get("commands.cast.success") + "\n" + message,
                                wrenchItem.getEmojiDisplay(), amountSpecified, "\u2009" + castItem.getEmoji(),
                                castItem.getName(), castCost, recipeString.toString().trim()
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
                                You need to equip a wrench if you want to use it. Wrenches have no broken type.""")
                        .setUsage("`~>cast [amount] <item>` - Casts the item you provide.")
                        .addParameter("item", "The item name or emoji.")
                        .addParameterOptional("amount", "The amount of items you want to cast. Depends on your wrench, maximum of 10.")
                        .build();
            }
        });
        cr.registerAlias("cast", "craft");

        castCommand.addSubCommand("list", new SubCommand() {
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
                    // Build recipe explanation
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
                    // End of build recipe explanation

                    var castLevel = (item instanceof Castable) ? ((Castable) item).getCastLevelRequired() : 1;
                    String fieldDescription = "%s\n**%s** %s %s\n**Recipe: ** %s\n**Wrench Tier: ** %s".formatted(
                            languageContext.get(item.getDesc()),
                            languageContext.get("commands.cast.ls.cost"),
                            item.getValue() / 2,
                            languageContext.get("commands.gamble.credits"),
                            recipe, castLevel
                    );

                    // Cursed, but Attribute implements Tiered so ;;
                    // But some stuff only implements Tiered.
                    if (item instanceof Tiered && !(item instanceof Attribute)) {
                        fieldDescription += "\n**Quality: ** %s".formatted(((Tiered) item).getTierStars());
                    }

                    if (item instanceof Attribute) {
                        fieldDescription += "\n**Durability: ** %,d\n**Quality: ** %s".formatted(
                                ((Attribute) item).getMaxDurability(),
                                ((Attribute) item).getTierStars()
                        );
                    }

                    fields.add(new MessageEmbed.Field("%s\u2009\u2009\u2009%s".formatted(item.getEmoji(), item.getName()),
                            fieldDescription, false
                    ));

                }

                DiscordUtils.sendPaginatedEmbed(ctx, builder, DiscordUtils.divideFields(3, fields), languageContext.get("commands.cast.ls.desc"));
            }
        });

        castCommand.createSubCommandAlias("list", "ls");
        castCommand.createSubCommandAlias("list", "1ist");
        castCommand.createSubCommandAlias("list", "Is");
        castCommand.createSubCommandAlias("list", "1s");
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
                        content = Utils.replaceArguments(optionalArguments, content, "season", "s")
                                .replaceAll("\"", "")
                                .trim();

                        //Get the necessary entities.
                        var seasonalPlayer = ctx.getSeasonPlayer();
                        var player = ctx.getPlayer();
                        var user = ctx.getDBUser();
                        var userData = user.getData();

                        var item = ItemHelper.fromAnyNoId(content, ctx.getLanguageContext()).orElse(null);
                        var playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();
                        var wrench = userData.getEquippedItems().of(PlayerEquipment.EquipmentType.WRENCH);
                        if (wrench == 0) {
                            ctx.sendLocalized("commands.cast.not_equipped", EmoteReference.ERROR);
                            return;
                        }

                        var wrenchItem = ItemHelper.fromId(wrench);

                        if (item == null) {
                            ctx.sendLocalized("commands.repair.no_item_found", EmoteReference.ERROR);
                            return;
                        }

                        if (!(item instanceof Broken)) {
                            ctx.sendLocalized("commands.repair.cant_repair", EmoteReference.ERROR, item.getName());
                            return;
                        }

                        if (!playerInventory.containsItem(item)) {
                            ctx.sendLocalized("commands.repair.no_main_item", EmoteReference.ERROR, item.getName());
                            return;
                        }

                        if (((Wrench) wrenchItem).getLevel() < 2) {
                            ctx.sendLocalized("commands.repair.not_enough_level", EmoteReference.ERROR);
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
                                        EmoteReference.ERROR, needed.getName(), brokenItem.getName(), amount, inventoryAmount
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
                        // end of recipe build

                        playerInventory.process(new ItemStack(brokenItem, -1));
                        playerInventory.process(new ItemStack(repairedItem, 1));

                        user.getData().increaseDustLevel(4);
                        user.save();

                        if (isSeasonal) {
                            seasonalPlayer.removeMoney(repairCost);
                            seasonalPlayer.save();
                        } else {
                            player.removeMoney(repairCost);
                            player.save();
                        }

                        var stats = ctx.getPlayerStats();
                        stats.incrementRepairedItems();
                        stats.saveUpdating();

                        ItemHelper.handleItemDurability(wrenchItem, ctx, player, user, seasonalPlayer, "commands.cast.autoequip.success", isSeasonal);

                        ctx.sendFormat(ctx.getLanguageContext().get("commands.repair.success"),
                                wrenchItem.getEmojiDisplay(), brokenItem.getEmoji(), brokenItem.getName(), repairedItem.getEmoji(),
                                repairedItem.getName(), repairCost, recipeString.toString().trim()
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
                        .addParameter("item", "The item name or emoji.")
                        .build();
            }
        });

        rp.addSubCommand("list", new SubCommand() {
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
                    var repairCost = mainItem.getValue() / 3;

                    fields.add(new MessageEmbed.Field("%s\u2009\u2009\u2009%s".formatted(item.getEmoji(), item.getName()),
                            "%s\n**%s** %s %s\n**Recipe: **%s\n**Item: ** %s %s".formatted(
                                    languageContext.get(item.getDesc()),
                                    languageContext.get("commands.repair.ls.cost"),
                                    repairCost, languageContext.get("commands.gamble.credits"),
                                    recipe,
                                    mainItem.getEmoji(), mainItem.getName()
                            ), false)
                    );
                }

                DiscordUtils.sendPaginatedEmbed(ctx, builder, DiscordUtils.divideFields(3, fields), languageContext.get("commands.repair.ls.desc"));
            }
        }).createSubCommandAlias("list", "ls")
                // why
                .createSubCommandAlias("list", "1ist")
                .createSubCommandAlias("list", "1s")
                .createSubCommandAlias("list", "Is");

        registry.registerAlias("repair", "fix");
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
                .prefix("salvage")
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

                        final var isSeasonal = ctx.isSeasonal();
                        //Get the necessary entities.
                        final var seasonalPlayer = ctx.getSeasonPlayer();
                        final var player = ctx.getPlayer();
                        final var user = ctx.getDBUser();
                        final var userData = user.getData();
                        final var playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();
                        content = content.replaceAll("\"", "").trim();

                        final var item = ItemHelper.fromAnyNoId(content, ctx.getLanguageContext()).orElse(null);
                        final var wrench = userData.getEquippedItems().of(PlayerEquipment.EquipmentType.WRENCH);
                        if (wrench == 0) {
                            ctx.sendLocalized("commands.cast.not_equipped", EmoteReference.ERROR);
                            return;
                        }

                        final var wrenchItem = ItemHelper.fromId(wrench);
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

                        if (!playerInventory.containsItem(item)) {
                            ctx.sendLocalized("commands.salvage.no_main_item", EmoteReference.ERROR);
                            return;
                        }

                        int dust = user.getData().getDustLevel();
                        if (dust > 95) {
                            ctx.sendLocalized("commands.salvage.dust", EmoteReference.ERROR, dust);
                            return;
                        }
                        final var salvageable = (Salvageable) original;
                        var returns = salvageable.getReturns().stream().map(ItemHelper::fromId).collect(Collectors.toList());
                        if (returns.isEmpty()) {
                            ctx.sendLocalized("commands.salvage.no_returnables", EmoteReference.SAD);
                            return;
                        }

                        var salvageCost = item.getValue() / 3;
                        var playerMoney = isSeasonal ? seasonalPlayer.getMoney() : player.getCurrentMoney();
                        if (playerMoney < salvageCost) {
                            ctx.sendLocalized("commands.salvage.not_enough_money", EmoteReference.ERROR, playerMoney, salvageCost);
                            return;
                        }

                        if (!RatelimitUtils.ratelimit(ratelimiter, ctx)) {
                            return;
                        }

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

                        var stats = ctx.getPlayerStats();
                        stats.incrementSalvagedItems();
                        stats.saveUpdating();

                        ItemHelper.handleItemDurability(wrenchItem, ctx, player, user, seasonalPlayer, "commands.cast.autoequip.success", isSeasonal);
                        ctx.sendLocalized("commands.salvage.success", wrenchItem.getEmojiDisplay(), item.getName(), toReturn.getName(), salvageCost);
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Salvages an item. Useful when you can't repair it but wanna get something back.
                                The cost is 1/3rd of the item price."""
                        )
                        .setUsage("`~>salvage <item>` - Salvages an item.")
                        .addParameter("item", "The item name or emoji.")
                        .build();
            }
        });

        sv.addSubCommand("list", new SubCommand() {
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

                    fields.add(new MessageEmbed.Field(item.getEmoji() + "\u2009\u2009\u2009" + item.getName(),
                            languageContext.get(item.getDesc()) + "\n**" +
                                    languageContext.get("commands.salvage.ls.cost") + "**" +
                                    salvageCost + " " + languageContext.get("commands.gamble.credits") +
                                    ".\n**" + languageContext.get("commands.salvage.ls.return") + "** " + recipeString,
                            false)
                    );
                }

                DiscordUtils.sendPaginatedEmbed(ctx, builder, DiscordUtils.divideFields(3, fields), languageContext.get("commands.salvage.ls.desc"));
            }

        }).createSubCommandAlias("list", "ls")
                .createSubCommandAlias("list", "1ist") // why
                .createSubCommandAlias("list", "1s") // why
                .createSubCommandAlias("list", "Is"); // why
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
                var translatedName = name.isEmpty() ? item.getName() : ctx.getLanguageContext().get(name);
                var type = ctx.getLanguageContext().get(item.getItemType().getDescription());

                if (item instanceof Attribute) {
                    var builder = new EmbedBuilder();
                    var attribute = ((Attribute) item);
                    var languageContext = ctx.getLanguageContext();
                    builder.setAuthor(languageContext.get("commands.iteminfo.embed.header").formatted(translatedName),
                            null, ctx.getAuthor().getEffectiveAvatarUrl())
                            .setColor(ctx.getMember().getColor() == null ? Color.PINK : ctx.getMember().getColor())
                            .addField(EmoteReference.DIAMOND.toHeaderString() + languageContext.get("commands.iteminfo.embed.type"),
                                    item.getEmoji() + " " + type, true
                            )
                            .addField(EmoteReference.EYES.toHeaderString() + languageContext.get("commands.iteminfo.embed.usefulness"),
                                    attribute.getType().toString(), true
                            )
                            .addField(EmoteReference.GLOWING_STAR.toHeaderString() + languageContext.get("commands.iteminfo.embed.tier"),
                                    attribute.getTierStars(), false
                            )
                            .addField(EmoteReference.ROCK.toHeaderString() + languageContext.get("commands.iteminfo.embed.durability"),
                                    String.format(
                                            Utils.getLocaleFromLanguage(languageContext),
                                            "%,d", attribute.getMaxDurability()),
                                    false
                            )
                            .addField(EmoteReference.CALENDAR.toHeaderString() + languageContext.get("commands.iteminfo.embed.attributes"),
                                    attribute.buildAttributes(), false
                            )
                            .addField(EmoteReference.TALKING.toHeaderString() + languageContext.get("commands.iteminfo.embed.desc"),
                                    languageContext.get(attribute.getExplanation()), false
                            );

                    if (attribute.getTier() == 1) {
                        builder.addField(EmoteReference.THINKING.toHeaderString() + languageContext.get("commands.iteminfo.embed.upgrade"),
                                languageContext.get("commands.iteminfo.embed.upgrade_content"), false
                        );
                    }

                    ctx.send(builder.build());
                } else {
                    var buyable = item.isBuyable();
                    var sellable = item.isSellable();
                    // Blame me for bad organization...
                    var credits = ctx.getLanguageContext().get("commands.slots.credits");
                    var none = ctx.getLanguageContext().get("general.none");

                    if (item instanceof Tiered) {
                        ctx.sendLocalized("commands.iteminfo.success_tiered", EmoteReference.BLUE_SMALL_MARKER,
                                item.getEmoji(), item.getName(), translatedName, type, description, ((Tiered) item).getTierStars(),
                                // This is pain
                                buyable ? item.getValue() : none, buyable ? credits : "",
                                sellable ? Math.round(item.getValue() * 0.9) : none, sellable ? credits : ""
                        );
                    } else {
                        ctx.sendLocalized("commands.iteminfo.success", EmoteReference.BLUE_SMALL_MARKER,
                                item.getEmoji(), item.getName(), translatedName, type, description,
                                // This is pain
                                buyable ? item.getValue() : none, buyable ? credits : "",
                                sellable ? Math.round(item.getValue() * 0.9) : none, sellable ? credits : ""
                        );
                    }
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shows the information of an item")
                        .setUsage("`~>iteminfo <item name>` - Shows the info of an item.")
                        .addParameter("item", "The item name or emoji.")
                        .build();
            }
        });
    }
}
