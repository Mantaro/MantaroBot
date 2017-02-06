package net.kodehawa.lib.konachan.main;

import com.google.gson.Gson;
import net.kodehawa.lib.konachan.main.entities.Tag;
import net.kodehawa.lib.konachan.main.entities.Wallpaper;
import net.kodehawa.lib.konachan.providers.DownloadProvider;
import net.kodehawa.lib.konachan.providers.WallpaperProvider;
import net.kodehawa.mantarobot.utils.Async;
import net.kodehawa.mantarobot.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.web.BinaryResource;
import us.monoid.web.Resty;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Konachan {
	private static final Logger LOGGER = LoggerFactory.getLogger("Konachan");

	private HashMap<String, Object> queryParams;
	private Resty resty= new Resty().identifyAsMozilla();
	private boolean safeForWork = false;

	public Konachan(boolean safeForWork) {
		this.safeForWork = safeForWork;
		queryParams = new HashMap<>();
	}

	private int getLimitRelatedTags() {
		return 5;
	}

	private boolean isSafeForWork() {
		return safeForWork;
	}

	public List<Wallpaper> get() {
		return this.posts(1, 25);
	}

	public List<Wallpaper> get(int limit) {
		return this.posts(1, limit);
	}

	public List<Wallpaper> posts(int page, int limit) {
		return this.get(page, limit, (String) null);
	}

	public void get(WallpaperProvider provider) {
		this.get(1, 25, null, provider);
	}

	public void get(int limit, WallpaperProvider provider) {
		this.get(1, limit, null, provider);
	}

	public void get(int page, int limit, WallpaperProvider provider) {
		this.get(page, limit, null, provider);
	}

	public void onSearch(int page, int limit, String search, WallpaperProvider provider) {
		this.get(page, limit, search, provider);
	}

	private void get(final int page, final int limit, final String search, final WallpaperProvider provider) {
		Async.asyncThread("Image fetch thread", () -> {
			try {
				if (provider == null) throw new IllegalStateException("Provider is null");
				List<Wallpaper> wallpapers = this.get(page, limit, search);
				Optional.ofNullable(search).ifPresent((s) -> {
					Tag[] tags;
					tags = this.getTags(search, 1, this.getLimitRelatedTags());
					provider.onSuccess(wallpapers, tags);
				});
			} catch (Exception ex) { LOGGER.warn("Error while retrieving a image from Konachan.", ex); }
		}).run();
	}

	private List<Wallpaper> get(int page, int limit, String search) {
		this.queryParams.put("limit", limit);
		this.queryParams.put("page", page);
		Optional.ofNullable(search).ifPresent((element) -> this.queryParams.put("tags", search.toLowerCase().trim()));
		String response = "[]";
		try {
			response = this.resty.text("http://konachan.com/post.json" + "?" + Utils.urlEncodeUTF8(this.queryParams)).toString();
		} catch (Exception e) { e.printStackTrace(); } finally { queryParams.clear(); }
		Gson gson = new Gson();
		Wallpaper[] wallpapers = gson.fromJson(response, Wallpaper[].class);
		return isSafeForWork() ? Arrays.stream(wallpapers).filter((wallpaper1) ->
				wallpaper1.getRating().equalsIgnoreCase("s")).collect(Collectors.toList()) : Arrays.asList(wallpapers);
	}

	private String saveWallpaper(String filename, String folderPath, String imageURL) throws IOException {
		if (filename == null) filename = imageURL.substring(imageURL.lastIndexOf('/') + 1, imageURL.length());
		resty.identifyAsMozilla();
		File imageFile = new File(folderPath + File.separator + filename);
		if (imageFile.exists()) return imageFile.getPath();
		BinaryResource binaryResource = resty.bytes(imageURL);
		binaryResource.save(imageFile);

		return folderPath + File.separator + filename;
	}

	public void saveWallpaper(final String filename, final String folderPath, final String imageURL, final DownloadProvider provider) {
		Async.asyncThread("Wallpaper Save", () ->{
			try {
				if (provider == null) return;
				String save = saveWallpaper(filename, folderPath, imageURL);
				Optional.ofNullable(save).ifPresent(provider::onSuccess);
				if (save == null) LOGGER.warn("Unknown error occurred while saving a wallpaper.");
			} catch (Exception ex) { LOGGER.warn("A error occurred while fetching the wallpaper.", ex); }
		}).run();
	}

	private Tag[] getTags(String tagName, int page, int limit) {
		queryParams.put("order", "count");
		queryParams.put("limit", limit);
		queryParams.put("page", page);
		queryParams.put("name", tagName.toLowerCase().trim());
		String response = "";
		try {
			response = this.resty.text("http://konachan.com/tag.json" + "?" + Utils.urlEncodeUTF8(this.queryParams)).toString();
		} catch (Exception e) { e.printStackTrace(); } finally { queryParams.clear(); }
		Gson gson = new Gson();
		return gson.fromJson(response, Tag[].class);
	}
}
