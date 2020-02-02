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

package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.MarriageData;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;

public class Marriage implements ManagedObject {
    public static final String DB_TABLE = "marriages";
    private final String player1;
    private final String player2;
    private final String id;
    private final MarriageData data;
    
    @JsonCreator
    @ConstructorProperties({"id", "player1", "player2", "data"})
    public Marriage(@JsonProperty("id") String id, @JsonProperty("player1") String player1, @JsonProperty("player2") String player2, MarriageData data) {
        this.id = id;
        this.player1 = player1;
        this.player2 = player2;
        this.data = data;
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
        return new Marriage(marriageId, userId1, userId2, new MarriageData());
    }
    
    @JsonIgnore
    public String getOtherPlayer(String id) {
        if(player1.equals(id))
            return player2;
        else if(player2.equals(id))
            return player1;
        else
            return null;
    }
    
    public String getPlayer1() {
        return this.player1;
    }
    
    public String getPlayer2() {
        return this.player2;
    }
    
    public String getId() {
        return this.id;
    }
    
    @JsonIgnore
    @Nonnull
    @Override
    public String getTableName() {
        return DB_TABLE;
    }
    
    public MarriageData getData() {
        return this.data;
    }
}
