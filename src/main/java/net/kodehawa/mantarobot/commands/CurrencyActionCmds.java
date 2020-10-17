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
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.currency.item.*;
import net.kodehawa.mantarobot.commands.currency.item.special.Axe;
import net.kodehawa.mantarobot.commands.currency.item.special.FishRod;
import net.kodehawa.mantarobot.commands.currency.item.special.Pickaxe;
import net.kodehawa.mantarobot.commands.currency.pets.HousePet;
import net.kodehawa.mantarobot.commands.currency.pets.HousePetType;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.SeasonalPlayerData;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.RandomCollection;
import net.kodehawa.mantarobot.utils.RatelimitUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.campaign.Campaign;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import org.apache.commons.lang3.tuple.Pair;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kodehawa.mantarobot.commands.currency.item.Items.handleDurability;

@Module
public class CurrencyActionCmds {
    private final SecureRandom random = new SecureRandom();

    @Subscribe
    public void mine(CommandRegistry cr) {
        cr.register("mine", new SimpleCommand(CommandCategory.CURRENCY) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .limit(1)
                    .spamTolerance(2)
                    .cooldown(5, TimeUnit.MINUTES)
                    .maxCooldown(5, TimeUnit.MINUTES)
                    .incrementDivider(10)
                    .premiumAware(true)
                    .pool(MantaroData.getDefaultJedisPool())
                    .prefix("mine")
                    .build();

            @Override
            protected void call(Context ctx, String content, String[] args) {
                boolean isSeasonal = ctx.isSeasonal();
                final I18nContext languageContext = ctx.getLanguageContext();

                final User user = ctx.getAuthor();
                final ManagedDatabase db = MantaroData.db();

                final Player player = ctx.getPlayer();
                final PlayerData playerData = player.getData();

                final SeasonPlayer seasonalPlayer = ctx.getSeasonPlayer();
                final SeasonalPlayerData seasonalPlayerData = seasonalPlayer.getData();

                final DBUser dbUser = ctx.getDBUser();
                final UserData userData = dbUser.getData();
                final Marriage marriage = userData.getMarriage();

                final Inventory inventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();

                Pickaxe item;
                int equipped = isSeasonal ?
                        seasonalPlayerData.getEquippedItems().of(PlayerEquipment.EquipmentType.PICK) :
                        userData.getEquippedItems().of(PlayerEquipment.EquipmentType.PICK);

                if (equipped == 0) {
                    ctx.sendLocalized("commands.mine.not_equipped", EmoteReference.ERROR);
                    return;
                }

                item = (Pickaxe) Items.fromId(equipped);

                if (!RatelimitUtils.handleIncreasingRatelimit(rateLimiter, user, ctx.getEvent(), languageContext, false))
                    return;

                long money = Math.max(30, random.nextInt(200)); //30 to 150 credits.

                // Add money buff to higher pickaxes.
                // TODO: Do this automatically? (we do it on fish)
                if (item == Items.STAR_PICKAXE || item == Items.COMET_PICKAXE)
                    money += random.nextInt(100);
                if (item == Items.SPARKLE_PICKAXE)
                    money += random.nextInt(300);
                if (item == Items.HELLFIRE_PICK)
                    money += random.nextInt(700);

                boolean waifuHelp = false;
                if (Items.handleEffect(PlayerEquipment.EquipmentType.POTION, userData.getEquippedItems(), Items.WAIFU_PILL, dbUser)) {
                    if (userData.getWaifus().entrySet().stream().anyMatch((w) -> w.getValue() > 10_000_000L)) {
                        money += Math.max(45, random.nextInt(200));
                        waifuHelp = true;
                    }
                }

                String reminder = random.nextInt(6) == 0 && item == Items.BROM_PICKAXE ? languageContext.get("commands.mine.reminder") : "";
                String message = String.format(languageContext.get("commands.mine.success") + reminder, item.getEmoji(), money, item.getName());

                boolean hasPotion = Items.handleEffect(PlayerEquipment.EquipmentType.POTION, userData.getEquippedItems(), Items.POTION_HASTE, dbUser);
                boolean petHelp = false;

                if(marriage != null) {
                    HousePet pet = marriage.getData().getPet();
                    if(pet != null) {
                        HousePet.ActivityResult ability = pet.handleAbility(HousePetType.HousePetAbility.CATCH, marriage.getData().getTimezone());
                        if(ability.passed()) {
                            petHelp = true;
                            var moneyIncrease = random.nextInt(pet.getType().getMaxCoinBuildup(pet.getLevel()));
                            money += moneyIncrease;
                            message += "\n" + pet.buildMessage(ability, languageContext, moneyIncrease, 0);
                        } else if (!ability.passed() && !ability.getLanguageString().isEmpty()) {
                            message += "\n" + pet.buildMessage(ability, languageContext, 0, 0);
                        }
                    }
                }

                //Diamond find
                if (random.nextInt(400) > (hasPotion || petHelp ? 290 : 350)) {
                    if (inventory.getAmount(Items.DIAMOND) == 5000) {
                        message += "\n" + languageContext.withRoot("commands", "mine.diamond.overflow");
                        money += Items.DIAMOND.getValue() * 0.9;
                    } else {
                        int amount = 1;

                        if (item == Items.STAR_PICKAXE || item == Items.COMET_PICKAXE)
                            amount += random.nextInt(2);
                        if (item == Items.SPARKLE_PICKAXE)
                            amount += random.nextInt(4);

                        inventory.process(new ItemStack(Items.DIAMOND, amount));
                        message += "\n" + EmoteReference.DIAMOND +
                                String.format(languageContext.withRoot("commands", "mine.diamond.success"), amount);
                    }

                    playerData.addBadgeIfAbsent(Badge.MINER);
                }

                //Gem find
                if (random.nextInt(400) > (hasPotion ? 278 : (petHelp ? 250 : 325))) {
                    List<Item> gem = Stream.of(Items.ALL)
                            .filter(i -> i.getItemType() == ItemType.MINE && !i.isHidden() && i.isSellable())
                            .collect(Collectors.toList());

                    //top notch handling for gems, 10/10 implementation -ign
                    ItemStack selectedGem = new ItemStack(gem.get(random.nextInt(gem.size())), Math.max(1, random.nextInt(5)));
                    Item itemGem = selectedGem.getItem();
                    if (inventory.getAmount(itemGem) + selectedGem.getAmount() >= 5000) {
                        message += "\n" + languageContext.withRoot("commands", "mine.gem.overflow");
                        money += itemGem.getValue() * 0.9;
                    } else {
                        inventory.process(selectedGem);
                        message += "\n" + EmoteReference.MEGA +
                                String.format(languageContext.withRoot("commands", "mine.gem.success"),
                                        itemGem.getEmoji() + " x" + selectedGem.getAmount()
                                );
                    }

                    if (waifuHelp)
                        message += "\n" + languageContext.get("commands.mine.waifu_help");

                    playerData.addBadgeIfAbsent(Badge.GEM_FINDER);
                }

                if(dbUser.isPremium() && money > 0) {
                    money += random.nextInt((int) money);
                }

                //Sparkle find
                if ((random.nextInt(400) > 395 && item == Items.COMET_PICKAXE) ||
                        (random.nextInt(400) > 390 && (item == Items.STAR_PICKAXE || item == Items.SPARKLE_PICKAXE || item == Items.HELLFIRE_PICK))) {
                    Item gem = Items.SPARKLE_FRAGMENT;
                    if (inventory.getAmount(gem) + 1 >= 5000) {
                        message += "\n" + languageContext.withRoot("commands", "mine.sparkle.overflow");
                        money += gem.getValue() * 0.9;
                    } else {
                        inventory.process(new ItemStack(gem, 1));
                        message += "\n" + EmoteReference.MEGA +
                                String.format(languageContext.withRoot("commands", "mine.sparkle.success"), gem.getEmoji());
                    }

                    playerData.addBadgeIfAbsent(Badge.GEM_FINDER);
                }

                PremiumKey key = db.getPremiumKey(dbUser.getData().getPremiumKey());
                if (random.nextInt(400) > 392) {
                    Item crate = (key != null && key.getDurationDays() > 1) ? Items.MINE_PREMIUM_CRATE : Items.MINE_CRATE;
                    if (inventory.getAmount(crate) + 1 > 5000) {
                        message += "\n" + languageContext.withRoot("commands", "mine.crate.overflow");
                    } else {
                        inventory.process(new ItemStack(crate, 1));
                        message += "\n" + EmoteReference.MEGA +
                                String.format(languageContext.withRoot("commands", "mine.crate.success"), crate.getEmoji(), crate.getName());
                    }
                }

                if (playerData.shouldSeeCampaign()) {
                    message += Campaign.PREMIUM.getStringFromCampaign(languageContext, dbUser.isPremium());
                    playerData.markCampaignAsSeen();
                }

                if (isSeasonal) {
                    seasonalPlayer.addMoney(money);
                    seasonalPlayer.saveAsync();
                } else {
                    playerData.incrementMiningExperience(random);
                    player.addMoney(money);
                }

                //Due to badges.
                player.save();
                if(marriage != null)
                    marriage.save();

                handleItemDurability(item, ctx, player, dbUser, seasonalPlayer, "commands.mine.autoequip.success", isSeasonal);

                ctx.send(message);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Mines minerals to gain some credits. A bit more lucrative than loot, but needs pickaxes.")
                        .setUsage("`~>mine` - Mines. You can gain minerals or mineral fragments by mining. " +
                                "This can used later on to cast rods or picks for better chances.")
                        .setSeasonal(true)
                        .build();
            }
        });
    }

    @Subscribe
    public void fish(CommandRegistry cr) {
        IncreasingRateLimiter fishRatelimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(2)
                .cooldown(4, TimeUnit.MINUTES)
                .maxCooldown(4, TimeUnit.MINUTES)
                .incrementDivider(10)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("fish")
                .premiumAware(true)
                .build();

        cr.register("fish", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                boolean isSeasonal = ctx.isSeasonal();
                I18nContext languageContext = ctx.getLanguageContext();

                Player player = ctx.getPlayer();
                PlayerData playerData = ctx.getPlayer().getData();

                SeasonPlayer seasonPlayer = ctx.getSeasonPlayer();
                DBUser dbUser = ctx.getDBUser();
                Marriage marriage = dbUser.getData().getMarriage();
                Inventory playerInventory = isSeasonal ? seasonPlayer.getInventory() : player.getInventory();
                FishRod item;

                int equipped = isSeasonal ?
                        //seasonal equipped
                        seasonPlayer.getData().getEquippedItems().of(PlayerEquipment.EquipmentType.ROD) :
                        //not seasonal
                        dbUser.getData().getEquippedItems().of(PlayerEquipment.EquipmentType.ROD);

                if (equipped == 0) {
                    ctx.sendLocalized("commands.fish.no_rod_equipped", EmoteReference.ERROR);
                    return;
                }

                //It can only be a rod, lol.
                item = (FishRod) Items.fromId(equipped);

                if (!RatelimitUtils.handleIncreasingRatelimit(fishRatelimiter, ctx.getAuthor(), ctx.getEvent(), languageContext, false))
                    return;

                //Level but starting at 0.
                int nominalLevel = item.getLevel() - 3;
                String extraMessage = "";

                int chance = random.nextInt(100);

                if (chance < 10) {
                    //Here your fish rod got dusty. Yes, on the sea.
                    int level = dbUser.getData().increaseDustLevel(random.nextInt(4));
                    ctx.sendLocalized("commands.fish.dust", EmoteReference.TALKING, level);
                    dbUser.save();

                    handleItemDurability(item, ctx, player, dbUser, seasonPlayer, "commands.fish.autoequip.success", isSeasonal);
                    return;
                } else if (chance < 35) {
                    //Here you found trash.
                    List<Item> common = Stream.of(Items.ALL)
                            .filter(i -> i.getItemType() == ItemType.COMMON && !i.isHidden() && i.isSellable() && i.getValue() < 45)
                            .collect(Collectors.toList());

                    Item selected = common.get(random.nextInt(common.size()));
                    if (playerInventory.getAmount(selected) >= 5000) {
                        ctx.sendLocalized("commands.fish.trash.overflow", EmoteReference.SAD);

                        handleItemDurability(item, ctx, player, dbUser, seasonPlayer, "commands.fish.autoequip.success", isSeasonal);
                        return;
                    }

                    playerInventory.process(new ItemStack(selected, 1));
                    ctx.sendLocalized("commands.fish.trash.success", EmoteReference.EYES, selected.getEmoji());
                } else {
                    //Here you actually caught fish, congrats.
                    List<Item> fish = Stream.of(Items.ALL)
                            .filter(i -> i.getItemType() == ItemType.FISHING && !i.isHidden() && i.isSellable())
                            .collect(Collectors.toList());
                    RandomCollection<Item> fishItems = new RandomCollection<>();


                    int money = 0;
                    boolean buff = Items.handleEffect(PlayerEquipment.EquipmentType.BUFF, dbUser.getData().getEquippedItems(), Items.FISHING_BAIT, dbUser);
                    int amount = buff ? Math.max(1, random.nextInt(item.getLevel() + 4)) : Math.max(1, random.nextInt(item.getLevel()));
                    if (nominalLevel >= 2)
                        amount += random.nextInt(4);

                    fish.forEach((i1) -> fishItems.add(3, i1));

                    if(marriage != null) {
                        HousePet pet = marriage.getData().getPet();
                        if(pet != null) {
                            HousePet.ActivityResult ability = pet.handleAbility(HousePetType.HousePetAbility.FISH, marriage.getData().getTimezone());
                            if(ability.passed()) {
                                var amountIncrease = pet.getType().getMaxItemBuildup(pet.getLevel());
                                amount += amountIncrease;
                                var moneyIncrease = random.nextInt(pet.getType().getMaxCoinBuildup(pet.getLevel()));
                                money += moneyIncrease;
                                extraMessage += "\n" + pet.buildMessage(ability, languageContext, moneyIncrease, amountIncrease);
                            } else if (!ability.passed() && !ability.getLanguageString().isEmpty()) {
                                extraMessage += "\n" + pet.buildMessage(ability, languageContext, 0, 0);
                            }
                        }
                    }

                    //Basically more chance if you have a better rod.
                    if (chance > (75 - nominalLevel))
                        money = Math.max(5, random.nextInt(130 + (3 * nominalLevel)));

                    //START OF WAIFU HELP IMPLEMENTATION
                    boolean waifuHelp = false;
                    if (Items.handleEffect(PlayerEquipment.EquipmentType.POTION, dbUser.getData().getEquippedItems(), Items.WAIFU_PILL, dbUser)) {
                        if (dbUser.getData().getWaifus().entrySet().stream().anyMatch((w) -> w.getValue() > 10_000_000L)) {
                            money += Math.max(10, random.nextInt(100));
                            waifuHelp = true;
                        }
                    }
                    //END OF WAIFU HELP IMPLEMENTATION

                    //START OF FISH LOOT CRATE HANDLING
                    if (random.nextInt(400) > 380) {
                        Item crate = dbUser.isPremium() ? Items.FISH_PREMIUM_CRATE : Items.FISH_CRATE;
                        if (playerInventory.getAmount(crate) >= 5000) {
                            extraMessage += "\n" + languageContext.get("commands.fish.crate.overflow");
                        } else {
                            playerInventory.process(new ItemStack(crate, 1));
                            extraMessage += "\n" + EmoteReference.MEGA +
                                    String.format(languageContext.get("commands.fish.crate.success"), crate.getEmoji(), crate.getName());
                        }
                    }
                    //END OF FISH LOOT CRATE HANDLING

                    if ((item == Items.SPARKLE_ROD || item == Items.HELLFIRE_ROD) && random.nextInt(30) > 20) {
                        if (random.nextInt(100) > 96) {
                            fish.addAll(Stream.of(Items.ALL)
                                    .filter(i -> i.getItemType() == ItemType.FISHING_RARE && !i.isHidden() && i.isSellable())
                                    .collect(Collectors.toList())
                            );
                        }

                        playerInventory.process(new ItemStack(Items.SHARK, 1));
                        extraMessage += "\n" + EmoteReference.MEGA + String.format(languageContext.get("commands.fish.shark_success"), Items.SHARK.getEmoji());
                        player.getData().setSharksCaught(player.getData().getSharksCaught() + 1);
                    }

                    // START OF ITEM ADD HANDLING
                    List<ItemStack> list = new ArrayList<>(amount);
                    boolean overflow = false;
                    for (int i = 0; i < amount; i++) {
                        Item it = fishItems.next();
                        if (playerInventory.getAmount(it) >= 5000) {
                            overflow = true;
                            continue;
                        }

                        list.add(new ItemStack(it, 1));
                    }
                    // END OF ITEM ADD HANDLING

                    if (buff)
                        extraMessage += "\n" + languageContext.get("commands.fish.bait");

                    if (overflow)
                        extraMessage += "\n" + String.format(languageContext.get("commands.fish.overflow"), EmoteReference.SAD);

                    List<ItemStack> reducedList = ItemStack.reduce(list);
                    playerInventory.process(reducedList);
                    if (isSeasonal) {
                        seasonPlayer.addMoney(money);
                    } else {
                        player.addMoney(money);
                        player.getData().incrementFishingExperience(random);
                    }

                    String itemDisplay = ItemStack.toString(reducedList);
                    boolean foundFish = !reducedList.isEmpty();
                    //END OF ITEM ADDING HANDLING

                    //Add fisher badge if the player found fish successfully.
                    if (foundFish)
                        player.getData().addBadgeIfAbsent(Badge.FISHER);

                    if (nominalLevel >= 3 && random.nextInt(110) > 90) {
                        playerInventory.process(new ItemStack(Items.SHELL, 1));
                        extraMessage += "\n" + EmoteReference.MEGA + String.format(languageContext.get("commands.fish.fossil_success"), Items.SHELL.getEmoji());
                    }


                    if(dbUser.isPremium() && money > 0) {
                        money += random.nextInt(money);
                    }

                    if (playerData.shouldSeeCampaign()) {
                        extraMessage += Campaign.PREMIUM.getStringFromCampaign(languageContext, dbUser.isPremium());
                        playerData.markCampaignAsSeen();
                    }


                    //START OF REPLY HANDLING
                    //Didn't find a thingy thing.
                    if (money == 0 && !foundFish) {
                        int level = dbUser.getData().increaseDustLevel(random.nextInt(4));
                        ctx.sendLocalized("commands.fish.dust", EmoteReference.TALKING, level);
                        dbUser.save();

                        handleItemDurability(item, ctx, player, dbUser, seasonPlayer, "commands.fish.autoequip.success", isSeasonal);
                        return;
                    }

                    //if there's money, but not fish
                    if (money > 0 && !foundFish) {
                        ctx.sendFormat(languageContext.get("commands.fish.success_money_noitem") + extraMessage, item.getEmoji(), money);
                    } else if (foundFish && money == 0) { //there's fish, but no money
                        ctx.sendFormat(languageContext.get("commands.fish.success") + extraMessage, item.getEmoji(), itemDisplay);
                    } else if (money > 0) { //there's money and fish
                        ctx.sendFormat(languageContext.get("commands.fish.success_money") + extraMessage,
                                item.getEmoji(), itemDisplay, money, (waifuHelp ? "\n" + languageContext.get("commands.fish.waifu_help") : "")
                        );
                    }
                    //END OF REPLY HANDLING
                }

                //Save all changes to the player object.
                player.save();
                if (isSeasonal)
                    seasonPlayer.save();

                // Save pet stats.
                if(marriage != null)
                    marriage.save();

                handleItemDurability(item, ctx, player, dbUser, seasonPlayer, "commands.fish.autoequip.success", isSeasonal);
            }


            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Starts a fishing session.")
                        .setUsage("`~>fish` - Starts fishing." +
                                " You can gain credits and fish items by fishing, which can be used later on for casting.")
                        .setSeasonal(true)
                        .build();
            }
        });
    }

    @Subscribe
    public void chop(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(3)
                .cooldown(4, TimeUnit.MINUTES)
                .maxCooldown(4, TimeUnit.MINUTES)
                .incrementDivider(10)
                .premiumAware(true)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("chop")
                .build();

        // TODO: Loot crates, Rare items
        cr.register("chop", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var isSeasonal = ctx.isSeasonal();
                var languageContext = ctx.getLanguageContext();

                var player = ctx.getPlayer();
                var playerData = ctx.getPlayer().getData();

                var seasonPlayer = ctx.getSeasonPlayer();
                var dbUser = ctx.getDBUser();
                var userData = dbUser.getData();
                var marriage = userData.getMarriage();
                var playerInventory = isSeasonal ? seasonPlayer.getInventory() : player.getInventory();
                var extraMessage = "\n";

                Axe item;
                var equipped = isSeasonal ?
                        //seasonal equipped
                        seasonPlayer.getData().getEquippedItems().of(PlayerEquipment.EquipmentType.AXE) :
                        //not seasonal
                        userData.getEquippedItems().of(PlayerEquipment.EquipmentType.AXE);

                if (equipped == 0) {
                    ctx.sendLocalized("commands.chop.not_equipped", EmoteReference.ERROR);
                    return;
                }

                item = (Axe) Items.fromId(equipped);

                if (!RatelimitUtils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx.getEvent(), languageContext, false))
                    return;

                var chance = random.nextInt(100);
                var hasPotion = Items.handleEffect(PlayerEquipment.EquipmentType.POTION, userData.getEquippedItems(), Items.POTION_HASTE, dbUser);
                if(hasPotion)
                    chance += 10;

                if(chance < 10) {
                    // Found nothing.
                    int level = userData.increaseDustLevel(random.nextInt(5));
                    dbUser.save();
                    // Process axe durability.
                    handleItemDurability(item, ctx, player, dbUser, seasonPlayer, "commands.chop.autoequip.success", isSeasonal);

                    ctx.sendLocalized("commands.chop.dust", EmoteReference.SAD, level);
                } else {
                    var money = chance > 50 ? random.nextInt(100) : 0;
                    var amount = random.nextInt(8);

                    if(marriage != null) {
                        HousePet pet = marriage.getData().getPet();
                        if(pet != null) {
                            HousePet.ActivityResult ability = pet.handleAbility(HousePetType.HousePetAbility.CHOP, marriage.getData().getTimezone());
                            if(ability.passed()) {
                                var amountIncrease = pet.getType().getMaxItemBuildup(pet.getLevel());
                                amount += amountIncrease;
                                var moneyIncrease = random.nextInt(pet.getType().getMaxCoinBuildup(pet.getLevel()));
                                money += moneyIncrease;
                                extraMessage += "\n" + pet.buildMessage(ability, languageContext, moneyIncrease, amountIncrease);
                            } else if (!ability.passed() && !ability.getLanguageString().isEmpty()) {
                                extraMessage += "\n" + pet.buildMessage(ability, languageContext, 0, 0);
                            }
                        }
                    }

                    if(hasPotion)
                        amount += 3;

                    // Add money buff to higher axes.
                    // TODO: Do this automatically? (we do it on fish)
                    if (item == Items.STAR_AXE || item == Items.COMET_AXE)
                        money += random.nextInt(100);
                    if (item == Items.SPARKLE_AXE)
                        money += random.nextInt(300);
                    if (item == Items.HELLFIRE_AXE)
                        money += random.nextInt(700);


                    // ---- Start of drop handling.
                    RandomCollection<Item> items = new RandomCollection<>();
                    var toDrop = handleChopDrop();
                    toDrop.forEach(i -> items.add(3, i));
                    List<Item> list = new ArrayList<>(amount);
                    for (int i = 0; i < amount; i++) {
                        list.add(items.next());
                    }

                    ArrayList<ItemStack> ita = new ArrayList<>();
                    list.forEach(it -> ita.add(new ItemStack(it, 1)));
                    boolean found = !ita.isEmpty();

                    // Make so it drops some decent amount of wood lol
                    if(ita.stream().anyMatch(is -> is.getItem() == Items.WOOD)) {
                        ita.add(new ItemStack(Items.WOOD, Math.max(1, random.nextInt(7))));
                    } else if (found) {
                        // Guarantee at least one wood.
                        ita.add(new ItemStack(Items.WOOD, 1));
                    }
                    // ---- End of drop handling.


                    // Reduce item stacks (aka join them) and process it.
                    List<ItemStack> reducedStack = ItemStack.reduce(ita);
                    var itemDisplay = ItemStack.toString(reducedStack);
                    playerInventory.process(reducedStack);

                    // Add money
                    if (isSeasonal) {
                        seasonPlayer.addMoney(money);
                    } else {
                        player.addMoney(money);
                        player.getData().incrementChopExperience(random);
                    }

                    // Ah yes, sellout
                    if(dbUser.isPremium() && money > 0)
                        money += random.nextInt(money);

                    if(found)
                        playerData.addBadgeIfAbsent(Badge.CHOPPER);

                    if (playerData.shouldSeeCampaign()) {
                        extraMessage += Campaign.PREMIUM.getStringFromCampaign(languageContext, dbUser.isPremium());
                        playerData.markCampaignAsSeen();
                    }

                    // Show a message depending on the outcome.
                    if(money > 0 && !found) {
                        ctx.sendFormat(languageContext.get("commands.chop.success_money_noitem") + extraMessage, item.getEmoji(), money);
                    } else if (found && money == 0) {
                        ctx.sendFormat(languageContext.get("commands.chop.success_only_item") + extraMessage, item.getEmoji(), itemDisplay);
                    } else if (!found && money == 0) {
                        // This doesn't actually increase the dust level, though.
                        var level = userData.getDustLevel();
                        ctx.sendLocalized("commands.chop.dust", EmoteReference.SAD, level);
                    } else {
                        ctx.sendFormat(languageContext.get("commands.chop.success") + extraMessage, item.getEmoji(), itemDisplay, money);
                    }

                    player.save();

                    // Save pet stuff.
                    if(marriage != null)
                        marriage.save();

                    // Process axe durability.
                    handleItemDurability(item, ctx, player, dbUser, seasonPlayer, "commands.chop.autoequip.success", isSeasonal);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Starts a chopping session.")
                        .setUsage("`~>chop` - Starts chopping trees." +
                                " You can gain credits and items by chopping, which can be used later on for casting, specially tools.")
                        .setSeasonal(true)
                        .build();
            }
        });
    }

    private List<Item> handleChopDrop() {
        List<Item> all = Arrays.stream(Items.ALL).filter(i -> i.getItemType() == ItemType.CHOP_DROP).collect(Collectors.toList());
        return all.stream().sorted(Comparator.comparingLong(Item::getValue)).collect(Collectors.toList());
    }

    private void handleItemDurability(Item item, Context ctx, Player p, DBUser u, SeasonPlayer sp, String i18n, boolean isSeasonal) {
        Pair<Boolean, Player> breakage = handleDurability(ctx, item, p, u, sp, isSeasonal);
        if (!breakage.getKey())
            return;

        //We need to get this again since reusing the old ones will cause :fire:
        Player pl = breakage.getValue();
        Inventory inv = pl.getInventory();

        if(u.getData().isAutoEquip() && inv.containsItem(item)) {
            u.getData().getEquippedItems().equipItem(item);
            inv.process(new ItemStack(item, -1));

            pl.save();
            u.save();

            ctx.sendLocalized(i18n, EmoteReference.CORRECT, item.getName());
        }
    }
}
