package net.kodehawa.mantarobot.commands.currency;

import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.commands.custom.Holder;
import net.kodehawa.mantarobot.utils.Expirator;
import org.apache.commons.collections4.list.SetUniqueList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.Utils.randomOrder;

public class InventoryResolver {
	public static class Item {
		private final String emoji, name, plural, desc;
		private final int value;

		public Item(String emoji, String name, String plural, String desc, int value) {
			this.emoji = emoji;
			this.name = name;
			this.desc = desc;
			this.value = value;
			this.plural = name;
		}

		public Item(String emoji, String name, String desc, int value) {
			this(emoji, name, name, desc, value);
		}

		public String getDesc() {
			return desc;
		}

		public String getEmoji() {
			return emoji;
		}

		public String getName() {
			return name;
		}

		public String getPlural() {
			return plural;
		}

		public int getValue() {
			return value;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("InventoryResolver");
	public static final List<Item> ITEMS = SetUniqueList.setUniqueList(new ArrayList<>());
	private static final Map<String, List<Holder<Item>>> DROPPED_ITEMS = new HashMap<>();
	private static final Expirator EXPIRATOR = new Expirator();
	private static Random r = new Random(System.currentTimeMillis());

	static {
		ITEMS.add(new Item(":headphones:", "Headphones", "That's what happens when you listen to too much music. Should be worth something, tho.", 5));
		ITEMS.add(new Item(":hammer:", "Ban Hammer", "Ban Hammers", "Left by an admin. +INF Dmg", 20));
		ITEMS.add(new Item(":boot:", "Kick Boot", "Kick Boots", "Left by an admin. +INF Knockback", 15));
		ITEMS.add(new Item(":floppy_disk:", "Floppy Disk", "Floppy Disks", "Might have some games.", 10));
		ITEMS.add(new Item(":pencil:", "My Maths", "\"Oh, I forgot my maths.\"", 5));
		ITEMS.add(new Item(":ping_pong:", "Ping Racket", "Ping Rackets", "I won the ping-pong with Discord by a few miliseconds", 5));
		ITEMS.add(new Item(":game_die:", "Loaded dice", "Stolen from `~>dice` command", 5));
		ITEMS.add(new Item(":musical_note:", "Forgotten Music", "Forgotten Musics", "Never downloaded. Probably has Copyright.", 2));
		ITEMS.add(new Item(":pencil2:", "Custom Commands Pencil", "Custom Command Pencils", "We have plenty of those!", 5));
	}

	public static void drop(TextChannel channel, Item item) {
		if (!ITEMS.contains(item)) throw new IllegalArgumentException("Item is not registered.");
		String id = channel.getId();
		Holder<Item> itemHolder = new Holder<>(item);
		DROPPED_ITEMS.computeIfAbsent(id, k -> new ArrayList<>()).add(itemHolder);
		EXPIRATOR.letExpire(System.currentTimeMillis() + 120000, () -> {
			if (DROPPED_ITEMS.containsKey(id)) DROPPED_ITEMS.get(id).remove(itemHolder);
		});

		LOGGER.info(String.format("%s -> Dropped %s ``%s``", id, item.emoji, item.desc));
	}

	public static void drop(TextChannel channel, int item) {
		drop(channel, item(item));
	}

	public static void dropWithChance(TextChannel channel, Item item, int weight) {
		if (r.nextInt(weight) == 0) drop(channel, item);
	}

	public static void dropWithChance(TextChannel channel, int item, int weight) {
		dropWithChance(channel, item(item), weight);
	}

	public static Item item(int id) {
		return ITEMS.get(id);
	}

	public static List<Item> loot(TextChannel channel) {
		if (!DROPPED_ITEMS.containsKey(channel.getId())) return new ArrayList<>();
		return DROPPED_ITEMS.remove(channel.getId()).stream().map(Holder::get).collect(Collectors.toList());
	}

	public static Map<Item, Integer> organize(List<Item> items) {
		Map<Item, Integer> result = new HashMap<>();
		items.forEach(item -> result.compute(item, (k, v) -> v == null ? 1 : v++));
		return result;
	}

	public static String print(Map<Item, Integer> inventory) {
		if (inventory.isEmpty()) return "There's only dust.";
		return inventory.entrySet().stream().map(entry -> (entry.getKey().getEmoji() + " x " + entry.getValue())).sorted(randomOrder()).collect(Collectors.joining(", "));
	}

	public static Map<Item, Integer> resolve(Map<Integer, Integer> map) {
		Map<Item, Integer> result = new HashMap<>();
		map.forEach((i, j) -> result.put(item(i), j));
		return result;
	}

	public static Map<Integer, Integer> serialize(Map<Item, Integer> map) {
		Map<Integer, Integer> result = new HashMap<>();
		map.forEach((item, j) -> result.put(ITEMS.indexOf(item), j));
		return result;
	}
}
