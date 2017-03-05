package net.kodehawa.lib.imageboard.e621.main.entities;

public class Furry {

	//Nice tags m9. Mantaro's lewdness just increased by 1000% by just creating this POJO to deserialize e621 XML.
	public String tags = null;
	public String description = null;
	public String file_url = null;
	public Integer width = null;
	public Integer height = null;

	public String getTags() {
		return tags;
	}

	public String getDescription() {
		return description;
	}

	public String getFile_url() {
		return file_url;
	}

	public Integer getWidth() {
		return width;
	}

	public Integer getHeight() {
		return height;
	}
}
