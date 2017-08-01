package net.kodehawa.mantarobot.commands.currency.item;

import br.com.brjdevs.java.utils.async.Async;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Items {
    public static final Item HEADPHONES, BAN_HAMMER, KICK_BOOT, FLOPPY_DISK, MY_MATHS, PING_RACKET,
            LOADED_DICE, FORGOTTEN_MUSIC, CC_PENCIL, OVERFLOWED_BAG, BROM_PICKAXE, POTION_HEALTH, POTION_STAMINA, LEWD_MAGAZINE, RING,
            LOOT_CRATE_KEY,
            BOOSTER, BERSERK, ENHANCER, RING_2, COMPANION, LOADED_DICE_2, LOVE_LETTER, CLOTHES, SHOES, DIAMOND, CHOCOLATE, COOKIES,
            NECKLACE, ROSE,
            DRESS, TUXEDO, LOOT_CRATE, STAR, STAR_2, SLOT_COIN;

    public static final Item[] ALL = {
            HEADPHONES = new Item("\uD83C\uDFA7", "Headphones", "That's what happens when you listen to too much music.  Should be worth " +
                    "something, tho.", 5, true, false),
            BAN_HAMMER = new Item("\uD83D\uDD28", "Ban Hammer", "Left by an admin. +INF Dmg", 15, false),
            KICK_BOOT = new Item("\uD83D\uDC62", "Kick Boot", "Left by an admin. +INF Knockback", 12, false),
            FLOPPY_DISK = new Item("\uD83D\uDCBE", "Floppy Disk", "Might have some games.", 13, false),
            MY_MATHS = new Item("\uD83D\uDCDD", "My Maths", "\"Oh, I forgot my maths.\"", 11, false),
            PING_RACKET = new Item("\uD83C\uDFD3", "Ping Racket", "I won the game of ping-pong with Discord by a few miliseconds.", 15, false),
            LOADED_DICE = new Item("\uD83C\uDFB2", "Loaded Die", "Stolen from `~>roll` command", 60, false),
            FORGOTTEN_MUSIC = new Item("\uD83C\uDFB5", "Forgotten Music", "Never downloaded. Probably has been copyrighted.", 15, false),
            CC_PENCIL = new Item("\u270f", "Pencil", "We have plenty of those!", 15, false),
            OVERFLOWED_BAG = new Item("\uD83D\uDCB0", "Moneybag", "What else?.", 80, true),
            BROM_PICKAXE = new Item("\u26cf", "Brom's Pickaxe", "That guy liked Minecraft way too much.", 55, true),
            POTION_HEALTH = new Item(EmoteReference.POTION1.getUnicode(), "Milk", "Good boy.", 45, true),
            POTION_STAMINA = new Item(EmoteReference.POTION2.getUnicode(), "Alcohol", "Hmm. I wonder what this is good for.", 45, true),
            LEWD_MAGAZINE = new Item(EmoteReference.MAGAZINE.getUnicode(), "Lewd Magazine", "Too many lewd commands.", 45, true),
            RING = new Item(EmoteReference.RING.getUnicode(), "Marriage Ring", "What basically makes your marriage official", 60, true),
            LOVE_LETTER = new Item(EmoteReference.LOVE_LETTER.getUnicode(), "Love Letter", "A letter from your beloved one.", 55, false),
            LOOT_CRATE_KEY = new Item(EmoteReference.KEY.getUnicode(), "Crate Key", "Used to open loot boxes.", 58, true),
            CLOTHES = new Item(EmoteReference.CLOTHES.getUnicode(), "Clothes", "Basically what you wear, but 100x more expensive.", 15,true),
            DIAMOND = new Item(EmoteReference.DIAMOND.getUnicode(), "Diamond", "Basically a better way of saving your money. It's shiny too.", 350, true),
            DRESS = new Item(EmoteReference.DRESS.getUnicode(), "Wedding Dress", "Isn't it cute?", 75, true),
            NECKLACE = new Item(EmoteReference.NECKLACE.getUnicode(), "Necklace", "Looks nice.", 17, true),
            TUXEDO = new Item(EmoteReference.TUXEDO.getUnicode(), "Tuxedo", "What you use when you're going to get married with a girl.", 24, true),
            SHOES = new Item(EmoteReference.SHOES.getUnicode(), "Shoes", "Something you use when you don't want to walk on dirt.", 9, true),
            ROSE = new Item(EmoteReference.ROSE.getUnicode(), "Rose", "The embodiment of your love.", 53, true),
            CHOCOLATE = new Item(EmoteReference.CHOCOLATE.getUnicode(), "Chocolate", "Yummy.", 45, true),
            COOKIES = new Item(EmoteReference.COOKIE.getUnicode(), "Cookie", "Delicious.", 48, true),
            LOADED_DICE_2 = new Item("\uD83C\uDFB2", "Loaded Die", "Stolen from `~>dice` command. Gives you a 50% more chance at getting a perfect score on dice."),
            BOOSTER = new Item(EmoteReference.RUNNER.getUnicode(), "Booster", "Gives you 5% more money in ~>loot and ~>daily per item. Stackable up to 10."),
            BERSERK = new Item(EmoteReference.CROSSED_SWORD.getUnicode(), "Berserk", "Gives you a 2% boost in gamble profits. Stackable up to 5."),
            COMPANION = new Item(EmoteReference.DOG.getUnicode(), "Companion", "Aw. Gives you a 10% boost in ~>daily. Not stackable."),
            RING_2 = new Item("\uD83D\uDC5A", "Special Ring", "Gives you a extra boost on ~>daily when giving it to your loved one. Yes, I know the picture doesn't match."),
            ENHANCER = new Item(EmoteReference.MAG.getUnicode(), "Enchancer", "Gives you a higher possibility of getting a win on ~>gamble. Non-stackable."),
            STAR = new Item(EmoteReference.STAR.getUnicode(), "Prize", "Pretty much, huh?",0, false, false, true),
            LOOT_CRATE = new Item(EmoteReference.LOOT_CRATE.getDiscordNotation(), "Loot Crate", "You can use this along with a loot key to open a loot crate!", 0,  false, false, true),
            STAR_2 = new Item(EmoteReference.STAR.getUnicode(), "Prize 2", "In the first place, how did you get so much money?", 500, true, false, true),
            SLOT_COIN = new Item("\uD83C\uDF9F", "Slot ticket", "Gives you extra chance in slots, also works as bulk storage.", 65, true, true)
    };

    static {
        Random r = new Random();
        Async.task("Market Thread", () -> Stream.of(ALL).forEach(item -> item.changePrices(r)), 30, TimeUnit.MINUTES);
    }

    public static Optional<Item> fromAny(String any) {
        try {
            Item item = fromId(Integer.parseInt(any));
            if (item != null) return Optional.of(item);
        }
        catch (NumberFormatException ignored) {
        }

        Optional<Item> itemOptional;

        itemOptional = fromEmoji(any);
        if (itemOptional.isPresent()) return itemOptional;

        itemOptional = fromName(any);
        if (itemOptional.isPresent()) return itemOptional;

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
