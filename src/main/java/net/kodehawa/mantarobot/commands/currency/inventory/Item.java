package net.kodehawa.mantarobot.commands.currency.inventory;

import java.util.Random;

public class Item {
	private final String emoji, name, desc;
	private final long value;
	private long price;
	private boolean staticPrice;

	public Item(String emoji, String name, String desc, long value) {
		this.emoji = emoji;
		this.name = name;
		this.desc = desc;
		this.value = value;
		this.price = value;
		this.staticPrice = false;
	}

	public Item(String emoji, String name, String desc, long value, boolean staticPrice) {
		this(emoji, name, desc, value);
		this.staticPrice = staticPrice;
	}

	public void changePrices(Random r) {
		if (staticPrice) return;
		long min = (long)(value * 0.9), max = (long)(value * 1.1), dif = max - min;
		price = min + r.nextInt((int) dif);
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

	public long getValue() {
		return price;
	}
}
