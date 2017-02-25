package net.kodehawa.mantarobot.commands.currency;

import net.kodehawa.mantarobot.data.MantaroData;

public class CurrencyManager {
	//Updated Jan 17
	public static final long USD_ESTIMATIVE = 1500000000000L;

	public static double creditsWorth() {
		return (double)USD_ESTIMATIVE / (double)MantaroData.getData().get().users.values().stream()
			.mapToLong(userData -> userData.money).sum();
	}
}