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
            Elements doc = Jsoup
                    .connect("https://www.google.com/search?q=" + query)
                    .userAgent(MantaroInfo.USER_AGENT)
                    .timeout(5000).get().select(".g");

            for(Element blocks : doc) {
                Elements e = blocks.select(".r>a");
                if(e.isEmpty()) continue;

                Element entry = e.get(0);
                String title = entry.text();
                String url = entry.absUrl("href").replace(")", "\\)");
                String desc = "No description";
                Elements tmpDesc = blocks.select(".st");
                if(!tmpDesc.isEmpty())
                    desc = tmpDesc.get(0).text();

                if(!url.contains("webcache.googleusercontent.com"))
                    results.add(new SearchResult(url, title, desc));

            }
        } catch(IOException e) {
            results.add(new SearchResult("http://worrydream.com/404notfound", "Error.", "An error occured while looking up this query..."));
        }

        return results;
    }

    public static class SearchResult {
        public final String title;
        public final String url;
        public final String description;

        SearchResult(String url, String title, String description) {
            this.url = url;
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public String getDescription() {
            return description;
        }
    }
}
