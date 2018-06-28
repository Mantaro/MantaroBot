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

package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerStatsData;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;

public class PlayerStats implements ManagedObject {
    public static final String DB_TABLE = "playerstats";

    @Getter
    private final String id;
    @Getter
    private long gambleWins;
    @Getter
    private long slotsWins;
    @Getter
    private long gambleWinAmount;
    @Getter
    private long slotsWinAmount;
    @Getter
    private final PlayerStatsData data;

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
}
