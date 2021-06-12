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

package net.kodehawa.mantarobot.commands.currency.item;

import net.kodehawa.mantarobot.commands.currency.item.special.Broken;
import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Breakable;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.attributes.Tiered;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Axe;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.FishRod;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Pickaxe;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.RandomCollection;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemHelper {
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
            PlayerData playerData = player.getData();
            DBUser dbUser = ctx.getDBUser();
            UserData userData = dbUser.getData();
            Inventory playerInventory = player.getInventory();

            if (!playerInventory.containsItem(ItemReference.MOP))
                return false;

            if (userData.getDustLevel() >= 5) {
                playerData.setTimesMopped(playerData.getTimesMopped() + 1);
                ctx.sendLocalized("general.misc_item_usage.mop", EmoteReference.DUST);

                if (userData.getDustLevel() == 100) {
                    playerData.addBadgeIfAbsent(Badge.DUSTY);
                }

                playerInventory.process(new ItemStack(ItemReference.MOP, -1));
                userData.setDustLevel(0);

                player.save();
                dbUser.save();
            } else {
                ctx.sendLocalized("general.misc_item_usage.mop_not_enough", EmoteReference.DUST);
                return false;
            }

            return true;
        }));

        ItemReference.POTION_CLEAN.setAction((ctx, season) -> {
            Player player = ctx.getPlayer();
            DBUser dbUser = ctx.getDBUser();
            UserData userData = dbUser.getData();
            Inventory playerInventory = player.getInventory();

            userData.getEquippedItems().resetEffect(PlayerEquipment.EquipmentType.POTION);
            playerInventory.process(new ItemStack(ItemReference.POTION_CLEAN, -1));

            player.save();
            dbUser.save();

            ctx.sendLocalized("general.misc_item_usage.milk", EmoteReference.CORRECT);
            return true;
        });
    }

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

        itemOptional = fromName(any, languageContext);
        if (itemOptional.isPresent()) {
            return itemOptional;
        }

        itemOptional = fromPartialName(any, languageContext);
        return itemOptional;
    }

    public static Optional<Item> fromEmoji(String emoji) {
        return Stream.of(ItemReference.ALL)
                .filter(item -> item.getEmoji().equals(emoji.replace("\ufe0f", "")))
                .findFirst();
    }

    public static Item fromId(int id) {
        return ItemReference.ALL[id];
    }

    public static Optional<Item> fromName(String name, I18nContext languageContext) {
        return Arrays.stream(ItemReference.ALL)
                .filter(item -> {
                    final var itemName = item.getName().toLowerCase().trim();
                    final var lookup = name.toLowerCase().trim();
                    final var translatedName = item.getTranslatedName();

                    // Either name or translated name
                    return itemName.equals(lookup) || (
                            !translatedName.isEmpty() && !languageContext.getContextLanguage().equals("en_US") &&
                            languageContext.get(translatedName).toLowerCase().trim().equals(lookup)
                    );
                })
                .findFirst();
    }

    public static Optional<Item> fromAlias(String name) {
        return Arrays.stream(ItemReference.ALL).filter(item -> {
            if (item.getAlias() == null) {
                return false;
            }

            return item.getAlias()
                    .toLowerCase()
                    .trim()
                    .equals(name.toLowerCase().trim());
        }).findFirst();
    }

    public static Optional<Item> fromPartialName(String name, I18nContext languageContext) {
        return Arrays.stream(ItemReference.ALL)
                .filter(item -> {
                    final var itemName = item.getName().toLowerCase().trim();
                    final var lookup = name.toLowerCase().trim();
                    final var translatedName = item.getTranslatedName();

                    return itemName.contains(lookup) || (
                            !translatedName.isEmpty() && !languageContext.getContextLanguage().equals("en_US") &&
                            languageContext.get(translatedName).toLowerCase().trim().contains(lookup)
                    );
                })
                .findFirst();
    }

    public static int idOf(Item item) {
        return Arrays.asList(ItemReference.ALL)
                .indexOf(item);
    }

    static boolean openLootCrate(Context ctx, ItemType.LootboxType type, int item, EmoteReference typeEmote, int bound, boolean season) {
        Player player = ctx.getPlayer();
        SeasonPlayer seasonPlayer = ctx.getSeasonPlayer();
        Inventory inventory = season ? seasonPlayer.getInventory() : player.getInventory();

        Item crate = fromId(item);

        if (inventory.containsItem(crate)) {
            if (inventory.containsItem(ItemReference.LOOT_CRATE_KEY)) {
                if (!RatelimitUtils.ratelimit(lootCrateRatelimiter, ctx, false))
                    return false;

                if (crate == ItemReference.LOOT_CRATE) {
                    player.getData().addBadgeIfAbsent(Badge.THE_SECRET);
                }

                //It saves the changes here.
                openLootBox(ctx, player, seasonPlayer, type, crate, typeEmote, bound, season);
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

    private static void openLootBox(Context ctx, Player player, SeasonPlayer seasonPlayer, ItemType.LootboxType type, Item crate,
                                    EmoteReference typeEmote, int bound, boolean seasonal) {
        List<Item> toAdd = selectItems(random.nextInt(bound) + bound, type);

        ArrayList<ItemStack> ita = new ArrayList<>();
        toAdd.forEach(item -> ita.add(new ItemStack(item, 1)));

        PlayerData data = player.getData();
        if ((type == ItemType.LootboxType.MINE || type == ItemType.LootboxType.MINE_PREMIUM) && toAdd.contains(ItemReference.SPARKLE_PICKAXE)) {
            data.addBadgeIfAbsent(Badge.DESTINY_REACHES);
        }

        if ((type == ItemType.LootboxType.FISH || type == ItemType.LootboxType.FISH_PREMIUM) && toAdd.contains(ItemReference.SHARK)) {
            data.addBadgeIfAbsent(Badge.TOO_BIG);
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

        boolean overflow = seasonal ? seasonPlayer.getInventory().merge(toShow) : player.getInventory().merge(toShow);

        if (seasonal) {
            seasonPlayer.getInventory().process(new ItemStack(ItemReference.LOOT_CRATE_KEY, -1));
            seasonPlayer.getInventory().process(new ItemStack(crate, -1));
        } else {
            player.getInventory().process(new ItemStack(ItemReference.LOOT_CRATE_KEY, -1));
            player.getInventory().process(new ItemStack(crate, -1));
        }

        data.setCratesOpened(data.getCratesOpened() + 1);
        player.save();

        if (seasonal) {
            seasonPlayer.save();
        }

        I18nContext lang = ctx.getLanguageContext();

        var show = toShow.stream()
                .map(itemStack -> "x%,d \u2009%s".formatted(itemStack.getAmount(), itemStack.getItem().toDisplayString()))
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
        if (high.length() >= 1) {
            extra = ".\n\n" + lang.get("general.misc_item_usage.crate.success_high").formatted(EmoteReference.POPPER, high);
        }

        ctx.sendFormat(lang.get("general.misc_item_usage.crate.success"),
                typeEmote.getDiscordNotation() + " ", show, extra);
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
                .filter(i -> i.isBuyable() || i.isSellable())
                .collect(Collectors.toList());

        return all.stream()
                .filter(predicate)
                .filter(item -> {
                    // Keep in mind the chances here aren't absolute for any means,
                    // and it depends on the RandomCollection created on selectItems
                    if (normal) {
                        if ((item instanceof Tiered && ((Tiered) item).getTier() >= 5)) {
                            return random.nextFloat() <= 0.02f; // 2% for 5* +
                        }

                        if ((item instanceof Tiered && ((Tiered) item).getTier() >= 3) || item.getValue() >= 100) {
                            return random.nextFloat() <= 0.05f;  // 5% for 3 and 4*
                        }
                    } else {
                        if ((item instanceof Tiered && ((Tiered) item).getTier() >= 5)) {
                            return random.nextFloat() <= 0.10f; // 10% for 5* +
                        }

                        if ((item instanceof Tiered && ((Tiered) item).getTier() >= 3) || item.getValue() >= 300) {
                            return random.nextFloat() <= 0.40f; // 40% for 3* +
                        }
                    }

                    return true;
                })
                .sorted(Comparator.comparingLong(i -> i.value))
                .collect(Collectors.toList());
    }

    public static boolean handleEffect(PlayerEquipment.EquipmentType type, PlayerEquipment equipment, Item item, DBUser user) {
        boolean isEffectPresent = equipment.getCurrentEffect(type) != null;

        if (isEffectPresent) {
            //Not the correct item to handle the effect of = not handling this call.
            if (item != equipment.getEffectItem(type)) {
                return false;
            }

            // Effect is active when it's been used less than the max amount
            if (!equipment.isEffectActive(type, ((Potion) item).getMaxUses())) {
                // Reset effect if the current amount equipped is 0. Else, subtract one from the current amount equipped.
                if (!equipment.getCurrentEffect(type).use()) { //This call subtracts one from the current amount equipped.
                    equipment.resetEffect(type);
                    // This has to go twice, because I have to return on the next statement.
                    // We remove something from a HashMap here, and somehow
                    // removing it from a HashMap will need a full replace (why?)
                    user.save();

                    return false;
                } else {
                    user.saveUpdating();
                    return true;
                }
            } else {
                equipment.incrementEffectUses(type);
                if (!equipment.isEffectActive(type, ((Potion) item).getMaxUses())) {
                    // Get the new amount. If the effect is not active we need to remove it
                    // This is obviously a little hacky, but that's what I get for not thinking about it before.
                    // This option will blow through the stack if the used amount > allowed amount,
                    // but we check if the effect is not active, therefore it will only go through and delete
                    // the element from the stack only when there's no more uses remaining on that part of the stack :)
                    // This bug took me two god damn years to fix.
                    equipment.getCurrentEffect(type).use();
                }

                user.saveUpdating();

                return true;
            }
        }

        return false;
    }

    public static Item getBrokenItemFrom(Item item) {
        for (Item i : ItemReference.ALL) {
            if (i instanceof Broken) {
                if (((Broken) i).getMainItem() == idOf(item))
                    return i;
            }
        }

        return null;
    }

    public static Pair<Boolean, Pair<Player, DBUser>> handleDurability(Context ctx, Item item,
                                                         Player player, DBUser user, SeasonPlayer seasonPlayer, boolean isSeasonal) {
        var playerInventory = isSeasonal ? seasonPlayer.getInventory() : player.getInventory();
        var userData = user.getData();
        var seasonPlayerData = seasonPlayer.getData();
        var equippedItems = isSeasonal ? seasonPlayerData.getEquippedItems() : userData.getEquippedItems();
        var subtractFrom = 0;

        if (handleEffect(PlayerEquipment.EquipmentType.POTION, equippedItems, ItemReference.POTION_STAMINA, user)) {
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

                playerInventory.process(new ItemStack(brokenItem, 1));
                successBroken = true;
            }

            if (!successBroken && brokenItem != null) {
                broken = "\n" + String.format(languageContext.get("commands.mine.broken_drop_miss"), EmoteReference.SAD);
            }

            var toReplace = languageContext.get("commands.mine.item_broke");
            if (!userData.isAutoEquip() && !isSeasonal) {
                toReplace += "\n" + languageContext.get("commands.mine.item_broke_autoequip");
            }

            ctx.sendFormat(toReplace, EmoteReference.SAD, item.getName(), broken);

            if (isSeasonal) {
                seasonPlayer.save();
            } else {
                player.getData().addBadgeIfAbsent(Badge.ITEM_BREAKER);
                player.saveUpdating();
                // We remove something from a HashMap here, and somehow
                // removing it from a HashMap will need a full replace (why?)
                user.save();
            }

            var stats = ctx.getPlayerStats();
            stats.incrementToolsBroken();
            stats.saveUpdating();

            //is broken
            return Pair.of(true, Pair.of(player, user));
        } else {
            if (isSeasonal) {
                seasonPlayer.saveUpdating();
            } else {
                if (item == ItemReference.HELLFIRE_PICK)
                    player.getData().addBadgeIfAbsent(Badge.HOT_MINER);
                if (item == ItemReference.HELLFIRE_ROD)
                    player.getData().addBadgeIfAbsent(Badge.HOT_FISHER);
                if (item == ItemReference.HELLFIRE_AXE)
                    player.getData().addBadgeIfAbsent(Badge.HOT_CHOPPER);

                player.saveUpdating();
                user.saveUpdating();
            }

            //is not broken
            return Pair.of(false, Pair.of(player, user));
        }
    }

    public static void handleItemDurability(Item item, Context ctx, Player player, DBUser dbUser,
                                      SeasonPlayer seasonPlayer, String i18n, boolean isSeasonal) {
        var breakage = handleDurability(ctx, item, player, dbUser, seasonPlayer, isSeasonal);
        if (!breakage.getKey()) {
            return;
        }

        if (isSeasonal) {
            return;
        }

        //We need to get this again since reusing the old ones will cause :fire:
        var finalPlayer = breakage.getValue().getKey();
        var finalUser = breakage.getValue().getValue();
        var inventory = finalPlayer.getInventory();
        var userData = finalUser.getData();

        if (userData.isAutoEquip() && inventory.containsItem(item)) {
            userData.getEquippedItems().equipItem(item);
            inventory.process(new ItemStack(item, -1));

            finalPlayer.save();
            finalUser.save();

            ctx.sendLocalized(i18n, EmoteReference.CORRECT, item.getName());
        }
    }
}
