package net.kodehawa.mantarobot.utils.http;

import java.io.IOException;

/**
 * Original: https://github.com/natanbc/GabrielBot/tree/master/src/main/java/gabrielbot/utils/http/RequestingException.java
 * Licensed under GPLv3.
 * <3
 *
 * @author natanbc
 * @since 04/07/2017
 */
public class RequestingException extends IOException {
    private final Request request;
    private final String requesterIdentifier;

    public RequestingException(Request request, String requesterIdentifier, Throwable cause) {
        super(cause);
        this.request = request;
        this.requesterIdentifier = requesterIdentifier;
    }

    public Request getRequest() {
        return request;
    }

    public String getRequesterIdentifier() {
        return requesterIdentifier;
    }
}
