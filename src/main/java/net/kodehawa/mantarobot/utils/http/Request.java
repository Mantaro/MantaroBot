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

import okhttp3.MediaType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Original: https://github.com/natanbc/GabrielBot/tree/master/src/main/java/gabrielbot/utils/http/Request.java
 * Licensed under GPLv3.
 * <3
 *
 * @author natanbc
 * @since 04/07/2017
 */
public class Request {
    private static final MediaType
            BINARY = MediaType.parse("application/octet-stream"),
            JSON = MediaType.parse("application/json"),
            TEXT = MediaType.parse("text/plain");

    final Map<String, String> headers = new HashMap<>();
    final String rateLimitKey;
    final HTTPRequester requester;
    final String url;
    byte[] body;
    MediaType type = BINARY;

    public Request(HTTPRequester requester, String url, String rateLimitKey) {
        this.requester = requester;
        this.url = url;
        this.rateLimitKey = rateLimitKey;
    }

    public Request header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public Request body(byte[] data) {
        this.type = BINARY;
        this.body = data;
        return this;
    }

    public Request body(String data) {
        this.type = TEXT;
        this.body = data.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    public Request body(JSONObject data) {
        this.type = JSON;
        this.body = data.toString().getBytes(StandardCharsets.UTF_8);
        return this;
    }

    public Request body(JSONArray data) {
        this.type = JSON;
        this.body = data.toString().getBytes(StandardCharsets.UTF_8);
        return this;
    }

    public Request mediaType(MediaType type) {
        this.type = type;
        return this;
    }

    public Request mediaType(String type) {
        return mediaType(MediaType.parse(type));
    }

    public Response get() throws RequestingException {
        return requester.get(this);
    }

    public Response post() throws RequestingException {
        return requester.post(this);
    }

    public HTTPRequester getRequester() {
        return requester;
    }
}
