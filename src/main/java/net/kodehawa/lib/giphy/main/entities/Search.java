package net.kodehawa.lib.giphy.main.entities;

public class Search {

	public class Data {
		public class Images {
			public class Original {
				public String frames = null;
				public String height = null;
				public String mp4 = null;
				public String mp4_size = null;
				public String size = null;
				public String url = null;
				public String webp = null;
				public String webp_size = null;
				public String width = null;

				public String getFrames() {
					return frames;
				}

				public String getHeight() {
					return height;
				}

				public String getMp4() {
					return mp4;
				}

				public String getMp4_size() {
					return mp4_size;
				}

				public String getSize() {
					return size;
				}

				public String getUrl() {
					return url;
				}

				public String getWebp() {
					return webp;
				}

				public String getWebp_size() {
					return webp_size;
				}

				public String getWidth() {
					return width;
				}
			}

			public Original original = null;
		}

		public String bitly_gif_url = null;
		public String bitly_url = null;
		public String caption = null;
		public String content_url = null;
		public String embed_url = null;
		public String id = null;
		public Images images = null;
		public String import_datetime = null;
		public String rating = null;
		public String slug = null;
		public String source = null;
		public String source_post_url = null;
		public String source_tld = null;
		public String trending_datetime = null;
		public String type = null;
		public String url = null;
		public String username = null;

		public String getBitly_gif_url() {
			return bitly_gif_url;
		}

		public String getBitly_url() {
			return bitly_url;
		}

		public String getCaption() {
			return caption;
		}

		public String getContent_url() {
			return content_url;
		}

		public String getEmbed_url() {
			return embed_url;
		}

		public String getId() {
			return id;
		}

		public Images getImages() {
			return images;
		}

		public String getImport_datetime() {
			return import_datetime;
		}

		public String getRating() {
			return rating;
		}

		public String getSlug() {
			return slug;
		}

		public String getSource() {
			return source;
		}

		public String getSource_post_url() {
			return source_post_url;
		}

		public String getSource_tld() {
			return source_tld;
		}

		public String getTrending_datetime() {
			return trending_datetime;
		}

		public String getType() {
			return type;
		}

		public String getUrl() {
			return url;
		}

		public String getUsername() {
			return username;
		}
	}

	public class Meta {
		public String msg = null;
		public Integer status = null;

		public String getMsg() {
			return msg;
		}

		public Integer getStatus() {
			return status;
		}
	}

	public class Pagination {
		public Integer count = null;
		public Integer offset = null;

		public Integer getCount() {
			return count;
		}

		public Integer getOffset() {
			return offset;
		}
	}

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
}
