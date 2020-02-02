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
import net.kodehawa.mantarobot.db.entities.helpers.PlayerStatsData;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;

public class PlayerStats implements ManagedObject {
    public static final String DB_TABLE = "playerstats";
    
    private final String id;
    private final PlayerStatsData data;
    private long gambleWins;
    private long slotsWins;
    private long gambleWinAmount;
    private long slotsWinAmount;
    
    @JsonCreator
    @ConstructorProperties({"id", "gambleWins", "slotsWins", "gambleWinAmount", "slotsWinAmount", "data"})
    public PlayerStats(@JsonProperty("id") String id, @JsonProperty("gambleWins") long gambleWins, @JsonProperty("slotsWins") long slotsWins, @JsonProperty("gambleWinAmount") long gambleWinAmount, @JsonProperty("slotsWinAmount") long slotsWinAmount, @JsonProperty("data") PlayerStatsData data) {
        this.id = id;
        this.gambleWins = gambleWins;
        this.slotsWins = slotsWins;
        this.gambleWinAmount = gambleWinAmount;
        this.slotsWinAmount = slotsWinAmount;
        this.data = data;
    }
    
    public static PlayerStats of(User user) {
        return of(user.getId());
    }
    
    public static PlayerStats of(Member member) {
        return of(member.getUser());
    }
    
    public static PlayerStats of(String userId) {
        return new PlayerStats(userId, 0L, 0L, 0L, 0L, new PlayerStatsData());
    }
    
    @JsonIgnore
    public void incrementGambleWins() {
        this.gambleWins += 1;
    }
    
    @JsonIgnore
    public void incrementSlotsWins() {
        this.slotsWins += 1;
    }
    
    @JsonIgnore
    public void addGambleWin(long amount) {
        this.gambleWinAmount += amount;
    }
    
    @JsonIgnore
    public void addSlotsWin(long amount) {
        this.slotsWinAmount += amount;
    }
    
    public String getId() {
        return this.id;
    }
    
    @Nonnull
    @Override
    public String getTableName() {
        return DB_TABLE;
    }
    
    @JsonIgnore
    @Nonnull
    @Override
    public String getDatabaseId() {
        return getId();
    }
    
    public long getGambleWins() {
        return this.gambleWins;
    }
    
    public long getSlotsWins() {
        return this.slotsWins;
    }
    
    public long getGambleWinAmount() {
        return this.gambleWinAmount;
    }
    
    public long getSlotsWinAmount() {
        return this.slotsWinAmount;
    }
    
    public PlayerStatsData getData() {
        return this.data;
    }
}
