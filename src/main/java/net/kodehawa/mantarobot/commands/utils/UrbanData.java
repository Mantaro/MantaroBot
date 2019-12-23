/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
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
 *
 */

package net.kodehawa.mantarobot.commands.utils;

import java.util.ArrayList;

public class UrbanData {

    public final ArrayList<List> list = null;
    
    public UrbanData() {
    }
    
    public ArrayList<List> getList() {
        return this.list;
    }
    
    public boolean equals(final Object o) {
        if(o == this) return true;
        if(!(o instanceof UrbanData)) return false;
        final UrbanData other = (UrbanData) o;
        if(!other.canEqual((Object) this)) return false;
        final Object this$list = this.list;
        final Object other$list = other.list;
        if(this$list == null ? other$list != null : !this$list.equals(other$list)) return false;
        return true;
    }
    
    protected boolean canEqual(final Object other) {
        return other instanceof UrbanData;
    }
    
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $list = this.list;
        result = result * PRIME + ($list == null ? 43 : $list.hashCode());
        return result;
    }
    
    public String toString() {
        return "UrbanData(list=" + this.list + ")";
    }
    
    public class List {
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
        
        public String getDefinition() {
            return this.definition;
        }
        
        public String getExample() {
            return this.example;
        }
        
        public String getPermalink() {
            return this.permalink;
        }
        
        public String getThumbs_down() {
            return this.thumbs_down;
        }
        
        public String getThumbs_up() {
            return this.thumbs_up;
        }
        
        public void setAuthor(String author) {
            this.author = author;
        }
        
        public void setDefinition(String definition) {
            this.definition = definition;
        }
        
        public void setExample(String example) {
            this.example = example;
        }
        
        public void setPermalink(String permalink) {
            this.permalink = permalink;
        }
        
        public void setThumbs_down(String thumbs_down) {
            this.thumbs_down = thumbs_down;
        }
        
        public void setThumbs_up(String thumbs_up) {
            this.thumbs_up = thumbs_up;
        }
        
        public boolean equals(final Object o) {
            if(o == this) return true;
            if(!(o instanceof List)) return false;
            final List other = (List) o;
            if(!other.canEqual((Object) this)) return false;
            final Object this$author = this.author;
            final Object other$author = other.author;
            if(this$author == null ? other$author != null : !this$author.equals(other$author)) return false;
            final Object this$definition = this.definition;
            final Object other$definition = other.definition;
            if(this$definition == null ? other$definition != null : !this$definition.equals(other$definition))
                return false;
            final Object this$example = this.example;
            final Object other$example = other.example;
            if(this$example == null ? other$example != null : !this$example.equals(other$example)) return false;
            final Object this$permalink = this.permalink;
            final Object other$permalink = other.permalink;
            if(this$permalink == null ? other$permalink != null : !this$permalink.equals(other$permalink)) return false;
            final Object this$thumbs_down = this.thumbs_down;
            final Object other$thumbs_down = other.thumbs_down;
            if(this$thumbs_down == null ? other$thumbs_down != null : !this$thumbs_down.equals(other$thumbs_down))
                return false;
            final Object this$thumbs_up = this.thumbs_up;
            final Object other$thumbs_up = other.thumbs_up;
            if(this$thumbs_up == null ? other$thumbs_up != null : !this$thumbs_up.equals(other$thumbs_up)) return false;
            return true;
        }
        
        protected boolean canEqual(final Object other) {
            return other instanceof List;
        }
        
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $author = this.author;
            result = result * PRIME + ($author == null ? 43 : $author.hashCode());
            final Object $definition = this.definition;
            result = result * PRIME + ($definition == null ? 43 : $definition.hashCode());
            final Object $example = this.example;
            result = result * PRIME + ($example == null ? 43 : $example.hashCode());
            final Object $permalink = this.permalink;
            result = result * PRIME + ($permalink == null ? 43 : $permalink.hashCode());
            final Object $thumbs_down = this.thumbs_down;
            result = result * PRIME + ($thumbs_down == null ? 43 : $thumbs_down.hashCode());
            final Object $thumbs_up = this.thumbs_up;
            result = result * PRIME + ($thumbs_up == null ? 43 : $thumbs_up.hashCode());
            return result;
        }
        
        public String toString() {
            return "UrbanData.List(author=" + this.author + ", definition=" + this.definition + ", example=" + this.example + ", permalink=" + this.permalink + ", thumbs_down=" + this.thumbs_down + ", thumbs_up=" + this.thumbs_up + ")";
        }
    }
}
