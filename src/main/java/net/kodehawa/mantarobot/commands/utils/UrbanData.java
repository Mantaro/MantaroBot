/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.utils;

import java.util.ArrayList;
import java.util.Objects;

public class UrbanData {

    public ArrayList<List> list = null;

    public UrbanData() { }

    public ArrayList<List> getList() {
        return this.list;
    }

    public static class List {
        public String author;
        public String definition;
        public String example;
        public String permalink;
        public String thumbs_down;
        public String thumbs_up;

        public List() {
        }

        public String getAuthor() {
            return this.author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getDefinition() {
            return this.definition;
        }

        public void setDefinition(String definition) {
            this.definition = definition;
        }

        public String getExample() {
            return this.example;
        }

        public void setExample(String example) {
            this.example = example;
        }

        public String getPermalink() {
            return this.permalink;
        }

        public void setPermalink(String permalink) {
            this.permalink = permalink;
        }

        public String getThumbs_down() {
            return this.thumbs_down;
        }

        public void setThumbs_down(String thumbs_down) {
            this.thumbs_down = thumbs_down;
        }

        public String getThumbs_up() {
            return this.thumbs_up;
        }

        public void setThumbs_up(String thumbs_up) {
            this.thumbs_up = thumbs_up;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof List;
        }
    }
}
