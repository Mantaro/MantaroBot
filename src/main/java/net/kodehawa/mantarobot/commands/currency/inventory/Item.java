package net.kodehawa.mantarobot.commands.currency.inventory;

public class Item {
	private final String emoji, name, desc;
	private final long value;

	public Item(String emoji, String name, String desc, long value) {
		this.emoji = emoji;
		this.name = name;
		this.desc = desc;
		this.value = value;
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
		return value;
	}
}
