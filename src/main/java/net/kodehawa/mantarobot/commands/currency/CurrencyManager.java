package net.kodehawa.mantarobot.commands.currency;

import net.kodehawa.mantarobot.data.MantaroData;

public class CurrencyManager {
	public static final long USD_ESTIMATIVE = 1500000000000L;
	public static double creditsWorth() {
		return USD_ESTIMATIVE / MantaroData.getData().get().users.values().stream()
			.mapToLong(userData -> userData.money).sum();
	}
}