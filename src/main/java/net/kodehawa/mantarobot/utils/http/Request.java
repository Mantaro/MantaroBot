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
    final HTTPRequester requester;
    final String url;
    final String rateLimitKey;
    MediaType type = BINARY;
    byte[] body;

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
