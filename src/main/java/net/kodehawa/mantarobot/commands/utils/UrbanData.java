package net.kodehawa.mantarobot.commands.utils;

import java.util.ArrayList;

public class UrbanData {

    public ArrayList<List> list = null;
    public String result_type = null;
    public ArrayList<String> tags = null;

    public class List {
        public String author = null;
        public String current_vote = null;
        public Integer defid = null;
        public String definition = null;
        public String example = null;
        public String permalink = null;
        public String thumbs_down = null;
        public String thumbs_up = null;
    }
}
