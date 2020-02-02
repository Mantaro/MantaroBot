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

package net.kodehawa.mantarobot.commands.currency.seasons;

public enum Season {
    FIRST("1st"),
    SECOND("2nd");

    private final String display;
    
    Season(String display) {
        this.display = display;
    }
    
    public static Season lookupFromString(String name) {
        for(Season b : Season.values()) {
            //field name search
            if(b.name().equalsIgnoreCase(name)) {
                return b;
            }
            
            //show name search
            if(b.display.equalsIgnoreCase(name)) {
                return b;
            }
        }
        return null;
    }
    
    public String getDisplay() {
        return this.display;
    }
}
