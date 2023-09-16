/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WeebAPIRequester {
    private static final Logger log = LoggerFactory.getLogger(WeebAPIRequester.class);
    private static final String ALL_TAGS = "/tags";
    private static final String ALL_TYPES = "/types";
    private static final String API_BASE_URL = "https://api.weeb.sh/images";
    private static final String AUTH_HEADER = "Bearer " + MantaroData.config().get().weebapiKey;
    private static final String RANDOM_IMAGE = "/random";

    // I know it's better to have a global OkHttp3 client, but we need a custom timeout handler here.
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            // Fail if we can't establish a connection in 1.5s.
            .connectTimeout(1500, TimeUnit.MILLISECONDS)
            // Fail if nothing gets sent in 2.5s.
            .readTimeout(2500, TimeUnit.MILLISECONDS)
            .build();

    public WeebAPIObject getRandomImageByType(String type, boolean nsfw, String filetype) throws JsonProcessingException {
        HashMap<String, Object> queryParams = new HashMap<>();
        queryParams.put("type", type);

        if (nsfw) {
            queryParams.put("nsfw", "only");
        }
        else {
            queryParams.put("nsfw", false);
        }

        if (filetype != null) {
            queryParams.put("filetype", filetype);
        }

        var req = request(RANDOM_IMAGE, Utils.urlEncodeUTF8(queryParams));
        if (req == null) {
            return null;
        }

        return JsonDataManager.fromJson(req, WeebAPIObject.class);
    }

    @SuppressWarnings("unused")
    public JSONObject getTypes() {
        var req = request(ALL_TYPES, null);
        if (req == null) {
            return null;
        }

        return new JSONObject(req);
    }

    @SuppressWarnings("unused")
    public JSONObject getTags() {
        var req = request(ALL_TAGS, null);
        if (req == null) {
            return null;
        }

        return new JSONObject(req);
    }

    private String request(String endpoint, String e) {
        try {
            var builder = new StringBuilder(endpoint);
            if (e != null) {
                builder.append("?");
                builder.append(e);
            }

            var r = new Request.Builder()
                    .url(API_BASE_URL + builder)
                    .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                    .addHeader("Authorization", AUTH_HEADER)
                    .build();

            try(var response = httpClient.newCall(r).execute()) {
                var body = response.body();
                if (body == null) {
                    throw new IllegalStateException("body == null");
                }

                return body.string();
            }
        } catch (Exception ex) {
            log.error("Error getting image from weeb.sh", ex);
            return null;
        }
    }

    public record WeebAPIObject(String id, String url, String fileType, boolean nsfw, String type, List<WeebAPITag> tags) { }
    @SuppressWarnings("unused")
    public record WeebAPITag(String user, boolean hidden, String name) { }
}
