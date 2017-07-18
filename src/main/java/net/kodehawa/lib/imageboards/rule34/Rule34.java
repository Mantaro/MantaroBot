package net.kodehawa.lib.imageboards.rule34;

import br.com.brjdevs.java.utils.async.Async;
import net.kodehawa.lib.imageboards.rule34.entities.Hentai;
import net.kodehawa.lib.imageboards.rule34.providers.HentaiProvider;
import net.kodehawa.mantarobot.utils.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class Rule34 {
    private static HashMap<String, Object> queryParams = new HashMap<>();

    public static void get(int limit, HentaiProvider provider) {
        get(limit, null, provider);
    }

    public static void onSearch(int limit, String search, HentaiProvider provider) {
        get(limit, search, provider);
    }

    private static void get(final int limit, final String search, final HentaiProvider provider) {
        Async.thread("Image fetch thread", () -> {
            try {
                if(provider == null) throw new IllegalStateException("Provider is null");
                List<Hentai> wallpapers = get(limit, search);
                provider.onSuccess(wallpapers);
            } catch(Exception ignored) {
            }
        });
    }

    private static List<Hentai> get(int limit, String search) {
        queryParams.put("limit", limit);
        Hentai[] wallpapers;
        Optional.ofNullable(search).ifPresent((element) -> {
            queryParams.put("tags", search.toLowerCase().trim());
            queryParams.remove("page");
        });

        try {
            String response = Utils.wgetResty("http://rule34.xxx/index.php?page=dapi&s=post&q=index" + "&" + Utils.urlEncodeUTF8(queryParams), null);
            wallpapers = Utils.XML_MAPPER.readValue(response, Hentai[].class);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            queryParams.clear();
        }
        return Arrays.asList(wallpapers);
    }
}
