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

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedMongoObject;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class PlayerStats implements ManagedMongoObject {
    @BsonIgnore
    public static final String DB_TABLE = "playerstats";
    @BsonIgnore
    public Map<String, Object> fieldTracker = new HashMap<>();

    @BsonId
    private String id;
    private long gambleWins;
    private long slotsWins;
    private long gambleWinAmount;
    private long slotsWinAmount;
    private long craftedItems;
    private long repairedItems;
    private long salvagedItems;
    private long toolsBroken;
    private long looted;
    private long mined;
    private long gambleLose;
    private long slotsLose;

    // Needed for serialization
    public PlayerStats() { }

    @SuppressWarnings("SameParameterValue")
    private PlayerStats(String id, long gambleWins, long slotsWins, long gambleWinAmount, long slotsWinAmount) {
        this.id = id;
        this.gambleWins = gambleWins;
        this.slotsWins = slotsWins;
        this.gambleWinAmount = gambleWinAmount;
        this.slotsWinAmount = slotsWinAmount;
    }

    public static PlayerStats of(User user) {
        return of(user.getId());
    }

    public static PlayerStats of(Member member) {
        return of(member.getUser());
    }

    public static PlayerStats of(String userId) {
        return new PlayerStats(userId, 0L, 0L, 0L, 0L);
    }

    @Override
    @Nonnull
    public String getId() {
        return this.id;
    }

    @Nonnull
    @Override
    @BsonIgnore
    public String getTableName() {
        return DB_TABLE;
    }

    @BsonIgnore
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

    public long getCraftedItems() {
        return craftedItems;
    }

    public long getRepairedItems() {
        return repairedItems;
    }

    public long getSalvagedItems() {
        return salvagedItems;
    }

    public long getToolsBroken() {
        return toolsBroken;
    }

    public long getLooted() {
        return this.looted;
    }

    public long getMined() {
        return this.mined;
    }

    public long getGambleLose() {
        return this.gambleLose;
    }

    public void setGambleLose(long gambleLose) {
        this.gambleLose = gambleLose;
    }

    public long getSlotsLose() {
        return this.slotsLose;
    }

    public void setSlotsLose(long slotsLose) {
        this.slotsLose = slotsLose;
    }

    public void setLooted(long looted) {
        this.looted = looted;
    }

    public void setMined(long mined) {
        this.mined = mined;
    }

    public void setCraftedItems(long craftedItems) {
        this.craftedItems = craftedItems;
    }

    public void setRepairedItems(long repairedItems) {
        this.repairedItems = repairedItems;
    }

    public void setToolsBroken(long toolsBroken) {
        this.toolsBroken = toolsBroken;
    }

    public void setSalvagedItems(long salvagedItems) {
        this.salvagedItems = salvagedItems;
    }

    @BsonIgnore
    public void addGambleWin(long amount) {
        this.gambleWinAmount += amount;
        fieldTracker.put("gambleWinAmount", this.gambleWinAmount);
    }

    @BsonIgnore
    public void addSlotsWin(long amount) {
        this.slotsWinAmount += amount;
        fieldTracker.put("slotsWinAmount", this.slotsWinAmount);
    }

    @BsonIgnore
    public void incrementMined() {
        this.mined += 1;
        fieldTracker.put("mined", this.mined);
    }

    @BsonIgnore
    public void incrementLooted() {
        this.looted += 1;
        fieldTracker.put("looted", this.looted);
    }

    @BsonIgnore
    public void incrementGambleWins() {
        this.gambleWins += 1;
        fieldTracker.put("gambleWins", this.gambleWins);
    }

    @BsonIgnore
    public void incrementSlotsWins() {
        this.slotsWins += 1;
        fieldTracker.put("slotsWins", this.slotsWins);
    }

    @BsonIgnore
    public void incrementGambleLose() {
        gambleLose += 1;
        fieldTracker.put("gambleLose", this.gambleLose);
    }

    @BsonIgnore
    public void incrementSlotsLose() {
        slotsLose += 1;
        fieldTracker.put("slotsLose", this.slotsLose);
    }

    @BsonIgnore
    public void incrementToolsBroken() {
        this.toolsBroken++;
        fieldTracker.put("toolsBroken", this.toolsBroken);
    }

    @BsonIgnore
    public void incrementCraftedItems() {
        this.craftedItems++;
        fieldTracker.put("craftedItems", this.craftedItems);
    }

    @BsonIgnore
    public void incrementCraftedItems(int amount) {
        this.craftedItems += amount;
        fieldTracker.put("craftedItems", this.craftedItems);
    }

    @BsonIgnore
    public void incrementRepairedItems() {
        this.repairedItems++;
        fieldTracker.put("repairedItems", this.repairedItems);
    }

    @BsonIgnore
    public void incrementSalvagedItems() {
        this.salvagedItems++;
        fieldTracker.put("salvagedItems", this.salvagedItems);
    }

    @BsonIgnore
    @Override
    public void updateAllChanged() {
        MantaroData.db().updateFieldValues(this, fieldTracker);
    }

    @Override
    public void insertOrReplace() {
        MantaroData.db().saveMongo(this, PlayerStats.class);
    }

    @Override
    public void delete() {
        MantaroData.db().deleteMongo(this, PlayerStats.class);
    }
}
