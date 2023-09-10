/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.anime;

import com.fasterxml.jackson.annotation.JsonIgnore;

//Kitsu API character data.
public class CharacterData {
    @SuppressWarnings("unused")
    private String id;
    @SuppressWarnings("unused")
    private String type;
    @SuppressWarnings("unused")
    private Attributes attributes;

    @SuppressWarnings("unused")
    public String getId() {
        return this.id;
    }

    @SuppressWarnings("unused")
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
        @SuppressWarnings("unused")
        private Names names;
        @SuppressWarnings("unused")
        private String name;
        @SuppressWarnings("unused")
        private String description;
        @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public static class Names {
        @SuppressWarnings("unused")
        private String en;
        @SuppressWarnings("unused")
        private String ja_jp;

        @SuppressWarnings("unused")
        public String getEn() {
            return this.en;
        }

        @SuppressWarnings("unused")
        public String getJa_jp() {
            return this.ja_jp;
        }
    }

    @SuppressWarnings("unused")
    public static class Image {
        @SuppressWarnings("unused")
        private String original;

        @SuppressWarnings("unused")
        public String getOriginal() {
            return this.original;
        }
    }

}
