package net.kodehawa.mantarobot.commands.currency.item;

import br.com.brjdevs.java.utils.extensions.Async;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

public class Items {
	public static final Item HEADPHONES, BAN_HAMMER, KICK_BOOT, FLOPPY_DISK, MY_MATHS, PING_RACKET,
			LOADED_DICE, FORGOTTEN_MUSIC, CC_PENCIL, OVERFLOWED_BAG, BROM_PICKAXE, POTION_HEALTH, POTION_STAMINA, LEWD_MAGAZINE, RING, LOOT_CRATE_KEY,
			BOOSTER, B4NZY_BYPASS, BERSERK, ENHANCER, RING_2, COMPANION, LOADED_DICE_2;

	public static final Item[] ALL = {
		HEADPHONES =
			new Item("\uD83C\uDFA7", "Headphones", "That's what happens when you listen to too much music. Should be worth something, tho.", 50, true, false),
		BAN_HAMMER =
			new Item("\uD83D\uDD28", "Ban Hammer", "Left by an admin. +INF Dmg", 350, false, false),
		KICK_BOOT =
			new Item("\uD83D\uDC62", "Kick Boot", "Left by an admin. +INF Knockback", 90,  false),
		FLOPPY_DISK =
			new Item("\uD83D\uDCBE", "Floppy Disk", "Might have some games.", 80, false),
		MY_MATHS =
			new Item("\uD83D\uDCDD", "My Maths", "\"Oh, I forgot my maths.\"", 50, false),
		PING_RACKET =
			new Item("\uD83C\uDFD3", "Ping Racket", "I won the ping-pong with Discord by a few miliseconds", 50, false),
		LOADED_DICE =
			new Item("\uD83C\uDFB2", "Loaded Dice", "Stolen from `~>dice` command", 100, false),
		FORGOTTEN_MUSIC =
			new Item("\uD83C\uDFB5", "Forgotten Music", "Never downloaded. Probably has Copyright.", 50, false),
		CC_PENCIL =
			new Item("\u270f", "Pencil", "We have plenty of those!", 50, false),
		OVERFLOWED_BAG =
			new Item("\uD83D\uDCB0", "Moneybag", "What else?.", 1000, true),
		BROM_PICKAXE =
			new Item("\u26cf", "Brom's Pickaxe", "That guy liked Minecraft way too much. Gives you a stackable boost when doing ~>mine.", 500, true),
		POTION_HEALTH =
			new Item(EmoteReference.POTION1.getUnicode(), "Milk", "Good boy.", 600, true),
		POTION_STAMINA =
			new Item(EmoteReference.POTION2.getUnicode(), "Alcohol", "Hmm. I wonder what's this good for.", 650, true),
		LEWD_MAGAZINE =
			new Item(EmoteReference.MAGAZINE.getUnicode(), "Lewd Magazine", "Too many lewd commands.", 250, true),
		RING =
			new Item(EmoteReference.RING.getUnicode(), "Marriage Ring", "What basically makes your marriage official", 1000 , true),
		//SPECIAL ITEMS AND MISC NEEDED FOR THEM START HERE:
		LOOT_CRATE_KEY =
			new Item(EmoteReference.KEY.getUnicode(), "Create Key", "Used to open loot boxes.", 10000 , true),
		LOADED_DICE_2 =
			new Item("\uD83C\uDFB2", "Loaded Dice", "Stolen from `~>dice` command. Gives you a 50% more chance at getting a perfect score on dice."),
		B4NZY_BYPASS =
			new Item(EmoteReference.MEGA.getUnicode(), "Bypasser", "Bypasses ratelimits in commands with a ratio of 50%. Non-stackable."),
		BOOSTER =
			new Item(EmoteReference.RUNNER.getUnicode(), "Booster", "Gives you 5% more money in ~>loot and ~>daily per item. Stackable up to 10."),
		BERSERK =
			new Item(EmoteReference.CROSSED_SWORD.getUnicode(), "Berserk", "Gives you a 3% boost in gamble. Stackable up to 5."),
		COMPANION =
			new Item(EmoteReference.DOG.getUnicode(), "Companion", "Aw. Gives you a 10% boost in ~>daily. Not stackable."),
		RING_2 =
			new Item("\uD83D\uDC5A", "Special Ring.", "Gives you a extra boost on ~>daily when giving it to your loved one. Yes, I know the picture doesn't match."),
		ENHANCER =
			new Item(EmoteReference.MAG.getUnicode(), "Enchancer", "Gives you a higher possibility of finding money when doing ~>loot")
	};

	static {
		Random r = new Random();
		Async.task("Market Thread", () -> Stream.of(ALL).forEach(item -> item.changePrices(r)), 3600);
	}

	public static Optional<Item> fromAny(String any) {
		try {
			Item item = fromId(Integer.parseInt(any));
			if (item != null) return Optional.of(item);
		} catch (NumberFormatException ignored) {}

		Optional<Item> itemOptional;

		itemOptional = fromEmoji(any);
		if (itemOptional.isPresent()) return itemOptional;

		itemOptional = fromName(any);
		if (itemOptional.isPresent()) return itemOptional;

		itemOptional = fromPartialName(any);
		if (itemOptional.isPresent()) return itemOptional;

		return Optional.empty();
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