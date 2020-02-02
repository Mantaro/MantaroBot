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

public class RateLimit {
    private final long timestamp;
    private final int triesLeft;
    private final long cooldown;
    private final int spamAttempts;
    
    RateLimit(long timestamp, int triesLeft, long cooldown, int spamAttempts) {
        this.timestamp = timestamp;
        this.triesLeft = triesLeft;
        this.cooldown = cooldown;
        this.spamAttempts = spamAttempts;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getTriesLeft() {
        return triesLeft;
    }
    
    public long getCooldown() {
        return cooldown;
    }
    
    public int getSpamAttempts() {
        return spamAttempts;
    }
    
    public long getCooldownReset() {
        return timestamp + cooldown;
    }
    
    @Override
    public String toString() {
        return "RateLimit{triesLeft=" + triesLeft + ", cooldown=" + cooldown + ", spamAttempts=" + spamAttempts + "}";
    }
}
