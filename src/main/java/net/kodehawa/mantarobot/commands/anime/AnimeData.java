package net.kodehawa.mantarobot.commands.anime;

import java.util.List;

public class AnimeData {
	public String average_score = null;
	public String description = null;
	public Integer duration = null;
	public String end_date = null;
	public List<String> genres = null;
	public Integer id = null;
	public String image_url_lge = null;
	public String image_url_sml = null;
	public String series_type = null;
	public String start_date = null;
	public String title_english = null;
	public String title_japanese = null;
	public Integer total_episodes = null;
	public String type = null;

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

	public List<String> getGenres() {
		return genres;
	}

	public Integer getId() {
		return id;
	}

	public String getImage_url_lge() {
		return image_url_lge;
	}

	public String getImage_url_sml() {
		return image_url_sml;
	}

	public String getSeries_type() {
		return series_type;
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

	public Integer getTotal_episodes() {
		return total_episodes;
	}

	public String getType() {
		return type;
	}
}
