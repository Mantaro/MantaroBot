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
package net.kodehawa.mantarobot.commands.currency.pets.global;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;

import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.Resolver.unserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Pet {
    @JsonIgnore
    private final transient Inventory petInventory = new Inventory();
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
    @ConstructorProperties({"owner", "name", "stats", "data", "element", "inventory", "age", "inventory"})
    public Pet(@JsonProperty("owner") String owner, @JsonProperty("name") String name, @JsonProperty("stats") PetStats stats, @JsonProperty("data") PetData data, @JsonProperty("element") Type element, @JsonProperty("age") long age, @JsonProperty("inventory") Map<Integer, Integer> inventory) {
        this.owner = owner;
        this.name = name;
        this.stats = stats;
        this.data = data;
        this.element = element;
        this.age = age;
        this.petInventory.replaceWith(unserialize(inventory));
    }

    public static Pet create(String owner, String name, Type element) {
        Pet pet = new Pet(owner, name, new PetStats(), new PetData(), element, 1, new HashMap<>());
        pet.setEpochCreatedAt(System.currentTimeMillis());
        return pet;
    }

    public Pet changeImage(ImageType type) {
        this.image = type;
        return this;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public ImageType getImage() {
        return this.image;
    }

    public void setImage(ImageType image) {
        this.image = image;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PetStats getStats() {
        return this.stats;
    }

    public void setStats(PetStats stats) {
        this.stats = stats;
    }

    public Type getElement() {
        return this.element;
    }

    public void setElement(Type element) {
        this.element = element;
    }

    public long getEpochCreatedAt() {
        return this.epochCreatedAt;
    }

    public void setEpochCreatedAt(long epochCreatedAt) {
        this.epochCreatedAt = epochCreatedAt;
    }

    public long getTier() {
        return this.tier;
    }

    public void setTier(long tier) {
        this.tier = tier;
    }

    public long getTradePrice() {
        return this.tradePrice;
    }

    public void setTradePrice(long tradePrice) {
        this.tradePrice = tradePrice;
    }

    public PetData getData() {
        return this.data;
    }

    public void setData(PetData data) {
        this.data = data;
    }

    public Inventory getPetInventory() {
        return this.petInventory;
    }

    public long getAge() {
        return System.currentTimeMillis() - getEpochCreatedAt();
    }

    public void setAge(long age) {
        this.age = age;
    }

    @JsonIgnore
    public long getAgeDays() {
        return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - getEpochCreatedAt());
    }

    public enum ImageType {
        //hello sukeban studios (https://va11halla.fandom.com/wiki/Lilim)
        SPACESHIP(""), CAT(""), DOG(""), ROBOT(""), LILIM(""), CATGIRL("");

        public final String image;

        ImageType(String image) {
            this.image = image;
        }

        public String getImage() {
            return this.image;
        }
    }

    public enum Type {
        EARTH("Earth", "commands.pet.types.earth"), WATER("Water", "commands.pet.types.water"), FIRE("Fire", "commands.pet.types.fire");

        final String readable;
        final String translatable;

        Type(String readable, String translatable) {
            this.readable = readable;
            this.translatable = translatable;
        }

        public String getReadable() {
            return this.readable;
        }

        public String getTranslatable() {
            return this.translatable;
        }
    }
}
