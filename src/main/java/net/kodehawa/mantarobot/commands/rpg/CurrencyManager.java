package net.kodehawa.mantarobot.commands.rpg;

import net.kodehawa.mantarobot.commands.rpg.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.data.MantaroData;

public class CurrencyManager {
	//Updated Jan 17
	public static final double USD_ESTIMATIVE = 1500000000000D; //1.5TRI

	public static double creditsWorth() {
		return USD_ESTIMATIVE / MantaroData.getData().get().users.values().stream()
			.mapToDouble(EntityPlayer::getMoney).sum();
	}
}