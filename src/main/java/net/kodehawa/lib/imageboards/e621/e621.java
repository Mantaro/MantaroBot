package net.kodehawa.lib.imageboards.e621;

import br.com.brjdevs.java.utils.async.Async;
import com.rethinkdb.model.MapObject;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.lib.imageboards.e621.main.entities.Furry;
import net.kodehawa.lib.imageboards.e621.providers.FurryProvider;
import net.kodehawa.lib.imageboards.konachan.Konachan;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import us.monoid.web.Resty;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Totally not a (stripped down) copy of {@link Konachan}
 * I mean, it works, that's what's important here.
 */
@Slf4j
public class e621 {
    private final Resty resty = new Resty().identifyAsMozilla();

    public void get(int page, int limit, FurryProvider provider) {
        this.get(page, limit, null, provider);
    }

    private void get(final int page, final int limit, final String search, final FurryProvider provider) {
        Async.thread("Image fetch thread", () -> {
            try {
                if(provider == null) throw new IllegalStateException("Provider is null");
                List<Furry> wallpapers = this.get(page, limit, search);
                provider.onSuccess(wallpapers);
            } catch(Exception ignored) {
            }
        });
    }

    private List<Furry> get(int page, int limit, String search) {
        Map<String, Object> queryParams = new MapObject<String, Object>()
                .with("limit", limit).with("page", page);
        Furry[] wallpapers;
        Optional.ofNullable(search).ifPresent((element) -> {
            queryParams.put("tags", search.toLowerCase().trim());
            queryParams.remove("page");
        });
        try {
            String response = this.resty.text("https://e621.net/post/index.json" + "?" + Utils.urlEncodeUTF8(queryParams)).toString();
            wallpapers = GsonDataManager.GSON_PRETTY.fromJson(response, Furry[].class);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            queryParams.clear();
        }
        return Arrays.asList(wallpapers);
    }

    public void onSearch(int page, int limit, String search, FurryProvider provider) {
        this.get(page, limit, search, provider);
    }
}