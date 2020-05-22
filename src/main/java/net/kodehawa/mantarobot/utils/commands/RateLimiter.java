/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.utils.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Prometheus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class defines the x ratelimit that will be taken into account when x user inputs a command.
 * The user will be not able to use the command until the ratelimit gets lifted, and instead it will send a message saying how much time is left (usually managed in
 * Currency commands themselves).
 * When the ratelimit gets reset, if the user tries to use the command again it will start all over again.
 * <p>
 * This class normally does the work of making abusable commands not-so abusable, like ~>loot. Also sorts daily or timely timeouts for other commands like daily and rep.
 * <p>
 * Made by natanbc, modified by Kodehawa.
 *
 * @since 01-06-2017
 */
public class RateLimiter {
    private static final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);

    static {
        Prometheus.THREAD_POOL_COLLECTOR.add("rate-limiter", ses);
    }

    private final long max;
    private final long timeout;
    private final ConcurrentHashMap<String, Pair<AtomicInteger, Long>> usersRateLimited = new ConcurrentHashMap<>();
    private boolean isPremiumAware = false;

    /**
     * Default constructor normally used in Currency commands to ratelimit all people.
     *
     * @param timeout How much time until the ratelimit gets lifted
     */
    public RateLimiter(TimeUnit timeUnit, int timeout) {
        this.max = 1;
        this.timeout = timeUnit.toMillis(timeout);
    }

    /**
     * Defines a premium-aware ratelimiter.
     * Premium users enjoy 25% less ratelimits on the bot in some commands.
     *
     * @param timeout How much time until the ratelimit gets lifted
     */
    public RateLimiter(TimeUnit timeUnit, int timeout, boolean isPremiumAware) {
        this.max = 1;
        this.isPremiumAware = isPremiumAware;
        this.timeout = timeUnit.toMillis(timeout);
    }

    /**
     * @param timeUnit The timeunit you'll input the RL time in. For example, TimeUnit#SECONDS.
     * @param max      How many times before you get ratelimited.
     * @param timeout  How much time until the ratelimit gets lifted.
     */
    public RateLimiter(TimeUnit timeUnit, int max, int timeout) {
        this.max = max;
        this.timeout = timeUnit.toMillis(timeout);
    }

    //Basically where you get b1nzy'd.
    public boolean process(String key) {
        boolean isPremium = isPremiumAware && MantaroData.db().getUser(key).isPremium();
        Pair<AtomicInteger, Long> p = usersRateLimited.get(key);

        //Put the user on the RL map if they aren't here already, but we already let him pass.
        if (p == null) {
            usersRateLimited.put(key, p = new Pair<>());
            p.first = new AtomicInteger();
        }

        AtomicInteger a = p.first;
        long i = a.get();
        if (i >= max) return false;

        a.incrementAndGet();
        long now = System.currentTimeMillis();

        Long tryAgain = p.second;
        if (tryAgain == null || tryAgain < now) {
            p.second = now + (isPremium ? (long) (timeout * 0.75) : timeout);
        }

        ses.schedule(a::decrementAndGet, isPremium ? (long) (timeout * 0.75) : timeout, TimeUnit.MILLISECONDS);
        return true;
    }

    //Method overload.
    public long tryAgainIn(String key) {
        Pair<AtomicInteger, Long> p = usersRateLimited.get(key);
        if (p == null || p.second == null)
            return 0;

        return Math.max(p.second - System.currentTimeMillis(), 0);
    }

    public long tryAgainIn(Member key) {
        return tryAgainIn(key.getUser().getId());
    }

    public long tryAgainIn(User key) {
        return tryAgainIn(key.getId());
    }

    public boolean process(User user) {
        return process(user.getId());
    }

    public boolean process(Member member) {
        return process(member.getUser());
    }

    public ConcurrentHashMap<String, Pair<AtomicInteger, Long>> getUsersRateLimited() {
        return this.usersRateLimited;
    }

    private static class Pair<F, S> {
        F first;
        S second;
    }
}
