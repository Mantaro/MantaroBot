package net.kodehawa.lib.imageboards.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class YandereImage {

	public Integer actual_preview_height = null;
	public final Integer actual_preview_width = null;
	public Integer approver_id = null;
	public final String author = null;
	public Integer change = null;
	public Integer created_at = null;
	public Integer creator_id = null;
	public final String file_ext = null;
	public final Integer file_size = null;
	public final String file_url = null;
	public List<String> frames = null;
	public List<String> frames_pending = null;
	public String frames_pending_string = null;
	public String frames_string = null;
	public Boolean has_children = null;
	public final Integer height = null;
	public final Integer id = null;
	public Boolean is_held = null;
	public Boolean is_note_locked = null;
	public Boolean is_pending = null;
	public Boolean is_rating_locked = null;
	public Boolean is_shown_in_index = null;
	public final Integer jpeg_file_size = null;
	public final Integer jpeg_height = null;
	public final String jpeg_url = null;
	public final Integer jpeg_width = null;
	public Integer last_commented_at = null;
	public Integer last_noted_at = null;
	public String md5 = null;
	public Integer parent_id = null;
	public final Integer preview_height = null;
	public final String preview_url = null;
	public final Integer preview_width = null;
	public final String rating = null;
	public final Integer sample_file_size = null;
	public final Integer sample_height = null;
	public final String sample_url = null;
	public final Integer sample_width = null;
	public final Integer score = null;
	public String source = null;
	public final String status = null;
	public final String tags = null;
	public Integer updated_at = null;
	public final Integer width = null;

	public Integer getActual_preview_width() {
		return actual_preview_width;
	}

	public String getAuthor() {
		return author;
	}

	public String getFile_ext() {
		return file_ext;
	}

	public Integer getFile_size() {
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

	public Integer getJpeg_file_size() {
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

	public Integer getSample_file_size() {
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

	public String getStatus() {
		return status;
	}

	public List<String> getTags() {
		return new ArrayList<>(Arrays.asList(tags.split(" ")));
	}

	public Integer getWidth() {
		return width;
	}
}
