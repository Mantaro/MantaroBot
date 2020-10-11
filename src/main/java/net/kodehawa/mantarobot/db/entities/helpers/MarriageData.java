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

package net.kodehawa.mantarobot.db.entities.helpers;

//Just in case we need more stuff here. Don't want to run into issues later down the road.
public class MarriageData {
    private long marriageCreationMillis;
    //You can create a read-only note for your partner to remember. Will be completely scraped when the marriage ends, and only readable by the ones who agreed to marry.
    private String loveLetter;

    public MarriageData() {
    }

    public long getMarriageCreationMillis() {
        return this.marriageCreationMillis;
    }

    public void setMarriageCreationMillis(long marriageCreationMillis) {
        this.marriageCreationMillis = marriageCreationMillis;
    }

    public String getLoveLetter() {
        return this.loveLetter;
    }

    public void setLoveLetter(String loveLetter) {
        this.loveLetter = loveLetter;
    }
}
