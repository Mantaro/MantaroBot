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
import lombok.Setter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.PlayerInterface;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.Map;

import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.Resolver.serialize;
import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.Resolver.unserialize;

public class Player implements PlayerInterface, ManagedObject {
    public static final String DB_TABLE = "players";
    @Getter
    private final PlayerData data;
    @Getter
    private final String id;
    private final transient Inventory inventory = new Inventory();
    @Getter
    private Long level;
    private Long money;
    @Setter
    private Long reputation;

    @JsonCreator
    @ConstructorProperties({"id", "level", "money", "reputation", "inventory", "data"})
    public Player(@JsonProperty("id") String id, @JsonProperty("level") Long level, @JsonProperty("money") Long money, @JsonProperty("reputation") Long reputation, @JsonProperty("inventory") Map<Integer, Integer> inventory, @JsonProperty("data") PlayerData data) {
        this.id = id;
        this.level = level == null ? 0 : level;
        this.money = money == null ? 0 : money;
        this.reputation = reputation == null ? 0 : reputation;
        this.data = data;
        this.inventory.replaceWith(unserialize(inventory));
    }

    /**
     * The Player.of methods are for resetting players or creating new ones when they don't exist.
     * @param user The user to create or reset.
     * @return The new Player.
     */
    public static Player of(User user) {
        return of(user.getId());
    }

    /**
     * The Player.of methods are for resetting players or creating new ones when they don't exist.
     * @param member The user to create or reset.
     * @return The new Player.
     */
    public static Player of(Member member) {
        return of(member.getUser());
    }

    /**
     * The Player.of methods are for resetting players or creating new ones when they don't exist.
     * @param userId The user to create or reset.
     * @return The new Player.
     */
    public static Player of(String userId) {
        return new Player(userId + ":g", 0L, 0L, 0L, new HashMap<>(), new PlayerData());
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

    /**
     * Adds x amount of money from the player.
     *
     * @param money How much?
     * @return pls dont overflow.
     */
    public boolean addMoney(long money) {
        if(money < 0) return false;
        try {
            this.money = Math.addExact(this.money, money);
            return true;
        } catch(ArithmeticException ignored) {
            this.money = 0L;
            this.getInventory().process(new ItemStack(Items.STAR, 1));
            return false;
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

    @JsonIgnore
    public String getGuildId() {
        return getId().split(":")[1];
    }

    @JsonIgnore
    public Inventory getInventory() {
        return inventory;
    }

    @JsonIgnore
    public String getUserId() {
        return getId().split(":")[0];
    }

    @JsonIgnore
    public boolean isGlobal() {
        return getGuildId().equals("g");
    }

    @JsonProperty("inventory")
    public Map<Integer, Integer> rawInventory() {
        return serialize(inventory.asList());
    }

    @JsonIgnore
    public Long getMoney() {
        return money;
    }

    @JsonIgnore
    public long getReputation() {
        return reputation;
    }

    /**
     * Removes x amount of money from the player. Only goes though if money removed sums more than zero (avoids negative values).
     *
     * @param money How much?
     */
    public boolean removeMoney(long money) {
        if(this.money - money < 0) return false;
        this.money -= money;
        return true;
    }

    public Player setLevel(long level) {
        this.level = level;
        return this;
    }

    public Player setMoney(long money) {
        this.money = money < 0 ? 0 : money;
        return this;
    }

    @Override
    public void savePlayer() {
        this.save();
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
}
