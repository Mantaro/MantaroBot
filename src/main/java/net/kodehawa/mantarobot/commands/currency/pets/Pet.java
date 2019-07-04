/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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

package net.kodehawa.mantarobot.commands.currency.pets;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.beans.ConstructorProperties;

public class Pet {
    public String owner;

    public String imagePath; //Choose from different pre-picked images. Configurable
    public String name; //Only letters.
    public PetStats stats;

    public long age;

    @JsonCreator
    @ConstructorProperties({"owner", "name", "stats", "age"})
    public Pet(String owner, String name, PetStats stats, long age) {
        this.owner = owner;
        this.name = name;
        this.stats = stats;
        this.age = age;
    }
}
