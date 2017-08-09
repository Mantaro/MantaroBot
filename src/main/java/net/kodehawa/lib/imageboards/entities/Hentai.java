package net.kodehawa.lib.imageboards.entities;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "posts")
public class Hentai {

	@JacksonXmlProperty(isAttribute = true)
	public String file_url;
	public Integer height;
	public String tags;
	public Integer width;

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
