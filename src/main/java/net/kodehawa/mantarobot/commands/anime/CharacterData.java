/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.anime;

import com.fasterxml.jackson.annotation.JsonIgnore;

//Kitsu API character data.
public class CharacterData {
    private String id;
    private String type;
    private Attributes attributes;
    
    public String getId() {
        return this.id;
    }
    
    public String getType() {
        return this.type;
    }
    
    public Attributes getAttributes() {
        return this.attributes;
    }
    
    @JsonIgnore
    public String getURL() {
        return "https://kitsu.io/character/" + id;
    }
    
    public static class Attributes {
        private Names names;
        private String name;
        
        private String description;
        
        private Image image;
        
        public Names getNames() {
            return this.names;
        }
        
        public String getName() {
            return this.name;
        }
        
        public String getDescription() {
            return this.description;
        }
        
        public Image getImage() {
            return this.image;
        }
    }
    
    public static class Names {
        private String en;
        private String ja_jp;
        
        public String getEn() {
            return this.en;
        }
        
        public String getJa_jp() {
            return this.ja_jp;
        }
    }
    
    public static class Image {
        private String original;
        
        public String getOriginal() {
            return this.original;
        }
    }
    
}
