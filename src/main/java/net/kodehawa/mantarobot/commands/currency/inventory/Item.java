package net.kodehawa.mantarobot.commands.currency.inventory;

import java.util.Random;

public class Item {
	protected final boolean staticPrice;
	protected final long value;
	private final String emoji, name, desc;
	private boolean sellable, buyable;
	protected long price;

	public Item(String emoji, String name, String desc, long value) {
		this(emoji, name, desc, value, false, true, true);
	}

	public Item(String emoji, String name, String desc, long value, boolean staticPrice, boolean sellable, boolean buyable) {
		this.emoji = emoji;
		this.name = name;
		this.desc = desc;
		this.value = value;
		this.price = value;
		this.staticPrice = staticPrice;
		this.sellable = sellable;
		this.buyable = buyable;
	}

	public Item(String emoji, String name, String desc, long value, boolean sellable, boolean buyable) {
		this(emoji, name, desc, value, false, sellable, buyable);
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

	public boolean isBuyable(){
		return buyable;
	}

	public boolean isSellable(){
		return sellable;
	}

	public long getValue() {
		return price;
	}
}
