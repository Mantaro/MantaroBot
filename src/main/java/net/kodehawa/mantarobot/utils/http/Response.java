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
    private final int code;
    private final byte[] data;
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
