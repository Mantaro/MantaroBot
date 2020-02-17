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

package net.kodehawa.mantarobot.db.entities.helpers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.kodehawa.mantarobot.commands.currency.item.PlayerEquipment;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Marriage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserData {
    private String birthday;
    private boolean hasReceivedFirstKey; //Placeholder here for rethonk plz
    private String premiumKey;
    private int reminderN;
    private String timezone;
    private String lang;
    private int dustLevel; //percentage
    private int equippedPick; //item id, 0 = nothing (even tho in theory 0 its headphones...)
    private int equippedRod; //item id, 0 = nothing
    private PlayerEquipment equippedItems = new PlayerEquipment(new HashMap<>(), new HashMap<>(), new HashMap<>()); //hashmap is type -> itemId

    private boolean receivedExpirationWarning; //premium key about to expire!
    private Map<String, String> keysClaimed = new HashMap<>(); //Map of user -> key. Will be used to account for keys the user can create themselves.

    //NEW MARRIAGE SYSTEM
    private String marriageId;
    //user id, value bought for.
    private Map<String, Long> waifus = new HashMap<>();
    private int waifuSlots = 3;
    private int timesClaimed;

    //Persistent reminders. UUID is saved here.
    private List<String> reminders = new ArrayList<>();

    //Hide tag (and ID on waifu) on marriage/waifu list
    private boolean privateTag = false; //just explicitly setting it to false to make sure people know it's the default.

    public UserData() {
    }

    @JsonIgnore
    public Marriage getMarriage() {
        //we're going full round trip here
        return MantaroData.db().getMarriage(marriageId);
    }

    @JsonIgnore
    public int increaseDustLevel(int by) {
        int increased = dustLevel + Math.min(1, by);
        if (increased >= 100)
            return dustLevel; //same as before, cap at 100.

        this.setDustLevel(increased);
        return this.dustLevel;
    }

    public String getBirthday() {
        return this.birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public boolean hasReceivedFirstKey() {
        return this.hasReceivedFirstKey;
    }

    public void setHasReceivedFirstKey(boolean hasReceivedFirstKey) {
        this.hasReceivedFirstKey = hasReceivedFirstKey;
    }

    public String getPremiumKey() {
        return this.premiumKey;
    }

    public void setPremiumKey(String premiumKey) {
        this.premiumKey = premiumKey;
    }

    public int getReminderN() {
        return this.reminderN;
    }

    public void setReminderN(int reminderN) {
        this.reminderN = reminderN;
    }

    public String getTimezone() {
        return this.timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getLang() {
        return this.lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public int getDustLevel() {
        return this.dustLevel;
    }

    public void setDustLevel(int dustLevel) {
        this.dustLevel = dustLevel;
    }

    public int getEquippedPick() {
        return this.equippedPick;
    }

    public void setEquippedPick(int equippedPick) {
        this.equippedPick = equippedPick;
    }

    public int getEquippedRod() {
        return this.equippedRod;
    }

    public void setEquippedRod(int equippedRod) {
        this.equippedRod = equippedRod;
    }

    public PlayerEquipment getEquippedItems() {
        return this.equippedItems;
    }

    public void setEquippedItems(PlayerEquipment equippedItems) {
        this.equippedItems = equippedItems;
    }

    public boolean hasReceivedExpirationWarning() {
        return this.receivedExpirationWarning;
    }

    public void setReceivedExpirationWarning(boolean receivedExpirationWarning) {
        this.receivedExpirationWarning = receivedExpirationWarning;
    }

    public Map<String, String> getKeysClaimed() {
        return this.keysClaimed;
    }

    public void setKeysClaimed(Map<String, String> keysClaimed) {
        this.keysClaimed = keysClaimed;
    }

    public String getMarriageId() {
        return this.marriageId;
    }

    public void setMarriageId(String marriageId) {
        this.marriageId = marriageId;
    }

    public Map<String, Long> getWaifus() {
        return this.waifus;
    }

    public void setWaifus(Map<String, Long> waifus) {
        this.waifus = waifus;
    }

    public int getWaifuSlots() {
        return this.waifuSlots;
    }

    public void setWaifuSlots(int waifuSlots) {
        this.waifuSlots = waifuSlots;
    }

    public int getTimesClaimed() {
        return this.timesClaimed;
    }

    public void setTimesClaimed(int timesClaimed) {
        this.timesClaimed = timesClaimed;
    }

    public List<String> getReminders() {
        return this.reminders;
    }

    public void setReminders(List<String> reminders) {
        this.reminders = reminders;
    }

    public boolean isPrivateTag() {
        return this.privateTag;
    }

    public void setPrivateTag(boolean privateTag) {
        this.privateTag = privateTag;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof UserData;
    }
}
