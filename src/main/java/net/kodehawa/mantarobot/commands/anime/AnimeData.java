package net.kodehawa.mantarobot.commands.anime;

import java.util.List;

public class AnimeData {
    public String airing_status = null;
    public String average_score = null;
    public String classification = null;
    public String description = null;
    public Integer duration = null;
    public String end_date = null;
    public String end_date_fuzzy = null;
    public Boolean favourite = null;
    public List<String> genres = null;
    public String hashtag = null;
    public Integer id = null;
    public String image_url_banner = null;
    public String image_url_lge = null;
    public String image_url_med = null;
    public String image_url_sml = null;
    public ListStats list_stats = null;
    public Integer popularity = null;
    public String season = null;
    public String series_type = null;
    public String source = null;
    public String start_date = null;
    public String start_date_fuzzy = null;
    public List<String> synonyms = null;
    public String title_english = null;
    public String title_japanese = null;
    public String title_romaji = null;
    public Integer total_episodes = null;
    public String type = null;
    public String updated_at = null;
    public String youtube_id = null;

    public String getAiring_status() {
        return airing_status;
    }

    public String getAverage_score() {
        return average_score;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDuration() {
        return duration;
    }

    public String getEnd_date() {
        return end_date;
    }

    public Boolean getFavourite() {
        return favourite;
    }

    public List<String> getGenres() {
        return genres;
    }

    public Integer getId() {
        return id;
    }

    public String getImage_url_banner() {
        return image_url_banner;
    }

    public String getImage_url_lge() {
        return image_url_lge;
    }

    public String getImage_url_med() {
        return image_url_med;
    }

    public String getImage_url_sml() {
        return image_url_sml;
    }

    public ListStats getList_stats() {
        return list_stats;
    }

    public String getSeries_type() {
        return series_type;
    }

    public String getSource() {
        return source;
    }

    public String getStart_date() {
        return start_date;
    }

    public String getTitle_english() {
        return title_english;
    }

    public String getTitle_japanese() {
        return title_japanese;
    }

    public String getTitle_romaji() {
        return title_romaji;
    }

    public Integer getTotal_episodes() {
        return total_episodes;
    }

    public String getType() {
        return type;
    }

    public static class ListStats {
        public Integer completed = null;
        public Integer dropped = null;
        public Integer on_hold = null;
        public Integer plan_to_watch = null;
        public Integer watching = null;
    }
}
