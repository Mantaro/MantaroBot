package net.kodehawa.lib.imageboard.rule34.main;

import com.mashape.unirest.http.Unirest;
import net.kodehawa.lib.imageboard.rule34.entities.Hentai;
import net.kodehawa.lib.imageboard.rule34.providers.HentaiProvider;
import net.kodehawa.mantarobot.utils.Async;
import net.kodehawa.mantarobot.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.web.Resty;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class Rule34 {
	private static final Logger LOGGER = LoggerFactory.getLogger("rule34");
	private final static Resty resty = new Resty().identifyAsMozilla();
	private static HashMap<String, Object> queryParams = new HashMap<>();

	public static void get(int limit, HentaiProvider provider) {
		get(limit, null, provider);
	}

	public static void onSearch(int limit, String search, HentaiProvider provider) {
		get(limit, search, provider);
	}

	private static void get(final int limit, final String search, final HentaiProvider provider) {
		Async.asyncThread("Image fetch thread", () -> {
			try {
				if (provider == null) throw new IllegalStateException("Provider is null");
				List<Hentai> wallpapers = get(limit, search);
				provider.onSuccess(wallpapers);
			} catch (Exception ex) {
				LOGGER.warn("Error while retrieving a image from rule34.", ex);
			}
		}).run();
	}

	private static List<Hentai> get(int limit, String search) {
		queryParams.put("limit", limit);
		Hentai[] wallpapers;
		Optional.ofNullable(search).ifPresent((element) -> queryParams.put("tags", search.toLowerCase().trim()));
		try {
			System.out.println("http://rule34.xxx/index.php?page=dapi&s=post&q=index" + "&" + Utils.urlEncodeUTF8(queryParams));
			String response = Unirest.get("http://rule34.xxx/index.php?page=dapi&s=post&q=index" + "&" + Utils.urlEncodeUTF8(queryParams))
					.header("User-Agent", "Mantaro")
					.header("Content-Type", "text/xml")
					.asString()
					.getBody();
			wallpapers =  Utils.XML_MAPPER.readValue(response, Hentai[].class);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			queryParams.clear();
		}
		return Arrays.asList(wallpapers);
	}
}
