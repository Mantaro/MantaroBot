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
import net.kodehawa.mantarobot.commands.currency.item.*;
import net.kodehawa.mantarobot.commands.currency.item.special.gems.CastedGem;
import net.kodehawa.mantarobot.commands.currency.item.special.gems.Gem;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.attributes.GemType;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Axe;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.FishRod;
import net.kodehawa.mantarobot.commands.currency.item.special.tools.Pickaxe;
import net.kodehawa.mantarobot.commands.currency.pets.HousePet;
import net.kodehawa.mantarobot.commands.currency.pets.HousePetType;
import net.kodehawa.mantarobot.commands.currency.pets.PetChoice;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.RandomCollection;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.campaign.Campaign;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Module
public class CurrencyActionCmds {
    private static final SecureRandom random = new SecureRandom();

    @Subscribe
    public void mine(CommandRegistry cr) {
        cr.register("mine", new SimpleCommand(CommandCategory.CURRENCY) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .limit(1)
                    .spamTolerance(3)
                    .cooldown(5, TimeUnit.MINUTES)
                    .maxCooldown(5, TimeUnit.MINUTES)
                    .incrementDivider(10)
                    .premiumAware(true)
                    .pool(MantaroData.getDefaultJedisPool())
                    .prefix("mine")
                    .build();

            @Override
            protected void call(Context ctx, String content, String[] args) {
                final var isSeasonal = ctx.isSeasonal();
                final var languageContext = ctx.getLanguageContext();

                final var player = ctx.getPlayer();
                final var playerData = player.getData();
                final var seasonalPlayer = ctx.getSeasonPlayer();
                final var seasonalPlayerData = seasonalPlayer.getData();

                final var dbUser = ctx.getDBUser();
                final var userData = dbUser.getData();
                final var marriage = ctx.getMarriage(userData);

                final var inventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();

                var equipped = isSeasonal ?
                        seasonalPlayerData.getEquippedItems().of(PlayerEquipment.EquipmentType.PICK) :
                        userData.getEquippedItems().of(PlayerEquipment.EquipmentType.PICK);

                if (equipped == 0) {
                    ctx.sendLocalized("commands.mine.not_equipped", EmoteReference.ERROR);
                    return;
                }

                if (!RatelimitUtils.ratelimit(rateLimiter, ctx, false)) {
                    return;
                }

                var message = "";
                var waifuHelp = false;
                var petHelp = false;

                var item = (Pickaxe) ItemHelper.fromId(equipped);
                var money = Math.max(30, random.nextInt(200)); // 30 to 150 credits.
                var moneyIncrease = item.getMoneyIncrease() <= 0 ? 1 : item.getMoneyIncrease();
                money += Math.max(moneyIncrease / 2, random.nextInt(moneyIncrease));

                if (ItemHelper.handleEffect(PlayerEquipment.EquipmentType.POTION, userData.getEquippedItems(), ItemReference.WAIFU_PILL, dbUser)) {
                    final var waifus = userData.getWaifus().entrySet();
                    if (waifus.stream().anyMatch((w) -> w.getValue() > 20_000L)) {
                        money += Math.max(20, random.nextInt(100));
                        waifuHelp = true;
                    }
                }

                var reminder = random.nextInt(6) == 0 && item == ItemReference.BROM_PICKAXE ?
                        languageContext.get("commands.mine.reminder") : "";

                var hasPotion = ItemHelper.handleEffect(
                        PlayerEquipment.EquipmentType.POTION, userData.getEquippedItems(),
                        ItemReference.POTION_HASTE, dbUser
                );

                HousePet pet = null;
                if (playerData.getActiveChoice(marriage) == PetChoice.MARRIAGE) {
                    if (marriage != null && marriage.getData().getPet() != null) {
                        pet = marriage.getData().getPet();
                    }
                } else {
                    pet = playerData.getPet();
                }

                if (pet != null) {
                    var rewards = handlePetBuff(pet, HousePetType.HousePetAbility.CATCH, languageContext, false);
                    money += rewards.getMoney();
                    message += rewards.getResult();

                    if (rewards.getMoney() > 0) {
                        petHelp = true;
                    }
                }

                // Diamond find
                var chance = 350;
                if (hasPotion || petHelp) {
                    chance = 290;
                }

                if (petHelp && hasPotion) {
                    chance = 240;
                }

                if (random.nextInt(400) >= chance) {
                    var amount = 1 + random.nextInt(item.getDiamondIncrease());
                    if (inventory.getAmount(ItemReference.DIAMOND) + amount > 5000) {
                        message += "\n" + languageContext.get("commands.mine.diamond.overflow").formatted(amount);
                        money += ItemReference.DIAMOND.getValue() * 0.9;
                    } else {
                        inventory.process(new ItemStack(ItemReference.DIAMOND, amount));
                        message += "\n" + EmoteReference.DIAMOND +
                                languageContext.get("commands.mine.diamond.success").formatted(amount);
                    }

                    playerData.addBadgeIfAbsent(Badge.MINER);
                }

                // Gem find
                var gemChance = item.getGemLuck();
                if (hasPotion) {
                    gemChance -= 10;
                } else if (petHelp) {
                    gemChance -= pet.getType().getGemLuckIncrease();
                }

                if (petHelp && hasPotion) {
                    gemChance -= pet.getType().getGemLuckIncrease() + 10;
                }

                if (random.nextInt(400) >= gemChance) {
                    List<Item> gem = Stream.of(ItemReference.ALL)
                            .filter(g -> {
                                if (g instanceof Gem) {
                                    return true;
                                } else if (g instanceof CastedGem) {
                                    return random.nextBoolean();
                                } else {
                                    return false;
                                }
                            })
                            // Give less probabilities of getting a rock because it can get annoying (lol)
                            .filter(i -> random.nextBoolean() || i != ItemReference.ROCK)
                            .collect(Collectors.toList());

                    final var itemGem = gem.get(random.nextInt(gem.size()));
                    final var isCastedGem = itemGem instanceof CastedGem;
                    final var isMoon = itemGem == ItemReference.MOON_RUNES;
                    final var selectedGem = new ItemStack(itemGem, Math.max(1, isCastedGem || isMoon ? random.nextInt(3) : random.nextInt(5)));

                    ItemStack extraGem = null;
                    Item extraItem = null;

                    // Extra chance of gettting a Gem Fragment or Moon Gem in case you didn't get a Gem already.
                    if (random.nextBoolean() && (!isCastedGem && ((Gem)itemGem).getType() != GemType.GEM)) {
                        List<Item> extra = Stream.of(ItemReference.ALL)
                                .filter(g -> g instanceof Gem || g instanceof CastedGem)
                                .filter(i -> {
                                    if (i instanceof Gem) {
                                        return ((Gem) i).getType() == GemType.GEM;
                                    } else {
                                        return true;
                                    }
                                })
                                .collect(Collectors.toList());

                        extraItem = extra.get(random.nextInt(extra.size()));
                        extraGem = new ItemStack(extraItem,
                                extraItem instanceof CastedGem ? Math.max(1, random.nextInt(2)) : Math.max(1, random.nextInt(3))
                        );
                    }

                    if (extraGem != null && (inventory.getAmount(extraItem) + extraGem.getAmount() >= 5000)) {
                        extraGem = null;
                    }

                    if (inventory.getAmount(itemGem) + selectedGem.getAmount() >= 5000) {
                        message += "\n" + languageContext.get("commands.mine.gem.overflow")
                                .formatted(itemGem.getEmoji() + " x" + selectedGem.getAmount());
                        money += itemGem.getValue() * 0.9;
                    } else {
                        inventory.process(selectedGem);

                        if (extraGem != null) {
                            inventory.process(extraGem);
                            message += "\n" + EmoteReference.MEGA + languageContext.get("commands.mine.gem.success_extra")
                                    .formatted(
                                            itemGem.getEmoji() + " x" + selectedGem.getAmount(),
                                            extraItem.getEmoji() + " x" + extraGem.getAmount()
                                    );
                        } else {
                            message += "\n" + EmoteReference.MEGA + languageContext.get("commands.mine.gem.success")
                                    .formatted(itemGem.getEmoji() + " x" + selectedGem.getAmount());
                        }
                    }

                    if (waifuHelp) {
                        message += "\n" + languageContext.get("commands.mine.waifu_help");
                    }

                    playerData.addBadgeIfAbsent(Badge.GEM_FINDER);
                }

                var bonus = money;
                if (random.nextBoolean()) {
                    bonus = money / 2;
                }

                if (dbUser.isPremium() && money > 0 && bonus > 0) {
                    money += random.nextInt(bonus);
                }

                // Sparkle find
                var sparkleChance = item.getSparkleLuck();
                if (random.nextInt(400) >= sparkleChance) {
                    var gem = ItemReference.SPARKLE_FRAGMENT;

                    if (inventory.getAmount(gem) + 1 >= 5000) {
                        message += "\n" + languageContext.get("commands.mine.sparkle.overflow");
                        money += gem.getValue() * 0.9;
                    } else {
                        inventory.process(new ItemStack(gem, 1));
                        message += "\n" + EmoteReference.MEGA +
                                languageContext.get("commands.mine.sparkle.success").formatted(gem.getEmoji());
                    }

                    playerData.addBadgeIfAbsent(Badge.GEM_FINDER);
                }

                if (random.nextInt(400) >= 392) {
                    var crate = dbUser.isPremium() ? ItemReference.MINE_PREMIUM_CRATE : ItemReference.MINE_CRATE;

                    if (inventory.getAmount(crate) + 1 > 5000) {
                        message += "\n" + languageContext.get("commands.mine.crate.overflow");
                    } else {
                        inventory.process(new ItemStack(crate, 1));
                        message += "\n" + EmoteReference.MEGA + languageContext.get("commands.mine.crate.success")
                                .formatted(crate.getEmoji(), crate.getName());
                    }
                }

                if (playerData.shouldSeeCampaign()) {
                    message += Campaign.PREMIUM.getStringFromCampaign(languageContext, dbUser.isPremium());
                    playerData.markCampaignAsSeen();
                }

                if (isSeasonal) {
                    seasonalPlayer.addMoney(money);
                    seasonalPlayer.saveUpdating();
                } else {
                    playerData.incrementMiningExperience(random);
                    player.addMoney(money);
                }

                handlePetBadges(player, marriage, pet);
                player.saveUpdating();

                if (marriage != null) {
                    marriage.saveUpdating();
                }

                ItemHelper.handleItemDurability(item, ctx, player, dbUser, seasonalPlayer, "commands.mine.autoequip.success", isSeasonal);
                message += "\n\n" + (languageContext.get("commands.mine.success") + reminder).formatted(item.getEmojiDisplay(), money, item.getName());

                ctx.send(message);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Mines minerals to gain some credits. A bit more lucrative than loot, but needs pickaxes.")
                        .setUsage("""
                                  `~>mine` - Mines. You can gain minerals or mineral fragments by mining.
                                  This can used later on to cast rods or picks for better chances.
                                  """
                        )
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
                final var isSeasonal = ctx.isSeasonal();
                final var languageContext = ctx.getLanguageContext();

                final var player = ctx.getPlayer();
                final var playerData = player.getData();

                final var seasonPlayer = ctx.getSeasonPlayer();
                final var dbUser = ctx.getDBUser();
                final var userData = dbUser.getData();
                final var marriage = ctx.getMarriage(userData);

                final var playerInventory = isSeasonal ? seasonPlayer.getInventory() : player.getInventory();

                FishRod item;

                var equipped = isSeasonal ?
                        //seasonal equipped
                        seasonPlayer.getData().getEquippedItems().of(PlayerEquipment.EquipmentType.ROD) :
                        //not seasonal
                        userData.getEquippedItems().of(PlayerEquipment.EquipmentType.ROD);

                if (equipped == 0) {
                    ctx.sendLocalized("commands.fish.no_rod_equipped", EmoteReference.ERROR);
                    return;
                }

                //It can only be a rod, lol.
                item = (FishRod) ItemHelper.fromId(equipped);

                if (!RatelimitUtils.ratelimit(fishRatelimiter, ctx, false)) {
                    return;
                }

                //Level but starting at 0.
                var nominalLevel = item.getLevel() - 3;
                var extraMessage = "";
                var chance = random.nextInt(100);
                var buff = ItemHelper.handleEffect(
                        PlayerEquipment.EquipmentType.BUFF,
                        userData.getEquippedItems(),
                        ItemReference.FISHING_BAIT, dbUser
                );

                if (buff) {
                    chance += 6;
                }

                if (chance < 10) {
                    //Here your fish rod got dusty. Yes, on the sea.
                    var level = userData.increaseDustLevel(random.nextInt(4));
                    ctx.sendLocalized("commands.fish.dust", EmoteReference.TALKING, level);
                    dbUser.saveUpdating();

                    ItemHelper.handleItemDurability(item, ctx, player, dbUser, seasonPlayer, "commands.fish.autoequip.success", isSeasonal);
                    return;
                } else if (chance < 20) {
                    //Here you found trash.
                    List<Item> common = Stream.of(ItemReference.ALL)
                            .filter(i -> i.getItemType() == ItemType.COMMON && !i.isHidden() && i.isSellable() && i.getValue() < 45)
                            .collect(Collectors.toList());

                    var selected = common.get(random.nextInt(common.size()));
                    if (playerInventory.getAmount(selected) >= 5000) {
                        ctx.sendLocalized("commands.fish.trash.overflow", EmoteReference.SAD);

                        ItemHelper.handleItemDurability(item, ctx, player, dbUser, seasonPlayer, "commands.fish.autoequip.success", isSeasonal);
                        return;
                    }

                    playerInventory.process(new ItemStack(selected, 1));
                    ctx.sendLocalized("commands.fish.trash.success", EmoteReference.EYES, selected.getEmoji());
                } else {
                    // Here you actually caught fish, congrats.
                    List<Item> fish = Stream.of(ItemReference.ALL)
                            .filter(i -> i.getItemType() == ItemType.FISHING && !i.isHidden() && i.isSellable())
                            .collect(Collectors.toList());

                    RandomCollection<Item> fishItems = new RandomCollection<>();
                    var money = 0;
                    var amount = Math.max(1, random.nextInt(item.getLevel()));

                    if (buff) {
                        amount = Math.max(1, random.nextInt(item.getLevel() + 4));
                        extraMessage += "\n" + languageContext.get("commands.fish.bait");
                    }

                    if (nominalLevel >= 2) {
                        amount += random.nextInt(4);
                    }

                    fish.forEach((i1) -> fishItems.add(3, i1));
                    HousePet pet = null;
                    if (playerData.getActiveChoice(marriage) == PetChoice.MARRIAGE) {
                        if (marriage != null && marriage.getData().getPet() != null) {
                            pet = marriage.getData().getPet();
                        }
                    } else {
                        pet = playerData.getPet();
                    }

                    if (pet != null) {
                        HousePet.ActivityReward rewards = handlePetBuff(pet, HousePetType.HousePetAbility.FISH, languageContext);
                        amount += rewards.getItems();
                        money += rewards.getMoney();
                        extraMessage += "\n" + rewards.getResult();
                    }

                    // Basically more chance if you have a better rod.
                    if (chance > (70 - nominalLevel)) {
                        if (nominalLevel >= 20) {
                            var moneyAmount = 300 + (8 * nominalLevel);
                            money += Math.max(140, random.nextInt(moneyAmount));
                        } else {
                            var moneyAmount = 150 + (4 * nominalLevel);
                            money += Math.max(25, random.nextInt(moneyAmount));
                        }
                    }

                    // START OF WAIFU HELP IMPLEMENTATION
                    boolean waifuHelp = false;
                    if (ItemHelper.handleEffect(PlayerEquipment.EquipmentType.POTION, userData.getEquippedItems(), ItemReference.WAIFU_PILL, dbUser)) {
                        if (userData.getWaifus().entrySet().stream().anyMatch((w) -> w.getValue() > 20_000L)) {
                            money += Math.max(10, random.nextInt(150));
                            waifuHelp = true;
                        }
                    }
                    // END OF WAIFU HELP IMPLEMENTATION

                    // START OF FISH LOOT CRATE HANDLING
                    if (random.nextInt(400) > 380) {
                        var crate = dbUser.isPremium() ? ItemReference.FISH_PREMIUM_CRATE : ItemReference.FISH_CRATE;

                        if (playerInventory.getAmount(crate) >= 5000) {
                            extraMessage += "\n" + languageContext.get("commands.fish.crate.overflow");
                        } else {
                            playerInventory.process(new ItemStack(crate, 1));
                            extraMessage += "\n" + EmoteReference.MEGA +
                                    languageContext.get("commands.fish.crate.success").formatted(crate.getEmoji(), crate.getName());
                        }
                    }
                    // END OF FISH LOOT CRATE HANDLING

                    if ((item == ItemReference.SPARKLE_ROD || item == ItemReference.HELLFIRE_ROD) && random.nextInt(30) > 20) {
                        if (random.nextInt(100) > 96) {
                            fish.addAll(Stream.of(ItemReference.ALL)
                                    .filter(i -> i.getItemType() == ItemType.FISHING_RARE && !i.isHidden() && i.isSellable())
                                    .collect(Collectors.toList())
                            );
                        }

                        playerInventory.process(new ItemStack(ItemReference.SHARK, 1));
                        extraMessage += "\n" + EmoteReference.MEGA +
                                languageContext.get("commands.fish.shark_success").formatted(ItemReference.SHARK.getEmoji());

                        player.getData().setSharksCaught(player.getData().getSharksCaught() + 1);
                    }

                    List<ItemStack> list = new ArrayList<>(amount);
                    var overflow = false;

                    for (int i = 0; i < amount; i++) {
                        Item it = fishItems.next();
                        if (playerInventory.getAmount(it) >= 5000) {
                            overflow = true;
                            continue;
                        }

                        list.add(new ItemStack(it, 1));
                    }
                    // END OF ITEM ADD HANDLING

                    if (overflow) {
                        extraMessage += "\n" + languageContext.get("commands.fish.overflow")
                                .formatted(EmoteReference.SAD);
                    }

                    List<ItemStack> reducedList = ItemStack.reduce(list);
                    playerInventory.process(reducedList);

                    if (isSeasonal) {
                        seasonPlayer.addMoney(money);
                    } else {
                        player.addMoney(money);
                        player.getData().incrementFishingExperience(random);
                    }

                    var itemDisplay = ItemStack.toString(reducedList);
                    var foundFish = !reducedList.isEmpty();

                    //Add fisher badge if the player found fish successfully.
                    if (foundFish) {
                        player.getData().addBadgeIfAbsent(Badge.FISHER);
                    }

                    handlePetBadges(player, marriage, pet);

                    if (nominalLevel >= 3 && random.nextInt(110) > 90) {
                        playerInventory.process(new ItemStack(ItemReference.SHELL, 1));
                        extraMessage += "\n" + EmoteReference.MEGA +
                                languageContext.get("commands.fish.fossil_success")
                                        .formatted(ItemReference.SHELL.getEmoji());
                    }


                    var bonus = money;
                    if (random.nextBoolean()) {
                        bonus = money / 2;
                    }

                    if (dbUser.isPremium() && money > 0 && bonus > 0) {
                        money += random.nextInt(bonus);
                    }

                    if (playerData.shouldSeeCampaign()) {
                        extraMessage += Campaign.PREMIUM.getStringFromCampaign(languageContext, dbUser.isPremium());
                        playerData.markCampaignAsSeen();
                    }

                    //START OF REPLY HANDLING
                    //Didn't find a thingy thing.
                    if (money == 0 && !foundFish) {
                        int level = userData.increaseDustLevel(random.nextInt(4));
                        ctx.sendLocalized("commands.fish.dust", EmoteReference.TALKING, level);
                        dbUser.saveUpdating();

                        ItemHelper.handleItemDurability(item, ctx, player, dbUser, seasonPlayer,
                                "commands.fish.autoequip.success", isSeasonal
                        );
                        return;
                    }

                    //if there's money, but not fish
                    if (money > 0 && !foundFish) {
                        ctx.sendFormat(languageContext.get("commands.fish.success_money_noitem") + extraMessage, item.getEmojiDisplay(), money, item.getName());
                    } else if (foundFish && money == 0) { //there's fish, but no money
                        ctx.sendFormat(languageContext.get("commands.fish.success") + extraMessage, item.getEmojiDisplay(), itemDisplay, item.getName());
                    } else if (money > 0) { //there's money and fish
                        ctx.sendFormat(languageContext.get("commands.fish.success_money") + extraMessage,
                                item.getEmojiDisplay(), itemDisplay, money, item.getName(), (waifuHelp ? "\n" + languageContext.get("commands.fish.waifu_help") : "")
                        );
                    }
                    //END OF REPLY HANDLING
                }

                //Save all changes to the player object.
                player.saveUpdating();

                if (isSeasonal) {
                    seasonPlayer.saveUpdating();
                }

                // Save pet stats.
                if (marriage != null) {
                    marriage.saveUpdating();
                }

                ItemHelper.handleItemDurability(item, ctx, player, dbUser, seasonPlayer, "commands.fish.autoequip.success", isSeasonal);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Starts a fishing session.")
                        .setUsage("""
                                  `~>fish` - Starts fishing.
                                  You can gain credits and fish items by fishing, which can be used later on for casting.
                                  """
                        )
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

        // TODO: rare items
        cr.register("chop", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                final var isSeasonal = ctx.isSeasonal();
                final var languageContext = ctx.getLanguageContext();

                final var player = ctx.getPlayer();
                final var playerData = player.getData();

                final var seasonPlayer = ctx.getSeasonPlayer();
                final var dbUser = ctx.getDBUser();
                final var userData = dbUser.getData();
                final var marriage = ctx.getMarriage(userData);
                final var playerInventory = isSeasonal ? seasonPlayer.getInventory() : player.getInventory();

                var extraMessage = "\n";
                var equipped = isSeasonal ?
                        //seasonal equipped
                        seasonPlayer.getData().getEquippedItems().of(PlayerEquipment.EquipmentType.AXE) :
                        //not seasonal
                        userData.getEquippedItems().of(PlayerEquipment.EquipmentType.AXE);

                if (equipped == 0) {
                    ctx.sendLocalized("commands.chop.not_equipped", EmoteReference.ERROR);
                    return;
                }

                final var item = (Axe) ItemHelper.fromId(equipped);

                if (!RatelimitUtils.ratelimit(rateLimiter, ctx, false)) {
                    return;
                }

                var chance = random.nextInt(100);
                var hasPotion = ItemHelper.handleEffect(
                        PlayerEquipment.EquipmentType.POTION, userData.getEquippedItems(), ItemReference.POTION_HASTE, dbUser
                );

                if (hasPotion) {
                    chance += 9;
                }

                if (chance < 10) {
                    // Found nothing.
                    int level = userData.increaseDustLevel(random.nextInt(5));
                    dbUser.save();
                    // Process axe durability.
                    ItemHelper.handleItemDurability(item, ctx, player, dbUser, seasonPlayer, "commands.chop.autoequip.success", isSeasonal);

                    ctx.sendLocalized("commands.chop.dust", EmoteReference.SAD, level);
                } else {
                    var money = chance > 50 ? Math.max(10, random.nextInt(100)) : 0;
                    var amount = random.nextInt(8);
                    var moneyIncrease = item.getMoneyIncrease() <= 0 ? 1 : item.getMoneyIncrease();
                    money += Math.max(moneyIncrease / 4, random.nextInt(moneyIncrease));

                    HousePet pet = null;
                    if (playerData.getActiveChoice(marriage) == PetChoice.MARRIAGE) {
                        if (marriage != null && marriage.getData().getPet() != null) {
                            pet = marriage.getData().getPet();
                        }
                    } else {
                        pet = playerData.getPet();
                    }

                    if (pet != null) {
                        HousePet.ActivityReward rewards = handlePetBuff(pet, HousePetType.HousePetAbility.CHOP, languageContext);
                        amount += rewards.getItems();
                        money += rewards.getMoney();
                        extraMessage += rewards.getResult();
                    }

                    if (hasPotion) {
                        amount += 3;
                    }

                    // ---- Start of drop handling.
                    RandomCollection<Item> items = new RandomCollection<>();
                    var toDrop = handleChopDrop();
                    toDrop.forEach(i -> items.add(3, i));
                    boolean overflow = false;

                    List<Item> list = new ArrayList<>(amount);
                    for (int i = 0; i < amount; i++) {
                        Item it = items.next();
                        if (playerInventory.getAmount(it) >= 5000) {
                            overflow = true;
                            continue;
                        }

                        list.add(it);
                    }

                    if (overflow) {
                        extraMessage += "\n" + languageContext.get("commands.chop.overflow").formatted(EmoteReference.SAD);
                    }

                    ArrayList<ItemStack> ita = new ArrayList<>();
                    list.forEach(it -> ita.add(new ItemStack(it, 1)));
                    var found = !ita.isEmpty();

                    // Make so it drops some decent amount of wood lol
                    if (ita.stream().anyMatch(is -> is.getItem() == ItemReference.WOOD)) {
                        int am = Math.max(1, random.nextInt(7));
                        if (playerInventory.getAmount(ItemReference.WOOD) + am <= 5000) {
                            ita.add(new ItemStack(ItemReference.WOOD, am));
                        }
                    } else if (found) {
                        // Guarantee at least one wood.
                        if (playerInventory.getAmount(ItemReference.WOOD) < 5000) {
                            ita.add(new ItemStack(ItemReference.WOOD, 1));
                        }
                    }

                    // Reduce item stacks (aka join them) and process it.
                    var reducedStack = ItemStack.reduce(ita);
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
                    var bonus = money;
                    if (random.nextBoolean()) {
                        bonus = money / 2;
                    }

                    if (dbUser.isPremium() && money > 0 && bonus > 0) {
                        money += random.nextInt(bonus);
                    }

                    if (found) {
                        playerData.addBadgeIfAbsent(Badge.CHOPPER);
                    }

                    if (random.nextInt(400) > 380) {
                        var crate = dbUser.isPremium() ? ItemReference.CHOP_PREMIUM_CRATE : ItemReference.CHOP_CRATE;
                        if (playerInventory.getAmount(crate) >= 5000) {
                            extraMessage += "\n" + languageContext.get("commands.chop.crate.overflow");
                        } else {
                            playerInventory.process(new ItemStack(crate, 1));
                            extraMessage += "\n" + EmoteReference.MEGA + languageContext.get("commands.chop.crate.success")
                                    .formatted(crate.getEmoji(), crate.getName());
                        }
                    }

                    handlePetBadges(player, marriage, pet);

                    if (playerData.shouldSeeCampaign()) {
                        extraMessage += Campaign.PREMIUM.getStringFromCampaign(languageContext, dbUser.isPremium());
                        playerData.markCampaignAsSeen();
                    }

                    // Show a message depending on the outcome.
                    if (money > 0 && !found) {
                        ctx.sendFormat(languageContext.get("commands.chop.success_money_noitem") + extraMessage, item.getEmojiDisplay(), money, item.getName());
                    } else if (found && money == 0) {
                        ctx.sendFormat(languageContext.get("commands.chop.success_only_item") + extraMessage, item.getEmojiDisplay(), itemDisplay, item.getName());
                    } else if (!found && money == 0) {
                        // This doesn't actually increase the dust level, though.
                        var level = userData.getDustLevel();
                        ctx.sendLocalized("commands.chop.dust", EmoteReference.SAD, level);
                    } else {
                        ctx.sendFormat(languageContext.get("commands.chop.success") + extraMessage, item.getEmojiDisplay(), itemDisplay, money, item.getName());
                    }

                    player.save();

                    // Save pet stuff.
                    if (marriage != null) {
                        marriage.save();
                    }

                    // Process axe durability.
                    ItemHelper.handleItemDurability(item, ctx, player, dbUser, seasonPlayer, "commands.chop.autoequip.success", isSeasonal);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Starts a chopping session.")
                        .setUsage("""
                                  `~>chop` - Starts chopping trees.
                                  You can gain credits and items by chopping, which can be used later on for casting, specially tools.
                                  """
                        )
                        .setSeasonal(true)
                        .build();
            }
        });
    }

    private HousePet.ActivityReward handlePetBuff(HousePet pet, HousePetType.HousePetAbility required,
                                                  I18nContext languageContext) {
        return handlePetBuff(pet, required, languageContext, true);
    }


    private HousePet.ActivityReward handlePetBuff(HousePet pet, HousePetType.HousePetAbility required,
                                                  I18nContext languageContext, boolean needsItem) {
        HousePet.ActivityResult ability = pet.handleAbility(required);
        if (ability.passed()) {
            var itemIncrease = 0;
            var buildup = pet.getType().getMaxItemBuildup(pet.getLevel());
            if (needsItem && buildup > 0) {
                itemIncrease = random.nextInt(buildup + 1);
            }

            var coinBuildup = pet.getType().getMaxCoinBuildup(pet.getLevel());
            var moneyIncrease = Math.max(1, random.nextInt(coinBuildup + 1));
            var message = "\n" + pet.buildMessage(ability, languageContext, moneyIncrease, itemIncrease);

            return new HousePet.ActivityReward(itemIncrease, moneyIncrease, message);
        } else if (!ability.getLanguageString().isEmpty()) {
            var message = "\n" + pet.buildMessage(ability, languageContext, 0, 0);
            return new HousePet.ActivityReward(0, 0, message);
        }

        return new HousePet.ActivityReward(0, 0, "");
    }

    private List<Item> handleChopDrop() {
        var all = Arrays.stream(ItemReference.ALL)
                .filter(i -> i.getItemType() == ItemType.CHOP_DROP)
                .collect(Collectors.toList());

        return all.stream()
                .sorted(Comparator.comparingLong(Item::getValue))
                .collect(Collectors.toList());
    }

    private void handlePetBadges(Player player, Marriage marriage, HousePet pet) {
        var playerData = player.getData();
        if (pet == null) {
            return;
        }

        if (pet.getType() == HousePetType.KODE) {
            playerData.addBadgeIfAbsent(Badge.THE_BEST_FRIEND);
        }

        if (playerData.getActiveChoice(marriage) == PetChoice.MARRIAGE) {
            playerData.addBadgeIfAbsent(Badge.BEST_FRIEND_MARRY);
        } else {
            playerData.addBadgeIfAbsent(Badge.BEST_FRIEND);
        }

        if (pet.getLevel() >= 50) {
            playerData.addBadgeIfAbsent(Badge.EXPERIENCED_PET_OWNER);
        }

        if (pet.getLevel() >= 100) {
            playerData.addBadgeIfAbsent(Badge.EXPERT_PET_OWNER);
        }
    }

}
