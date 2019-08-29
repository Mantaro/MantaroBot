package net.kodehawa.mantarobot.commands.anime;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.kodehawa.mantarobot.MantaroInfo;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static net.kodehawa.mantarobot.utils.Utils.httpClient;

public class KitsuRetriever {
    private static Gson gson = new Gson();

    public static List<CharacterData> searchCharacters(String name) {
        try {
            Request request = new Request.Builder()
                    .url(String.format("https://kitsu.io/api/edge/characters?filter[name]=%s", URLEncoder.encode(name, "UTF-8")))
                    .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                    .get()
                    .build();

            Response response = httpClient.newCall(request).execute();
            String body = response.body().string();
            response.close();

            Type collectionType = new TypeToken<List<CharacterData>>() {}.getType();

            JsonObject json = new JsonParser().parse(body).getAsJsonObject();
            JsonArray jarr = json.getAsJsonObject().getAsJsonArray("data");
            return gson.fromJson(jarr, collectionType);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static List<AnimeData> searchAnime(String name) {
        try {
            Request request = new Request.Builder()
                    .url(String.format("https://kitsu.io/api/edge/anime?filter[text]=%s", URLEncoder.encode(name, "UTF-8")))
                    .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                    .get()
                    .build();

            Response response = httpClient.newCall(request).execute();
            String body = response.body().string();
            response.close();

            Type collectionType = new TypeToken<List<AnimeData>>() {}.getType();

            JsonObject json = new JsonParser().parse(body).getAsJsonObject();
            JsonArray jarr = json.getAsJsonObject().getAsJsonArray("data");
            return gson.fromJson(jarr, collectionType);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}
