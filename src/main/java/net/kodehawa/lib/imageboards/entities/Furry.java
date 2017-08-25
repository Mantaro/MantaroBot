/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

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
