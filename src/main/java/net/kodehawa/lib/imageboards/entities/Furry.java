package net.kodehawa.lib.imageboards.entities;

public class Furry {

	public String description = null;
	public String file_url = null;
	public Integer height = null;
	//Nice tags m9. Mantaro's lewdness just increased by 1000% by just creating this POJO to deserialize e621 XML.
	public String tags = null;
	public Integer width = null;

	public String getDescription() {
		return description;
	}

	public String getFile_url() {
		return file_url;
	}

	public Integer getHeight() {
		return height;
	}

	public String getTags() {
		return tags;
	}

	public Integer getWidth() {
		return width;
	}
}
