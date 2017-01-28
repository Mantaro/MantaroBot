package net.kodehawa.mantarobot.commands.utils;

import java.util.ArrayList;

public class UrbanData {

	public ArrayList<String> tags = null;
	public String result_type = null;
	public ArrayList<List> list = null;

	public class List{
		public String definition = null;
		public String permalink = null;
		public String thumbs_up = null;
		public String author = null;
		public Integer defid = null;
		public String current_vote = null;
		public String example = null;
		public String thumbs_down = null;
	}
}
