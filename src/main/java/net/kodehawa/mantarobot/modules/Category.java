package net.kodehawa.mantarobot.modules;

public enum Category {
	ACTION("ActionCmds"), FUN("Fun"), AUDIO("Audio"), INFO("Info"), MISC("Misc"), GAMES("Games"), MODERATION("Moderation"), CUSTOM("Custom"), OWNER("Owner");

	private final String s;

	Category(String s) {
		this.s = s;
	}

	@Override
	public String toString() {
		return s;
	}
}
