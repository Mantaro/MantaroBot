/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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

package net.kodehawa.mantarobot.commands.action;

import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

import java.util.HashMap;

@Slf4j
public class WeebAPIRequester {
    private final String ALL_TAGS = "/tags";
    private final String ALL_TYPES = "/types";
    private final String API_BASE_URL = "https://api.weeb.sh/images";
    private final String WEEBSH_TOKEN = MantaroData.config().get().weebapiKey;
    private final String RANDOM_IMAGE = "/random";
    private final OkHttpClient httpClient = new OkHttpClient();

    public Pair<String, String> getRandomImageByType(String type, boolean nsfw, String filetype) {
        HashMap<String, Object> queryParams = new HashMap<>();
        queryParams.put("type", type);
        if(nsfw)
            queryParams.put("nsfw", "only");
        else
            queryParams.put("nsfw", false);

        if(filetype != null)
            queryParams.put("filetype", filetype);

        String r = request(RANDOM_IMAGE, Utils.urlEncodeUTF8(queryParams));
        if(r == null)
            return Pair.of(null, null);

        JSONObject object = new JSONObject(r);
        return Pair.of(object.getString("url"), object.getString("id"));
    }

    public String getRandomImageByTags(String tags, boolean nsfw, String filetype) {
        HashMap<String, Object> queryParams = new HashMap<>();
        queryParams.put("tags", tags);

        if(nsfw)
            queryParams.put("nsfw", "only");
        else
            queryParams.put("nsfw", false);

        if(filetype != null)
            queryParams.put("filetype", filetype);

        String r = request(RANDOM_IMAGE, Utils.urlEncodeUTF8(queryParams));
        if(r == null)
            return null;

        return new JSONObject(r).getString("url");
    }

    public JSONObject getTypes() {
        String r = request(ALL_TYPES, null);
        if(r == null)
            return null;

        return new JSONObject(r);
    }

    public JSONObject getTags() {
        String r = request(ALL_TAGS, null);
        if(r == null)
            return null;

        return new JSONObject(r);
    }

    private String request(String endpoint, String e) {
        try {
            StringBuilder builder = new StringBuilder(endpoint);
            if(e != null) {
                builder.append("?");
                builder.append(e);
            }

            Request r = new Request.Builder()
                    .url(API_BASE_URL + builder.toString())
                    .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                    .addHeader("Authorization", WEEBSH_TOKEN.startsWith("Wolke ") ? WEEBSH_TOKEN : "Bearer " + WEEBSH_TOKEN)
                    .build();

            Response r1 = httpClient.newCall(r).execute();
            String response = r1.body().string();

            r1.close();
            return response;
        } catch(Exception ex) {
            log.error("Error getting image from weeb.sh", ex);
            return null;
        }
    }
}
