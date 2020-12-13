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

package net.kodehawa.mantarobot.commands.currency.item;

import net.kodehawa.mantarobot.commands.currency.item.special.*;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

public class ItemReference {
    public static final Item HEADPHONES, BAN_HAMMER, KICK_BOOT, FLOPPY_DISK, MY_MATHS, PING_RACKET,
            LOADED_DICE, FORGOTTEN_MUSIC, CC_PENCIL, OVERFLOWED_BAG, BROM_PICKAXE, MILK, ALCOHOL, LEWD_MAGAZINE, RING,
            LOOT_CRATE_KEY, BOOSTER, BERSERK, ENHANCER, RING_2, COMPANION, LOADED_DICE_2, LOVE_LETTER, CLOTHES, SHOES,
            DIAMOND, CHOCOLATE, COOKIES, NECKLACE, ROSE, DRESS, TUXEDO, LOOT_CRATE, STAR, STAR_2, SLOT_COIN, HOUSE, CAR,
            BELL_SPECIAL, CHRISTMAS_TREE_SPECIAL, PANTS, POTION_HASTE, POTION_CLEAN, POTION_STAMINA, FISHING_ROD, FISH,
            TROPICAL_FISH, BLOWFISH, COMET_GEM, STAR_GEM, COBWEB, GEM_FRAGMENT, MOP, CLAIM_KEY, COFFEE, WAIFU_PILL, FISHING_BAIT, DIAMOND_PICKAXE,
            TELEVISION, WRENCH, MOTORCYCLE, COMET_PICKAXE, STAR_PICKAXE, PIZZA, OLD_SPARKLE_FRAGMENT, GEM5_PICKAXE, MINE_CRATE, FISH_CRATE,
            FISH_PREMIUM_CRATE, MINE_PREMIUM_CRATE, COMET_ROD, STAR_ROD, OLD_SPARKLE_ROD, SPARKLE_PICKAXE, SPARKLE_FRAGMENT, SPARKLE_ROD, SHELL,
            SHARK, WRENCH_COMET, WRENCH_SPARKLE, CRAB, SQUID, SHRIMP, MOON_RUNES, SNOWFLAKE, BROKEN_SPARKLE_PICK, BROKEN_COMET_PICK,
            BROKEN_STAR_PICK, BROKEN_COMET_ROD, BROKEN_STAR_ROD, BROKEN_SPARKLE_ROD, INCUBATOR_EGG, WATER_BOTTLE, MAGIC_WATCH, STEAK,
            CHICKEN, MILK_2, DOG_FOOD, CAT_FOOD, HAMSTER_FOOD, WOOD, AXE, COMET_AXE, STAR_AXE, SPARKLE_AXE, HELLFIRE_AXE, MOON_AXE,
            MOON_PICK, MOON_ROD, HELLFIRE_PICK, HELLFIRE_ROD, PET_HOUSE, LEAVES, APPLE, PEAR, CHERRY_BLOSSOM, ROCK, BROKEN_MOON_PICK,
            BROKEN_MOON_ROD, BROKEN_COMET_AXE, BROKEN_STAR_AXE, BROKEN_SPARKLE_AXE, BROKEN_MOON_AXE, BROKEN_HELLFIRE_PICK,
            BROKEN_HELLFIRE_AXE, BROKEN_HELLFIRE_ROD;

    public static final Item[] ALL = {
            HEADPHONES = new Item(ItemType.COLLECTABLE, "\uD83C\uDFA7",
                    "Headphones", "items.headphones", "items.description.headphones",
                    5, true, false, false
            ),

            BAN_HAMMER = new Item(ItemType.COLLECTABLE, "\uD83D\uDD28",
                    "Ban Hammer", "items.ban_hammer", "items.description.ban_hammer",
                    15, false, false
            ),

            KICK_BOOT = new Item(ItemType.COLLECTABLE, "\uD83D\uDC62",
                    "Kick Boot", "items.kick_boot",
                    "items.description.kick_boot",
                    12, false, false
            ),

            FLOPPY_DISK = new Item(ItemType.COLLECTABLE, "\uD83D\uDCBE",
                    "Floppy Disk", "items.floppy", "items.description.floppy",
                    13, false, false
            ),

            MY_MATHS = new Item(ItemType.COLLECTABLE, "\uD83D\uDCDD",
                    "My Maths", "items.maths", "items.description.maths",
                    11, false, false
            ),

            PING_RACKET = new Item(ItemType.COLLECTABLE, "\uD83C\uDFD3",
                    "Ping Racket", "items.racket", "items.description.racket",
                    15, false, false
            ),

            LOADED_DICE = new Item(ItemType.COLLECTABLE, "\uD83C\uDFB2",
                    "Loaded Die", "items.die", "items.description.loaded_die",
                    45, false, false
            ),

            FORGOTTEN_MUSIC = new Item(ItemType.COLLECTABLE, "\uD83C\uDFB5",
                    "Forgotten Music", "items.forgotten", "items.description.forgotten",
                    15, false, false
            ),

            CC_PENCIL = new Item(ItemType.COLLECTABLE, "\u270f",
                    "Pencil", "items.pencil", "items.description.pencil",
                    15, false, false
            ),

            OVERFLOWED_BAG = new Item(ItemType.COLLECTABLE, "\uD83D\uDCB0",
                    "Moneybag", "items.moneybag", "items.description.moneybag",
                    100, true
            ),

            BROM_PICKAXE = new Pickaxe(ItemType.MINE_PICK, 0.19f, "\u26cf",
                    "Brom's Pickaxe", "items.pick", "items.description.pick",
                    100, true, 40, 0
            ),

            MILK = new Item(ItemType.COMMON, EmoteReference.POTION1.getUnicode(),
                    "Milk", "items.milk", "items.description.milk",
                    25, true
            ),

            ALCOHOL = new Item(ItemType.COMMON, EmoteReference.POTION2.getUnicode(),
                    "Old Beverage", "items.beverage", "items.description.beverage",
                    25, true
            ),

            LEWD_MAGAZINE = new Item(ItemType.COMMON, EmoteReference.MAGAZINE.getUnicode(),
                    "Lewd Magazine",
                    "items.lewd", "items.description.lewd",
                    25, true
            ),

            RING = new Item(ItemType.COMMON, EmoteReference.RING.getUnicode(),
                    "Marriage Ring", "items.ring", "items.description.ring",
                    60, true
            ),

            LOVE_LETTER = new Item(ItemType.COLLECTABLE, EmoteReference.LOVE_LETTER.getUnicode(),
                    "Love Letter", "items.letter", "items.description.letter",
                    45, false, false
            ),

            LOOT_CRATE_KEY = new Item(ItemType.COMMON, EmoteReference.KEY.getUnicode(),
                    "Crate Key", "items.key", "items.description.key",
                    58, true
            ),

            CLOTHES = new Item(ItemType.COMMON, EmoteReference.CLOTHES.getUnicode(),
                    "Clothes", "items.clothes", "items.description.clothes",
                    30, true
            ),

            DIAMOND = new Item(ItemType.COMMON, EmoteReference.DIAMOND.getUnicode(),
                    "Diamond", "items.diamond", "items.description.diamond",
                    200, true
            ),

            DRESS = new Item(ItemType.COMMON, EmoteReference.DRESS.getUnicode(), "Wedding Dress",
                    "items.dress", "items.description.dress",
                    75, true
            ),

            NECKLACE = new Item(ItemType.COMMON, EmoteReference.NECKLACE.getUnicode(),
                    "Necklace", "items.necklace", "items.description.necklace",
                    17, true
            ),

            TUXEDO = new Item(ItemType.COMMON, EmoteReference.TUXEDO.getUnicode(),
                    "Tuxedo", "items.tuxedo", "items.description.tuxedo",
                    50, true
            ),

            SHOES = new Item(ItemType.COMMON, EmoteReference.SHOES.getUnicode(),
                    "Shoes", "items.shoes", "items.description.shoes",
                    10, true
            ),

            ROSE = new Item(ItemType.COMMON, EmoteReference.ROSE.getUnicode(),
                    "Rose", "items.rose", "items.description.rose",
                    25, true
            ),

            CHOCOLATE = new Item(ItemType.COMMON, EmoteReference.CHOCOLATE.getUnicode(),
                    "Chocolate", "items.chocolate", "items.description.chocolate",
                    23, true
            ),

            COOKIES = new Item(ItemType.COMMON, EmoteReference.COOKIE.getUnicode(),
                    "Cookie", "items.cookie", "items.description.cookie",
                    10, true
            ),

            // Left overs from v1
            LOADED_DICE_2 = new Item("\uD83C\uDFB2",
                    "Special Loaded Die", "items.description.die_2"
            ),

            BOOSTER = new Item(EmoteReference.RUNNER.getUnicode(),
                    "Booster", "items.description.booster"
            ),

            BERSERK = new Item(EmoteReference.CROSSED_SWORD.getUnicode(),
                    "Berserk", "items.description.berserk"
            ),

            COMPANION = new Item(EmoteReference.DOG.getUnicode(),
                    "Companion", "items.description.companion"
            ),

            RING_2 = new Item("\uD83D\uDC5A",
                    "Special Ring", "items.description.special_ring"
            ),

            ENHANCER = new Item(EmoteReference.MAG.getUnicode(),
                    "Enchancer", "items.description.enchancer"
            ),

            STAR = new Item(ItemType.COLLECTABLE, "\uE335",
                    "Prize", "items.prize", "items.description.prize",
                    0, false, false, true
            ),
            // Left overs from v1 end

            LOOT_CRATE = new Item(ItemType.CRATE, EmoteReference.LOOT_CRATE.getDiscordNotation(),
                    "Loot Crate", "items.crate", "items.description.crate",
                    0, false, false, true,
                    (ctx, season) -> ItemHelper.openLootCrate(ctx, ItemType.LootboxType.RARE, 33, EmoteReference.LOOT_CRATE, 3, season)
            ),

            STAR_2 = new Item(ItemType.COMMON, EmoteReference.STAR.getUnicode(), "Consolation Prize",
                    "items.prize_2", "items.description.prize_2",
                    500, true, false, true
            ),

            SLOT_COIN = new Item(ItemType.COMMON, "\uD83C\uDF9F",
                    "Slot ticket", "items.slot_ticket", "items.description.slot_ticket",
                    65, true, true
            ),

            HOUSE = new Item(ItemType.COMMON, EmoteReference.HOUSE.getUnicode(),
                    "House", "items.house", "items.description.house",
                    750, true, true
            ),

            CAR = new Item(ItemType.COMMON, "\uD83D\uDE97",
                    "Car", "items.car", "items.description.car",
                    300, true, true
            ),

            // Chistmas event
            BELL_SPECIAL = new Item(ItemType.RARE, "\uD83D\uDD14",
                    "Christmas bell", "items.bell", "items.description.bell",
                    0, false, false, true
            ),

            CHRISTMAS_TREE_SPECIAL = new Item(ItemType.RARE, "\uD83C\uDF84",
                    "Christmas tree", "items.tree", "items.description.tree",
                    0, false, false, true
            ),
            // Chistmas event end

            PANTS = new Item(ItemType.COMMON, "\uD83D\uDC56",
                    "Pants", "items.pants", "items.description.pants",
                    20, true
            ),

            POTION_HASTE = new Potion(ItemType.POTION, 2, "\uD83C\uDF76",
                    "Haste Potion", "items.haste", "items.description.haste",
                    490, true
            ),

            POTION_CLEAN = new Item(ItemType.POTION, "\uD83C\uDF7C",
                    "Milky Potion", "items.milky", "items.description.milky",
                    50, true
            ),

            POTION_STAMINA = new Potion(ItemType.POTION, 3, "\uD83C\uDFFA",
                    "Energy Beverage", "items.energy", "items.description.energy",
                    450, true
            ),

            FISHING_ROD = new FishRod(ItemType.FISHROD, 3, 1, 25,
                    "\uD83C\uDFA3",
                    "Fishing Rod", "Rod", "items.rod", "items.description.rod",
                    65, true,
                    "", 40, 0
            ),

            FISH = new Fish(ItemType.FISHING, 1, "\uD83D\uDC1F",
                    "Fish", "items.fish", "items.description.fish",
                    10, false
            ),

            TROPICAL_FISH = new Fish(ItemType.FISHING, 2, "\uD83D\uDC20",
                    "Tropical Fish", "items.tropical_fish", "items.description.tropical_fish",
                    30, false
            ),

            BLOWFISH = new Fish(ItemType.FISHING, 3, "\uD83D\uDC21",
                    "Blowfish", "items.blowfish", "items.description.blowfish",
                    15, false
            ),

            COMET_GEM = new Item(ItemType.CAST_OBTAINABLE, "\u2604",
                    "Comet Gem", "items.comet_gem", "items.description.comet_gem",
                    40,
                    true, false,
                    "3;1", 51, 24
            ),

            STAR_GEM = new Item(ItemType.CAST_OBTAINABLE, "\ud83c\udf1f",
                    "Star Gem", "items.star_gem", "items.description.star_gem",
                    60,
                    true, false,
                    "4;1", 51, 25
            ),

            COBWEB = new Item(ItemType.MINE, "\uD83D\uDD78",
                    "Cobweb", "items.cobweb", "items.description.cobweb",
                    10, false
            ),

            GEM_FRAGMENT = new Item(ItemType.MINE, "\uD83D\uDCAB",
                    "Gem Fragment", "items.fragment", "items.description.fragment",
                    50, false
            ),

            MOP = new Item(ItemType.INTERACTIVE, "\uD83E\uDDF9",
                    "Mop", "items.mop", "items.description.mop",
                    10, true
            ),

            CLAIM_KEY = new Item(ItemType.WAIFU, "\uD83D\uDDDD",
                    "Claim Key", "items.claim_key", "items.description.claim_key",
                    1, false, true
            ),

            COFFEE = new Item(ItemType.COMMON, "\u2615",
                    "Coffee", "items.coffee", "items.description.coffee",
                    10, true
            ),

            WAIFU_PILL = new Potion(ItemType.POTION, 3, "\ud83d\udc8a",
                    "Waifu Pill", "items.waifu_pill", "items.description.waifu_pill",
                    370, true
            ),

            FISHING_BAIT = new Potion(ItemType.BUFF, 1, "\uD83D\uDC1B",
                    "Fishing Bait", "items.bait", "items.description.bait",
                    15, true
            ),

            DIAMOND_PICKAXE = new Pickaxe(ItemType.MINE_PICK, 0.16f,
                    1, 20, EmoteReference.DIAMOND_PICK.getDiscordNotation(),
                    "Diamond Pickaxe", "items.diamond_pick", "items.description.diamond_pick",
                    100, true, false,
                    "1;3;7", 150, 40, 10, 18, 101
            ),

            TELEVISION = new Item(ItemType.COMMON, "\uD83D\uDCFA",
                    "Television", "items.tv", "items.description.tv",
                    45, true
            ),

            WRENCH = new Wrench(ItemType.WRENCH, 65, 1, 1.0d,
                    "\ud83d\udd27",
                    "Wrench", "items.wrench", "items.description.wrench",
                    50, true
            ),

            MOTORCYCLE = new Item(ItemType.COMMON, "\uD83C\uDFCD",
                    "Motorcycle", "items.motorcycle", "items.description.motorcycle",
                    150, true
            ),

            COMET_PICKAXE = new Pickaxe(ItemType.MINE_PICK, 0.13f,
                    1, 10, EmoteReference.COMET_PICK.getDiscordNotation(),
                    "Comet Pickaxe", "items.comet_pick", "items.description.comet_pick",
                    290, true, false,
                    "1;2;7", 180, 100, 10, 48, 101
            ),

            STAR_PICKAXE = new Pickaxe(ItemType.MINE_PICK, 0.09f,
                    1, 10, EmoteReference.STAR_PICK.getDiscordNotation(),
                    "Star Pickaxe", "items.star_pick", "items.description.star_pick",
                    350, true, false,
                    "1;2;7", 220, 100,10, 49, 101
            ),

            PIZZA = new Item(ItemType.COMMON, "\uD83C\uDF55",
                    "Pizza", "items.pizza", "items.description.pizza",
                    15, true, false
            ),

            OLD_SPARKLE_FRAGMENT = new Item(ItemType.DEPRECATED, "\u200B",
                    "Old Sparkle Fragment", "general.deprecated", "general.deprecated",
                    0, false, false
            ),

            GEM5_PICKAXE = new Item(ItemType.DEPRECATED, "\u26cf",
                    "Old Sparkly Pickaxe", "general.deprecated", "general.deprecated",
                    550, true, false
            ),

            MINE_CRATE = new Item(ItemType.CRATE, EmoteReference.MINE_CRATE.getDiscordNotation(),
                    "Gem Crate", "items.mine_crate", "items.description.mine_crate",
                    0, false, false, true,
                    (ctx, season) ->
                            ItemHelper.openLootCrate(ctx, ItemType.LootboxType.MINE, 66, EmoteReference.MINE_CRATE, 3, season)
            ),

            FISH_CRATE = new Item(ItemType.CRATE, EmoteReference.FISH_CRATE.getDiscordNotation(),
                    "Fish Treasure", "items.fish_crate", "items.description.fish_crate",
                    0,
                    false, false, true,
                    (ctx, season) ->
                            ItemHelper.openLootCrate(ctx, ItemType.LootboxType.FISH, 67, EmoteReference.FISH_CRATE, 3, season)
            ),

            FISH_PREMIUM_CRATE = new Item(ItemType.CRATE, EmoteReference.PREMIUM_FISH_CRATE.getDiscordNotation(),
                    "Fish Premium Treasure", "items.fish_premium_crate", "items.description.fish_premium_crate",
                    0,
                    false, false, true,
                    (ctx, season) ->
                            ItemHelper.openLootCrate(ctx, ItemType.LootboxType.FISH_PREMIUM, 68, EmoteReference.PREMIUM_FISH_CRATE, 5, season)
            ),

            MINE_PREMIUM_CRATE = new Item(ItemType.CRATE, EmoteReference.PREMIUM_MINE_CRATE.getDiscordNotation(),
                    "Gem Premium Crate", "items.mine_premium_crate", "items.description.mine_premium_crate",
                    0,
                    false, false, true,
                    (ctx, season) ->
                            ItemHelper.openLootCrate(ctx, ItemType.LootboxType.MINE_PREMIUM, 69, EmoteReference.PREMIUM_MINE_CRATE, 5, season)
            ),

            COMET_ROD = new FishRod(ItemType.FISHROD, 6,
                    1, 15, EmoteReference.COMET_ROD.getDiscordNotation(),
                    "Comet Gem Rod", "Comet Rod", "items.comet_rod", "items.description.comet_rod",
                    150,
                    "1;2;3", 130, 44, 48, 101
            ),

            STAR_ROD = new FishRod(ItemType.FISHROD, 9,
                    1, 10, EmoteReference.STAR_ROD.getDiscordNotation(),
                    "Star Gem Rod", "Star Rod", "items.star_rod", "items.description.star_rod",
                    250,
                    "1;2;3", 170, 44, 49, 101
            ),

            OLD_SPARKLE_ROD = new FishRod(ItemType.DEPRECATED, 3,
                    -1, -1, "\uD83C\uDFA3",
                    "Old Sparkly Rod", "general.deprecated", "general.deprecated",
                    65,
                    "", 2
            ),

            SPARKLE_PICKAXE = new Pickaxe(ItemType.MINE_RARE_PICK, 0.04f,
                    3, 5, EmoteReference.SPARKLE_PICK.getDiscordNotation(),
                    "Sparkle Pickaxe", "items.sparkle_pick", "items.description.sparkle_pick",
                    1200, true, false,
                    "1;3;1;7", 450, 300, 10, 74, 18, 101
            ),

            SPARKLE_FRAGMENT = new Item(ItemType.MINE_RARE, "\u2728",
                    "Sparkle Fragment", "items.sparkle", "items.description.sparkle",
                    605, false
            ),

            SPARKLE_ROD = new FishRod(ItemType.FISHROD_RARE, 14,
                    3, 4, EmoteReference.SPARKLE_ROD.getDiscordNotation(),
                    "Sparkle Rod", "items.sparkle_rod", "items.description.sparkle_rod",
                    800,
                    "1;2;1;3", 300, 44, 74, 18, 101
            ),

            SHELL = new Fish(ItemType.FISHING_RARE, 5, "\uD83D\uDC1A",
                    "Shell", "items.shell", "items.description.shell",
                    1150, false
            ),

            SHARK = new Fish(ItemType.FISHING_RARE, 10, "\uD83E\uDD88",
                    "Shark", "items.shark", "items.description.shark",
                    500, false
            ),

            WRENCH_COMET = new Wrench(ItemType.WRENCH, 90,
                    3, 0.90d, EmoteReference.COMET_WRENCH.getDiscordNotation(),
                    "Comet Wrench", "items.star_wrench", "items.description.star_wrench",
                    200, true, false,
                    "1;2;2", 59, 48, 83
            ),

            WRENCH_SPARKLE = new Wrench(ItemType.WRENCH, 96,
                    4, 0.65d, EmoteReference.SPARKLE_WRENCH.getDiscordNotation(),
                    "Sparkle Wrench", "items.sparkle_wrench", "items.description.sparkle_wrench",
                    500, true, false,
                    "1;2;1;2;1", 59, 74, 18, 83, 84
            ),

            CRAB = new Fish(ItemType.FISHING, 2, "\uD83E\uDD80",
                    "Crab", "items.crab", "items.description.crab",
                    30, false
            ),

            SQUID = new Fish(ItemType.FISHING, 3, "\uD83E\uDD91",
                    "Squid", "items.squid", "items.description.squid",
                    35, false
            ),

            SHRIMP = new Fish(ItemType.FISHING, 3, "\uD83E\uDD90",
                    "Shrimp", "items.shrimp", "items.description.shrimp",
                    35, false
            ),

            MOON_RUNES = new Item(ItemType.MINE, "\uD83C\uDF19",
                    "Moon Runes", "items.moon", "items.description.moon",
                    100, false
            ),

            SNOWFLAKE = new Item(ItemType.MINE, "\u2744\uFE0F",
                    "Snowflake", "items.flake", "items.description.flake",
                    25, true, false,
                    "1;1", 51, 50
            ),

            BROKEN_SPARKLE_PICK = new Broken(73, EmoteReference.BROKEN_SPARKLE_PICK.getDiscordNotation(),
                    "Broken Sparkle Pickaxe", "items.broken_sparkle_pick", "items.description.broken_sparkle_pick",
                    100, "1,74;4,84;2,50"
            ),

            BROKEN_COMET_PICK = new Broken(61, EmoteReference.BROKEN_COMET_PICK.getDiscordNotation(),
                    "Broken Comet Pickaxe", "items.broken_comet_pick", "items.description.broken_comet_pick",
                    40, "1,48;3,84;2,50"
            ),

            BROKEN_STAR_PICK = new Broken(62, EmoteReference.BROKEN_STAR_PICK.getDiscordNotation(),
                    "Broken Star Pickaxe", "items.broken_star_pick", "items.description.broken_star_pick",
                    40, "1,49;3,84;3,50"
            ),

            BROKEN_SPARKLE_ROD = new Broken(75, EmoteReference.BROKEN_SPARKLE_ROD.getDiscordNotation(),
                    "Broken Sparkle Rod", "items.broken_sparkle_rod", "items.description.broken_sparkle_rod",
                    90, "1,74;4,84;2,50"
            ),

            BROKEN_COMET_ROD = new Broken(70, EmoteReference.BROKEN_COMET_ROD.getDiscordNotation(),
                    "Broken Comet Rod", "items.broken_comet_rod", "items.description.broken_comet_rod",
                    30, "1,48;3,84;2,50"
            ),

            BROKEN_STAR_ROD = new Broken(71, EmoteReference.BROKEN_STAR_ROD.getDiscordNotation(),
                    "Broken Star Rod", "items.broken_star_rod", "items.description.broken_star_rod",
                    30, "1,49;3,84;3,50"
            ),

            INCUBATOR_EGG = new Item(ItemType.PET, "\uD83E\uDD5A",
                    "Incubator Egg", "items.incubator_egg", "items.description.incubator_egg",
                    300, false, false,
                    "4;3;1", 11, 12, 18
            ),

            WATER_BOTTLE = new Water(ItemType.PET, "\ud83d\udeb0",
                    "Water Bottle", "items.water_bottle", "items.description.water_bottle",
                    20, true
            ),

            MAGIC_WATCH = new Item(ItemType.COLLECTABLE, "\u231A",
                    "Magical Watch", "items.magic_watch", "items.description.magic_watch",
                    0, false, false
            ),

            PET_HOUSE = new Item(ItemType.PET, EmoteReference.PET_HOUSE.getDiscordNotation(),
                    "Pet House", "items.pet_house", "items.description.pet_house",
                    170, true, true
            ),

            STEAK = new Food(Food.FoodType.GENERAL, 10, "\ud83e\udd69",
                    "Steak", "items.steak", "items.description.steak",
                    90, true
            ),

            CHICKEN = new Food(Food.FoodType.CAT, 10, "\ud83d\udc14",
                    "Chicken", "items.chicken", "items.description.chicken",
                    60, true
            ),

            MILK_2 = new Food(Food.FoodType.CAT, 10, "\ud83e\udd5b",
                    "Milk Bottle", "items.milk_3", "items.description.milk_3",
                    40, true
            ),

            DOG_FOOD = new Food(Food.FoodType.DOG, 10, "\ud83c\udf56",
                    "Dog Food", "items.dog_food", "items.description.dog_food",
                    75, true
            ),

            CAT_FOOD = new Food(Food.FoodType.CAT, 10, "\ud83e\udd6b",
                    "Cat Food", "items.cat_food", "items.description.cat_food",
                    75, true
            ),

            HAMSTER_FOOD = new Food(Food.FoodType.HAMSTER, 10, "\ud83c\udf31",
                    "Hamster Food", "items.hamster_food", "items.description.hamster_food",
                    60, true
            ),

            WOOD = new Item(ItemType.CHOP_DROP, "\ud83e\udeb5",
                    "Wood", "items.wood", "items.description.wood",
                    25, false
            ),

            MOON_PICK = new Pickaxe(ItemType.MINE_RARE_PICK, 0.1f,
                    2, 7, EmoteReference.MOON_PICK.getDiscordNotation(),
                    "Moon Pickaxe", "items.moon_pick", "items.description.moon_pick",
                    1000, true, false,
                    "1;3;2;5;10", 320, 130,10, 83, 18, 76, 101
            ),

            HELLFIRE_PICK = new Pickaxe(ItemType.MINE_RARE_PICK_NODROP, 0.00001f,
                    3, 1, EmoteReference.HELLFIRE_PICK.getDiscordNotation(),
                    "Hellfire Pickaxe", "items.hellfire_pick", "items.description.hellfire_pick",
                    15000, true, false,
                    "450;1;175;1;175;1;55;1;50;50", 3000, 1500, 18, 57, 48, 61, 49, 62, 74, 73, 101, 76
            ),

            AXE = new Axe(ItemType.CHOP_AXE, 0.19f, "\uD83E\uDE93",
                    "Axe", "items.axe", "items.description.axe",
                    100, true, 35, 0
            ),

            COMET_AXE = new Axe(ItemType.CHOP_AXE, 0.13f,
                    1, 10, EmoteReference.COMET_AXE.getDiscordNotation(),
                    "Comet Axe", "items.comet_axe", "items.description.comet_axe",
                    290, true, false,
                    "1;3;8", 170, 100, 104, 48, 101
            ),

            STAR_AXE = new Axe(ItemType.CHOP_AXE, 0.09f,
                    1, 10, EmoteReference.STAR_AXE.getDiscordNotation(),
                    "Star Axe", "items.star_axe", "items.description.star_axe",
                    350, true, false,
                    "1;3;7", 220, 100, 104, 49, 101
            ),

            SPARKLE_AXE = new Axe(ItemType.CHOP_RARE_AXE, 0.04f,
                    3, 5, EmoteReference.SPARKLE_AXE.getDiscordNotation(),
                    "Sparkle Axe", "items.sparkle_axe", "items.description.sparkle_axe",
                    1200, true, false,
                    "1;3;2;8", 500, 300, 104, 74, 18, 101
            ),

            MOON_AXE = new Axe(ItemType.CHOP_RARE_AXE, 0.1f,
                    2, 7, EmoteReference.MOON_AXE.getDiscordNotation(),
                    "Moon Axe", "items.moon_axe", "items.description.moon_axe",
                    1000, true, false,
                    "1;3;2;3;10", 350, 130, 104, 83, 18, 76, 101
            ),

            HELLFIRE_AXE = new Axe(ItemType.CHOP_RARE_AXE_NODROP, 0.00001f,
                    3, 1, EmoteReference.HELLFIRE_AXE.getDiscordNotation(),
                    "Hellfire Axe", "items.hellfire_axe", "items.description.hellfire_axe",
                    15000, true, false,
                    "450;175;1;175;1;55;1;50;50", 3100, 900, 18, 48, 105, 49, 106, 74, 107, 101, 76
            ),

            MOON_ROD = new FishRod(ItemType.FISHROD_RARE, 12, 2,
                    4, EmoteReference.MOON_ROD.getDiscordNotation(),
                    "Moon Rod", "items.moon_rod", "items.description.moon_rod",
                    800, "1;2;3;3", 200, 44, 83, 76, 101
            ),

            HELLFIRE_ROD = new FishRod(ItemType.FISHROD_RARE_NODROP, 14,
                    3, 4, EmoteReference.HELLFIRE_ROD.getDiscordNotation(),
                    "Hellfire Rod", "items.hellfire_rod", "items.description.hellfire_rod",
                    15000,
                    "450;175;1;175;1;50;1;35;50", 2300, 18, 48, 70, 49, 71, 74, 75, 101, 76
            ),

            LEAVES = new Item(ItemType.CHOP_DROP, "\ud83c\udf43",
                    "Leaves", "items.leaves", "items.description.leaves",
                    5, false
            ),

            APPLE = new Item(ItemType.CHOP_DROP, "\ud83c\udf4e",
                    "Apple", "items.apple", "items.description.apple",
                    10, false
            ),

            PEAR = new Item(ItemType.CHOP_DROP, "\ud83c\udf50",
                    "Pear", "items.pear", "items.description.pear", 10, false
            ),

            CHERRY_BLOSSOM = new Item(ItemType.CHOP_DROP, "\ud83c\udf38",
                    "Cherry Blossom", "items.cherry_blossom", "items.description.cherry_blossom",
                    5, false
            ),

            ROCK = new Item(ItemType.MINE, "\ud83e\udea8",
                    "Rock", "items.rock", "items.description.rock",
                    10, false
            ),

            BROKEN_MOON_PICK = new Broken(102, EmoteReference.BROKEN_MOON_PICK.getDiscordNotation(),
                    "Broken Moon Pickaxe", "items.broken_moon_pick", "items.description.broken_moon_pick",
                    40, "1,83;3,84;2,50"
            ),

            BROKEN_MOON_ROD = new Broken(110, EmoteReference.BROKEN_MOON_ROD.getDiscordNotation(),
                    "Broken Moon Rod", "items.broken_moon_rod", "items.description.broken_moon_rod",
                    40, "1,83;3,84;3,50"
            ),

            BROKEN_COMET_AXE = new Broken(105, EmoteReference.BROKEN_COMET_AXE.getDiscordNotation(),
                    "Broken Comet Axe", "items.broken_comet_axe", "items.description.broken_comet_axe",
                    30, "1,48;3,84;2,50"
            ),

            BROKEN_STAR_AXE = new Broken(106, EmoteReference.BROKEN_STAR_AXE.getDiscordNotation(),
                    "Broken Star Axe", "items.broken_star_axe", "items.description.broken_star_axe",
                    30, "1,49;3,84;2,50"
            ),

            BROKEN_SPARKLE_AXE = new Broken(107, EmoteReference.BROKEN_SPARKLE_AXE.getDiscordNotation(),
                    "Broken Sparkle Axe", "items.broken_sparkle_axe", "items.description.broken_sparkle_axe",
                    100, "1,74;5,84;3,50"
            ),

            BROKEN_MOON_AXE = new Broken(108, EmoteReference.BROKEN_MOON_AXE.getDiscordNotation(),
                    "Broken Moon Axe", "items.broken_moon_axe", "items.description.broken_moon_axe",
                    30, "1,83;5,84;3,50"
            ),

            BROKEN_HELLFIRE_PICK = new Broken(103, EmoteReference.BROKEN_HELLFIRE_PICK.getDiscordNotation(),
                    "Broken Hellfire Pick", "items.broken_hellfire_pick", "items.description.broken_hellfire_pick",
                    5000, "90,18;80,48;80,49;20,74;25,76;10,84;30,50"
            ),

            BROKEN_HELLFIRE_AXE = new Broken(109, EmoteReference.BROKEN_HELLFIRE_AXE.getDiscordNotation(),
                    "Broken Hellfire Axe", "items.broken_hellfire_axe", "items.description.broken_hellfire_axe",
                    5000, "90,18;80,48;80,49;20,74;25,76;10,84;30,50"
            ),

            BROKEN_HELLFIRE_ROD = new Broken(111, EmoteReference.BROKEN_HELLFIRE_ROD.getDiscordNotation(),
                    "Broken Hellfire Rod", "items.broken_hellfire_rod", "items.description.broken_hellfire_rod",
                    5000, "90,18;80,48;80,49;20,74;25,76;10,84;30,50"
            ),
    };
}
