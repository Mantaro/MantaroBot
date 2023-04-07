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

package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.currency.pets.HousePet;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedMongoObject;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;

import javax.annotation.Nonnull;

public class Marriage implements ManagedMongoObject {
    @BsonIgnore
    public static final String DB_TABLE = "marriages";

    @BsonId
    private String id;
    private String player1;
    private String player2;

    // Serialization constructor
    public Marriage() { }

    public Marriage(String id, String player1, String player2) {
        this.id = id;
        this.player1 = player1;
        this.player2 = player2;
    }

    /**
     * The Marriage.of methods are for resetting marriages or creating new ones when they don't exist.
     *
     * @return The new Marriage.
     */
    public static Marriage of(String marriageId, User user1, User user2) {
        return of(marriageId, user1.getId(), user2.getId());
    }

    /**
     * The Marriage.of methods are for resetting marriages or creating new ones when they don't exist.
     *
     * @return The new Marriage.
     */
    public static Marriage of(String marriageId, Member member1, Member member2) {
        return of(marriageId, member1.getUser(), member2.getUser());
    }

    /**
     * The Marriage.of methods are for resetting marriages or creating new ones when they don't exist.
     *
     * @return The new Marriage.
     */
    public static Marriage of(String marriageId, String userId1, String userId2) {
        return new Marriage(marriageId, userId1, userId2);
    }

    @JsonIgnore
    public String getOtherPlayer(String id) {
        if (player1.equals(id)) {
            return player2;
        } else if (player2.equals(id)) {
            return player1;
        } else {
            return null;
        }
    }

    public String getPlayer1() {
        return this.player1;
    }

    public String getPlayer2() {
        return this.player2;
    }

    private long marriageCreationMillis;
    private String loveLetter;

    @BsonProperty("hasHouse")
    private boolean hasHouse;
    private String houseName;

    @BsonProperty("hasCar")
    private boolean hasCar;
    private String carName;

    private HousePet pet;
    private String timezone;
    private long lockedUntil;

    public long getMarriageCreationMillis() {
        return this.marriageCreationMillis;
    }

    public String getLoveLetter() {
        return this.loveLetter;
    }

    public boolean hasHouse() {
        return hasHouse;
    }

    public String getHouseName() {
        return houseName;
    }

    public boolean hasCar() {
        return hasCar;
    }

    public String getCarName() {
        return carName;
    }

    public HousePet getPet() {
        return pet;
    }

    public String getTimezone() {
        return timezone;
    }

    public long getLockedUntil() {
        return lockedUntil;
    }

    public void setHouseName(String houseName) {
        this.houseName = houseName;
        updateField("houseName", houseName);
    }

    public void setHasHouse(boolean hasHouse) {
        this.hasHouse = hasHouse;
        updateField("hasHouse", hasHouse);
    }

    public void setHasCar(boolean hasCar) {
        this.hasCar = hasCar;
        updateField("hasCar", hasCar);
    }

    public void setCarName(String carName) {
        this.carName = carName;
        updateField("carName", carName);
    }

    public void setPet(HousePet pet) {
        this.pet = pet;
        updateField("pet", pet);
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
        updateField("timezone", timezone);
    }

    public void setLockedUntil(long lockedUntil) {
        this.lockedUntil = lockedUntil;
        updateField("lockedUntil", lockedUntil);
    }

    public void setMarriageCreationMillis(long marriageCreationMillis) {
        this.marriageCreationMillis = marriageCreationMillis;
        updateField("marriageCreationMillis", lockedUntil);
    }

    public void setLoveLetter(String loveLetter) {
        this.loveLetter = loveLetter;
        updateField("loveLetter", lockedUntil);
    }

    //it's 3am and i cba to replace usages of this so whatever
    @BsonIgnore
    public boolean isLocked() {
        return getLockedUntil() - System.currentTimeMillis() > 0;
    }

    @BsonIgnore
    public void setLocked(boolean locked) {
        setLockedUntil(locked ? System.currentTimeMillis() + 35000 : 0);
    }

    @Nonnull
    public String getId() {
        return this.id;
    }

    @BsonIgnore
    @Nonnull
    @Override
    public String getTableName() {
        return DB_TABLE;
    }

    @Override
    public void save() {
        MantaroData.db().saveMongo(this, Marriage.class);
    }

    @Override
    public void delete() {
        MantaroData.db().deleteMongo(this, Marriage.class);
    }
}
