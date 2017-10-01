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

package net.kodehawa.mantarobot.commands.utils;

import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GoogleCrawler {

    public static List<SearchResult> get(String query) {
        List<SearchResult> results = new ArrayList<>();

        try {
            Document doc = Jsoup
                    .connect("https://www.google.com/search?q=" + query + "&num=20")
                    .userAgent("JDA/DiscordBot (MantaroBot " + MantaroInfo.VERSION + ")")
                    .timeout(5000).get();

            Elements links = doc.select("a[href]");
            for(Element link : links) {
                String temp = link.attr("href");
                if(temp.startsWith("/url?q=")) {
                    URL tempUrl = new URL(temp.replace("/url?q=", ""));
                    //doesn't work *always* but it works most of the times so cannot complain
                    String path = tempUrl.getFile().substring(0, tempUrl.getFile().indexOf('&'));
                    String base = tempUrl.getProtocol() + "://" + tempUrl.getHost() + path;
                    //link.text() = title, base = url.
                    if(!base.contains("webcache.googleusercontent.com"))
                        if(!link.text().isEmpty())
                            results.add(new SearchResult(base, link.text()));
                }
            }
        } catch(IOException e) {
            results.add(new SearchResult("http://worrydream.com/404notfound", "Error."));
        }

        return results;
    }

    public static class SearchResult {
        public final String title;
        public final String url;

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
}
