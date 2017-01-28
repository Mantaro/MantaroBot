package net.kodehawa.mantarobot.commands.utils;

import java.util.List;

public class AnimeData {
	public Integer id = null;
	public String average_score = null;
	public String description = null;
	public String end_date_fuzzy = null;
	public String start_date_fuzzy = null;
	public String title_english = null;
	public String title_romaji = null;
	public String title_japanese = null;
	public String type = null;
	public String series_type = null;
	public String start_date = null;
	public String end_date = null;
	public String season = null;
	public Integer popularity = null;
	public Boolean favourite = null;
	public String image_url_sml = null;
	public String image_url_med = null;
	public String image_url_lge = null;
	public String image_url_banner = null;
	public List<String> genres = null;
	public List<String> synonyms = null;
	public String youtube_id = null;
	public String hashtag = null;
	public String updated_at = null;
	public Integer total_episodes = null;
	public Integer duration = null;
	public String airing_status = null;
	public String source = null;
	public String classification = null;
	public List<ListStats> list_stats = null;

	public static class ListStats {
		public Integer completed = null;
		public Integer on_hold = null;
		public Integer dropped = null;
		public Integer plan_to_watch = null;
		public Integer watching = null;
	}
}
