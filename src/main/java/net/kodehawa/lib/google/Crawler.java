package net.kodehawa.lib.google;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Crawler {

	public static class SearchResult {
		public String title;
		public String url;

		SearchResult(String url, String title) {
			this.url = url;
			this.title = title;
		}

		public String getTitle() {
			return title;
		}

		public String getUrl() {
			return url;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("Crawler");

	public static List<SearchResult> get(String query) {
		List<SearchResult> results = new ArrayList<>();

		try {
			Document doc = Jsoup
				.connect("https://www.google.com/search?q=" + query + "&num=20")
				.userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
				.timeout(5000).get();

			Elements links = doc.select("a[href]");
			for (Element link : links) {
				String temp = link.attr("href");
				if (temp.startsWith("/url?q=")) {
					URL tempUrl = new URL(temp.replace("/url?q=", ""));
					//doesn't work *always* but it works most of the times so cannot complain
					String path = tempUrl.getFile().substring(0, tempUrl.getFile().indexOf('&'));
					String base = tempUrl.getProtocol() + "://" + tempUrl.getHost() + path;
					results.add(new SearchResult(base, link.text()));
				}
			}
		} catch (IOException e) {
			results.add(new SearchResult("http://worrydream.com/404notfound", "Error."));
		}

		return results;
	}
}
