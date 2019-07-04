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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.beans.ConstructorProperties;

@Getter
@Setter
public class Pet {
    private String owner;

    private ImageType image; //Choose from different pre-picked images. Configurable
    private String name; //Only letters.
    private PetStats stats;
    private Type element;

    private long epochCreatedAt;
    private long age;
    private long tier; //Calculated between 1 to 20 according to current pet stats.
    private long tradePrice; //Calculated using stats + tier.

    private PetData data;

    @JsonCreator
    @ConstructorProperties({"owner", "name", "stats", "data", "element", "inventory", "age"})
    public Pet(@JsonProperty("owner") String owner, @JsonProperty("name") String name, @JsonProperty("stats") PetStats stats, @JsonProperty("data") PetData data, @JsonProperty("element") Type element, @JsonProperty("age")  long age) {
        this.owner = owner;
        this.name = name;
        this.stats = stats;
        this.data = data;
        this.element = element;
        this.age = age;
    }

    public static Pet create(String owner, String name, Type element) {
        Pet pet = new Pet(owner, name, new PetStats(), new PetData(), element, 1);
        pet.setEpochCreatedAt(System.currentTimeMillis());
        return pet;
    }

    public Pet changeImage(ImageType type) {
        this.image = type;
        return this;
    }

    //TODO
    public long calculateTier() {
        return 1;
    }

    public enum ImageType {
        SPACESHIP("");

        @Getter
        public String image;
        ImageType(String image) {
            this.image = image;
        }
    }

    @Getter
    public enum Type {
        EARTH("Earth", "commands.pet.types.earth"), WATER("Water", "commands.pet.types.water"), FIRE("Fire", "commands.pet.types.fire");

        String readable;
        String translatable;
        Type(String readable, String translatable) {
            this.readable = readable;
            this.translatable = translatable;
        }
    }
}
