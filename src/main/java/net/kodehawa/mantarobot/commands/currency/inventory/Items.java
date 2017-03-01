package net.kodehawa.mantarobot.commands.currency.inventory;

import net.kodehawa.mantarobot.utils.Async;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

public class Items {
	public static final Item HEADPHONES, BAN_HAMMER, KICK_BOOT, FLOPPY_DISK, MY_MATHS, PING_RACKET, LOADED_DICE, FORGOTTEN_MUSIC, CC_PENCIL, OVERFLOWED_BAG, BROM_PICKAXE;

	public static final Item[] ALL = {
		HEADPHONES =
			new Item("\uD83C\uDFA7", "Headphones", "That's what happens when you listen to too much music. Should be worth something, tho.", 50, true, false),
		BAN_HAMMER =
			new Item("\uD83D\uDD28", "Ban Hammer", "Left by an admin. +INF Dmg", 20, true, false),
		KICK_BOOT =
			new Item("\uD83D\uDC62", "Kick Boot", "Left by an admin. +INF Knockback", 15, true, false),
		FLOPPY_DISK =
			new Item("\uD83D\uDCBE", "Floppy Disk", "Might have some games.", 15, true, false),
		MY_MATHS =
			new Item("\uD83D\uDCDD", "My Maths", "\"Oh, I forgot my maths.\"", 10, true, false),
		PING_RACKET =
			new Item("\uD83C\uDFD3", "Ping Racket", "I won the ping-pong with Discord by a few miliseconds", 25, true, false),
		LOADED_DICE =
			new Item("\uD83C\uDFB2", "Loaded dice", "Stolen from `~>dice` command", 30, true, false),
		FORGOTTEN_MUSIC =
			new Item("\uD83C\uDFB5", "Forgotten Music", "Never downloaded. Probably has Copyright.", 5, true, false),
		CC_PENCIL =
			new Item("\u270f", "Pencil", "We have plenty of those!", 5, true, false),
		OVERFLOWED_BAG =
			new Item("\uD83D\uDCB0", "Overflowed Moneybag", "A reward from gaining too much money while testing.", 2, true, false) {
				@Override public void changePrices(Random r) {
					price = value << (r.nextInt(10) + 15);
				}
			},
		BROM_PICKAXE =
			new Item("\u26cf", "Brom's Pickaxe", "That guy liked Minecraft way too much. Gives you a stackable boost when doing ~>mine.", 100, false, true)
	};

	static {
		Random r = new Random();
		Async.startAsyncTask("Market Thread", () -> Stream.of(ALL).forEach(item -> item.changePrices(r)), 3600);
	}

	public static Optional<Item> fromEmoji(String emoji) {
		return Stream.of(ALL).filter(item -> item.getEmoji().equals(emoji)).findFirst();
	}

	public static Optional<Item> fromName(String name) {
		return Arrays.stream(ALL).filter(item -> item.getName().toLowerCase().trim().equals(name.toLowerCase().trim())).findFirst();
	}

	public static Optional<Item> fromPartialName(String name) {
		return Arrays.stream(ALL).filter(item -> item.getName().toLowerCase().trim().contains(name.toLowerCase().trim())).findFirst();
	}

	public static Optional<Item> fromAny(String any) {
		try {
			Item item = fromId(Integer.parseInt(any));
			if (item != null) return Optional.of(item);
		} catch (NumberFormatException ignored) {
		}

		Optional<Item> itemOptional;

		itemOptional = fromEmoji(any);
		if (itemOptional.isPresent()) return itemOptional;

		itemOptional = fromName(any);
		if (itemOptional.isPresent()) return itemOptional;

		itemOptional = fromPartialName(any);
		if (itemOptional.isPresent()) return itemOptional;

		return Optional.empty();
	}

	public static Item fromId(int id) {
		return ALL[id];
	}

	public static int idOf(Item item) {
		return Arrays.asList(ALL).indexOf(item);
	}
}