package net.kodehawa.mantarobot.commands.custom;

import net.dv8tion.jda.core.entities.TextChannel;

import java.util.HashSet;
import java.util.Set;

public class TextChannelLock {
	private static final Set<String> LOCKS = new HashSet<>();

	public static Runnable adquireLock(TextChannel channel) {
		if (LOCKS.contains(channel.getId())) return null;
		LOCKS.add(channel.getId());

		return () -> LOCKS.remove(channel.getId());
	}
}
