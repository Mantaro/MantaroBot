package net.kodehawa.lib.imageboards.konachan;

import br.com.brjdevs.java.utils.async.Async;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.lib.imageboards.konachan.main.entities.Tag;
import net.kodehawa.lib.imageboards.konachan.main.entities.Wallpaper;
import net.kodehawa.lib.imageboards.konachan.providers.WallpaperProvider;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import us.monoid.web.Resty;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class Konachan {
    private final Resty resty = new Resty().identifyAsMozilla();
    private HashMap<String, Object> queryParams;
    private boolean safeForWork = false;

    public Konachan(boolean safeForWork) {
        this.safeForWork = safeForWork;
        queryParams = new HashMap<>();
    }

    public List<Wallpaper> get() {
        return this.posts(1, 25);
    }

    public List<Wallpaper> get(int limit) {
        return this.posts(1, limit);
    }

    public void get(int limit, WallpaperProvider provider) {
        this.get(1, limit, null, provider);
    }

    public void get(int page, int limit, WallpaperProvider provider) {
        this.get(page, limit, null, provider);
    }

    private void get(final int page, final int limit, final String search, final WallpaperProvider provider) {
        Async.thread("Image fetch thread", () -> {
            try {
                if(provider == null) throw new IllegalStateException("Provider is null");
                List<Wallpaper> wallpapers = this.get(page, limit, search);
                Optional.ofNullable(search).ifPresent((s) -> {
                    Tag[] tags;
                    tags = this.getTags(search, 1, 5);
                    provider.onSuccess(wallpapers, tags);
                });
            } catch(Exception ignored) {
            }
        });
    }

    private List<Wallpaper> get(int page, int limit, String search) {
        this.queryParams.put("limit", limit);
        this.queryParams.put("page", page);
        Optional.ofNullable(search).ifPresent((element) -> {
            queryParams.put("tags", search.toLowerCase().trim());
            queryParams.remove("page");
        });

        String response;
        try {
            response = this.resty.text("http://konachan.com/post.json" + "?" + Utils.urlEncodeUTF8(this.queryParams)).toString();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            queryParams.clear();
        }
        Wallpaper[] wallpapers = GsonDataManager.GSON_PRETTY.fromJson(response, Wallpaper[].class);
        return isSafeForWork() ? Arrays.stream(wallpapers).filter((wallpaper1) ->
                wallpaper1.getRating().equalsIgnoreCase("s")).collect(Collectors.toList()) : Arrays.asList(wallpapers);
    }

    private Tag[] getTags(String tagName, int page, int limit) {
        queryParams.put("order", "count");
        queryParams.put("limit", limit);
        queryParams.put("page", page);
        queryParams.put("name", tagName.toLowerCase().trim());
        String response = "";
        try {
            response = this.resty.text("http://konachan.com/tag.json" + "?" + Utils.urlEncodeUTF8(this.queryParams)).toString();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            queryParams.clear();
        }
        return GsonDataManager.GSON_PRETTY.fromJson(response, Tag[].class);
    }

    private boolean isSafeForWork() {
        return safeForWork;
    }

    public void onSearch(int page, int limit, String search, WallpaperProvider provider) {
        this.get(page, limit, search, provider);
    }

    public List<Wallpaper> posts(int page, int limit) {
        return this.get(page, limit, (String) null);
    }
}
