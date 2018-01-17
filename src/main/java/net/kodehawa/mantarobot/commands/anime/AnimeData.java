/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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

package net.kodehawa.mantarobot.commands.anime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

import java.util.List;

public class AnimeData {
    private String average_score = null;
    private String description = null;
    private Integer duration = null;
    private String end_date = null;
    private List<String> genres = null;
    private Integer id = null;
    private String image_url_lge = null;
    private String image_url_sml = null;
    private String series_type = null;
    private String start_date = null;
    private String title_english = null;
    private String title_japanese = null;
    private Integer total_episodes = null;
    private String type = null;

    @JsonIgnore
    public static AnimeData[] fromJson(String json) {
        return GsonDataManager.GSON_PRETTY.fromJson(json, AnimeData[].class);
    }

    public String getAverageScore() {
        return average_score;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDuration() {
        return duration;
    }

    public String getEndDate() {
        return end_date;
    }

    public List<String> getGenres() {
        return genres;
    }

    public Integer getId() {
        return id;
    }

    public String getLargeImageUrl() {
        return image_url_lge;
    }

    public String getSmallImageUrl() {
        return image_url_sml;
    }

    public String getSeriesType() {
        return series_type;
    }

    public String getStartDate() {
        return start_date;
    }

    public String getTitleEnglish() {
        return title_english;
    }

    public String getTitleJapanese() {
        return title_japanese;
    }

    public Integer getTotalEpisodes() {
        return total_episodes;
    }

    public String getType() {
        return type;
    }
}
