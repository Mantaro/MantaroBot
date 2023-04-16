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
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;
import net.kodehawa.mantarobot.commands.currency.item.PlayerEquipment;
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
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Defer;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.UserDatabase;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RandomCollection;
import net.kodehawa.mantarobot.utils.commands.campaign.Campaign;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Module
public class CurrencyActionCmds {
    private static final SecureRandom random = new SecureRandom();
    private static final IncreasingRateLimiter mineRateLimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(3)
            .cooldown(5, TimeUnit.MINUTES)
            .maxCooldown(5, TimeUnit.MINUTES)
            .incrementDivider(10)
            .premiumAware(true)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("mine")
            .build();

    private static final IncreasingRateLimiter fishRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(2)
            .cooldown(4, TimeUnit.MINUTES)
            .maxCooldown(4, TimeUnit.MINUTES)
            .incrementDivider(10)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("fish")
            .premiumAware(true)
            .build();

    private static final IncreasingRateLimiter chopRateLimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(3)
            .cooldown(4, TimeUnit.MINUTES)
            .maxCooldown(4, TimeUnit.MINUTES)
            .incrementDivider(10)
            .premiumAware(true)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("chop")
            .build();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Mine.class);
        cr.registerSlash(Fish.class);
        cr.registerSlash(Chop.class);
    }

    @Name("mine")
    @Defer // Just in case
    @Description("Mines minerals to gain some credits. A bit more lucrative than loot, but needs pickaxes.")
    @Category(CommandCategory.CURRENCY)
    @Help(description = "Mines minerals to gain some credits. A bit more lucrative than loot, but needs pickaxes.", usage = """
            `/mine` - Mines. You can gain minerals or mineral fragments by mining.
            This can used later on to cast rods or picks for better chances.
            """)
    public static class Mine extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            final var player = ctx.getPlayer();
            final var dbUser = ctx.getDBUser();
            final var marriage = ctx.getMarriage(dbUser);

            mine(ctx, player, dbUser, marriage);
        }
    }

    @Name("fish")
    @Defer // Just in case
    @Description("Starts a fishing session.")
    @Category(CommandCategory.CURRENCY)
    @Help(description = "Starts a fishing session.", usage = """
            `/fish` - Starts fishing.
            This can used later on to cast rods or picks for better chances.
            """)
    public static class Fish extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            final var player = ctx.getPlayer();
            final var dbUser = ctx.getDBUser();
            final var marriage = ctx.getMarriage(dbUser);

            fish(ctx, player, dbUser, marriage);
        }
    }

    @Name("chop")
    @Defer // Just in case
    @Description("Starts a chopping session.")
    @Category(CommandCategory.CURRENCY)
    @Help(description = "Starts a chopping session.", usage = """
            `/chop` - Starts chopping trees.
            You can gain credits and items by chopping, which can be used later on for casting, specially tools.
            """)
    public static class Chop extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            final var player = ctx.getPlayer();
            final var dbUser = ctx.getDBUser();
            final var marriage = ctx.getMarriage(dbUser);

            chop(ctx, player, dbUser, marriage);
        }
    }

    @Subscribe
    public void mine(CommandRegistry cr) {
        cr.register("mine", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                final var player = ctx.getPlayer();
                final var dbUser = ctx.getDBUser();
                final var marriage = ctx.getMarriage(dbUser);

                mine(ctx, player, dbUser, marriage);
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
                        .build();
            }
        });
    }

    @Subscribe
    public void fish(CommandRegistry cr) {
        cr.register("fish", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                final var player = ctx.getPlayer();
                final var dbUser = ctx.getDBUser();
                final var marriage = ctx.getMarriage(dbUser);

                fish(ctx, player, dbUser, marriage);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Starts a fishing session.")
                        .setUsage("""
                                  `~>fish` - Starts fishing.
                                  `~>fish` - Starts fishing.
                                  """
                        )
                        .setSeasonal(true)
                        .build();
            }
        });
    }

    @Subscribe
    public void chop(CommandRegistry cr) {
        cr.register("chop", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                final var player = ctx.getPlayer();
                final var dbUser = ctx.getDBUser();
                final var marriage = ctx.getMarriage(dbUser);

                chop(ctx, player, dbUser, marriage);
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

    private static void mine(IContext ctx, Player player, UserDatabase dbUser, Marriage marriage) {
        final var languageContext = ctx.getLanguageContext();
        final var equipped = dbUser.getEquippedItems().of(PlayerEquipment.EquipmentType.PICK);

        if (equipped == 0) {
            ctx.sendLocalized("commands.mine.not_equipped", EmoteReference.ERROR);
            return;
        }

        if (!RatelimitUtils.ratelimit(mineRateLimiter, ctx, false)) {
            return;
        }

        var message = "";
        var waifuHelp = false;
        var petHelp = false;

        var item = (Pickaxe) ItemHelper.fromId(equipped);
        var money = Math.max(30, random.nextInt(200)); // 30 to 150 credits.
        var moneyIncrease = item.getMoneyIncrease() <= 0 ? 1 : item.getMoneyIncrease();
        money += Math.max(moneyIncrease / 2, random.nextInt(moneyIncrease));

        if (ItemHelper.handleEffect(PlayerEquipment.EquipmentType.POTION, dbUser.getEquippedItems(), ItemReference.WAIFU_PILL, dbUser)) {
            final var waifus = dbUser.waifuEntrySet();
            if (waifus.stream().anyMatch((w) -> w.getValue() > 20_000L)) {
                money += Math.max(20, random.nextInt(100));
                waifuHelp = true;
            }
        }

        var reminder = random.nextInt(6) == 0 && item == ItemReference.BROM_PICKAXE ?
                languageContext.get("commands.mine.reminder") : "";

        var hasPotion = ItemHelper.handleEffect(
                PlayerEquipment.EquipmentType.POTION, dbUser.getEquippedItems(),
                ItemReference.POTION_HASTE, dbUser
        );

        HousePet pet = null;
        if (player.getActiveChoice(marriage) == PetChoice.MARRIAGE) {
            if (marriage != null && marriage.getPet() != null) {
                pet = marriage.getPet();
            }
        } else {
            pet = player.getPet();
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
            if (player.getItemAmount(ItemReference.DIAMOND) + amount > 5000) {
                message += "\n" + languageContext.get("commands.mine.diamond.overflow").formatted(amount);
                money += Math.round((ItemReference.DIAMOND.getValue() * 0.9) * amount);
            } else {
                player.processItem(ItemReference.DIAMOND, amount);
                message += "\n" + EmoteReference.DIAMOND + languageContext.get("commands.mine.diamond.success").formatted(amount);
            }

            player.addBadgeIfAbsent(Badge.MINER);
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
                    .toList();

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
                        }).toList();

                extraItem = extra.get(random.nextInt(extra.size()));
                extraGem = new ItemStack(extraItem,
                        extraItem instanceof CastedGem ? 1 : Math.max(1, random.nextInt(3))
                );
            }

            if (extraGem != null && (player.getItemAmount(extraItem) + extraGem.getAmount() >= 5000)) {
                extraGem = null;
            }

            if (player.getItemAmount(itemGem) + selectedGem.getAmount() >= 5000) {
                message += "\n" + languageContext.get("commands.mine.gem.overflow")
                        .formatted(itemGem.getEmojiDisplay() + " x" + selectedGem.getAmount());
                money += Math.round((itemGem.getValue() * 0.9) * selectedGem.getAmount());
            } else {
                player.processItem(selectedGem);

                if (extraGem != null) {
                    player.processItem(extraGem);
                    message += "\n" + EmoteReference.MEGA + languageContext.get("commands.mine.gem.success_extra")
                            .formatted(
                                    itemGem.getEmoji() + " x" + selectedGem.getAmount(),
                                    extraItem.getEmoji() + " x" + extraGem.getAmount()
                            );
                } else {
                    message += "\n" + EmoteReference.MEGA + languageContext.get("commands.mine.gem.success")
                            .formatted(itemGem.getEmojiDisplay() + " x" + selectedGem.getAmount());
                }
            }

            if (waifuHelp) {
                message += "\n" + languageContext.get("commands.mine.waifu_help");
            }

            player.addBadgeIfAbsent(Badge.GEM_FINDER);
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

            if (player.getItemAmount(gem) + 1 >= 5000) {
                message += "\n" + languageContext.get("commands.mine.sparkle.overflow");
                money += Math.round(gem.getValue() * 0.9);
            } else {
                player.processItem(gem, 1);
                message += "\n" + EmoteReference.MEGA +
                        languageContext.get("commands.mine.sparkle.success").formatted(gem.getEmojiDisplay());
            }

            player.addBadgeIfAbsent(Badge.GEM_FINDER);
        }

        if (random.nextInt(400) >= 392) {
            var crate = dbUser.isPremium() ? ItemReference.MINE_PREMIUM_CRATE : ItemReference.MINE_CRATE;

            if (player.getItemAmount(crate) + 1 > 5000) {
                message += "\n" + languageContext.get("commands.mine.crate.overflow");
            } else {
                player.processItem(crate, 1);
                message += "\n" + EmoteReference.MEGA + languageContext.get("commands.mine.crate.success")
                        .formatted(crate.getEmojiDisplay(), crate.getName());
            }
        }

        if (player.shouldSeeCampaign()) {
            message += Campaign.PREMIUM.getStringFromCampaign(languageContext, dbUser.isPremium());
            player.markCampaignAsSeen();
        }

        player.incrementMiningExperience(random);
        player.addMoney(money);

        handlePetBadges(player, marriage, pet);
        player.updateAllChanged();
        if (pet != null && player.getPetChoice() == PetChoice.PERSONAL) {
            pet.updateAllChanged(player);
        }

        if (marriage != null && pet != null && player.getPetChoice() == PetChoice.MARRIAGE) {
            pet.updateAllChanged(marriage);
        }

        ItemHelper.handleItemDurability(item, ctx, player, dbUser, "commands.mine.autoequip.success");
        message += "\n\n" + (languageContext.get("commands.mine.success") + reminder).formatted(item.getEmojiDisplay(), money, item.getName());
        ctx.sendStripped(message);
    }

    private static void fish(IContext ctx, Player player, UserDatabase dbUser, Marriage marriage) {
        final var languageContext = ctx.getLanguageContext();
        FishRod item;
        var equipped = dbUser.getEquippedItems().of(PlayerEquipment.EquipmentType.ROD);

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
                dbUser.getEquippedItems(),
                ItemReference.FISHING_BAIT, dbUser
        );

        if (buff) {
            chance += 6;
        }

        if (chance < 10) {
            //Here your fish rod got dusty. Yes, on the sea.
            var level = dbUser.increaseDustLevel(random.nextInt(4));
            dbUser.updateAllChanged();
            ctx.sendLocalized("commands.fish.dust", EmoteReference.TALKING, level);

            ItemHelper.handleItemDurability(item, ctx, player, dbUser, "commands.fish.autoequip.success");
            return;
        } else if (chance < 20) {
            //Here you found trash.
            List<Item> common = Stream.of(ItemReference.ALL)
                    .filter(i -> i.getItemType() == ItemType.COMMON && !i.isHidden() && i.isSellable() && i.getValue() < 45).toList();

            var selected = common.get(random.nextInt(common.size()));
            if (player.getItemAmount(selected) >= 5000) {
                ctx.sendLocalized("commands.fish.trash.overflow", EmoteReference.SAD);
                ItemHelper.handleItemDurability(item, ctx, player, dbUser, "commands.fish.autoequip.success");
                return;
            }

            player.processItem(selected, 1);
            ctx.sendLocalized("commands.fish.trash.success", EmoteReference.EYES, selected.getEmojiDisplay());

            ItemHelper.handleItemDurability(item, ctx, player, dbUser, "commands.fish.autoequip.success");
            return;
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
            if (player.getActiveChoice(marriage) == PetChoice.MARRIAGE) {
                if (marriage != null && marriage.getPet() != null) {
                    pet = marriage.getPet();
                }
            } else {
                pet = player.getPet();
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
            if (ItemHelper.handleEffect(PlayerEquipment.EquipmentType.POTION, dbUser.getEquippedItems(), ItemReference.WAIFU_PILL, dbUser)) {
                if (dbUser.waifuEntrySet().stream().anyMatch((w) -> w.getValue() > 20_000L)) {
                    money += Math.max(10, random.nextInt(150));
                    waifuHelp = true;
                }
            }
            // END OF WAIFU HELP IMPLEMENTATION

            // START OF FISH LOOT CRATE HANDLING
            if (random.nextInt(400) > 380) {
                var crate = dbUser.isPremium() ? ItemReference.FISH_PREMIUM_CRATE : ItemReference.FISH_CRATE;

                if (player.getItemAmount(crate) >= 5000) {
                    extraMessage += "\n" + languageContext.get("commands.fish.crate.overflow");
                } else {
                    player.processItem(crate, 1);
                    extraMessage += "\n" + EmoteReference.MEGA +
                            languageContext.get("commands.fish.crate.success").formatted(crate.getEmojiDisplay(), crate.getName());
                }
            }
            // END OF FISH LOOT CRATE HANDLING

            if ((item == ItemReference.SPARKLE_ROD || item == ItemReference.HELLFIRE_ROD) && random.nextInt(30) > 20) {
                if (random.nextInt(100) > 96) {
                    fish.addAll(Stream.of(ItemReference.ALL)
                            .filter(i -> i.getItemType() == ItemType.FISHING_RARE && !i.isHidden() && i.isSellable()).toList()
                    );
                }

                player.processItem(ItemReference.SHARK, 1);
                extraMessage += "\n" + EmoteReference.MEGA +
                        languageContext.get("commands.fish.shark_success").formatted(ItemReference.SHARK.getEmojiDisplay());

                player.sharksCaught(player.getSharksCaught() + 1);
            }

            List<ItemStack> list = new ArrayList<>(amount);
            AtomicBoolean overflow = new AtomicBoolean(false);
            for (int i = 0; i < amount; i++) {
                Item it = fishItems.next();
                list.add(new ItemStack(it, 1));
            }

            ArrayList<ItemStack> ita = new ArrayList<>();
            var stack = ItemStack.reduce(list);
            stack.forEach(it -> {
                if (player.getItemAmount(it.getItem()) + it.getAmount() <= 5000) {
                    ita.add(it);
                } else {
                    overflow.set(true);
                }
            });

            // END OF ITEM ADD HANDLING
            if (overflow.get()) {
                extraMessage += "\n" + languageContext.get("commands.fish.overflow")
                        .formatted(EmoteReference.SAD);
            }

            var reduced = ItemStack.reduce(ita);
            player.processItems(reduced);
            var itemDisplay = ItemStack.toString(reduced);
            var foundFish = !reduced.isEmpty();

            //Add fisher badge if the player found fish successfully.
            if (foundFish) {
                player.addBadgeIfAbsent(Badge.FISHER);
            }

            handlePetBadges(player, marriage, pet);

            if (nominalLevel >= 3 && random.nextInt(110) > 90) {
                player.processItem(ItemReference.SHELL, 1);
                extraMessage += "\n" + EmoteReference.MEGA +
                        languageContext.get("commands.fish.fossil_success").formatted(ItemReference.SHELL.getEmojiDisplay());
            }

            var bonus = money;
            if (random.nextBoolean()) {
                bonus = money / 2;
            }

            if (dbUser.isPremium() && money > 0 && bonus > 0) {
                money += random.nextInt(bonus);
            }

            if (player.shouldSeeCampaign()) {
                extraMessage += Campaign.PREMIUM.getStringFromCampaign(languageContext, dbUser.isPremium());
                player.markCampaignAsSeen();
            }

            player.addMoney(money);
            player.incrementFishingExperience(random);

            //START OF REPLY HANDLING
            //Didn't find a thingy thing.
            if (money == 0 && !foundFish) {
                int level = dbUser.increaseDustLevel(random.nextInt(4));
                ctx.sendLocalized("commands.fish.dust", EmoteReference.TALKING, level);
                dbUser.updateAllChanged();

                ItemHelper.handleItemDurability(item, ctx, player, dbUser, "commands.fish.autoequip.success");
                return;
            }

            //if there's money, but not fish
            if (money > 0 && !foundFish) {
                ctx.sendFormatStripped(extraMessage + "\n\n" + languageContext.get("commands.fish.success_money_noitem"), item.getEmojiDisplay(), money, item.getName());
            } else if (foundFish && money == 0) { //there's fish, but no money
                ctx.sendFormatStripped(extraMessage + "\n\n" + languageContext.get("commands.fish.success"), item.getEmojiDisplay(), itemDisplay, item.getName());
            } else if (money > 0) { //there's money and fish
                ctx.sendFormatStripped(extraMessage + "\n\n" + languageContext.get("commands.fish.success_money"),
                        item.getEmojiDisplay(), itemDisplay, money, item.getName(), (waifuHelp ? "\n" + languageContext.get("commands.fish.waifu_help") : "")
                );
            }

            //Save all changes to the player object.
            player.updateAllChanged();
            if (pet != null && player.getPetChoice() == PetChoice.PERSONAL) {
                pet.updateAllChanged(player);
            }

            // Save pet stats.
            if (marriage != null && pet != null && player.getPetChoice() == PetChoice.MARRIAGE) {
                pet.updateAllChanged(marriage);
            }

            ItemHelper.handleItemDurability(item, ctx, player, dbUser, "commands.fish.autoequip.success");
        }
    }

    private static void chop(IContext ctx, Player player, UserDatabase dbUser, Marriage marriage) {
        final var languageContext = ctx.getLanguageContext();
        var extraMessage = "\n";
        var equipped = dbUser.getEquippedItems().of(PlayerEquipment.EquipmentType.AXE);

        if (equipped == 0) {
            ctx.sendLocalized("commands.chop.not_equipped", EmoteReference.ERROR);
            return;
        }

        final var item = (Axe) ItemHelper.fromId(equipped);
        if (!RatelimitUtils.ratelimit(chopRateLimiter, ctx, false)) {
            return;
        }

        var chance = random.nextInt(100);
        var hasPotion = ItemHelper.handleEffect(
                PlayerEquipment.EquipmentType.POTION, dbUser.getEquippedItems(), ItemReference.POTION_HASTE, dbUser
        );

        if (hasPotion) {
            chance += 9;
        }

        if (chance < 10) {
            // Found nothing.
            int level = dbUser.increaseDustLevel(random.nextInt(5));
            dbUser.updateAllChanged();
            // Process axe durability.
            ItemHelper.handleItemDurability(item, ctx, player, dbUser, "commands.chop.autoequip.success");

            ctx.sendLocalized("commands.chop.dust", EmoteReference.SAD, level);
        } else {
            var money = chance > 50 ? Math.max(10, random.nextInt(100)) : 0;
            var amount = random.nextInt(8);
            var moneyIncrease = item.getMoneyIncrease() <= 0 ? 1 : item.getMoneyIncrease();
            money += Math.max(moneyIncrease / 4, random.nextInt(moneyIncrease));

            HousePet pet = null;
            if (player.getActiveChoice(marriage) == PetChoice.MARRIAGE) {
                if (marriage != null && marriage.getPet() != null) {
                    pet = marriage.getPet();
                }
            } else {
                pet = player.getPet();
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
            AtomicBoolean overflow = new AtomicBoolean(false);

            List<ItemStack> list = new ArrayList<>(amount);
            for (int i = 0; i < amount; i++) {
                Item it = items.next();
                list.add(new ItemStack(it, 1));
            }

            ArrayList<ItemStack> ita = new ArrayList<>();
            var stack = ItemStack.reduce(list);
            stack.forEach(it -> {
                if (player.getItemAmount(it.getItem()) + it.getAmount() <= 5000) {
                    ita.add(it);
                } else {
                    overflow.set(true);
                }
            });

            if (overflow.get()) {
                extraMessage += "\n" + languageContext.get("commands.chop.overflow").formatted(EmoteReference.SAD);
            }

            var found = !ita.isEmpty();
            // Make so it drops some decent amount of wood lol
            if (ita.stream().anyMatch(is -> is.getItem() == ItemReference.WOOD)) {
                // There should only be one, as we merged it beforehand.
                var wood = ita.stream().filter(is -> is.getItem() == ItemReference.WOOD).toList();
                int am = Math.max(1, random.nextInt(7));
                if (player.getItemAmount(ItemReference.WOOD) + am + wood.get(0).getAmount() <= 5000) {
                    ita.add(new ItemStack(ItemReference.WOOD, am));
                }
            } else if (found) {
                // Guarantee at least one wood.
                if (player.getItemAmount(ItemReference.WOOD) + 1 <= 5000) {
                    ita.add(new ItemStack(ItemReference.WOOD, 1));
                }
            }

            // Reduce item stacks (aka join them) and process it.
            var reduced = ItemStack.reduce(ita);
            var itemDisplay = ItemStack.toString(reduced);
            player.processItems(reduced);

            // Ah yes, sellout
            var bonus = money;
            if (random.nextBoolean()) {
                bonus = money / 2;
            }

            if (dbUser.isPremium() && money > 0 && bonus > 0) {
                money += random.nextInt(bonus);
            }

            if (found) {
                player.addBadgeIfAbsent(Badge.CHOPPER);
            }

            if (random.nextInt(400) > 380) {
                var crate = dbUser.isPremium() ? ItemReference.CHOP_PREMIUM_CRATE : ItemReference.CHOP_CRATE;
                if (player.getItemAmount(crate) >= 5000) {
                    extraMessage += "\n" + languageContext.get("commands.chop.crate.overflow");
                } else {
                    player.processItem(crate, 1);
                    extraMessage += "\n" + EmoteReference.MEGA + languageContext.get("commands.chop.crate.success")
                            .formatted(crate.getEmojiDisplay(), crate.getName());
                }
            }

            // Add money
            player.addMoney(money);
            player.incrementChopExperience(random);

            handlePetBadges(player, marriage, pet);

            if (player.shouldSeeCampaign()) {
                extraMessage += Campaign.PREMIUM.getStringFromCampaign(languageContext, dbUser.isPremium());
                player.markCampaignAsSeen();
            }

            // Show a message depending on the outcome.
            if (money > 0 && !found) {
                ctx.sendFormatStripped(extraMessage + "\n\n" + languageContext.get("commands.chop.success_money_noitem"), item.getEmojiDisplay(), money, item.getName());
            } else if (found && money == 0) {
                ctx.sendFormatStripped(extraMessage + "\n\n" + languageContext.get("commands.chop.success_only_item"), item.getEmojiDisplay(), itemDisplay, item.getName());
            } else if (!found && money == 0) {
                // This doesn't actually increase the dust level, though.
                var level = dbUser.getDustLevel();
                ctx.sendLocalized("commands.chop.dust", EmoteReference.SAD, level);
            } else {
                ctx.sendFormatStripped(extraMessage + "\n\n" + languageContext.get("commands.chop.success"), item.getEmojiDisplay(), itemDisplay, money, item.getName());
            }

            player.updateAllChanged();
            if (pet != null && player.getPetChoice() == PetChoice.PERSONAL) {
                pet.updateAllChanged(player);
            }

            // Save pet stuff.
            if (marriage != null && pet != null &&player.getPetChoice() == PetChoice.MARRIAGE) {
                pet.updateAllChanged(marriage);
            }

            // Process axe durability.
            ItemHelper.handleItemDurability(item, ctx, player, dbUser, "commands.chop.autoequip.success");
        }
    }

    private static HousePet.ActivityReward handlePetBuff(HousePet pet, HousePetType.HousePetAbility required,
                                                         I18nContext languageContext) {
        return handlePetBuff(pet, required, languageContext, true);
    }


    private static HousePet.ActivityReward handlePetBuff(HousePet pet, HousePetType.HousePetAbility required,
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

    private static List<Item> handleChopDrop() {
        var all = Arrays.stream(ItemReference.ALL)
                .filter(i -> i.getItemType() == ItemType.CHOP_DROP).toList();

        return all.stream()
                .sorted(Comparator.comparingLong(Item::getValue))
                .collect(Collectors.toList());
    }

    private static void handlePetBadges(Player player, Marriage marriage, HousePet pet) {
        if (pet == null) {
            return;
        }

        if (pet.getType() == HousePetType.KODE) {
            player.addBadgeIfAbsent(Badge.THE_BEST_FRIEND);
        }

        if (player.getActiveChoice(marriage) == PetChoice.MARRIAGE) {
            player.addBadgeIfAbsent(Badge.BEST_FRIEND_MARRY);
        } else {
            player.addBadgeIfAbsent(Badge.BEST_FRIEND);
        }

        if (pet.getLevel() >= 50) {
            player.addBadgeIfAbsent(Badge.EXPERIENCED_PET_OWNER);
        }

        if (pet.getLevel() >= 100) {
            player.addBadgeIfAbsent(Badge.EXPERT_PET_OWNER);
        }

        if (pet.getLevel() >= 300) {
            player.addBadgeIfAbsent(Badge.LEGENDARY_PET_OWNER);
        }
    }

}
