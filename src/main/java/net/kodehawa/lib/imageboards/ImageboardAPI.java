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

package net.kodehawa.lib.imageboards;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.Gson;
import net.kodehawa.lib.imageboards.util.Requester;
import net.kodehawa.lib.imageboards.util.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ImageboardAPI<T> {

    private static final ObjectMapper XML_MAPPER = new XmlMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Gson gson = new Gson();
    private final Requester requester = new Requester();
    private Boards apiHome;
    private Class<T[]> clazz;
    private Type type;

    public ImageboardAPI(Boards landing, Type type, Class<T[]> clazz1) {
        this.apiHome = landing;
        this.type = type;
        this.clazz = clazz1;
    }

    public void get(int limit, Consumer<List<T>> handler) {
        get(limit, null, handler);
    }

    public void get(Consumer<List<T>> handler) {
        get(60, null, handler);
    }

    public void getBlocking(int limit, Consumer<List<T>> handler) {
        getBlocking(limit, null, handler);
    }

    public void getBlocking(Consumer<List<T>> handler) {
        getBlocking(60, null, handler);
    }

    public void onSearch(int limit, String search, Consumer<List<T>> handler) {
        get(limit, search, handler);
    }

    public void onSearch(String search, Consumer<List<T>> handler) {
        get(60, search, handler);
    }

    public void onSearchBlocking(int limit, String search, Consumer<List<T>> handler) {
        getBlocking(limit, search, handler);
    }

    public void onSearchBlocking(String search, Consumer<List<T>> handler) {
        getBlocking(60, search, handler);
    }

    private List<T> get(int limit, String search) throws Exception {
        HashMap<String, Object> queryParams = new HashMap<>();
        queryParams.put("limit", limit);
        T[] wallpapers;

        if(search != null) queryParams.put("tags", search.toLowerCase().trim());

        try {
            String response = requester.request(apiHome + apiHome.separator + Utils.urlEncodeUTF8(queryParams));
            if(response == null) return null;

            wallpapers = type.equals(Type.JSON) ? gson.fromJson(response, clazz) : XML_MAPPER.readValue(response, clazz);
        } catch(Exception e) {
            return null;
        }

        return Arrays.asList(wallpapers);
    }

    private void get(int limit, String search, Consumer<List<T>> result) {
        executorService.execute(() -> {
            try {
                List<T> wallpapers = get(limit, search);
                result.accept(wallpapers);
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void getBlocking(int limit, String search, Consumer<List<T>> result) {
        try {
            List<T> wallpapers = get(limit, search);
            result.accept(wallpapers);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public enum Type {
        JSON, XML
    }

    public enum Boards {
        //Lewd APIs
        R34("https://rule34.xxx/index.php?page=dapi&s=post&q=index", "&"),
        E621("https://e621.net/post/index.json", "?"),
        //Normal APIs
        KONACHAN("http://konachan.com/post.json", "?"),
        YANDERE("https://yande.re/post.json", "?");

        private String separator;
        private String url;

        Boards(String url, String separator) {
            this.url = url;
            this.separator = separator;
        }

        @Override
        public String toString() {
            return url;
        }
    }
}
