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
