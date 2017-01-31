package net.kodehawa.mantarobot.commands.info;

import net.kodehawa.mantarobot.utils.ExpirationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandStatsManager {
	private static final char ACTIVE_BLOCK = '\u2588';
	private static final char EMPTY_BLOCK = '\u200b';
	private static final ExpirationManager EXPIRATOR = new ExpirationManager();
	private static final int MINUTE = 60000, HOUR = 3600000, DAY = 86400000;
	private static final Map<String, AtomicInteger>
		TOTAL_CMDS = new HashMap<>(),
		DAY_CMDS = new HashMap<>(),
		HOUR_CMDS = new HashMap<>(),
		MINUTE_CMDS = new HashMap<>();

	public static String bar(int percent, int total) {
		int activeBlocks = (int) ((float) percent / 100f * total);
		StringBuilder builder = new StringBuilder().append('`').append(EMPTY_BLOCK);
		for (int i = 0; i < total; i++) builder.append(activeBlocks >= i ? ACTIVE_BLOCK : ' ');
		return builder.append(EMPTY_BLOCK).append('`').toString();
	}

	public static void log(String cmd) {
		if (cmd.isEmpty()) return;
		long millis = System.currentTimeMillis();
		TOTAL_CMDS.computeIfAbsent(cmd, k -> new AtomicInteger(0)).incrementAndGet();
		DAY_CMDS.computeIfAbsent(cmd, k -> new AtomicInteger(0)).incrementAndGet();
		HOUR_CMDS.computeIfAbsent(cmd, k -> new AtomicInteger(0)).incrementAndGet();
		MINUTE_CMDS.computeIfAbsent(cmd, k -> new AtomicInteger(0)).incrementAndGet();
		EXPIRATOR.letExpire(millis + MINUTE, () -> MINUTE_CMDS.get(cmd).decrementAndGet());
		EXPIRATOR.letExpire(millis + HOUR, () -> HOUR_CMDS.get(cmd).decrementAndGet());
		EXPIRATOR.letExpire(millis + DAY, () -> DAY_CMDS.get(cmd).decrementAndGet());
	}
}
