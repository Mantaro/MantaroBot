package net.kodehawa.mantarobot.modules;

public enum Category {
	MUSIC(CommandPermission.USER, "Audio"),
	ACTION(CommandPermission.USER, "Action"),
	CURRENCY(CommandPermission.USER, "Currency"),
	GAMES(CommandPermission.USER, "Game"),
	IMAGE(CommandPermission.USER, "Image"),
	FUN(CommandPermission.USER, "Fun"),
	MODERATION(CommandPermission.ADMIN, "Moderation"),
	OWNER(CommandPermission.OWNER, "Owner"),
	INFO(CommandPermission.USER, "Info"),
	UTILS(CommandPermission.USER, "Utility"),
	MISC(CommandPermission.USER, "Misc");

	public final CommandPermission permission;
	private final String s;

	Category(CommandPermission p, String s) {
		this.permission = p;
		this.s = s;
	}

	@Override
	public String toString() {
		return s;
	}
}
