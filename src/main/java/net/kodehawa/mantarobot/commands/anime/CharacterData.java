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

package net.kodehawa.mantarobot.commands.anime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

public class CharacterData {
    private Integer id = null;
    private String image_url_lge = null;
    private String image_url_med = null;
    private String info = null;
    private String name_alt = null;
    private String name_first = null;
    private String name_japanese = null;
    private String name_last = null;

    @JsonIgnore
    public static CharacterData[] fromJson(String json) {
        return GsonDataManager.GSON_PRETTY.fromJson(json, CharacterData[].class);
    }

    @JsonIgnore
    public static CharacterData fromJsonFirst(String json) {
        return fromJson(json)[0];
    }

    public Integer getId() {
        return id;
    }

    public String getLargeImageUrl() {
        return image_url_lge;
    }

    public String getMedImageUrl() {
        return image_url_med;
    }

    public String getInfo() {
        return info;
    }

    public String getNameAlt() {
        return name_alt;
    }

    public String getFirstName() {
        return name_first;
    }

    public String getJapaneseName() {
        return name_japanese;
    }

    public String getLastName() {
        return name_last;
    }
}
