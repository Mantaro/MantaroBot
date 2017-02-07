package net.kodehawa.lib.giphy.main.entities;

public class Random {
	public Data data = null;
	public Meta meta = null;

	public Data getData() {
		return data;
	}

	public Meta getMeta() {
		return meta;
	}

	public class Data {
		public String type = null;
		public String id = null;
		public String url = null;
		public String image_original_url = null;
		public String image_url = null;
		public String image_mp4_url = null;
		public String image_frames = null;
		public String image_width = null;
		public String image_height = null;
		public String fixed_height_downsampled_url = null;

		public String getType() {
			return type;
		}

		public String getId() {
			return id;
		}

		public String getUrl() {
			return url;
		}

		public String getImage_original_url() {
			return image_original_url;
		}

		public String getImage_url() {
			return image_url;
		}

		public String getImage_mp4_url() {
			return image_mp4_url;
		}

		public String getImage_frames() {
			return image_frames;
		}

		public String getImage_width() {
			return image_width;
		}

		public String getImage_height() {
			return image_height;
		}

		public String getFixed_height_downsampled_url() {
			return fixed_height_downsampled_url;
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
}
