package net.kodehawa.mantarobot.modules;

public enum Category {
	MUSIC("Audio"),
	ACTION("Action"),
	RPG("RPG"),
	GAMES("Game"),
	IMAGE("Image"),
	FUN("Fun"),
	MODERATION("Moderation"),
	OWNER("Owner"),
	INFO("Info"),
	UTILS("Utility"),
	MISC("Misc");

	private final String s;

	Category(String s) {
		this.s = s;
	}

	@Override
	public String toString() {
		return s;
	}
}
