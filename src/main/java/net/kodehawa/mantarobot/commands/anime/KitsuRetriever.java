/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.anime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static net.kodehawa.mantarobot.utils.Utils.httpClient;

public class KitsuRetriever {
    public static List<CharacterData> searchCharacters(String name) throws IOException {
        Request request = new Request.Builder()
                .url(String.format("https://kitsu.io/api/edge/characters?filter[name]=%s", URLEncoder.encode(name, StandardCharsets.UTF_8)))
                .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                .get()
                .build();

        Response response = httpClient.newCall(request).execute();
        String body = response.body().string();
        response.close();

        JSONObject json = new JSONObject(body);
        JSONArray arr = json.getJSONArray("data");
        return JsonDataManager.fromJson(arr.toString(), new TypeReference<List<CharacterData>>() { });
    }

    public static List<AnimeData> searchAnime(String name) throws IOException {
        Request request = new Request.Builder()
                .url(String.format("https://kitsu.io/api/edge/anime?filter[text]=%s", URLEncoder.encode(name, StandardCharsets.UTF_8)))
                .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                .get()
                .build();

        Response response = httpClient.newCall(request).execute();
        String body = response.body().string();
        response.close();

        JSONObject json = new JSONObject(body);
        JSONArray arr = json.getJSONArray("data");
        return JsonDataManager.fromJson(arr.toString(), new TypeReference<List<AnimeData>>() { });
    }
}
