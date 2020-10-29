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

package net.kodehawa.mantarobot.commands.currency.seasons;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.SeasonalPlayerData;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.Map;

import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.Resolver.serialize;
import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.Resolver.unserialize;

public class SeasonPlayer implements ManagedObject {
    public static final String DB_TABLE = "seasonalplayers";
    private final SeasonalPlayerData data;
    private final String id;
    private final transient Inventory inventory = new Inventory();
    private Long money;
    private Long reputation;
    private final Season season;

    @JsonCreator
    @ConstructorProperties({"id", "season", "money", "inventory", "reputation", "data"})
    public SeasonPlayer(@JsonProperty("id") String id, @JsonProperty("season") Season season, @JsonProperty("money") Long money, @JsonProperty("inventory") Map<Integer, Integer> inventory, @JsonProperty("reputation") Long reputation, @JsonProperty("data") SeasonalPlayerData data) {
        this.id = id;
        this.money = money == null ? 0 : money;
        this.season = season;
        this.reputation = reputation == null ? 0 : reputation;
        this.data = data;
        this.inventory.replaceWith(unserialize(inventory));
    }

    public static SeasonPlayer of(User user, Season season) {
        return of(user.getId(), season);
    }

    public static SeasonPlayer of(Member member, Season season) {
        return of(member.getUser(), season);
    }

    public static SeasonPlayer of(String userId, Season season) {
        return new SeasonPlayer(userId + ":" + season, season, 0L, new HashMap<>(), 0L, new SeasonalPlayerData());
    }

    @JsonIgnore
    public String getUserId() {
        return getId().split(":")[0];
    }

    /**
     * Adds x amount of money from the player.
     *
     * @param money How much?
     */
    public void addMoney(long money) {
        if (money < 0) {
            return;
        }

        try {
            this.money = Math.addExact(this.money, money);
        } catch (ArithmeticException ignored) {
            this.money = 0L;
        }
    }

    /**
     * Adds x amount of reputation to a player. Normally 1.
     *
     * @param rep how much?
     */
    public void addReputation(long rep) {
        this.reputation += rep;
        this.setReputation(reputation);
    }

    /**
     * Removes x amount of money from the player. Only goes though if money removed sums more than zero (avoids negative values).
     *
     * @param money How much?
     */
    public boolean removeMoney(long money) {
        if (this.money - money < 0) return false;
        this.money -= money;
        return true;
    }

    @JsonProperty("inventory")
    public Map<Integer, Integer> rawInventory() {
        return serialize(inventory.asList());
    }

    @JsonIgnore
    public Inventory getInventory() {
        return inventory;
    }

    //it's 3am and i cba to replace usages of this so whatever
    @JsonIgnore
    public boolean isLocked() {
        return data.getLockedUntil() - System.currentTimeMillis() > 0;
    }

    @JsonIgnore
    public void setLocked(boolean locked) {
        data.setLockedUntil(locked ? System.currentTimeMillis() + 35000 : 0);
    }

    public SeasonalPlayerData getData() {
        return this.data;
    }

    @Nonnull
    public String getId() {
        return this.id;
    }

    @JsonIgnore
    @Override
    @Nonnull
    public String getTableName() {
        return DB_TABLE;
    }

    @JsonIgnore
    @Nonnull
    @Override
    public String getDatabaseId() {
        return getUserId();
    }

    public Long getMoney() {
        return this.money;
    }

    public void setMoney(long money) {
        this.money = money < 0 ? 0 : money;
    }

    public Long getReputation() {
        return this.reputation;
    }

    public void setReputation(Long reputation) {
        this.reputation = reputation;
    }

    public Season getSeason() {
        return this.season;
    }
}
