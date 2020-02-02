/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.utils.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.data.MantaroData;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class NewRateLimiter {
    private final ReferenceCountedMap map = new ReferenceCountedMap();
    private final ScheduledExecutorService executor;
    private final long timeoutMillis;
    private final long delta;
    private final int spamThreshold;
    private int limit = 1;
    private boolean isPremiumAware = false;
    
    public NewRateLimiter(ScheduledExecutorService executor, int limit, int spamThreshold, long timeoutMillis, long delta) {
        this.executor = executor;
        this.limit = limit;
        this.spamThreshold = spamThreshold;
        this.timeoutMillis = timeoutMillis;
        this.delta = delta;
    }
    
    public NewRateLimiter(ScheduledExecutorService executor, int limit, int spamThreshold, long timeout, TimeUnit unit, long delta) {
        this.executor = executor;
        this.limit = limit;
        this.spamThreshold = spamThreshold;
        this.timeoutMillis = unit.toMillis(timeout);
        this.delta = delta;
    }
    
    public NewRateLimiter(ScheduledExecutorService executor, int spamThreshold, long timeout, TimeUnit unit, long delta) {
        this.executor = executor;
        this.spamThreshold = spamThreshold;
        this.timeoutMillis = unit.toMillis(timeout);
        this.delta = delta;
    }
    
    public NewRateLimiter(ScheduledExecutorService executor, int limit, int spamThreshold, long timeout, TimeUnit unit, long delta, boolean isPremiumAware) {
        this.executor = executor;
        this.limit = limit;
        this.spamThreshold = spamThreshold;
        this.timeoutMillis = unit.toMillis(timeout);
        this.delta = delta;
        this.isPremiumAware = isPremiumAware;
    }
    
    public NewRateLimiter(ScheduledExecutorService executor, int spamThreshold, long timeout, TimeUnit unit, long delta, boolean isPremiumAware) {
        this.executor = executor;
        this.spamThreshold = spamThreshold;
        this.timeoutMillis = unit.toMillis(timeout);
        this.delta = delta;
        this.isPremiumAware = isPremiumAware;
    }
    
    public NewRateLimiter(ScheduledExecutorService executor, int limit, int spamThreshold, long timeoutMillis, long delta, boolean isPremiumAware) {
        this.executor = executor;
        this.limit = limit;
        this.spamThreshold = spamThreshold;
        this.timeoutMillis = timeoutMillis;
        this.delta = delta;
        this.isPremiumAware = isPremiumAware;
    }
    
    protected void onSpamDetected(String key, int times) {
    }
    
    protected long getCoolDown(String key) {
        return timeoutMillis + ThreadLocalRandom.current().nextLong(delta);
    }
    
    public boolean test(String key) {
        if(executor.isShutdown()) {
            throw new IllegalStateException("The executor has been shut down! No ratelimits can be processed");
        }
        
        boolean isPremium = isPremiumAware && MantaroData.db().getUser(key).isPremium();
        ReferenceCountedLimit l = map.get(key);
        if(l.times >= limit) {
            l.attemptsAfterRateLimited++;
            l.referenceCount--;
            if(l.attemptsAfterRateLimited >= spamThreshold) {
                onSpamDetected(key, l.attemptsAfterRateLimited);
            }
            return false;
        }
        
        long now = System.currentTimeMillis();
        if(l.tryAgainIn < now) {
            l.tryAgainIn = now + (isPremium ? (long) (timeoutMillis * 0.75) : timeoutMillis);
        }
        
        executor.schedule(() -> {
            map.dispose(key, l);
            l.times--;
            l.attemptsAfterRateLimited = 0;
        }, getCoolDown(key), TimeUnit.MILLISECONDS);
        
        return ++l.times <= limit;
    }
    
    public long tryAgainIn(String key) {
        ReferenceCountedLimit l = map.getDirect(key);
        if(l == null)
            return 0;
        
        return Math.max(l.tryAgainIn - System.currentTimeMillis(), 0);
    }
    
    public long tryAgainIn(Member key) {
        return tryAgainIn(key.getUser().getId());
    }
    
    public long tryAgainIn(User key) {
        return tryAgainIn(key.getId());
    }
    
    public ScheduledExecutorService getExecutor() {
        return executor;
    }
    
    private static class ReferenceCountedMap {
        private final ConcurrentHashMap<String, ReferenceCountedLimit> map = new ConcurrentHashMap<>();
        
        ReferenceCountedLimit get(String key) {
            return map.compute(key, (k, v) -> {
                if(v != null) {
                    v.referenceCount++;
                    return v;
                }
                return new ReferenceCountedLimit();
            });
        }
        
        ReferenceCountedLimit getDirect(String key) {
            return map.get(key);
        }
        
        void dispose(String key, ReferenceCountedLimit object) {
            map.compute(key, (k, v) -> {
                if(v == object) {
                    int i = --v.referenceCount;
                    if(i <= 0) return null;
                    return v;
                }
                
                return null;
            });
        }
    }
    
    private static class ReferenceCountedLimit {
        int referenceCount = 1;
        int times;
        int attemptsAfterRateLimited;
        long tryAgainIn;
    }
}
