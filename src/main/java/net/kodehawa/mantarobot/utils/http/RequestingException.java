/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

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
