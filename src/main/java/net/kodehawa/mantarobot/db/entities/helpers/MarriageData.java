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

import com.fasterxml.jackson.annotation.JsonProperty;
import net.kodehawa.mantarobot.commands.currency.pets.HousePet;

public class MarriageData {
    private long marriageCreationMillis;
    private String loveLetter;
    @JsonProperty("hasHouse")
    private boolean hasHouse;
    private String houseName;
    @JsonProperty("hasCar")
    private boolean hasCar;
    private String carName;
    private HousePet pet;
    private String timezone;

    public MarriageData() { }

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

    public boolean hasHouse() {
        return hasHouse;
    }

    public void setHasHouse(boolean hasHouse) {
        this.hasHouse = hasHouse;
    }

    public String getHouseName() {
        return houseName;
    }

    public void setHouseName(String houseName) {
        this.houseName = houseName;
    }

    public boolean hasCar() {
        return hasCar;
    }

    public void setHasCar(boolean hasCar) {
        this.hasCar = hasCar;
    }

    public String getCarName() {
        return carName;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }

    public void setPet(HousePet pet) {
        this.pet = pet;
    }

    public HousePet getPet() {
        return pet;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
