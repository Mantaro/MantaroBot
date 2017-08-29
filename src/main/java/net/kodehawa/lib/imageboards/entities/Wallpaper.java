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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Wallpaper {

    private String author;
    private Integer created_at;
    private long file_size;
    private String file_url;
    private Integer height;
    private Integer id;
    private long jpeg_file_size;
    private Integer jpeg_height;
    private String jpeg_url;
    private Integer jpeg_width;
    private Integer preview_height;
    private String preview_url;
    private Integer preview_width;
    private String rating;
    private long sample_file_size;
    private Integer sample_height;
    private String sample_url;
    private Integer sample_width;
    private Integer score;
    private String source;
    private String status;
    private String tags;
    private Integer width;

    public String getAuthor() {
        return author;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getId() {
        return id;
    }

    public String getJpeg_url() {
        return jpeg_url;
    }

    public String getRating() {
        return rating;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getTags() {
        return tags == null ? new ArrayList<>(Arrays.asList("empty", "")) : new ArrayList<>(Arrays.asList(tags.split(" ")));
    }

    public Integer getWidth() {
        return width;
    }
}
