package net.kodehawa.lib.imageboards;

import br.com.brjdevs.java.utils.async.Async;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ImageboardAPI<T> {
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

        private String url;
        private String separator;

        Boards(String url, String separator){
            this.url = url;
            this.separator = separator;
        }

        @Override
        public String toString() {
            return url;
        }
    }

    private HashMap<String, Object> queryParams;
    private Boards apiHome;
    private Class<T[]> clazz;
    private Type type;

    public ImageboardAPI(Boards landing, Type type, Class<T[]> clazz1){
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

    public void onSearch(int limit, String search, Consumer<List<T>> handler) {
        get(limit, search, handler);
    }

    public void onSearch(String search, Consumer<List<T>> handler) {
        get(60, search, handler);
    }

    private List<T> get(int limit, String search){
        queryParams = new HashMap<>();
        queryParams.put("limit", limit);
        T[] wallpapers;

        if(search != null) queryParams.put("tags", search.toLowerCase().trim());

        try {
            String response = Utils.wgetResty(apiHome + apiHome.separator + Utils.urlEncodeUTF8(queryParams), null);
            wallpapers = type.equals(Type.JSON) ? GsonDataManager.GSON_PRETTY.fromJson(response, clazz) : Utils.XML_MAPPER.readValue(response, clazz);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }

        return Arrays.asList(wallpapers);
    }

    private void get(int limit, String search, Consumer<List<T>> result){
        Async.thread("Image fetch thread", () -> {
            try {
                List<T> wallpapers = get(limit, search);
                result.accept(wallpapers);
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
    }
}