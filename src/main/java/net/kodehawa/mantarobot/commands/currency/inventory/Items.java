package net.kodehawa.mantarobot.commands.currency.inventory;

import net.kodehawa.mantarobot.utils.Async;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

public class Items {
	public static final Item HEADPHONES, BAN_HAMMER, KICK_BOOT, FLOPPY_DISK, MY_MATHS, PING_RACKET, LOADED_DICE, FORGOTTEN_MUSIC, CC_PENCIL, OVERFLOWED_BAG, BROM_PICKAXE;
	//TODO USE UNICODE INSTEAD OF DISCORD NOTATION BECAUSE ***REASONS***
	public static final Item[] ALL = {
		HEADPHONES =
			new Item("\uD83C\uDFA7", "Headphones", "That's what happens when you listen to too much music. Should be worth something, tho.", 5),
		BAN_HAMMER =
			new Item("\uD83D\uDD28", "Ban Hammer", "Left by an admin. +INF Dmg", 20),
		KICK_BOOT =
			new Item("\uD83D\uDC62", "Kick Boot", "Left by an admin. +INF Knockback", 15),
		FLOPPY_DISK =
			new Item("\uD83D\uDCBE", "Floppy Disk", "Might have some games.", 10),
		MY_MATHS =
			new Item("\uD83D\uDCDD", "My Maths", "\"Oh, I forgot my maths.\"", 5),
		PING_RACKET =
			new Item("\uD83C\uDFD3", "Ping Racket", "I won the ping-pong with Discord by a few miliseconds", 5),
		LOADED_DICE =
			new Item("\uD83C\uDFB2", "Loaded dice", "Stolen from `~>dice` command", 5),
		FORGOTTEN_MUSIC =
			new Item("\uD83C\uDFB5", "Forgotten Music", "Never downloaded. Probably has Copyright.", 2),
		CC_PENCIL =
			new Item("\u270f", "Custom Commands Pencil", "We have plenty of those!", 5),
		OVERFLOWED_BAG =
			new Item("\uD83D\uDCB0", "Overflowed Moneybag", "Congratulations, you fucked up the game!", 1000),
		BROM_PICKAXE =
			new Item("\u26cf", "Brom's Pickaxe", "That guy liked Minecraft way too much.", 10)
	};

	static {
		Random r = new Random();
		Async.startAsyncTask("Market Thread", () -> Stream.of(ALL).forEach(item -> item.changePrices(r)), 3600);
	}

	public static Optional<Item> fromEmoji(String emoji) {
		return Stream.of(ALL).filter(item -> item.getEmoji().equals(emoji)).findFirst();
	}

	public static Item fromId(int id) {
		return ALL[id];
	}

	public static int idOf(Item item) {
		return Arrays.asList(ALL).indexOf(item);
	}
}