package net.kodehawa.mantarobot.commands.currency.inventory;

public class Item {
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
