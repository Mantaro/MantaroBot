package net.kodehawa.lib.imageboards.konachan.main.entities;

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

    public Integer getCreated_at() {
        return created_at;
    }

    public long getFile_size() {
        return file_size;
    }

    public String getFile_url() {
        return file_url;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getId() {
        return id;
    }

    public long getJpeg_file_size() {
        return jpeg_file_size;
    }

    public Integer getJpeg_height() {
        return jpeg_height;
    }

    public String getJpeg_url() {
        return jpeg_url;
    }

    public Integer getJpeg_width() {
        return jpeg_width;
    }

    public Integer getPreview_height() {
        return preview_height;
    }

    public String getPreview_url() {
        return preview_url;
    }

    public Integer getPreview_width() {
        return preview_width;
    }

    public String getRating() {
        return rating;
    }

    public long getSample_file_size() {
        return sample_file_size;
    }

    public Integer getSample_height() {
        return sample_height;
    }

    public String getSample_url() {
        return sample_url;
    }

    public Integer getSample_width() {
        return sample_width;
    }

    public Integer getScore() {
        return score;
    }

    public String getSource() {
        return source;
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
