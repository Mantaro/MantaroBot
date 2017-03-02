package net.kodehawa.lib.giphy.main;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.lib.giphy.main.entities.Gif;
import net.kodehawa.lib.giphy.main.entities.RandomGif;
import net.kodehawa.lib.giphy.main.entities.Search;
import net.kodehawa.lib.giphy.provider.Provider;
import net.kodehawa.lib.giphy.provider.RandomProvider;
import net.kodehawa.lib.giphy.provider.SearchProvider;
import net.kodehawa.mantarobot.utils.Async;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Optional;

public class Giphy {
	private String GIPHY_URL = "http://api.giphy.com/v1/gifs/";
	private String PUBLIC_API_KEY = "dc6zaTOxFJmzC";
	private HashMap<String, Object> query = new HashMap<>();

	public void random(final String tags, GuildMessageReceivedEvent event, final RandomProvider provider) {
		Async.asyncThread("Gif lookup", () -> {
			if (provider == null) throw new IllegalStateException("Provider is null");
			query.put("api_key", PUBLIC_API_KEY);
			Optional.ofNullable(tags).ifPresent(r -> query.put("tag", tags));
			String json = Utils.wget(GIPHY_URL + "random?" + Utils.urlEncodeUTF8(query), event);
			query.clear();
			provider.onSuccess(GsonDataManager.GSON_PRETTY.fromJson(json, RandomGif.class));
		}).run();
	}

	public void search(GuildMessageReceivedEvent event, final String query, @Nullable final String limit, @Nullable final String offset,
					   @Nullable final String rating, final SearchProvider provider) {
		Async.asyncThread("Gif lookup", () -> {
			if (provider == null) throw new IllegalStateException("Provider is null");
			this.query.put("q", query);
			Optional.ofNullable(limit).ifPresent(r -> this.query.put("limit", limit));
			Optional.ofNullable(offset).ifPresent(r -> this.query.put("offset", offset));
			Optional.ofNullable(rating).ifPresent(r -> this.query.put("rating", rating));
			this.query.put("api_key", PUBLIC_API_KEY);
			String json = Utils.wget(GIPHY_URL + "search?" + Utils.urlEncodeUTF8(this.query), event);
			this.query.clear();
			provider.onSuccess(GsonDataManager.GSON_PRETTY.fromJson(json, Search.class));
		}).run();
	}

	public void translate(final String term, @Nullable final String rating, GuildMessageReceivedEvent event, final Provider provider) {
		Async.asyncThread("Gif lookup", () -> {
			if (provider == null) throw new IllegalStateException("Provider is null");
			query.put("s", term);
			Optional.ofNullable(rating).ifPresent(r -> query.put("rating", r));
			query.put("api_key", PUBLIC_API_KEY);
			String json = Utils.wget(GIPHY_URL + "translate?" + Utils.urlEncodeUTF8(query), event);
			query.clear();
			provider.onSuccess(GsonDataManager.GSON_PRETTY.fromJson(json, Gif.class));
		}).run();
	}

	public void trending(GuildMessageReceivedEvent event, final SearchProvider provider) {
		Async.asyncThread("Gif lookup", () -> {
			if (provider == null) throw new IllegalStateException("Provider is null"); //shouldn't happen anyway
			query.put("api_key", PUBLIC_API_KEY);
			String json = Utils.wget(GIPHY_URL + "trending?" + Utils.urlEncodeUTF8(query), event);
			query.clear();
			provider.onSuccess(GsonDataManager.GSON_PRETTY.fromJson(json, Search.class));
		}).run();
	}
}
