package net.kodehawa.mantarobot.utils.http;

/**
 * Original: https://github.com/natanbc/GabrielBot/tree/master/src/main/java/gabrielbot/utils/http/RateLimitedException.java
 * Licensed under GPLv3.
 * <3
 *
 * @author natanbc
 * @since 04/07/2017
 */
public class RateLimitedException extends RuntimeException {
    private final long tryAgainIn;

    public RateLimitedException(long tryAgainIn) {
        this.tryAgainIn = tryAgainIn;
    }

    public long getTryAgainIn() {
        return tryAgainIn;
    }
}
