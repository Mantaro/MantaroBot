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
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.PlayerEquipment;
import net.kodehawa.mantarobot.commands.currency.item.special.Broken;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Castable;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Salvageable;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.attributes.Attribute;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.attributes.Tiered;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Wrench;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Defer;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.PlayerStats;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.campaign.Campaign;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.Color;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Module
public class ItemCmds {
    private static final IncreasingRateLimiter castRateLimiter = new IncreasingRateLimiter.Builder()
            .spamTolerance(3)
            .limit(1)
            .cooldown(5, TimeUnit.SECONDS)
            .cooldownPenaltyIncrease(2, TimeUnit.SECONDS)
            .maxCooldown(2, TimeUnit.MINUTES)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("cast")
            .build();

    private static final IncreasingRateLimiter repairRateLimiter = new IncreasingRateLimiter.Builder()
            .spamTolerance(3)
            .limit(1)
            .cooldown(5, TimeUnit.SECONDS)
            .cooldownPenaltyIncrease(2, TimeUnit.SECONDS)
            .maxCooldown(2, TimeUnit.MINUTES)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("repair")
            .build();

    private static final SecureRandom random = new SecureRandom();
    private static final IncreasingRateLimiter salvageRateLimiter = new IncreasingRateLimiter.Builder()
            .spamTolerance(3)
            .limit(1)
            .cooldown(5, TimeUnit.SECONDS)
            .cooldownPenaltyIncrease(2, TimeUnit.SECONDS)
            .maxCooldown(2, TimeUnit.MINUTES)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("salvage")
            .build();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Cast.class);
        cr.registerSlash(Repair.class);
        cr.registerSlash(Salvage.class);
        cr.registerSlash(ItemInfo.class);
    }

    @Description("Hub for cast-related commands.")
    @Category(CommandCategory.CURRENCY)
    @Help(
            description = """
                            Hub for cast-related commands.
                            You can use `/help cast` item to get help on how to cast an item.
                            """
    )
    public static class Cast extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Name("item")
        @Defer
        @Description("Cast an item.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "item", description = "The item to cast.", required = true),
                @Options.Option(type = OptionType.INTEGER, name = "amount", description = "The amount to cast.", maxValue = 10)
        })
        @Help(
                description = """
                            Allows you to cast any castable item given you have the necessary elements.
                            Casting requires you to have the necessary materials to cast the item, and it has a cost of `item value / 2`.
                            Cast-able items are only able to be acquired by this command. They're non-buyable items, though you can sell them for a profit.
                            You need to equip a wrench if you want to use it. Wrenches have no broken type.""",
                usage = "`/cast item item:<item name> amount:[amount]`",
                parameters = {
                        @Help.Parameter(name = "item", description = "The item to cast."),
                        @Help.Parameter(name = "amount", description = "The amount of the item to cast, 1 by default. Depends on your wrench, maximum is 10.", optional = true)
                }
        )
        public static class CastItem extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var itemName = ctx.getOptionAsString("item");
                var amountSpecified = ctx.getOptionAsInteger("amount", 1);

                // Get the necessary entities.
                var player = ctx.getPlayer();
                var user = ctx.getDBUser();
                var wrench = user.getEquippedItems().of(PlayerEquipment.EquipmentType.WRENCH);
                if (wrench == 0) {
                    ctx.reply("commands.cast.not_equipped", EmoteReference.ERROR);
                    return;
                }

                var wrenchItem = (Wrench) ItemHelper.fromId(wrench);
                var toCast = ItemHelper.fromAnyNoId(itemName, ctx.getLanguageContext());
                if (toCast.isEmpty()) {
                    ctx.reply("commands.cast.no_item_found", EmoteReference.ERROR);
                    return;
                }

                if (!RatelimitUtils.ratelimit(castRateLimiter, ctx)) {
                    return;
                }

                var castItem = toCast.get();
                // This is a good way of getting if it's castable,
                // since implementing an interface wouldn't cut it (some rods aren't castable, for example)
                if (!castItem.getItemType().isCastable()) {
                    ctx.reply("commands.cast.item_not_cast", EmoteReference.ERROR);
                    return;
                }

                if (castItem.getRecipe() == null || castItem.getRecipe().isEmpty()) {
                    ctx.reply("commands.cast.item_not_cast", EmoteReference.ERROR);
                    return;
                }

                if (amountSpecified > 1 && wrenchItem.getLevel() < 2) {
                    ctx.reply("commands.cast.low_tier", EmoteReference.ERROR, wrenchItem.getLevel());
                    return;
                }

                // Build recipe.
                Map<Item, Integer> castMap = new HashMap<>();
                var recipe = castItem.getRecipe();
                var splitRecipe = recipe.split(";");

                // How many parenthesis again?
                var castCost = (long) (((castItem.getValue() / 2) * amountSpecified) * wrenchItem.getMultiplierReduction());

                var money = player.getCurrentMoney();
                var isItemCastable = castItem instanceof Castable;
                var wrenchLevelRequired = isItemCastable ? ((Castable) castItem).getCastLevelRequired() : 1;

                if (money < castCost) {
                    ctx.reply("commands.cast.not_enough_money", EmoteReference.ERROR, castCost);
                    return;
                }

                if (wrenchItem.getLevel() < wrenchLevelRequired) {
                    ctx.reply("commands.cast.not_enough_wrench_level", EmoteReference.ERROR, wrenchItem.getLevel(), wrenchLevelRequired);
                    return;
                }

                var limit = (isItemCastable ? ((Castable) castItem).getMaximumCastAmount() : 5);
                if (wrenchItem.getTier() >= 4)
                    limit *= 2;

                if (amountSpecified > limit) {
                    ctx.reply("commands.cast.too_many_amount", EmoteReference.ERROR, limit, amountSpecified);
                    return;
                }

                var playerInventory = player.inventory();
                var dust = user.getDustLevel();
                if (dust > 95) {
                    ctx.reply("commands.cast.dust", EmoteReference.ERROR, dust);
                    return;
                }

                // build recipe
                var increment = 0;
                var recipeString = new StringBuilder();
                for (int i : castItem.getRecipeTypes()) {
                    var item = ItemHelper.fromId(i);
                    var amount = Integer.parseInt(splitRecipe[increment]) * amountSpecified;

                    if (!playerInventory.containsItem(item)) {
                        ctx.reply("commands.cast.no_item", EmoteReference.ERROR, item.getName(), amount);
                        return;
                    }

                    int inventoryAmount = playerInventory.getAmount(item);
                    if (inventoryAmount < amount) {
                        ctx.reply("commands.cast.not_enough_items",
                                EmoteReference.ERROR, item.getName(), castItem.getName(), amount, inventoryAmount
                        );
                        return;
                    }

                    castMap.put(item, amount);
                    recipeString.append(amount).append("x \u2009").append(item.getEmojiDisplay()).append(item.getName()).append(" ");
                    increment++;
                }

                if (playerInventory.getAmount(castItem) + amountSpecified > 5000) {
                    ctx.reply("commands.cast.too_many", EmoteReference.ERROR);
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
                    player.addBadgeIfAbsent(Badge.HOT_MINER);
                if (castItem == ItemReference.HELLFIRE_ROD)
                    player.addBadgeIfAbsent(Badge.HOT_FISHER);
                if (castItem == ItemReference.HELLFIRE_AXE)
                    player.addBadgeIfAbsent(Badge.HOT_CHOPPER);

                var message = "";
                if (player.shouldSeeCampaign()) {
                    message += Campaign.PREMIUM.getStringFromCampaign(ctx.getLanguageContext(), user.isPremium());
                    player.markCampaignAsSeen();
                }

                user.increaseDustLevel(3);
                user.updateAllChanged();

                player.removeMoney(castCost);
                player.save();

                PlayerStats stats = ctx.getPlayerStats();
                stats.incrementCraftedItems(amountSpecified);
                stats.updateAllChanged();

                ItemHelper.handleItemDurability(wrenchItem, ctx, player, user, "commands.cast.autoequip.success");
                ctx.replyRaw(ctx.getLanguageContext().get("commands.cast.success") + "\n" + message,
                        wrenchItem.getEmojiDisplay(), amountSpecified, "\u2009" + castItem.getEmoji(),
                        castItem.getName(), castCost, recipeString.toString().trim()
                );
            }
        }

        @Name("list")
        @Description("Shows a list of castable items.")
        public static class CastList extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var castableItems = Arrays.stream(ItemReference.ALL)
                        .sorted(Comparator.comparingInt(i -> i.getItemType().ordinal()))
                        .filter(i -> i.getItemType().isCastable() && i.getRecipeTypes() != null && i.getRecipe() != null)
                        .toList();

                List<MessageEmbed.Field> fields = new LinkedList<>();
                var lang = ctx.getLanguageContext();
                var builder = new EmbedBuilder()
                        .setAuthor(lang.get("commands.cast.ls.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setColor(Color.PINK)
                        .setFooter(lang.get("general.requested_by").formatted(ctx.getMember().getEffectiveName()), null);

                for (var item : castableItems) {
                    // Build recipe explanation
                    if (item.getRecipe().isEmpty()) {
                        continue;
                    }

                    var recipeAmount = item.getRecipe().split(";");
                    var ai = new AtomicInteger();

                    var recipe = Arrays.stream(item.getRecipeTypes()).mapToObj((i) -> {
                        var recipeItem = ItemHelper.fromId(i);
                        return "%sx \u2009%s\u2009 *%s*".formatted(
                                recipeAmount[ai.getAndIncrement()],
                                recipeItem.getEmoji(),
                                recipeItem.getName()
                        );
                    }).collect(Collectors.joining(",\u2009 "));
                    // End of build recipe explanation

                    var castLevel = (item instanceof Castable) ? ((Castable) item).getCastLevelRequired() : 1;
                    String fieldDescription = "%s\n**%s** %s %s\n**Recipe: ** %s\n**Wrench Tier: ** %s".formatted(
                            lang.get(item.getDesc()),
                            lang.get("commands.cast.ls.cost"),
                            item.getValue() / 2,
                            lang.get("commands.gamble.credits"),
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

                DiscordUtils.sendPaginatedEmbed(ctx.getUtilsContext(), builder, DiscordUtils.divideFields(3, fields), lang.get("commands.cast.ls.desc"));
            }
        }
    }

    @Description("The hub for item repair commands.")
    @Category(CommandCategory.CURRENCY)
    public static class Repair extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Name("item")
        @Defer
        @Description("Repair an item.")
        @Help(description = """
                            Allows you to repair any broken item given you have the necessary elements.
                            Repairing requires you to have the necessary materials to cast the item, and it has a cost of `item value / 3`.
                            """,
                usage = "`/repair item:<item name of the broken version>`",
                parameters = @Help.Parameter(name = "item", description = "The item to repair. You can check a list of repairable items using `/repair list`")
        )
        @Options({
                @Options.Option(type = OptionType.STRING, name = "item", description = "The item to repair.", required = true),
        })
        public static class RepairItem extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var itemName = ctx.getOptionAsString("item");

                //Get the necessary entities.
                var player = ctx.getPlayer();
                var user = ctx.getDBUser();

                var item = ItemHelper.fromAnyNoId(itemName, ctx.getLanguageContext()).orElse(null);
                var playerInventory = player.inventory();
                var wrench = user.getEquippedItems().of(PlayerEquipment.EquipmentType.WRENCH);
                if (wrench == 0) {
                    ctx.reply("commands.cast.not_equipped", EmoteReference.ERROR);
                    return;
                }

                var wrenchItem = ItemHelper.fromId(wrench);
                if (item == null) {
                    ctx.reply("commands.repair.no_item_found", EmoteReference.ERROR);
                    return;
                }

                if (!(item instanceof Broken brokenItem)) {
                    ctx.reply("commands.repair.cant_repair", EmoteReference.ERROR, item.getName());
                    return;
                }

                if (!playerInventory.containsItem(item)) {
                    ctx.reply("commands.repair.no_main_item", EmoteReference.ERROR, item.getName());
                    return;
                }

                if (((Wrench) wrenchItem).getLevel() < 2) {
                    ctx.reply("commands.repair.not_enough_level", EmoteReference.ERROR);
                    return;
                }

                var dust = user.getDustLevel();
                if (dust > 95) {
                    ctx.reply("commands.repair.dust", EmoteReference.ERROR, dust);
                    return;
                }

                if (!RatelimitUtils.ratelimit(repairRateLimiter, ctx)) {
                    return;
                }

                var repairedItem = ItemHelper.fromId(brokenItem.getMainItem());
                var repairCost = repairedItem.getValue() / 3;

                var playerMoney = player.getCurrentMoney();
                if (playerMoney < repairCost) {
                    ctx.reply("commands.repair.not_enough_money", EmoteReference.ERROR, playerMoney, repairCost);
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
                        ctx.reply("commands.repair.no_item_recipe", EmoteReference.ERROR, needed.getName());
                        return;
                    }

                    var inventoryAmount = playerInventory.getAmount(needed);
                    if (inventoryAmount < amount) {
                        ctx.reply("commands.repair.not_enough_items",
                                EmoteReference.ERROR, needed.getName(), brokenItem.getName(), amount, inventoryAmount
                        );
                        return;
                    }

                    recipeMap.put(needed, amount);
                    recipeString.append(amount).append("x ").append(needed.getEmojiDisplay()).append(needed.getName()).append(" ");
                }

                for (var entry : recipeMap.entrySet()) {
                    var i = entry.getKey();
                    var amount = entry.getValue();
                    playerInventory.process(new ItemStack(i, -amount));
                }
                // end of recipe build

                playerInventory.process(new ItemStack(brokenItem, -1));
                playerInventory.process(new ItemStack(repairedItem, 1));

                user.increaseDustLevel(4);
                user.updateAllChanged();

                player.removeMoney(repairCost);
                player.save();

                var stats = ctx.getPlayerStats();
                stats.incrementRepairedItems();
                stats.updateAllChanged();

                ItemHelper.handleItemDurability(wrenchItem, ctx, player, user, "commands.cast.autoequip.success");
                ctx.replyRaw(ctx.getLanguageContext().get("commands.repair.success"),
                        wrenchItem.getEmojiDisplay(), brokenItem.getEmoji(), brokenItem.getName(), repairedItem.getEmoji(),
                        repairedItem.getName(), repairCost, recipeString.toString().trim()
                );
            }
        }

        @Name("list")
        @Description("List all items that can be repaired.")
        public static class ListItems extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var repairableItems = Arrays.stream(ItemReference.ALL)
                        .sorted(Comparator.comparingInt(i -> i.getItemType().ordinal()))
                        .filter(Broken.class::isInstance)
                        .map(Broken.class::cast).toList();

                var lang = ctx.getLanguageContext();
                List<MessageEmbed.Field> fields = new LinkedList<>();
                var builder = new EmbedBuilder()
                        .setAuthor(lang.get("commands.repair.ls.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setColor(Color.PINK)
                        .setFooter(lang.get("general.requested_by").formatted(ctx.getMember().getEffectiveName(), null));

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

                        recipeString.append(amount).append("x \u2009")
                                .append(needed.getEmoji())
                                .append("\u2009 *")
                                .append(needed.getName())
                                .append("*|");
                    }

                    var recipe = String.join(", ", recipeString.toString().split("\\|"));
                    var repairCost = mainItem.getValue() / 3;

                    fields.add(new MessageEmbed.Field("%s\u2009\u2009\u2009%s".formatted(item.getEmoji(), item.getName()),
                            "%s\n**%s** %s %s\n**Recipe: **%s\n**Item: ** %s %s".formatted(
                                    lang.get(item.getDesc()),
                                    lang.get("commands.repair.ls.cost"),
                                    repairCost, lang.get("commands.gamble.credits"),
                                    recipe,
                                    mainItem.getEmoji(), mainItem.getName()
                            ), false)
                    );
                }

                DiscordUtils.sendPaginatedEmbed(ctx.getUtilsContext(), builder, DiscordUtils.divideFields(3, fields), lang.get("commands.repair.ls.desc"));
            }
        }
    }

    @Description("The hub for salvage-related commands.")
    @Category(CommandCategory.CURRENCY)
    public static class Salvage extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Name("item")
        @Defer
        @Description("Salvages an item.")
        @Options(@Options.Option(type = OptionType.STRING, name = "item", description = "The item to salvage.", required = true))
        @Help(description = """
                            Salvages an item. Useful when you can't repair it but wanna get something back.
                            The cost is 1/3rd of the item price.""",
            usage = "`/salvage item:<item name of the broken version>`",
            parameters = @Help.Parameter(name = "item", description = "The item to salvage.")
        )
        public static class SalvageItem extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                final var itemName = ctx.getOptionAsString("item");

                //Get the necessary entities.
                final var player = ctx.getPlayer();
                final var user = ctx.getDBUser();
                final var playerInventory = player.inventory();
                final var item = ItemHelper.fromAnyNoId(itemName, ctx.getLanguageContext()).orElse(null);
                final var wrench = user.getEquippedItems().of(PlayerEquipment.EquipmentType.WRENCH);
                if (wrench == 0) {
                    ctx.reply("commands.cast.not_equipped", EmoteReference.ERROR);
                    return;
                }

                final var wrenchItem = ItemHelper.fromId(wrench);
                if (item == null) {
                    ctx.reply("commands.salvage.no_item_found", EmoteReference.ERROR);
                    return;
                }

                if (!(item instanceof final Broken broken)) {
                    ctx.reply("commands.salvage.not_broken", EmoteReference.ERROR, item.getName());
                    return;
                }

                final var original = ItemHelper.fromId(broken.getMainItem());
                if (!(original instanceof final Salvageable salvageable)) {
                    ctx.reply("commands.salvage.cant_salvage", EmoteReference.ERROR, item.getName());
                    return;
                }

                if (!playerInventory.containsItem(item)) {
                    ctx.reply("commands.salvage.no_main_item", EmoteReference.ERROR);
                    return;
                }

                int dust = user.getDustLevel();
                if (dust > 95) {
                    ctx.reply("commands.salvage.dust", EmoteReference.ERROR, dust);
                    return;
                }
                var returns = salvageable.getReturns().stream().map(ItemHelper::fromId).toList();
                if (returns.isEmpty()) {
                    ctx.reply("commands.salvage.no_returnables", EmoteReference.SAD);
                    return;
                }

                var salvageCost = item.getValue() / 3;
                var playerMoney = player.getCurrentMoney();
                if (playerMoney < salvageCost) {
                    ctx.reply("commands.salvage.not_enough_money", EmoteReference.ERROR, playerMoney, salvageCost);
                    return;
                }

                if (!RatelimitUtils.ratelimit(salvageRateLimiter, ctx)) {
                    return;
                }

                var toReturn = returns.get(random.nextInt(returns.size()));
                playerInventory.process(new ItemStack(toReturn, 1));
                playerInventory.process(new ItemStack(broken, -1));

                user.increaseDustLevel(3);
                user.updateAllChanged();

                player.removeMoney(salvageCost);
                player.save();

                var stats = ctx.getPlayerStats();
                stats.incrementSalvagedItems();
                stats.updateAllChanged();

                ItemHelper.handleItemDurability(wrenchItem, ctx, player, user, "commands.cast.autoequip.success");
                ctx.reply("commands.salvage.success", wrenchItem.getEmojiDisplay(), item.getEmojiDisplay(), item.getName(), toReturn.getEmojiDisplay(), toReturn.getName(), salvageCost);
            }
        }

        @Name("list")
        @Description("List all salvageable items.")
        @Help(description = "List all salvageable items.")
        public static class ListItems extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var broken = Arrays.stream(ItemReference.ALL)
                        .sorted(Comparator.comparingInt(i -> i.getItemType().ordinal()))
                        .filter(Broken.class::isInstance)
                        .map(Broken.class::cast).toList();

                List<MessageEmbed.Field> fields = new LinkedList<>();
                var lang = ctx.getLanguageContext();
                var builder = new EmbedBuilder()
                        .setAuthor(lang.get("commands.salvage.ls.header"),
                                null, ctx.getAuthor().getEffectiveAvatarUrl()
                        )
                        .setColor(Color.PINK)
                        .setFooter(lang.get("general.requested_by").formatted(ctx.getMember().getEffectiveName()),
                                null
                        );

                for (var item : broken) {
                    var mainItem = item.getItem();
                    if (!(mainItem instanceof Salvageable salvageable)) {
                        continue;
                    }

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
                            lang.get(item.getDesc()) + "\n**" +
                                    lang.get("commands.salvage.ls.cost") + "**" +
                                    salvageCost + " " + lang.get("commands.gamble.credits") +
                                    ".\n**" + lang.get("commands.salvage.ls.return") + "** " + recipeString,
                            false)
                    );
                }

                DiscordUtils.sendPaginatedEmbed(ctx.getUtilsContext(), builder, DiscordUtils.divideFields(3, fields), lang.get("commands.salvage.ls.desc"));
            }
        }
    }

    @Description("Shows the information of an item.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "item", description = "The item to see the info of.", required = true)
    })
    @Help(description = "Shows the information of an item.", usage = "`/iteminfo item:<item name>`", parameters = @Help.Parameter(name = "item", description = "The name of the item."))
    public static class ItemInfo extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var itemName = ctx.getOptionAsString("item");
            var itemOptional = ItemHelper.fromAnyNoId(itemName, ctx.getLanguageContext());
            if (itemOptional.isEmpty()) {
                ctx.reply("commands.iteminfo.no_item", EmoteReference.ERROR);
                return;
            }

            var item = itemOptional.get();
            var description = ctx.getLanguageContext().get(item.getDesc());
            var name = item.getTranslatedName();
            var translatedName = name.isEmpty() ? item.getName() : ctx.getLanguageContext().get(name);
            var type = ctx.getLanguageContext().get(item.getItemType().getDescription());

            if (item instanceof Attribute attribute) {
                var builder = new EmbedBuilder();
                var lang = ctx.getLanguageContext();
                builder.setAuthor(lang.get("commands.iteminfo.embed.header").formatted(translatedName),
                                null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setColor(ctx.getMemberColor())
                        .addField(EmoteReference.DIAMOND.toHeaderString() + lang.get("commands.iteminfo.embed.type"),
                                item.getEmoji() + " " + type, true
                        )
                        .addField(EmoteReference.EYES.toHeaderString() + lang.get("commands.iteminfo.embed.usefulness"),
                                attribute.getType().toString(), true
                        )
                        .addField(EmoteReference.GLOWING_STAR.toHeaderString() + lang.get("commands.iteminfo.embed.tier"),
                                attribute.getTierStars(), false
                        )
                        .addField(EmoteReference.ROCK.toHeaderString() + lang.get("commands.iteminfo.embed.durability"),
                                String.format(
                                        Utils.getLocaleFromLanguage(lang),
                                        "%,d", attribute.getMaxDurability()),
                                false
                        )
                        .addField(EmoteReference.CALENDAR.toHeaderString() + lang.get("commands.iteminfo.embed.attributes"),
                                attribute.buildAttributes(lang), false
                        )
                        .addField(EmoteReference.TALKING.toHeaderString() + lang.get("commands.iteminfo.embed.desc"),
                                lang.get(attribute.getExplanation()), false
                        );

                if (attribute.getTier() == 1) {
                    builder.addField(EmoteReference.THINKING.toHeaderString() + lang.get("commands.iteminfo.embed.upgrade"),
                            lang.get("commands.iteminfo.embed.upgrade_content"), false
                    );
                }

                ctx.reply(builder.build());
            } else {
                var buyable = item.isBuyable();
                var sellable = item.isSellable();
                // Blame me for bad organization...
                var credits = ctx.getLanguageContext().get("commands.slots.credits");
                var none = ctx.getLanguageContext().get("general.none");

                if (item instanceof Tiered) {
                    ctx.reply("commands.iteminfo.success_tiered", EmoteReference.BLUE_SMALL_MARKER,
                            item.getEmoji(), item.getName(), translatedName, type, description, ((Tiered) item).getTierStars(),
                            // This is pain
                            buyable ? item.getValue() : none, buyable ? credits : "",
                            sellable ? Math.round(item.getValue() * 0.9) : none, sellable ? credits : ""
                    );
                } else {
                    ctx.reply("commands.iteminfo.success", EmoteReference.BLUE_SMALL_MARKER,
                            item.getEmoji(), item.getName(), translatedName, type, description,
                            // This is pain
                            buyable ? item.getValue() : none, buyable ? credits : "",
                            sellable ? Math.round(item.getValue() * 0.9) : none, sellable ? credits : ""
                    );
                }
            }
        }
    }
}
