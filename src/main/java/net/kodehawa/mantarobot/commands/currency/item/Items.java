/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.currency.item;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.item.special.*;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.utils.RandomCollection;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import org.slf4j.Logger;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kodehawa.mantarobot.utils.Utils.handleDefaultIncreasingRatelimit;

@SuppressWarnings("WeakerAccess")
public class Items {
    //quite a lot of items if you ask me
    public static final Item HEADPHONES, BAN_HAMMER, KICK_BOOT, FLOPPY_DISK, MY_MATHS, PING_RACKET,
            LOADED_DICE, FORGOTTEN_MUSIC, CC_PENCIL, OVERFLOWED_BAG, BROM_PICKAXE, MILK, ALCOHOL, LEWD_MAGAZINE, RING,
            LOOT_CRATE_KEY, BOOSTER, BERSERK, ENHANCER, RING_2, COMPANION, LOADED_DICE_2, LOVE_LETTER, CLOTHES, SHOES,
            DIAMOND, CHOCOLATE, COOKIES, NECKLACE, ROSE, DRESS, TUXEDO, LOOT_CRATE, STAR, STAR_2, SLOT_COIN, HOUSE, CAR,
            BELL_SPECIAL, CHRISTMAS_TREE_SPECIAL, PANTS, POTION_HASTE, POTION_CLEAN, POTION_STAMINA, FISHING_ROD, FISH_1,
            FISH_2, FISH_3, GEM_1, GEM_2, GEM_3, GEM_4, MOP, CLAIM_KEY, COFFEE, WAIFU_PILL, FISHING_BAIT, DIAMOND_PICKAXE,
            TELEVISION, WRENCH, MOTORCYCLE, GEM1_PICKAXE, GEM2_PICKAXE, PIZZA, GEM_5, GEM5_PICKAXE, MINE_CRATE, FISH_CRATE,
            FISH_PREMIUM_CRATE, MINE_PREMIUM_CRATE, GEM1_ROD, GEM2_ROD, GEM5_ROD, GEM5_PICKAXE_2, GEM5_2, GEM5_ROD_2, FISH_4,
            FISH_5, WRENCH_COMET, WRENCH_SPARKLE, FISH_6, FISH_7, FISH_8, GEM_6, GEM_7, BROKEN_SPARKLE_PICK, BROKEN_COMET_PICK,
            BROKEN_STAR_PICK, BROKEN_COMET_ROD, BROKEN_STAR_ROD, BROKEN_SPARKLE_ROD, INCUBATOR_EGG, WATER_BOTTLE;

    private static final Random r = new Random();
    private static final IncreasingRateLimiter lootCrateRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(2)
            .cooldown(4, TimeUnit.MINUTES)
            .maxCooldown(4, TimeUnit.MINUTES)
            .randomIncrement(false)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("lootcrate")
            .build();

    private static final IncreasingRateLimiter fishRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(2)
            .cooldown(4, TimeUnit.MINUTES)
            .maxCooldown(4, TimeUnit.MINUTES)
            .randomIncrement(false)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("fish")
            .build();
    private static final Config config = MantaroData.config().get();
    public static final Item[] ALL = {
            HEADPHONES = new Item(ItemType.COLLECTABLE, "\uD83C\uDFA7", "Headphones", "items.headphones", "items.description.headphones", 5, true, false, false),
            BAN_HAMMER = new Item(ItemType.COLLECTABLE, "\uD83D\uDD28", "Ban Hammer", "items.ban_hammer", "items.description.ban_hammer", 15, false, false),
            KICK_BOOT = new Item(ItemType.COLLECTABLE, "\uD83D\uDC62", "Kick Boot", "items.kick_boot", "items.description.kick_boot", 12, false, false),
            FLOPPY_DISK = new Item(ItemType.COLLECTABLE, "\uD83D\uDCBE", "Floppy Disk", "items.floppy", "items.description.floppy", 13, false, false),
            MY_MATHS = new Item(ItemType.COLLECTABLE, "\uD83D\uDCDD", "My Maths", "items.maths", "items.description.maths", 11, false, false),
            PING_RACKET = new Item(ItemType.COLLECTABLE, "\uD83C\uDFD3", "Ping Racket", "items.racket", "items.description.racket", 15, false, false),
            LOADED_DICE = new Item(ItemType.COLLECTABLE, "\uD83C\uDFB2", "Loaded Die", "items.die", "items.description.loaded_die", 45, false, false),
            FORGOTTEN_MUSIC = new Item(ItemType.COLLECTABLE, "\uD83C\uDFB5", "Forgotten Music", "items.forgotten", "items.description.forgotten", 15, false, false),
            CC_PENCIL = new Item(ItemType.COLLECTABLE, "\u270f", "Pencil", "items.pencil", "items.description.pencil", 15, false, false),
            OVERFLOWED_BAG = new Item(ItemType.COLLECTABLE, "\uD83D\uDCB0", "Moneybag", "items.moneybag", "items.description.moneybag", 95, true),
            BROM_PICKAXE = new Pickaxe(ItemType.MINE_PICK, 0.19f, "\u26cf", "Brom's Pickaxe", "items.pick", "items.description.pick", 100, true, 35),
            MILK = new Item(ItemType.COMMON, EmoteReference.POTION1.getUnicode(), "Milk", "items.milk", "items.description.milk", 25, true),
            ALCOHOL = new Item(ItemType.COMMON, EmoteReference.POTION2.getUnicode(), "Old Beverage", "items.beverage", "items.description.beverage", 25, true),
            LEWD_MAGAZINE = new Item(ItemType.COMMON, EmoteReference.MAGAZINE.getUnicode(), "Lewd Magazine", "items.lewd", "items.description.lewd", 25, true),
            RING = new Item(ItemType.COMMON, EmoteReference.RING.getUnicode(), "Marriage Ring", "items.ring", "items.description.ring", 60, true),
            LOVE_LETTER = new Item(ItemType.COLLECTABLE, EmoteReference.LOVE_LETTER.getUnicode(), "Love Letter", "items.letter", "items.description.letter", 45, false, false),
            LOOT_CRATE_KEY = new Item(ItemType.COMMON, EmoteReference.KEY.getUnicode(), "Crate Key", "items.key", "items.description.key", 58, true),
            CLOTHES = new Item(ItemType.COMMON, EmoteReference.CLOTHES.getUnicode(), "Clothes", "items.clothes", "items.description.clothes", 30, true),
            DIAMOND = new Item(ItemType.COMMON, EmoteReference.DIAMOND.getUnicode(), "Diamond", "items.diamond", "items.description.diamond", 350, true),
            DRESS = new Item(ItemType.COMMON, EmoteReference.DRESS.getUnicode(), "Wedding Dress", "items.dress", "items.description.dress", 75, true),
            NECKLACE = new Item(ItemType.COMMON, EmoteReference.NECKLACE.getUnicode(), "Necklace", "items.necklace", "items.description.necklace", 17, true),
            TUXEDO = new Item(ItemType.COMMON, EmoteReference.TUXEDO.getUnicode(), "Tuxedo", "items.tuxedo", "items.description.tuxedo", 50, true),
            SHOES = new Item(ItemType.COMMON, EmoteReference.SHOES.getUnicode(), "Shoes", "items.shoes", "items.description.shoes", 10, true),
            ROSE = new Item(ItemType.COMMON, EmoteReference.ROSE.getUnicode(), "Rose", "items.rose", "items.description.rose", 25, true),
            CHOCOLATE = new Item(ItemType.COMMON, EmoteReference.CHOCOLATE.getUnicode(), "Chocolate", "items.chocolate", "items.description.chocolate", 23, true),
            COOKIES = new Item(ItemType.COMMON, EmoteReference.COOKIE.getUnicode(), "Cookie", "items.cookie", "items.description.cookie", 10, true),

            // ---------------------------------- LEFT OVERS FROM CURRENCY V1 STARTS HERE ----------------------------------
            //CANNOT REMOVE BECAUSE WE WERE MEME ENOUGH TO FUCKING SAVE THEM BY THEIR IDS
            LOADED_DICE_2 = new Item("\uD83C\uDFB2", "Special Loaded Die", "items.description.die_2"),
            BOOSTER = new Item(EmoteReference.RUNNER.getUnicode(), "Booster", "items.description.booster"),
            BERSERK = new Item(EmoteReference.CROSSED_SWORD.getUnicode(), "Berserk", "items.description.berserk"),
            COMPANION = new Item(EmoteReference.DOG.getUnicode(), "Companion", "items.description.companion"),
            RING_2 = new Item("\uD83D\uDC5A", "Special Ring", "items.description.special_ring"),
            ENHANCER = new Item(EmoteReference.MAG.getUnicode(), "Enchancer", "items.description.enchancer"),
            STAR = new Item(ItemType.COLLECTABLE, "\uE335", "Prize", "items.prize", "items.description.prize", 0, false, false, true),

            // ---------------------------------- LEFT OVERS FROM CURRENCY V1 END HERE ----------------------------------
            LOOT_CRATE = new Item(ItemType.CRATE, EmoteReference.LOOT_CRATE.getDiscordNotation(), "Loot Crate", "items.crate", "items.description.crate", 0, false, false, true, (event, context, season) -> openLootCrate(event, context.getLeft(), ItemType.LootboxType.RARE, 33, EmoteReference.LOOT_CRATE, 3, season)),
            STAR_2 = new Item(ItemType.COMMON, EmoteReference.STAR.getUnicode(), "Consolation Prize", "items.prize_2", "items.description.prize_2", 500, true, false, true),
            SLOT_COIN = new Item(ItemType.COMMON, "\uD83C\uDF9F", "Slot ticket", "items.slot_ticket", "items.description.slot_ticket", 65, true, true),
            HOUSE = new Item(ItemType.COMMON, EmoteReference.HOUSE.getUnicode(), "House", "items.house", "items.description.house", 5000, true, true),
            CAR = new Item(ItemType.COMMON, "\uD83D\uDE97", "Car", "items.car", "items.description.car", 1000, true, true),

            // ---------------------------------- CHRISTMAS 2017 EVENT STARTS HERE ----------------------------------
            BELL_SPECIAL = new Item(ItemType.RARE, "\uD83D\uDD14", "Christmas bell", "items.bell", "items.description.bell", 0, false, false, true),
            CHRISTMAS_TREE_SPECIAL = new Item(ItemType.RARE, "\uD83C\uDF84", "Christmas tree", "items.tree", "items.description.tree", 0, false, false, true),
            // ---------------------------------- CHRISTMAS 2017 EVENT ENDS HERE ----------------------------------

            // ---------------------------------- 5.0 ITEMS START HERE ----------------------------------
            PANTS = new Item(ItemType.COMMON, "\uD83D\uDC56", "Pants", "items.pants", "items.description.pants", 20, true),
            POTION_HASTE = new Potion(ItemType.POTION, 2, "\uD83C\uDF76", "Haste Potion", "items.haste", "items.description.haste", 490, true),
            POTION_CLEAN = new Item(ItemType.POTION, "\uD83C\uDF7C", "Milky Potion", "items.milky", "items.description.milky", 50, true),
            POTION_STAMINA = new Potion(ItemType.POTION, 3, "\uD83C\uDFFA", "Energy Beverage", "items.energy", "items.description.energy", 450, true),
            FISHING_ROD = new FishRod(ItemType.INTERACTIVE, 3, 1, 25, "\uD83C\uDFA3", "Fishing Rod", "Rod", "items.rod", "items.description.rod", 65, true, "", 25, 0),
            FISH_1 = new Fish(ItemType.FISHING, 1, "\uD83D\uDC1F", "Fish", "items.fish", "items.description.fish", 10, false),
            FISH_2 = new Fish(ItemType.FISHING, 2, "\uD83D\uDC20", "Tropical Fish", "items.tropical_fish", "items.description.tropical_fish", 30, false),
            FISH_3 = new Fish(ItemType.FISHING, 3, "\uD83D\uDC21", "Blowfish", "items.blowfish", "items.description.blowfish", 15, false),
            // ---------------------------------- 5.0 MINING ITEMS START HERE ----------------------------------
            GEM_1 = new Item(ItemType.CAST_OBTAINABLE, "\u2604", "Comet Gem", "items.comet_gem", "items.description.comet_gem", 40, true, false, "3;1", 51, 24),
            GEM_2 = new Item(ItemType.CAST_OBTAINABLE, "\ud83c\udf1f", "Star Gem", "items.star_gem", "items.description.star_gem", 60, true, false, "4;1", 51, 25),
            GEM_3 = new Item(ItemType.MINE, "\uD83D\uDD78", "Cobweb", "items.cobweb", "items.description.cobweb", 10, false),
            GEM_4 = new Item(ItemType.MINE, "\uD83D\uDCAB", "Gem Fragment", "items.fragment", "items.description.fragment", 50, false),
            // ---------------------------------- 5.0 ITEMS START HERE (again lol) ----------------------------------
            MOP = new Item(ItemType.INTERACTIVE, "\u3030", "Mop", "items.mop", "items.description.mop", 10, true),
            CLAIM_KEY = new Item(ItemType.WAIFU, "\uD83D\uDDDD", "Claim Key", "items.claim_key", "items.description.claim_key", 1, false, true),
            COFFEE = new Item(ItemType.COMMON, "\u2615", "Coffee", "items.coffee", "items.description.coffee", 10, true),
            WAIFU_PILL = new Potion(ItemType.POTION, 2, "\ud83d\udc8a", "Waifu Pill", "items.waifu_pill", "items.description.waifu_pill", 670, true),
            FISHING_BAIT = new Potion(ItemType.BUFF, 1, "\uD83D\uDC1B", "Fishing Bait", "items.bait", "items.description.bait", 15, true),
            DIAMOND_PICKAXE = new Pickaxe(ItemType.MINE_PICK, 0.16f, 1, 20, EmoteReference.DIAMOND_PICK.getDiscordNotation(), "Diamond Pickaxe", "items.diamond_pick", "items.description.diamond_pick", 100, true, false, "1;2", 150, 10, 18),
            TELEVISION = new Item(ItemType.COMMON, "\uD83D\uDCFA", "Television", "items.tv", "items.description.tv", 45, true),
            WRENCH = new Wrench(ItemType.COMMON, 65, 1, 1.0d, "\ud83d\udd27", "Wrench", "items.wrench", "items.description.wrench", 50, true),
            //car is 1000 credits, so this is 350
            MOTORCYCLE = new Item(ItemType.COMMON, "\uD83C\uDFCD", "Motorcycle", "items.motorcycle", "items.description.motorcycle", 350, true),
            GEM1_PICKAXE = new Pickaxe(ItemType.MINE_PICK, 0.13f, 1, 10, EmoteReference.COMET_PICK.getDiscordNotation(), "Comet Pickaxe", "items.comet_pick", "items.description.comet_pick", 290, true, false, "1;2", 160, 10, 48),
            GEM2_PICKAXE = new Pickaxe(ItemType.MINE_PICK, 0.09f, 1, 10, EmoteReference.STAR_PICK.getDiscordNotation(), "Star Pickaxe", "items.star_pick", "items.description.star_pick", 350, true, false, "1;2", 210, 10, 49),
            PIZZA = new Item(ItemType.COMMON, "\uD83C\uDF55", "Pizza", "items.pizza", "items.description.pizza", 15, true, false),
            GEM_5 = new Item(ItemType.COMMON, "\u200B", "Old Sparkle Fragment", "general.deprecated", "general.deprecated", 0, false, false),
            GEM5_PICKAXE = new Item(ItemType.COMMON, "\u26cf", "Old Sparkle Pickaxe", "general.deprecated", "general.deprecated", 550, true, false),
            MINE_CRATE = new Item(ItemType.CRATE, EmoteReference.MINE_CRATE.getDiscordNotation(), "Gem Crate", "items.mine_crate", "items.description.mine_crate", 0, false, false, true, (event, context, season) -> openLootCrate(event, context.getLeft(), ItemType.LootboxType.MINE, 66, EmoteReference.MINE_CRATE, 3, season)),
            // ---------------------------------- 5.0 FISH ITEMS START HERE ----------------------------------
            FISH_CRATE = new Item(ItemType.CRATE, EmoteReference.FISH_CRATE.getDiscordNotation(), "Fish Treasure", "items.fish_crate", "items.description.fish_crate", 0, false, false, true, (event, context, season) -> openLootCrate(event, context.getLeft(), ItemType.LootboxType.FISH, 67, EmoteReference.FISH_CRATE, 3, season)),
            FISH_PREMIUM_CRATE = new Item(ItemType.CRATE, EmoteReference.PREMIUM_FISH_CRATE.getDiscordNotation(), "Fish Premium Treasure", "items.fish_premium_crate", "items.description.fish_premium_crate", 0, false, false, true, (event, context, season) -> openLootCrate(event, context.getLeft(), ItemType.LootboxType.FISH_PREMIUM, 68, EmoteReference.PREMIUM_FISH_CRATE, 5, season)),
            MINE_PREMIUM_CRATE = new Item(ItemType.CRATE, EmoteReference.PREMIUM_MINE_CRATE.getDiscordNotation(), "Gem Premium Crate", "items.mine_premium_crate", "items.description.mine_premium_crate", 0, false, false, true, (event, context, season) -> openLootCrate(event, context.getLeft(), ItemType.LootboxType.MINE_PREMIUM, 69, EmoteReference.PREMIUM_MINE_CRATE, 5, season)),
            GEM1_ROD = new FishRod(ItemType.CAST_FISH, 6, 1, 15, EmoteReference.COMET_ROD.getDiscordNotation(), "Comet Gem Rod", "Comet Rod", "items.comet_rod", "items.description.comet_rod", 150, "1;3", 90, 44, 48),
            GEM2_ROD = new FishRod(ItemType.CAST_FISH, 9, 2, 10, EmoteReference.STAR_ROD.getDiscordNotation(), "Star Gem Rod", "Star Rod", "items.star_rod", "items.description.star_rod", 250, "1;3", 130, 44, 49),
            GEM5_ROD = new FishRod(ItemType.COMMON, 3, -1, -1, "\uD83C\uDFA3", "Old Sparkle Rod", "general.deprecated", "general.deprecated", 65, "", 2),
            GEM5_PICKAXE_2 = new Pickaxe(ItemType.MINE_RARE_PICK, 0.04f, 3, 5, EmoteReference.SPARKLE_PICK.getDiscordNotation(), "Sparkle Pickaxe", "items.sparkle_pick", "items.description.sparkle_pick", 1200, true, false, "1;3;1", 450, 10, 74, 18),
            GEM5_2 = new Item(ItemType.MINE_RARE, "\u2728", "Sparkle Fragment", "items.sparkle", "items.description.sparkle", 605, false),
            GEM5_ROD_2 = new FishRod(ItemType.CAST_FISH, 14, 3, 4, EmoteReference.SPARKLE_ROD.getDiscordNotation(), "Sparkle Rod", "items.sparkle_rod", "items.description.sparkle_rod", 800, "1;3;1", 250, 44, 74, 18),
            FISH_4 = new Fish(ItemType.FISHING_RARE, 5, "\uD83D\uDC1A", "Shell", "items.shell", "items.description.shell", 1150, false),
            FISH_5 = new Fish(ItemType.FISHING_RARE, 10, "\uD83E\uDD88", "Shark", "items.shark", "items.description.shark", 600, false),
            WRENCH_COMET = new Wrench(ItemType.WRENCH, 90, 3, 0.90d, EmoteReference.COMET_WRENCH.getDiscordNotation(), "Comet Wrench", "items.star_wrench", "items.description.star_wrench", 200, true, false, "1;2;2", 59, 48, 83),
            WRENCH_SPARKLE = new Wrench(ItemType.WRENCH, 96, 4, 0.65d, EmoteReference.SPARKLE_WRENCH.getDiscordNotation(), "Sparkle Wrench", "items.sparkle_wrench", "items.description.sparkle_wrench", 500, true, false, "1;2;1;2;1", 59, 74, 18, 83, 84),
            FISH_6 = new Fish(ItemType.FISHING, 2, "\uD83E\uDD80", "Crab", "items.crab", "items.description.crab", 30, false),
            FISH_7 = new Fish(ItemType.FISHING, 3, "\uD83E\uDD91", "Squid", "items.squid", "items.description.squid", 35, false),
            FISH_8 = new Fish(ItemType.FISHING, 3, "\uD83E\uDD90", "Shrimp", "items.shrimp", "items.description.shrimp", 35, false),
            GEM_6 = new Item(ItemType.MINE, "\uD83C\uDF19", "Moon Runes", "items.moon", "items.description.moon", 100, false),
            GEM_7 = new Item(ItemType.MINE, "\u2744\uFE0F", "Snowflake", "items.flake", "items.description.flake", 25, true, false, "1;1", 51, 50),
            // ---------------------------------- 5.3 BROKEN ITEMS START HERE ----------------------------------
            BROKEN_SPARKLE_PICK = new Broken(73, EmoteReference.BROKEN_SPARKLE_PICK.getDiscordNotation(), "Broken Sparkle Pickaxe", "items.broken_sparkle_pick", "items.description.broken_sparkle_pick", 100, "1,74;4,84;2,50"),
            BROKEN_COMET_PICK = new Broken(61, EmoteReference.BROKEN_COMET_PICK.getDiscordNotation(), "Broken Comet Pickaxe", "items.broken_comet_pick", "items.description.broken_comet_pick", 40, "1,48;3,84;2,50"),
            BROKEN_STAR_PICK = new Broken(ItemType.BROKEN_MINE_COMMON, 62, EmoteReference.BROKEN_STAR_PICK.getDiscordNotation(), "Broken Star Pickaxe", "items.broken_star_pick", "items.description.broken_star_pick", 40, "1,49;3,84;3,50"),
            BROKEN_SPARKLE_ROD = new Broken(75, EmoteReference.BROKEN_SPARKLE_ROD.getDiscordNotation(), "Broken Sparkle Rod", "items.broken_sparkle_rod", "items.description.broken_sparkle_rod", 90, "1,74;4,84;2,50"),
            BROKEN_COMET_ROD = new Broken(ItemType.BROKEN_FISHING_COMMON, 70, EmoteReference.BROKEN_COMET_ROD.getDiscordNotation(), "Broken Comet Rod", "items.broken_comet_rod", "items.description.broken_comet_rod", 30, "1,48;3,84;2,50"),
            BROKEN_STAR_ROD = new Broken(71, EmoteReference.BROKEN_STAR_ROD.getDiscordNotation(), "Broken Star Rod", "items.broken_star_rod", "items.description.broken_star_rod", 30, "1,49;3,84;3,50"),
            // ---------------------------------- 5.3 BROKEN ITEMS END HERE ----------------------------------
            // ---------------------------------- 5.4 PET ITEMS START HERE ----------------------------------
            INCUBATOR_EGG = new Item(ItemType.PET, "\uD83E\uDD5A", "Incubator Egg", "items.incubator_egg", "items.description.incubator_egg", 300, false, false, "4;3;1", 11, 12, 18),
            WATER_BOTTLE = new Item(ItemType.PET, "", "Water Bottle", "items.water_bottle", "items.description.water_bottle", 100, true, true),

    };
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Items.class);


    public static void setItemActions() {
        final SecureRandom random = new SecureRandom();
        log.info("Registering item actions...");
        final ManagedDatabase managedDatabase = MantaroData.db();

        MOP.setAction(((event, ctx, season) -> {
            Player p = managedDatabase.getPlayer(event.getAuthor());
            DBUser dbUser = managedDatabase.getUser(event.getAuthor());
            I18nContext lang = ctx.getLeft();

            Inventory playerInventory = p.getInventory();
            if (!playerInventory.containsItem(MOP))
                return false;

            event.getChannel().sendMessageFormat(lang.get("general.misc_item_usage.mop"), EmoteReference.DUST).queue();
            playerInventory.process(new ItemStack(MOP, -1));
            p.save();
            dbUser.getData().setDustLevel(0);
            dbUser.save();
            return true;
        }));

        //Basically fish command.
        FISHING_ROD.setAction((event, context, season) -> {
            boolean isSeasonal = season;

            Player p = managedDatabase.getPlayer(event.getAuthor());
            SeasonPlayer sp = managedDatabase.getPlayerForSeason(event.getAuthor(), config.getCurrentSeason());
            DBUser u = managedDatabase.getUser(event.getAuthor());
            Inventory playerInventory = isSeasonal ? sp.getInventory() : p.getInventory();

            I18nContext lang = context.getLeft();
            FishRod item;

            int equipped = isSeasonal ?
                    //seasonal equipped
                    sp.getData().getEquippedItems().of(PlayerEquipment.EquipmentType.ROD) :
                    //not seasonal
                    u.getData().getEquippedItems().of(PlayerEquipment.EquipmentType.ROD);

            if (equipped == 0) {
                event.getChannel().sendMessageFormat(lang.get("commands.fish.no_rod_equipped"), EmoteReference.ERROR).queue();
                return false;
            }

            //It can only be a rod, lol.
            item = (FishRod) Items.fromId(equipped);

            if (!handleDefaultIncreasingRatelimit(fishRatelimiter, event.getAuthor(), event, lang, false))
                return false;

            //Level but starting at 0.
            int nominalLevel = item.getLevel() - 3;
            String extraMessage = "";

            boolean broken = handleDurability(event, lang, item, p, u, sp, isSeasonal);
            if (broken) {
                //Handled in the handleDurability method.
                return false;
            } else {
                int select = random.nextInt(100);

                if (select < 10) {
                    //Here your fish rod got dusty. Yes, on the sea.
                    int level = u.getData().increaseDustLevel(r.nextInt(4));
                    event.getChannel().sendMessageFormat(lang.get("commands.fish.dust"), EmoteReference.TALKING, level).queue();
                    u.save();
                    return false;
                } else if (select < 35) {
                    //Here you found trash.
                    List<Item> common = Stream.of(ALL)
                            .filter(i -> i.getItemType() == ItemType.COMMON && !i.isHidden() && i.isSellable() && i.value < 45)
                            .collect(Collectors.toList());

                    Item selected = common.get(random.nextInt(common.size()));
                    if (playerInventory.getAmount(selected) >= 5000) {
                        event.getChannel().sendMessageFormat(lang.get("commands.fish.trash.overflow"), EmoteReference.SAD).queue();
                        return true;
                    }

                    playerInventory.process(new ItemStack(selected, 1));
                    event.getChannel().sendMessageFormat(lang.get("commands.fish.trash.success"), EmoteReference.EYES, selected.getEmoji()).queue();
                } else {
                    //Here you actually caught fish, congrats.
                    List<Item> fish = Stream.of(ALL)
                            .filter(i -> i.getItemType() == ItemType.FISHING && !i.isHidden() && i.isSellable())
                            .collect(Collectors.toList());
                    RandomCollection<Item> fishItems = new RandomCollection<>();

                    int money = 0;
                    boolean buff = handleEffect(PlayerEquipment.EquipmentType.BUFF, u.getData().getEquippedItems(), FISHING_BAIT, u);
                    int amount = buff ? Math.max(1, random.nextInt(item.getLevel() + 4)) : Math.max(1, random.nextInt(item.getLevel()));
                    if (nominalLevel >= 2)
                        amount += random.nextInt(4);

                    fish.forEach((i1) -> fishItems.add(3, i1));

                    //Basically more chance if you have a better rod.
                    if (select > (75 - nominalLevel)) {
                        money = Math.max(5, random.nextInt(130 + (3 * nominalLevel)));
                    }

                    //START OF WAIFU HELP IMPLEMENTATION
                    boolean waifuHelp = false;
                    if (handleEffect(PlayerEquipment.EquipmentType.POTION, u.getData().getEquippedItems(), WAIFU_PILL, u)) {
                        if (u.getData().getWaifus().entrySet().stream().anyMatch((w) -> w.getValue() > 10_000_000L)) {
                            money += Math.max(10, random.nextInt(100));
                            waifuHelp = true;
                        }
                    }
                    //END OF WAIFU HELP IMPLEMENTATION

                    //START OF FISH LOOT CRATE HANDLING
                    if (r.nextInt(400) > 380) {
                        Item crate = u.isPremium() ? Items.FISH_PREMIUM_CRATE : Items.FISH_CRATE;
                        if (playerInventory.getAmount(crate) >= 5000) {
                            extraMessage += "\n" + lang.get("commands.fish.crate.overflow");
                        } else {
                            playerInventory.process(new ItemStack(crate, 1));
                            extraMessage += "\n" + EmoteReference.MEGA + String.format(lang.get("commands.fish.crate.success"), crate.getEmoji(), crate.getName());
                        }
                    }
                    //END OF FISH LOOT CRATE HANDLING

                    if (item == GEM5_ROD_2 && r.nextInt(30) > 20) {
                        if (r.nextInt(100) > 96) {
                            fish.addAll(Stream.of(ALL)
                                    .filter(i -> i.getItemType() == ItemType.FISHING_RARE && !i.isHidden() && i.isSellable())
                                    .collect(Collectors.toList())
                            );
                        }

                        playerInventory.process(new ItemStack(FISH_5, 1));
                        extraMessage += "\n" + EmoteReference.MEGA + String.format(lang.get("commands.fish.shark_success"), FISH_5.getEmoji());
                    }

                    //START OF ITEM ADDING HANDLING
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

                    if (buff) {
                        extraMessage += "\n" + lang.get("commands.fish.bait");
                    }

                    if (overflow) {
                        extraMessage += "\n" + String.format(lang.get("commands.fish.overflow"), EmoteReference.SAD);
                    }

                    List<ItemStack> reducedList = ItemStack.reduce(list);
                    playerInventory.process(reducedList);
                    if (isSeasonal)
                        sp.addMoney(money);
                    else
                        p.addMoney(money);

                    String itemDisplay = ItemStack.toString(reducedList);
                    boolean foundFish = !reducedList.isEmpty();
                    //END OF ITEM ADDING HANDLING

                    //Add fisher badge if the player found fish succesfully.
                    if (foundFish) {
                        p.getData().addBadgeIfAbsent(Badge.FISHER);
                    }

                    if (nominalLevel >= 3 && r.nextInt(110) > 90) {
                        playerInventory.process(new ItemStack(FISH_4, 1));
                        extraMessage += "\n" + EmoteReference.MEGA + String.format(lang.get("commands.fish.fossil_success"), FISH_4.getEmoji());
                    }

                    //START OF REPLY HANDLING
                    //Didn't find a thingy thing.
                    if (money == 0 && !foundFish) {
                        int level = u.getData().increaseDustLevel(r.nextInt(4));
                        event.getChannel().sendMessageFormat(lang.get("commands.fish.dust"), EmoteReference.TALKING, level).queue();
                        u.save();
                        return false;
                    }


                    //if there's money, but not fish
                    if (money > 0 && !foundFish) {
                        event.getChannel().sendMessageFormat(lang.get("commands.fish.success_money_noitem") + extraMessage, item.getEmoji(), money).queue();
                    } else if (foundFish && money == 0) { //there's fish, but no money
                        event.getChannel().sendMessageFormat(lang.get("commands.fish.success") + extraMessage, item.getEmoji(), itemDisplay).queue();
                    } else if (money > 0 && foundFish) { //there's money and fish
                        event.getChannel().sendMessageFormat(lang.get("commands.fish.success_money") + extraMessage,
                                item.getEmoji(), itemDisplay, money, (waifuHelp ? "\n" + lang.get("commands.fish.waifu_help") : "")
                        ).queue();
                    }
                    //END OF REPLY HANDLING
                }

                //Save all changes to the player object.
                p.save();
                if (isSeasonal)
                    sp.save();

                return true;
            }
        });

        POTION_CLEAN.setAction((event, ctx, season) -> {
            I18nContext lang = ctx.getLeft();
            Player p = managedDatabase.getPlayer(event.getAuthor());
            DBUser u = managedDatabase.getUser(event.getAuthor());

            u.getData().getEquippedItems().resetEffect(PlayerEquipment.EquipmentType.POTION);
            u.save();

            p.getInventory().process(new ItemStack(POTION_CLEAN, -1));
            p.save();

            event.getChannel().sendMessageFormat(lang.get("general.misc_item_usage.milk"), EmoteReference.CORRECT).queue();
            return true;
        });
    }

    public static Optional<Item> fromAny(String any) {
        try {
            Item item = fromId(Integer.parseInt(any));

            if (item != null)
                return Optional.of(item);
        } catch (NumberFormatException ignored) {
        }

        return fromAnyNoId(any);
    }

    public static Optional<Item> fromAnyNoId(String any) {
        Optional<Item> itemOptional;

        itemOptional = fromEmoji(any);
        if (itemOptional.isPresent())
            return itemOptional;

        itemOptional = fromAlias(any);
        if (itemOptional.isPresent())
            return itemOptional;

        itemOptional = fromName(any);
        if (itemOptional.isPresent())
            return itemOptional;

        itemOptional = fromPartialName(any);
        return itemOptional;
    }

    public static Optional<Item> fromEmoji(String emoji) {
        return Stream.of(ALL).filter(item -> item.getEmoji().equals(emoji.replace("\ufe0f", ""))).findFirst();
    }

    public static Item fromId(int id) {
        return ALL[id];
    }

    public static Optional<Item> fromName(String name) {
        return Arrays.stream(ALL).filter(item -> item.getName().toLowerCase().trim().equals(name.toLowerCase().trim())).findFirst();
    }

    public static Optional<Item> fromAlias(String name) {
        return Arrays.stream(ALL).filter(item -> {
            if (item.getAlias() == null) {
                return false;
            }

            return item.getAlias().toLowerCase().trim().equals(name.toLowerCase().trim());
        }).findFirst();
    }

    public static Optional<Item> fromPartialName(String name) {
        return Arrays.stream(ALL).filter(item -> item.getName().toLowerCase().trim().contains(name.toLowerCase().trim())).findFirst();
    }

    public static int idOf(Item item) {
        return Arrays.asList(ALL).indexOf(item);
    }

    private static boolean openLootCrate(GuildMessageReceivedEvent event, I18nContext lang, ItemType.LootboxType type, int item, EmoteReference typeEmote, int bound, boolean season) {
        ManagedDatabase managedDatabase = MantaroData.db();

        Player player = managedDatabase.getPlayer(event.getAuthor());
        SeasonPlayer seasonPlayer = managedDatabase.getPlayerForSeason(event.getAuthor(), config.getCurrentSeason());
        Inventory inventory = season ? seasonPlayer.getInventory() : player.getInventory();

        Item crate = fromId(item);

        if(inventory.containsItem(crate)) {
            if(inventory.containsItem(LOOT_CRATE_KEY)) {
                if(!handleDefaultIncreasingRatelimit(lootCrateRatelimiter, event.getAuthor(), event, lang, false))
                    return false;

                if(crate == LOOT_CRATE) {
                    player.getData().addBadgeIfAbsent(Badge.THE_SECRET);
                }

                //It saves the changes here.
                openLootBox(event, player, seasonPlayer, lang, type, crate, typeEmote, bound, season);
                return true;
            } else {
                event.getChannel().sendMessageFormat(lang.get("general.misc_item_usage.crate.no_key"), EmoteReference.ERROR).queue();
                return false;
            }
        } else {
            event.getChannel().sendMessageFormat(lang.get("general.misc_item_usage.crate.no_crate"), EmoteReference.ERROR).queue();
            return false;
        }
    }

    private static void openLootBox(GuildMessageReceivedEvent event, Player player, SeasonPlayer seasonPlayer, I18nContext lang, ItemType.LootboxType type, Item crate, EmoteReference typeEmote, int bound, boolean seasonal) {
        List<Item> toAdd = selectItems(r.nextInt(bound) + bound, type);

        ArrayList<ItemStack> ita = new ArrayList<>();
        toAdd.forEach(item -> ita.add(new ItemStack(item, 1)));

        if((type == ItemType.LootboxType.MINE || type == ItemType.LootboxType.MINE_PREMIUM) && toAdd.contains(GEM5_PICKAXE) && toAdd.contains(GEM5_PICKAXE_2)) {
            player.getData().addBadgeIfAbsent(Badge.DESTINY_REACHES);
        }

        if((type == ItemType.LootboxType.FISH || type == ItemType.LootboxType.FISH_PREMIUM) && toAdd.contains(FISH_5)) {
            player.getData().addBadgeIfAbsent(Badge.TOO_BIG);
        }


        boolean overflow = seasonal ? seasonPlayer.getInventory().merge(ita) : player.getInventory().merge(ita);

        if(seasonal) {
            seasonPlayer.getInventory().process(new ItemStack(Items.LOOT_CRATE_KEY, -1));
            seasonPlayer.getInventory().process(new ItemStack(crate, -1));
        } else {
            player.getInventory().process(new ItemStack(Items.LOOT_CRATE_KEY, -1));
            player.getInventory().process(new ItemStack(crate, -1));
        }

        player.saveAsync();
        seasonPlayer.saveAsync();

        event.getChannel().sendMessage(String.format(lang.get("general.misc_item_usage.crate.success"),
                typeEmote.getDiscordNotation() + " ", toAdd.stream().map(item -> item.getEmoji() + " " + item.getName()).collect(Collectors.joining(", ")),
                overflow ? ". " + lang.get("general.misc_item_usage.crate.overflow") : "")).queue();
    }

    //Maybe compact this a bit? works fine, just icks me a bit.
    @SuppressWarnings("fallthrough")
    private static List<Item> selectItems(int amount, ItemType.LootboxType type) {
        List<Item> common = handleItemDrop(i -> i.getItemType() == ItemType.COMMON);
        List<Item> rare = handleItemDrop(i -> i.getItemType() == ItemType.RARE);
        List<Item> premium = handleItemDrop(i -> i.getItemType() == ItemType.PREMIUM);

        List<Item> mine = handleItemDrop(i -> i.getItemType() == ItemType.MINE || i.getItemType() == ItemType.CAST_OBTAINABLE || i.getItemType() == ItemType.BROKEN_MINE_COMMON);
        List<Item> fish = handleItemDrop(i -> i.getItemType() == ItemType.FISHING || i.getItemType() == ItemType.BROKEN_FISHING_COMMON);

        List<Item> premiumMine = handleItemDrop(i -> i.getItemType() == ItemType.CAST_MINE ||
                i.getItemType() == ItemType.MINE || i.getItemType() == ItemType.MINE_RARE || i.getItemType() == ItemType.CAST_OBTAINABLE || i.getItemType() == ItemType.MINE_RARE_PICK || i.getItemType() == ItemType.BROKEN_COMMON || i.getItemType() == ItemType.BROKEN);
        List<Item> premiumFish = handleItemDrop(i -> i.getItemType() == ItemType.CAST_FISH ||
                i.getItemType() == ItemType.FISHING || i.getItemType() == ItemType.FISHING_RARE || i.getItemType() == ItemType.BROKEN_FISHING_COMMON || i.getItemType() == ItemType.BROKEN_FISHING);

        RandomCollection<Item> items = new RandomCollection<>();

        switch (type) {
            case PREMIUM:
                premium.forEach(i -> items.add(2, i));
            case RARE:
                rare.forEach(i -> items.add(5, i));
            case COMMON:
                common.forEach(i -> items.add(20, i));
                break; //fallthrough intended until here.
            case FISH_PREMIUM:
                premiumFish.forEach(i -> items.add(5, i));
                break;
            case MINE_PREMIUM:
                premiumMine.forEach(i -> items.add(5, i));
                break;
            case MINE:
                mine.forEach(i -> items.add(8, i));
                break;
            case FISH:
                fish.forEach(i -> items.add(8, i));
        }

        List<Item> list = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            list.add(items.next());
        }

        return list;
    }

    private static List<Item> handleItemDrop(Predicate<Item> predicate) {
        List<Item> all = Arrays.stream(Items.ALL).filter(i -> i.isBuyable() || i.isSellable()).collect(Collectors.toList());

        return all.stream().filter(predicate)
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

            //Effect is active when it's been used less than the max amount
            if (!equipment.isEffectActive(type, ((Potion) item).getMaxUses())) {
                //Reset effect if the current amount equipped is 0. Else, subtract one from the current amount equipped.
                if (!equipment.getCurrentEffect(type).use()) { //This call subtracts one from the current amount equipped.
                    equipment.resetEffect(type);
                    //This has to go twice, because I have to return on the next statement.
                    user.save();

                    return false;
                } else {
                    user.save();
                    return true;
                }
            } else {
                equipment.incrementEffectUses(type);
                user.save();

                return true;
            }
        }

        return false;
    }

    public static Item getBrokenItemFrom(Item item) {
        for (Item i : ALL) {
            if (i instanceof Broken) {
                if (((Broken) i).getMainItem() == Items.idOf(item))
                    return i;
            }
        }

        return null;
    }

    public static boolean handleDurability(GuildMessageReceivedEvent event, I18nContext lang, Item item, Player player, DBUser user, SeasonPlayer seasonPlayer, boolean isSeasonal) {
        Inventory playerInventory = isSeasonal ? seasonPlayer.getInventory() : player.getInventory();

        float amount = r.nextInt(6);
        boolean assumeBroken = false;
        PlayerEquipment equippedItems = isSeasonal ? seasonPlayer.getData().getEquippedItems() : user.getData().getEquippedItems();
        float subtractFrom = (float)
                (handleEffect(PlayerEquipment.EquipmentType.POTION, equippedItems, POTION_STAMINA, user) ?
                        r.nextInt(5) : r.nextInt(2));

        //We do validation before this...
        PlayerEquipment.EquipmentType equipmentType = equippedItems.getTypeFor(item);
        int durability = equippedItems.reduceDurability(equipmentType, (int) Math.max(1, (amount - subtractFrom)));

        if (durability < 10) {
            assumeBroken = true;
        }

        if (assumeBroken) {
            equippedItems.resetOfType(equipmentType);

            String broken = "";
            Item brokenItem = getBrokenItemFrom(item);
            if (brokenItem != null && r.nextInt(100) > 20) {
                broken = "\n" + String.format(lang.get("commands.mine.broken_drop"), EmoteReference.HEART, brokenItem.getEmoji(), brokenItem.getName());
                playerInventory.process(new ItemStack(brokenItem, 1));
            }

            event.getChannel().sendMessageFormat(lang.get("commands.mine.item_broke"), EmoteReference.SAD, item.getName(), broken).queue();
            if (isSeasonal)
                seasonPlayer.save();
            else
                player.save();

            user.save();

            //is broken
            return true;
        } else {
            if (isSeasonal)
                seasonPlayer.save();
            else
                player.save();

            //Why do I keep forgetting this.
            user.save();

            //is not broken
            return false;
        }
    }
}
