/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.currency.item;

import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class Items {
    public static final Item HEADPHONES, BAN_HAMMER, KICK_BOOT, FLOPPY_DISK, MY_MATHS, PING_RACKET,
            LOADED_DICE, FORGOTTEN_MUSIC, CC_PENCIL, OVERFLOWED_BAG, BROM_PICKAXE, POTION_HEALTH, POTION_STAMINA, LEWD_MAGAZINE, RING,
            LOOT_CRATE_KEY,
            BOOSTER, BERSERK, ENHANCER, RING_2, COMPANION, LOADED_DICE_2, LOVE_LETTER, CLOTHES, SHOES, DIAMOND, CHOCOLATE, COOKIES,
            NECKLACE, ROSE,
            DRESS, TUXEDO, LOOT_CRATE, STAR, STAR_2, SLOT_COIN, HOUSE, CAR, BELL_SPECIAL, CHRISTMAS_TREE_SPECIAL, PANTS;

    public static final Item[] ALL = {
            HEADPHONES = new Item("\uD83C\uDFA7", "Headphones", "That's what happens when you listen to too much music. Should be worth " +
                    "something, tho.", 5, true, false),
            BAN_HAMMER = new Item("\uD83D\uDD28",
                    "Ban Hammer", "Left by an admin. +INF Dmg", 15, false),
            KICK_BOOT = new Item("\uD83D\uDC62",
                    "Kick Boot", "Left by an admin. +INF Knockback", 12, false),
            FLOPPY_DISK = new Item("\uD83D\uDCBE",
                    "Floppy Disk", "Might have some games.", 13, false),
            MY_MATHS = new Item("\uD83D\uDCDD",
                    "My Maths", "\"Oh, I forgot my maths.\"", 11, false),
            PING_RACKET = new Item("\uD83C\uDFD3",
                    "Ping Racket", "I won the game of ping-pong with Discord by a few milliseconds.", 15, false),
            LOADED_DICE = new Item("\uD83C\uDFB2",
                    "Loaded Die", "Stolen from `~>roll` command", 60, false),
            FORGOTTEN_MUSIC = new Item("\uD83C\uDFB5",
                    "Forgotten Music", "Never downloaded. Probably has been copyrighted.", 15, false),
            CC_PENCIL = new Item("\u270f",
                    "Pencil", "We have plenty of those!", 15, false),
            OVERFLOWED_BAG = new Item("\uD83D\uDCB0",
                    "Moneybag", "What else?.", 95, true),
            BROM_PICKAXE = new Item("\u26cf",
                    "Brom's Pickaxe", "That guy liked Minecraft way too much.", 75, true),
            POTION_HEALTH = new Item(EmoteReference.POTION1.getUnicode(),
                    "Milk", "Good boy.", 45, true),
            POTION_STAMINA = new Item(EmoteReference.POTION2.getUnicode(),
                    "Alcohol", "Hmm. I wonder what this is good for.", 45, true),
            LEWD_MAGAZINE = new Item(EmoteReference.MAGAZINE.getUnicode(),
                    "Lewd Magazine", "Too many lewd commands.", 45, true),
            RING = new Item(EmoteReference.RING.getUnicode(),
                    "Marriage Ring", "Basically what makes your marriage official", 60, true),
            LOVE_LETTER = new Item(EmoteReference.LOVE_LETTER.getUnicode(),
                    "Love Letter", "A letter from your beloved one.", 45, false),
            LOOT_CRATE_KEY = new Item(EmoteReference.KEY.getUnicode(),
                    "Crate Key", "Used to open loot boxes with `~>opencrate`", 58, true),
            CLOTHES = new Item(EmoteReference.CLOTHES.getUnicode(),
                    "Clothes", "Basically what you wear.", 15, true),
            DIAMOND = new Item(EmoteReference.DIAMOND.getUnicode(),
                    "Diamond", "Basically a better way of saving your money. It's shiny too.", 350, true),
            DRESS = new Item(EmoteReference.DRESS.getUnicode(),
                    "Wedding Dress", "Isn't it cute?", 75, true),
            NECKLACE = new Item(EmoteReference.NECKLACE.getUnicode(),
                    "Necklace", "Looks nice.", 17, true),
            TUXEDO = new Item(EmoteReference.TUXEDO.getUnicode(),
                    "Tuxedo", "What you wear when you're going to get married with a girl.", 24, true),
            SHOES = new Item(EmoteReference.SHOES.getUnicode(),
                    "Shoes", "Cause walking barefoot is just nasty.", 9, true),
            ROSE = new Item(EmoteReference.ROSE.getUnicode(),
                    "Rose", "The embodiment of your love.", 53, true),
            CHOCOLATE = new Item(EmoteReference.CHOCOLATE.getUnicode(),
                    "Chocolate", "Yummy.", 45, true),
            COOKIES = new Item(EmoteReference.COOKIE.getUnicode(),
                    "Cookie", "Delicious.", 48, true),

            // ---------------------------------- LEFT OVERS FROM CURRENCY V1 STARTS HERE ----------------------------------
            //CANNOT REMOVE BECAUSE WE WERE MEME ENOUGH TO FUCKING SAVE THEM BY THEIR IDS
            LOADED_DICE_2 = new Item("\uD83C\uDFB2",
                    "Special Loaded Die", "Even more loaded. `Leftover from Currency version 1. No longer obtainable.`"),
            BOOSTER = new Item(EmoteReference.RUNNER.getUnicode(),
                    "Booster", "Used to give you some kind of boost, now its broken. `Leftover from Currency version 1. No longer obtainable.`"),
            BERSERK = new Item(EmoteReference.CROSSED_SWORD.getUnicode(),
                    "Berserk", "Currency Berserker? Anyone? `Leftover from Currency version 1. No longer obtainable.`"),
            COMPANION = new Item(EmoteReference.DOG.getUnicode(),
                    "Companion", "Aw, such a cute dog. `Leftover from Currency version 1. No longer obtainable.`"),
            RING_2 = new Item("\uD83D\uDC5A",
                    "Special Ring", "It's so special, it's not even a ring. `Leftover from Currency version 1. No longer obtainable.`"),
            ENHANCER = new Item(EmoteReference.MAG.getUnicode(),
                    "Enchancer", "A broken enchanter, I wonder if it could be fixed? `Leftover from Currency version 1. No longer obtainable.`"),
            STAR = new Item(EmoteReference.STAR.getUnicode(),
                    "Prize", "Pretty much, huh? `Leftover from Currency version 1. No longer obtainable.`", 0, false, false, true),
            // ---------------------------------- LEFT OVERS FROM CURRENCY V1 END HERE ----------------------------------

            LOOT_CRATE = new Item(EmoteReference.LOOT_CRATE.getDiscordNotation(),
                    "Loot Crate", "You can use this along with a loot key to open a loot crate! `~>opencrate`", 0, false, false, true),
            STAR_2 = new Item(EmoteReference.STAR.getUnicode(),
                    "Prize 2", "In the first place, how did you get so much money?", 500, true, false, true),
            SLOT_COIN = new Item("\uD83C\uDF9F",
                    "Slot ticket", "Gives you extra chance in slots, also works as bulk storage.", 65, true, true),
            HOUSE = new Item(EmoteReference.HOUSE.getUnicode(),
                    "House", "Cozy place to live in.", 5000, true, true),
            CAR = new Item("\uD83D\uDE97",
                    "Car", "To move around.", 1000, true, true),

            // ---------------------------------- CHRISTMAS 2017 EVENT STARTS HERE ----------------------------------
            BELL_SPECIAL = new Item("\uD83D\uDD14", "Christmas bell",
                    "Christmas event 2017 reward. Gives you a cozy christmas feeling on your tree.", 0, false, false, true),
            CHRISTMAS_TREE_SPECIAL = new Item("\uD83C\uDF84", "Christmas tree",
                    "Christmas event 2017 reward. Who doesn't like a christmas tree?.", 0, false, false, true),
            // ---------------------------------- CHRISTMAS 2017 EVENT ENDS HERE ----------------------------------
            PANTS = new Item("\uD83D\uDC56", "Pants", "Basically what you wear on your legs... hopefully.", 20, true)
    };

    public static Optional<Item> fromAny(String any) {
        try {
            Item item = fromId(Integer.parseInt(any));

            if(item != null)
                return Optional.of(item);
        } catch(NumberFormatException ignored) {}

        return fromAnyNoId(any);
    }

    public static Optional<Item> fromAnyNoId(String any) {
        Optional<Item> itemOptional;

        itemOptional = fromEmoji(any);
        if(itemOptional.isPresent())
            return itemOptional;

        itemOptional = fromName(any);
        if(itemOptional.isPresent())
            return itemOptional;

        itemOptional = fromPartialName(any);
        return itemOptional;
    }

    public static Optional<Item> fromEmoji(String emoji) {
        return Stream.of(ALL).filter(item -> item.getEmoji().equals(emoji)).findFirst();
    }

    public static Item fromId(int id) {
        return ALL[id];
    }

    public static Optional<Item> fromName(String name) {
        return Arrays.stream(ALL).filter(item -> item.getName().toLowerCase().trim().equals(name.toLowerCase().trim())).findFirst();
    }

    public static Optional<Item> fromPartialName(String name) {
        return Arrays.stream(ALL).filter(item -> item.getName().toLowerCase().trim().contains(name.toLowerCase().trim())).findFirst();
    }

    public static int idOf(Item item) {
        return Arrays.asList(ALL).indexOf(item);
    }
}
