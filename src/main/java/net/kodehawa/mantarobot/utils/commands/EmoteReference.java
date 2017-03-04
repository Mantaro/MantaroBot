package net.kodehawa.mantarobot.utils.commands;

import java.util.Optional;

public enum EmoteReference {

	ERROR(":heavy_multiplication_x:", "\u2716"), ERROR2(":x:", "\u274C"), DICE(":game_die:", "\uD83C\uDFB2"), SAD(":frowning:", "\uD83D\uDE26"),
	CORRECT(":white_check_mark:", "\u2705"), OK(":ok_hand:", "\uD83D\uDC4C"), STOP(":octagonal_sign:", "\uD83D\uDED1"), TALKING(":speech_balloon:", "\uD83D\uDCAC"),
	CRYING(":sob:", "\uD83D\uDE2D"), WARNING(":warning:", "\u26a0"), POPPER(":tada:", "\uD83C\uDF89"), ZAP(":zap:", "\u26a1"), MEGA(":mega:", "\uD83D\uDCE3"),
	CONFUSED(":confused:", "\uD83D\uDE15"), WORRIED(":worried:", "\uD83D\uDE1F"), THINKING(":thinking:", "\uD83E\uDD14"), STOPWATCH(":stopwatch:", "\u23f1"),
	BUY(":inbox_tray:", "\uD83D\uDCE5"), SELL(":outbox_tray:", "\uD83D\uDCE4"), MARKET(":shopping_car:", "\uD83D\uDED2"), MONEY(":money_bag:", "\uD83D\uDCB0"),
	PENCIL(":pencil:", "\uD83D\uDCDD"), SMILE(":smile:", "\uD83D\uDE04"), PICK(":pick:", "\u26cf"), HEART(":heart:", "\u2764"), RUNNER(":runner:", "\uD83C\uDFC3");

	String discordNotation;
	String unicode;

	EmoteReference(String discordNotation, String unicode) {
		this.discordNotation = discordNotation;
		this.unicode = unicode;
	}

	@Override
	public String toString() {
		return Optional.ofNullable(unicode).orElse(discordNotation) + " ";
	}

	public String getDiscordNotation() {
		return discordNotation;
	}

	public String getUnicode() {
		return unicode;
	}
}
