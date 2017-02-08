package net.kodehawa.lib.giphy.main.entities;

public class Search {

	public Data[] data = null;
	public Meta meta = null;
	public Pagination pagination = null;

	public Data[] getData() {
		return data;
	}

	public Meta getMeta() {
		return meta;
	}

	public Pagination getPagination() {
		return pagination;
	}

	public class Data {
		public String type = null;
		public String id = null;
		public String slug = null;
		public String url = null;
		public String bitly_gif_url = null;
		public String bitly_url = null;
		public String embed_url = null;
		public String username = null;
		public String source = null;
		public String rating = null;
		public String caption = null;
		public String content_url = null;
		public String source_tld = null;
		public String source_post_url = null;
		public String import_datetime = null;
		public String trending_datetime = null;
		public Images images = null;

		public class Images {
			public Original original = null;

			public class Original {
				public String url = null;
				public String width = null;
				public String height = null;
				public String size = null;
				public String frames = null;
				public String mp4 = null;
				public String mp4_size = null;
				public String webp = null;
				public String webp_size = null;

				public String getUrl() {
					return url;
				}

				public String getWidth() {
					return width;
				}

				public String getHeight() {
					return height;
				}

				public String getSize() {
					return size;
				}

				public String getFrames() {
					return frames;
				}

				public String getMp4() {
					return mp4;
				}

				public String getMp4_size() {
					return mp4_size;
				}

				public String getWebp() {
					return webp;
				}

				public String getWebp_size() {
					return webp_size;
				}
			}
		}

		public String getType() {
			return type;
		}

		public String getId() {
			return id;
		}

		public String getSlug() {
			return slug;
		}

		public String getUrl() {
			return url;
		}

		public String getBitly_gif_url() {
			return bitly_gif_url;
		}

		public String getBitly_url() {
			return bitly_url;
		}

		public String getEmbed_url() {
			return embed_url;
		}

		public String getUsername() {
			return username;
		}

		public String getSource() {
			return source;
		}

		public String getRating() {
			return rating;
		}

		public String getCaption() {
			return caption;
		}

		public String getContent_url() {
			return content_url;
		}

		public String getSource_tld() {
			return source_tld;
		}

		public String getSource_post_url() {
			return source_post_url;
		}

		public String getImport_datetime() {
			return import_datetime;
		}

		public String getTrending_datetime() {
			return trending_datetime;
		}

		public Images getImages() {
			return images;
		}
	}

	public class Meta {
		public Integer status = null;
		public String msg = null;

		public Integer getStatus() {
			return status;
		}

		public String getMsg() {
			return msg;
		}
	}

	public class Pagination{
		public Integer count = null;
		public Integer offset = null;

		public Integer getCount() {
			return count;
		}

		public Integer getOffset() {
			return offset;
		}
	}
}
