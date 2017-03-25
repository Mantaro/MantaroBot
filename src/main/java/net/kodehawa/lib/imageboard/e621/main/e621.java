package net.kodehawa.lib.imageboard.e621.main;

import br.com.brjdevs.java.utils.extensions.Async;
import net.kodehawa.lib.imageboard.e621.main.entities.Furry;
import net.kodehawa.lib.imageboard.e621.providers.FurryProvider;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.web.Resty;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Totally not a (stripped down) copy of {@link net.kodehawa.lib.imageboard.konachan.main.Konachan}
 * I mean, it works, that's what's important here.
 */
public class e621 {
	private static final Logger LOGGER = LoggerFactory.getLogger("e621");
	private final Resty resty = new Resty().identifyAsMozilla();
	private HashMap<String, Object> queryParams = new HashMap<>();

	public void get(int page, int limit, FurryProvider provider) {
		this.get(page, limit, null, provider);
	}

	private void get(final int page, final int limit, final String search, final FurryProvider provider) {
		Async.thread("Image fetch thread", () -> {
			try {
				if (provider == null) throw new IllegalStateException("Provider is null");
				List<Furry> wallpapers = this.get(page, limit, search);
				provider.onSuccess(wallpapers);
			} catch (Exception ignored) {
			}
		});
	}

	private List<Furry> get(int page, int limit, String search) {
		this.queryParams.put("limit", limit);
		this.queryParams.put("page", page);
		Furry[] wallpapers;
		Optional.ofNullable(search).ifPresent((element) -> this.queryParams.put("tags", search.toLowerCase().trim()));
		try {
			String response = this.resty.text("https://e621.net/post/index.json" + "?" + Utils.urlEncodeUTF8(this.queryParams)).toString();
			wallpapers = GsonDataManager.GSON_PRETTY.fromJson(response, Furry[].class);
		} catch (Exception e) {
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