package net.kodehawa.mantarobot.commands.rpg.item;

import java.util.Random;

public class Item {
	protected final boolean staticPrice;
	protected final long value;
	private final String emoji, name, desc;
	protected long price, maxSize;
	private boolean sellable, buyable;

	public Item(String emoji, String name, String desc, long value) {
		this(emoji, name, desc, value, false, true, true, 100);
	}

	public Item(String emoji, String name, String desc, long value, boolean staticPrice, boolean sellable, boolean buyable, long maxSize) {
		this.emoji = emoji;
		this.name = name;
		this.desc = desc;
		this.value = value;
		this.price = value;
		this.staticPrice = staticPrice;
		this.sellable = sellable;
		this.buyable = buyable;
		this.maxSize = maxSize;
	}

	public Item(String emoji, String name, String desc, long value, boolean sellable, boolean buyable) {
		this(emoji, name, desc, value, false, sellable, buyable, 100);
	}

	public void changePrices(Random r) {
		if (staticPrice) return;
		long min = (long) (value * 0.9), max = (long) (value * 1.1), dif = max - min;
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

	public boolean isBuyable() {
		return buyable;
	}

	public boolean isSellable() {
		return sellable;
	}

	public long maxSize() {
		return maxSize;
	}
}
