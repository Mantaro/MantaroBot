package net.kodehawa.mantarobot.utils.http;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Original: https://github.com/natanbc/GabrielBot/tree/master/src/main/java/gabrielbot/utils/http/Response.java
 * Licensed under GPLv3.
 * <3
 *
 * @author natanbc
 * @since 04/07/2017
 */
public class Response {
    private final byte[] data;
    private final int code;
    private final Map<String, List<String>> headers;

    public Response(byte[] data, int code, Map<String, List<String>> headers) {
        this.data = data;
        this.code = code;
        this.headers = Collections.unmodifiableMap(headers);
    }

    public int code() {
        return code;
    }

    public Map<String, List<String>> headers() {
        return headers;
    }

    public InputStream asStream() {
        return new ByteArrayInputStream(data);
    }

    public String asString(Charset charset) {
        return new String(data, charset);
    }

    public String asString() {
        return asString(StandardCharsets.UTF_8);
    }

    public JSONObject asObject() {
        return new JSONObject(asString());
    }

    public JSONArray asArray() {
        return new JSONArray(asString());
    }
}
