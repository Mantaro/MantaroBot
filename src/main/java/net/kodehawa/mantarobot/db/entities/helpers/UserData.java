/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
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
import java.util.Objects;

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
    private PlayerEquipment equippedItems = new PlayerEquipment(new HashMap<>(), new HashMap<>()); //hashmap is type -> itemId
    
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
        if(increased >= 100)
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
    
    public boolean isHasReceivedFirstKey() {
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
    
    public boolean isReceivedExpirationWarning() {
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
    
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $birthday = this.getBirthday();
        result = result * PRIME + ($birthday == null ? 43 : $birthday.hashCode());
        result = result * PRIME + (this.isHasReceivedFirstKey() ? 79 : 97);
        final Object $premiumKey = this.getPremiumKey();
        result = result * PRIME + ($premiumKey == null ? 43 : $premiumKey.hashCode());
        result = result * PRIME + this.getReminderN();
        final Object $timezone = this.getTimezone();
        result = result * PRIME + ($timezone == null ? 43 : $timezone.hashCode());
        final Object $lang = this.getLang();
        result = result * PRIME + ($lang == null ? 43 : $lang.hashCode());
        result = result * PRIME + this.getDustLevel();
        result = result * PRIME + this.getEquippedPick();
        result = result * PRIME + this.getEquippedRod();
        final Object $equippedItems = this.getEquippedItems();
        result = result * PRIME + ($equippedItems == null ? 43 : $equippedItems.hashCode());
        result = result * PRIME + (this.isReceivedExpirationWarning() ? 79 : 97);
        final Object $keysClaimed = this.getKeysClaimed();
        result = result * PRIME + ($keysClaimed == null ? 43 : $keysClaimed.hashCode());
        final Object $marriageId = this.getMarriageId();
        result = result * PRIME + ($marriageId == null ? 43 : $marriageId.hashCode());
        final Object $waifus = this.getWaifus();
        result = result * PRIME + ($waifus == null ? 43 : $waifus.hashCode());
        result = result * PRIME + this.getWaifuSlots();
        result = result * PRIME + this.getTimesClaimed();
        final Object $reminders = this.getReminders();
        result = result * PRIME + ($reminders == null ? 43 : $reminders.hashCode());
        result = result * PRIME + (this.isPrivateTag() ? 79 : 97);
        return result;
    }
    
    public boolean equals(final Object o) {
        if(o == this) return true;
        if(!(o instanceof UserData)) return false;
        final UserData other = (UserData) o;
        if(!other.canEqual(this)) return false;
        final Object this$birthday = this.getBirthday();
        final Object other$birthday = other.getBirthday();
        if(!Objects.equals(this$birthday, other$birthday)) return false;
        if(this.isHasReceivedFirstKey() != other.isHasReceivedFirstKey()) return false;
        final Object this$premiumKey = this.getPremiumKey();
        final Object other$premiumKey = other.getPremiumKey();
        if(!Objects.equals(this$premiumKey, other$premiumKey)) return false;
        if(this.getReminderN() != other.getReminderN()) return false;
        final Object this$timezone = this.getTimezone();
        final Object other$timezone = other.getTimezone();
        if(!Objects.equals(this$timezone, other$timezone)) return false;
        final Object this$lang = this.getLang();
        final Object other$lang = other.getLang();
        if(!Objects.equals(this$lang, other$lang)) return false;
        if(this.getDustLevel() != other.getDustLevel()) return false;
        if(this.getEquippedPick() != other.getEquippedPick()) return false;
        if(this.getEquippedRod() != other.getEquippedRod()) return false;
        final Object this$equippedItems = this.getEquippedItems();
        final Object other$equippedItems = other.getEquippedItems();
        if(!Objects.equals(this$equippedItems, other$equippedItems))
            return false;
        if(this.isReceivedExpirationWarning() != other.isReceivedExpirationWarning()) return false;
        final Object this$keysClaimed = this.getKeysClaimed();
        final Object other$keysClaimed = other.getKeysClaimed();
        if(!Objects.equals(this$keysClaimed, other$keysClaimed))
            return false;
        final Object this$marriageId = this.getMarriageId();
        final Object other$marriageId = other.getMarriageId();
        if(!Objects.equals(this$marriageId, other$marriageId)) return false;
        final Object this$waifus = this.getWaifus();
        final Object other$waifus = other.getWaifus();
        if(!Objects.equals(this$waifus, other$waifus)) return false;
        if(this.getWaifuSlots() != other.getWaifuSlots()) return false;
        if(this.getTimesClaimed() != other.getTimesClaimed()) return false;
        final Object this$reminders = this.getReminders();
        final Object other$reminders = other.getReminders();
        if(!Objects.equals(this$reminders, other$reminders)) return false;
        return this.isPrivateTag() == other.isPrivateTag();
    }
    
    public String toString() {
        return "UserData(birthday=" + this.getBirthday() + ", hasReceivedFirstKey=" + this.isHasReceivedFirstKey() + ", premiumKey=" + this.getPremiumKey() + ", reminderN=" + this.getReminderN() + ", timezone=" + this.getTimezone() + ", lang=" + this.getLang() + ", dustLevel=" + this.getDustLevel() + ", equippedPick=" + this.getEquippedPick() + ", equippedRod=" + this.getEquippedRod() + ", equippedItems=" + this.getEquippedItems() + ", receivedExpirationWarning=" + this.isReceivedExpirationWarning() + ", keysClaimed=" + this.getKeysClaimed() + ", marriageId=" + this.getMarriageId() + ", waifus=" + this.getWaifus() + ", waifuSlots=" + this.getWaifuSlots() + ", timesClaimed=" + this.getTimesClaimed() + ", reminders=" + this.getReminders() + ", privateTag=" + this.isPrivateTag() + ")";
    }
}
