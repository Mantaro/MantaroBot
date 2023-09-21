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

package net.kodehawa.mantarobot.commands.currency.item;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.kodehawa.mantarobot.commands.currency.item.special.Broken;
import net.kodehawa.mantarobot.commands.currency.item.special.Food;
import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Breakable;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Salvageable;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.attributes.Tiered;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Axe;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.FishRod;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Pickaxe;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Wrench;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.command.slash.AutocompleteContext;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.MongoUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.Tuple;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RandomCollection;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemHelper {
    private static Item[] equipableItems;
    private static Item[] castableItems;
    private static Broken[] brokenItems;
    private static Broken[] salvageableItems;
    private static Item[] usableItems;
    private static Food[] petFoodItems;
    private static final Logger log = LoggerFactory.getLogger(ItemHelper.class);
    private static final SecureRandom random = new SecureRandom();
    private static final IncreasingRateLimiter lootCrateRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(2)
            .cooldown(15, TimeUnit.SECONDS)
            .maxCooldown(2, TimeUnit.MINUTES)
            .randomIncrement(false)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("lootcrate")
            .premiumAware(true)
            .build();
    public static void setItemActions() {
        log.info("Registering item actions...");

        ItemReference.MOP.setAction(((ctx, season) -> {
            Player player = ctx.getPlayer();
            MongoUser dbUser = ctx.getDBUser();
            if (!player.containsItem(ItemReference.MOP))
                return false;

            if (dbUser.getDustLevel() >= 5) {
                player.timesMopped(player.getTimesMopped() + 1);
                ctx.sendLocalized("general.misc_item_usage.mop", EmoteReference.DUST);

                if (dbUser.getDustLevel() == 100) {
                    player.addBadgeIfAbsent(Badge.DUSTY);
                }

                player.processItem(ItemReference.MOP, -1);
                dbUser.dustLevel(0);

                player.updateAllChanged();
                dbUser.updateAllChanged();
            } else {
                ctx.sendLocalized("general.misc_item_usage.mop_not_enough", EmoteReference.DUST);
                return false;
            }

            return true;
        }));

        ItemReference.POTION_CLEAN.setAction((ctx, season) -> {
            Player player = ctx.getPlayer();
            MongoUser dbUser = ctx.getDBUser();
            var equipped = dbUser.getEquippedItems();
            equipped.resetEffect(null);
            player.processItem(ItemReference.POTION_CLEAN, -1);

            player.updateAllChanged();
            equipped.updateAllChanged(dbUser);

            ctx.sendLocalized("general.misc_item_usage.milk", EmoteReference.CORRECT);
            return true;
        });

        // This tends to be an issue: the name doesn't match the emoji name.
        // I don't want to match all of them by emoji name, as that can lead to bad results and conflicts,
        // so alias them to the emoji name, so people can open them more easily.
        ItemReference.MINE_CRATE.registerItemAlias("mine lootbox");
        ItemReference.FISH_CRATE.registerItemAlias("fish lootbox");
        ItemReference.CHOP_CRATE.registerItemAlias("chop lootbox");
        ItemReference.MINE_PREMIUM_CRATE.registerItemAlias("premium mine lootbox");
        ItemReference.FISH_PREMIUM_CRATE.registerItemAlias("premium fish lootbox");
        ItemReference.CHOP_PREMIUM_CRATE.registerItemAlias("premium chop lootbox");
    }

    @SuppressWarnings("unused")
    public static Optional<Item> fromAny(String any, I18nContext languageContext) {
        try {
            Item item = fromId(Integer.parseInt(any));

            if (item != null) {
                return Optional.of(item);
            }
        } catch (NumberFormatException ignored) { }

        return fromAnyNoId(any, languageContext);
    }

    public static Optional<Item> fromAnyNoId(String any, I18nContext languageContext) {
        Optional<Item> itemOptional;

        itemOptional = fromEmoji(any);
        if (itemOptional.isPresent()) {
            return itemOptional;
        }

        itemOptional = fromAlias(any);
        if (itemOptional.isPresent()) {
            return itemOptional;
        }

        itemOptional = fromAliasList(any);
        if (itemOptional.isPresent()) {
            return itemOptional;
        }

        itemOptional = fromName(any, languageContext);
        if (itemOptional.isPresent()) {
            return itemOptional;
        }

        itemOptional = fromPartialName(any, languageContext);
        return itemOptional;
    }

    public static Optional<Item> fromEmoji(String emoji) {
        return Stream.of(ItemReference.ALL)
                .filter(item -> isMatchingEmoji(item, emoji))
                .findFirst();
    }

    public static Item fromId(int id) {
        return ItemReference.ALL[id];
    }

    public static Optional<Item> fromName(String name, I18nContext languageContext) {
        return Arrays.stream(ItemReference.ALL)
                .filter(item -> isMatchingItemName(item, name, languageContext))
                .findFirst();
    }

    public static Optional<Item> fromTranslationSlice(String slice) {
        return Arrays.stream(ItemReference.ALL)
                .filter(item -> item.getTranslatedName().equals("items." + slice))
                .findFirst();
    }

    public static Optional<Item> fromAlias(String name) {
        return Arrays.stream(ItemReference.ALL).filter(item -> isMatchingAlias(item, name)).findFirst();
    }

    public static Optional<Item> fromAliasList(String name) {
        return Arrays.stream(ItemReference.ALL).filter(item -> hasMatchingAlias(item, name)).findFirst();
    }

    public static Optional<Item> fromPartialName(String name, I18nContext languageContext) {
        return Arrays.stream(ItemReference.ALL)
                .filter(item -> isPartialNameMatch(item, name, languageContext))
                .findFirst();
    }

    public static int idOf(Item item) {
        return Arrays.asList(ItemReference.ALL)
                .indexOf(item);
    }

    static boolean openLootCrate(IContext ctx, ItemType.LootboxType type, int item, EmoteReference typeEmote, int bound) {
        Player player = ctx.getPlayer();
        Item crate = fromId(item);
        int amount = 1;
        // cant really make this work without slash without changing the whole openCrate logic :(
        if (ctx instanceof SlashContext sCtx) {
            if (sCtx.getOptionAsBoolean("max")) {
                amount = Math.max(1, Math.min(5, Math.min(player.getItemAmount(crate), player.getItemAmount(ItemReference.LOOT_CRATE_KEY))));
            } else {
                amount = sCtx.getOptionAsInteger("amount", 1);
            }
        }

        if (amount > 5) {
            ctx.sendLocalized("general.misc_item_usage.crate.too_many", EmoteReference.ERROR);
            return false;
        }

        if (player.getItemAmount(crate) >= amount) {
            if (player.getItemAmount(ItemReference.LOOT_CRATE_KEY) >= amount) {
                if (!RatelimitUtils.ratelimit(lootCrateRatelimiter, ctx, false))
                    return false;

                if (crate == ItemReference.LOOT_CRATE) {
                    player.addBadgeIfAbsent(Badge.THE_SECRET);
                }

                //It saves the changes here.
                openLootBox(ctx, player, amount, type, crate, typeEmote, bound);
                return true;
            } else {
                ctx.sendLocalized("general.misc_item_usage.crate.no_key", EmoteReference.ERROR);
                return false;
            }
        } else {
            ctx.sendLocalized("general.misc_item_usage.crate.no_crate", EmoteReference.ERROR);
            return false;
        }
    }

    private static void openLootBox(IContext ctx, Player player, int amount, ItemType.LootboxType type, Item crate,
                                    EmoteReference typeEmote, int bound) {
        List<Item> toAdd = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            toAdd.addAll(selectItems(random.nextInt(bound) + bound, type));
        }

        ArrayList<ItemStack> ita = new ArrayList<>();
        toAdd.forEach(item -> ita.add(new ItemStack(item, 1)));

        if ((type == ItemType.LootboxType.MINE || type == ItemType.LootboxType.MINE_PREMIUM) && toAdd.contains(ItemReference.SPARKLE_PICKAXE)) {
            player.addBadgeIfAbsent(Badge.DESTINY_REACHES);
        }

        if ((type == ItemType.LootboxType.FISH || type == ItemType.LootboxType.FISH_PREMIUM) && toAdd.contains(ItemReference.SHARK)) {
            player.addBadgeIfAbsent(Badge.TOO_BIG);
        }

        var toShow = ItemStack.reduce(ita);
        // Tools must only drop one, if any.
        toShow = toShow.stream().map(stack -> {
            var item = stack.getItem();
            if (stack.getAmount() > 1 && ((item instanceof Pickaxe) || (item instanceof FishRod) || (item instanceof Axe))) {
                return new ItemStack(item, 1);
            }

            return stack;
        }).collect(Collectors.toList());

        boolean overflow = player.mergeInventory(toShow);

        player.processItem(ItemReference.LOOT_CRATE_KEY, -amount);
        player.processItem(crate, -amount);
        player.cratesOpened(player.getCratesOpened() + amount);
        player.updateAllChanged();

        I18nContext lang = ctx.getLanguageContext();
        var show = toShow.stream()
                .map(itemStack -> "%,dx \u2009%s".formatted(itemStack.getAmount(), itemStack.getItem().toDisplayString()))
                .collect(Collectors.joining(", "));

        var extra = "";
        if (overflow) {
            extra = ". " + lang.get("general.misc_item_usage.crate.overflow");
        }

        var high = toShow.stream()
                .filter(stack -> stack.getItem() instanceof Tiered)
                .filter(stack -> ((Tiered) stack.getItem()).getTier() >= 4)
                .map(stack -> "%s \u2009(%d \u2b50)".formatted(stack.getItem().getEmoji(), ((Tiered) stack.getItem()).getTier()))
                .collect(Collectors.joining(", "));
        if (!high.isEmpty()) {
            extra = ".\n\n" + lang.get("general.misc_item_usage.crate.success_high").formatted(EmoteReference.POPPER, high);
        }

        ctx.sendFormat(lang.get("general.misc_item_usage.crate.success"),
                typeEmote.getDiscordNotation() + " ", amount, show, extra);
    }

    @SuppressWarnings("fallthrough")
    private static List<Item> selectItems(int amount, ItemType.LootboxType type) {
        List<Item> common = handleItemDrop(i -> i.getItemType() == ItemType.COMMON, true);
        List<Item> rare = handleItemDrop(i -> i.getItemType() == ItemType.RARE);
        List<Item> premium = handleItemDrop(i -> i.getItemType() == ItemType.PREMIUM);

        List<Item> mine = handleItemDrop(i ->
                (i.getItemType() == ItemType.MINE ||
                i.getItemType() == ItemType.CAST_OBTAINABLE ||
                i.getItemType() == ItemType.MINE_PICK) && i != ItemReference.ROCK, true
        );

        List<Item> fish = handleItemDrop(i -> i.getItemType() == ItemType.FISHING ||  i.getItemType() == ItemType.FISHROD, true);
        List<Item> chop = handleItemDrop(i -> i.getItemType() == ItemType.CHOP_DROP ||  i.getItemType() == ItemType.CHOP_AXE, true);

        List<Item> premiumMine = handleItemDrop(i ->
                i.getItemType() == ItemType.CAST_MINE ||
                i.getItemType() == ItemType.MINE_PICK ||
                i.getItemType() == ItemType.MINE ||
                i.getItemType() == ItemType.MINE_RARE ||
                i.getItemType() == ItemType.CAST_OBTAINABLE ||
                i.getItemType() == ItemType.MINE_RARE_PICK
        );

        List<Item> premiumFish = handleItemDrop(i ->
                i.getItemType() == ItemType.FISHROD ||
                i.getItemType() == ItemType.FISHROD_RARE ||
                i.getItemType() == ItemType.FISHING ||
                i.getItemType() == ItemType.FISHING_RARE
        );

        List<Item> premiumChop = handleItemDrop(i ->
                i.getItemType() == ItemType.CHOP_DROP ||
                        i.getItemType() == ItemType.CHOP_AXE ||
                        i.getItemType() == ItemType.CHOP_RARE_AXE
        );

        RandomCollection<Item> items = new RandomCollection<>();
        switch (type) {
            case PREMIUM:
                premium.forEach(i -> items.add(2, i));
            case RARE:
                rare.forEach(i -> items.add(5, i));
            case COMMON:
                common.forEach(i -> items.add(20, i));
                break; //fallthrough intended until here.
            case CHOP_PREMIUM:
                premiumChop.forEach(i -> items.add(8, i));
                break;
            case FISH_PREMIUM:
                premiumFish.forEach(i -> items.add(8, i));
                break;
            case MINE_PREMIUM:
                premiumMine.forEach(i -> items.add(8, i));
                break;
            case MINE:
                mine.forEach(i -> items.add(8, i));
                break;
            case FISH:
                fish.forEach(i -> items.add(8, i));
                break;
            case CHOP:
                chop.forEach(i -> items.add(8, i));
        }

        List<Item> list = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            list.add(items.next());
        }

        return list;
    }

    private static List<Item> handleItemDrop(Predicate<Item> predicate) {
        return handleItemDrop(predicate, false);
    }


    private static List<Item> handleItemDrop(Predicate<Item> predicate, boolean normal) {
        List<Item> all = Arrays.stream(ItemReference.ALL)
                .filter(i -> i.isBuyable() || i.isSellable()).toList();

        return all.stream()
                .filter(predicate)
                .filter(item -> {
                    // Keep in mind the chances here aren't absolute for any means,
                    // and it depends on the RandomCollection created on selectItems
                    if (normal) {
                        if ((item instanceof Tiered tiered && tiered.getTier() >= 5)) {
                            return random.nextFloat() <= 0.02f; // 2% for 5* +
                        }

                        if ((item instanceof Tiered tiered && tiered.getTier() >= 3) || item.getValue() >= 100) {
                            return random.nextFloat() <= 0.05f;  // 5% for 3 and 4*
                        }
                    } else {
                        if ((item instanceof Tiered tiered && tiered.getTier() >= 5)) {
                            return random.nextFloat() <= 0.10f; // 10% for 5* +
                        }

                        if ((item instanceof Tiered tiered && tiered.getTier() >= 3) || item.getValue() >= 300) {
                            return random.nextFloat() <= 0.40f; // 40% for 3* +
                        }
                    }

                    return true;
                })
                .sorted(Comparator.comparingLong(i -> i.value))
                .collect(Collectors.toList());
    }

    public static boolean handleEffect(PlayerEquipment equipment, Item item, MongoUser user) {
        if (!(item instanceof Potion potion)) return false;
        var type = potion.getEffectType();
        boolean isEffectPresent = equipment.getCurrentEffect(type) != null;

        if (isEffectPresent) {
            //Not the correct item to handle the effect of = not handling this call.
            if (item != equipment.getEffectItem(type)) {
                return false;
            }

            // Effect is active when it's been used less than the max amount
            if (!equipment.isEffectActive(type, potion.getMaxUses())) {
                // Reset effect if the current amount equipped is 0. Else, subtract one from the current amount equipped.
                if (!equipment.useEffect(type)) { //This call subtracts one from the current amount equipped.
                    equipment.resetEffect(type);
                    // This has to go twice, because I have to return on the next statement.
                    equipment.updateAllChanged(user);

                    return false;
                } else {
                    equipment.updateAllChanged(user);
                    return true;
                }
            } else {
                equipment.incrementEffectUses(type);
                if (!equipment.isEffectActive(type, potion.getMaxUses())) {
                    // Get the new amount. If the effect is not active we need to remove it
                    // This is obviously a little hacky, but that's what I get for not thinking about it before.
                    // This option will blow through the stack if the used amount > allowed amount,
                    // but we check if the effect is not active, therefore it will only go through and delete
                    // the element from the stack only when there's no more uses remaining on that part of the stack :)
                    // This bug took me two god damn years to fix.
                    equipment.useEffect(type);
                }

                equipment.updateAllChanged(user);

                return true;
            }
        }

        return false;
    }

    public static Item getBrokenItemFrom(Item item) {
        for (Broken i : getBrokenItems()) {
            if (i.getMainItem() == idOf(item))
                return i;
        }

        return null;
    }

    public static Tuple<Boolean, Player, MongoUser> handleDurability(IContext ctx, Item item, Player player, MongoUser user) {
        var equippedItems = user.getEquippedItems();
        var subtractFrom = 0;

        if (handleEffect(equippedItems, ItemReference.POTION_STAMINA, user)) {
            subtractFrom = random.nextInt(7);
        } else {
            subtractFrom = random.nextInt(10);
        }

        //We do validation before this...
        var equipmentType = equippedItems.getTypeFor(item);

        //This is important for previously equipped items before we implemented durability.
        if (!equippedItems.getDurability().containsKey(equipmentType) && item instanceof Breakable) {
            equippedItems.resetDurabilityTo(equipmentType, ((Breakable) item).getMaxDurability());
        }

        var durability = equippedItems.reduceDurability(equipmentType, Math.max(3, subtractFrom));
        var assumeBroken = durability < 5;
        var languageContext = ctx.getLanguageContext();

        if (assumeBroken) {
            equippedItems.resetOfType(equipmentType);

            var broken = "";
            var brokenItem = getBrokenItemFrom(item);
            var successBroken = false;
            if (brokenItem != null && (item.getValue() > 10000 || random.nextInt(100) >= 20)) {
                broken = "\n" + String.format(languageContext.get("commands.mine.broken_drop"),
                        EmoteReference.HEART, brokenItem.getEmoji(), brokenItem.getName()
                );

                player.processItem(brokenItem, 1);
                successBroken = true;
            }

            if (!successBroken && brokenItem != null) {
                broken = "\n" + String.format(languageContext.get("commands.mine.broken_drop_miss"), EmoteReference.SAD);
            }

            var toReplace = languageContext.get("commands.mine.item_broke");
            if (!user.isAutoEquip()) {
                toReplace += "\n" + languageContext.get("commands.mine.item_broke_autoequip");
            }

            ctx.sendFormat(toReplace, EmoteReference.SAD, item.getName(), broken);

            if (player.addBadgeIfAbsent(Badge.ITEM_BREAKER) || successBroken) {
                player.updateAllChanged();
            }

            equippedItems.updateAllChanged(user);

            var stats = ctx.db().getPlayerStats(ctx.getAuthor());
            stats.incrementToolsBroken();
            stats.updateAllChanged();

            //is broken
            return Tuple.of(true, player, user);
        } else {
            var addedBadge = false;
            if (item == ItemReference.HELLFIRE_PICK) {
                player.addBadgeIfAbsent(Badge.HOT_MINER);
                addedBadge = true;
            }
            if (item == ItemReference.HELLFIRE_ROD) {
                player.addBadgeIfAbsent(Badge.HOT_FISHER);
                addedBadge = true;
            }

            if (item == ItemReference.HELLFIRE_AXE) {
                player.addBadgeIfAbsent(Badge.HOT_CHOPPER);
                addedBadge = true;
            }

            if (addedBadge) {
                player.updateAllChanged();
            }

            equippedItems.updateAllChanged(user);

            //is not broken
            return Tuple.of(false, player, user);
        }
    }

    public static void handleItemDurability(Item item, IContext ctx, Player player, MongoUser dbUser, String i18n) {
        var breakage = handleDurability(ctx, item, player, dbUser);
        if (!breakage.first()) {
            return;
        }

        //We need to get this again since reusing the old ones will cause :fire:
        var finalPlayer = breakage.second();
        var finalUser = breakage.third();
        if (finalUser.isAutoEquip() && finalPlayer.containsItem(item)) {
            var equipped = finalUser.getEquippedItems();
            equipped.equipItem(item);
            finalPlayer.processItem(item, -1);

            finalPlayer.updateAllChanged();
            equipped.updateAllChanged(dbUser);

            ctx.sendLocalized(i18n, EmoteReference.CORRECT, item.getName());
        }
    }

    private static boolean isMatchingItemName(Item item, String name, I18nContext languageContext) {
        final var itemName = item.getName().toLowerCase().trim();
        final var lookup = name.toLowerCase().trim();
        final var translatedName = item.getTranslatedName();

        // Either name or translated name
        return itemName.equals(lookup) || (
                !translatedName.isEmpty() && !languageContext.getContextLanguage().equals("en_US") &&
                        languageContext.get(translatedName).toLowerCase().trim().equals(lookup)
        );
    }

    private static boolean isMatchingEmoji(Item item, String emoji) {
        return item.getEmoji().equals(emoji.replace("\ufe0f", ""));
    }

    private static boolean isMatchingAlias(Item item, String name) {
        if (item.getAlias() == null) {
            return false;
        }
        return item.getAlias()
                .toLowerCase()
                .trim()
                .equals(name.toLowerCase().trim());
    }

    private static boolean hasMatchingAlias(Item item, String name) {
        if (item.getAliases().isEmpty()) {
            return false;
        }

        final var lookup = name.toLowerCase().trim();
        return item.getAliases().stream().anyMatch(lookup::equals);
    }

    private static boolean isPartialNameMatch(Item item, String name, I18nContext languageContext) {
        final var itemName = item.getName().toLowerCase().trim();
        final var lookup = name.toLowerCase().trim();
        final var translatedName = item.getTranslatedName();

        return itemName.contains(lookup) || (
                !translatedName.isEmpty() && !languageContext.getContextLanguage().equals("en_US") &&
                        languageContext.get(translatedName).toLowerCase().trim().contains(lookup)
        );
    }

    public static List<Item> findFrom(Item[] items, String search, I18nContext langContext) {
        return Stream.of(items)
                .filter(item -> {
                    // each item should only traverse as many helper funcs as needed
                    // so, we return as early as we can
                    if (isMatchingEmoji(item, search)) return true;
                    if (isMatchingAlias(item, search)) return true;
                    if (hasMatchingAlias(item, search)) return true;
                    if (isMatchingItemName(item, search, langContext)) return true;
                    return isPartialNameMatch(item, search, langContext);
                }).toList();

    }

    private static void handleAutoComplete(Item[] items, AutocompleteContext event) {
        if (event.getFocused().getName().equals("item")) {
            final String search = event.getOption("item").getAsString();
            if (search.isBlank()) {
                // This internally calls to ImmutableCollections.EMPTY_LIST.
                // Sending empty sends "no results for search" on the Discord :tm: client.
                // Which is fine and relays them that they need to start searching anyway.
                event.replyChoices(List.of());
                return;
            }

            final List<Item> matches = findFrom(items, search, event.getI18n());
            final List<Command.Choice> choices = new ArrayList<>();
            for (Item item : matches) {
                var isEnglish = event.getI18n().getContextLanguage().equalsIgnoreCase("en_us");
                var fullChoice = event.getI18n().get(item.getTranslatedName()) + " (" + item.getName() + ")";
                // we fall back to english if the choice would be too long
                // because we want to avoid confusion
                choices.add(new Command.Choice(
                        isEnglish || fullChoice.length() > OptionData.MAX_CHOICE_VALUE_LENGTH ? item.getName() : fullChoice,
                        item.getName()
                ));
            }
            event.replyChoices(choices);
        }
    }

    public static void autoCompleteCastable(AutocompleteContext event) {
        handleAutoComplete(getCastableItems(), event);
    }

    public static void autoCompleteEquipable(AutocompleteContext event) {
        handleAutoComplete(getEquipableItems(), event);
    }

    public static void autoCompleteRepairable(AutocompleteContext event) {
        handleAutoComplete(getBrokenItems(), event);
    }

    public static void autoCompleteSalvageable(AutocompleteContext event) {
        handleAutoComplete(getSalvageableItems(), event);
    }

    public static void autoCompleteUsable(AutocompleteContext event) {
        handleAutoComplete(getUsableItems() ,event);
    }

    public static void autoCompletePetFood(AutocompleteContext event) {
        handleAutoComplete(getPetFoodItems(), event);
    }

    public static Item[] getEquipableItems() {
        return Objects.requireNonNullElseGet(equipableItems, () -> equipableItems = Stream.of(ItemReference.ALL)
                .filter(i -> i instanceof Pickaxe || i instanceof Axe || i instanceof FishRod || i instanceof Wrench)
                .toArray(Item[]::new));
    }

    public static Item[] getCastableItems() {
        return Objects.requireNonNullElseGet(castableItems, () -> castableItems = Stream.of(ItemReference.ALL)
                .filter(i -> i.getItemType().isCastable() && !i.getRecipe().isEmpty())
                .sorted(Comparator.comparingInt(i -> i.getItemType().ordinal()))
                .toArray(Item[]::new));
    }

    public static Broken[] getBrokenItems() {
        return Objects.requireNonNullElseGet(brokenItems, () -> brokenItems = Stream.of(ItemReference.ALL)
                .filter(Broken.class::isInstance)
                .sorted(Comparator.comparingInt(i -> i.getItemType().ordinal()))
                .map(Broken.class::cast)
                .toArray(Broken[]::new));
    }

    public static Broken[] getSalvageableItems() {
        return Objects.requireNonNullElseGet(salvageableItems, () -> salvageableItems = Stream.of(getBrokenItems())
                .filter(i -> i.getItem() instanceof Salvageable)
                .toArray(Broken[]::new));
    }

    public static Item[] getUsableItems() {
        return Objects.requireNonNullElseGet(usableItems, () -> usableItems = Stream.of(ItemReference.ALL)
                .filter(i -> i.getItemType() == ItemType.INTERACTIVE ||
                        i.getItemType() == ItemType.POTION ||
                        i.getItemType() == ItemType.CRATE ||
                        i.getItemType() == ItemType.BUFF)
                .toArray(Item[]::new));
    }


    public static Food[] getPetFoodItems() {
        return Objects.requireNonNullElseGet(petFoodItems, () -> petFoodItems = Stream.of(ItemReference.ALL)
                .filter(Food.class::isInstance)
                .map(Food.class::cast)
                .toArray(Food[]::new));
    }
}
