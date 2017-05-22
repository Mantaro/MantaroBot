package net.kodehawa.mantarobot.commands.currency;

import lombok.Getter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.utils.Expirator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Made by @AdrianTodt
 */
public class RateLimiter {
	private static final Expirator EXPIRATOR = new Expirator();
	private final int timeout;
	@Getter private final HashMap<String, Long> usersRateLimited = new HashMap<>();

	public RateLimiter(int timeout) {
		this.timeout = timeout;
	}

	public RateLimiter(TimeUnit unit, int timeout) {
		this.timeout = (int) unit.toMillis(timeout);
	}

	public boolean process(String userId) {
		if (usersRateLimited.containsKey(userId)) return false;
		long expiration = System.currentTimeMillis() + timeout;
		usersRateLimited.put(userId, expiration);
		Expirator.Expirable ex = () -> usersRateLimited.remove(userId);
		EXPIRATOR.put(expiration, ex);
		return true;
	}

	public boolean process(User user) {
		return process(user.getId());
	}

	public boolean process(Member member) {
		return process(member.getUser());
	}
}
