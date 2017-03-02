package net.kodehawa.lib.giphy.main.entities;

public class RandomGif {
	public class Data {
		public String fixed_height_downsampled_url = null;
		public String id = null;
		public String image_frames = null;
		public String image_height = null;
		public String image_mp4_url = null;
		public String image_original_url = null;
		public String image_url = null;
		public String image_width = null;
		public String type = null;
		public String url = null;

		public String getFixed_height_downsampled_url() {
			return fixed_height_downsampled_url;
		}

		public String getId() {
			return id;
		}

		public String getImage_frames() {
			return image_frames;
		}

		public String getImage_height() {
			return image_height;
		}

		public String getImage_mp4_url() {
			return image_mp4_url;
		}

		public String getImage_original_url() {
			return image_original_url;
		}

		public String getImage_url() {
			return image_url;
		}

		public String getImage_width() {
			return image_width;
		}

		public String getType() {
			return type;
		}

		public String getUrl() {
			return url;
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
	public Data data = null;
	public Meta meta = null;

	public Data getData() {
		return data;
	}

	public Meta getMeta() {
		return meta;
	}
}
