package net.kodehawa.mantarobot.commands.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageData {

	public Integer id = null;
	public String tags = null;
	public Integer created_at = null;
	public Integer updated_at = null;
	public Integer creator_id = null;
	public Integer approver_id = null;
	public String author = null;
	public Integer change = null;
	public String source = null;
	public Integer score = null;
	public String md5 = null;
	public Integer file_size = null;
	public String file_ext = null;
	public String file_url = null;
	public Boolean is_shown_in_index = null;
	public String preview_url = null;
	public Integer preview_width = null;
	public Integer preview_height = null;
	public Integer actual_preview_width = null;
	public Integer actual_preview_height = null;
	public String sample_url = null;
	public Integer sample_width = null;
	public Integer sample_height = null;
	public Integer sample_file_size = null;
	public String jpeg_url = null;
	public Integer jpeg_width = null;
	public Integer jpeg_height = null;
	public Integer jpeg_file_size = null;
	public String rating = null;
	public Boolean is_rating_locked = null;
	public Boolean has_children = null;
	public Integer parent_id = null;
	public String status = null;
	public Boolean is_pending = null;
	public Integer width = null;
	public Integer height = null;
	public Boolean is_held = null;
	public String frames_pending_string = null;
	public List<String> frames_pending = null;
	public String frames_string = null;
	public List<String> frames = null;
	public Boolean is_note_locked = null;
	public Integer last_noted_at = null;
	public Integer last_commented_at = null;

	public List<String> getTags(){
		return new ArrayList<>(Arrays.asList(tags.split(" ")));
	}
}
