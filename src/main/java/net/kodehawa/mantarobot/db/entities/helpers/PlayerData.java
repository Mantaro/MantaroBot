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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.item.PotionEffect;
import net.kodehawa.mantarobot.commands.currency.pets.Pet;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.profile.ProfileComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerData {
    public long experience = 0;
    private List<Badge> badges = new ArrayList<>();
    //Fix massive misspelling fuck up.
    @JsonProperty("dailyStrike")
    private long dailyStreak;
    private String description = null;
    private long gamesWon = 0;
    private long lastDailyAt;
    private long lockedUntil = 0;
    private Long marriedSince = null;
    private String marriedWith = null;
    private long moneyOnBank = 0;
    //null = most important badge shows.
    private Badge mainBadge = null;
    private long marketUsed;
    private boolean showBadge = true;
    private PotionEffect activePotion;
    private PotionEffect activeBuff;
    private long waifuCachedValue;
    private List<ProfileComponent> profileComponents = new LinkedList<>();
    
    //lol?
    //this is needed so it actually works, even though it does absolutely nothing
    //thanks rethonkdb
    private List<Pet> profilePets = new LinkedList<>();
    
    private long petSlots = 4;
    private Map<String, Pet> pets = new HashMap<>();
    
    public PlayerData() {
    }
    
    @JsonIgnore
    //LEGACY SUPPORT
    //Marriage UUID data is on UserData now!
    public boolean isMarried() {
        return marriedWith != null && MantaroBot.getInstance().getShardManager().getUserById(marriedWith) != null;
    }
    
    @JsonIgnore
    public boolean hasBadge(Badge b) {
        return badges.contains(b);
    }
    
    @JsonIgnore
    public boolean addBadgeIfAbsent(Badge b) {
        if(hasBadge(b)) {
            return false;
        }
        
        badges.add(b);
        return true;
    }
    
    @JsonIgnore
    public boolean removeBadge(Badge b) {
        if(!hasBadge(b)) {
            return false;
        }
        
        badges.remove(b);
        return true;
    }
    
    public long getExperience() {
        return this.experience;
    }
    
    public void setExperience(long experience) {
        this.experience = experience;
    }
    
    public List<Badge> getBadges() {
        return this.badges;
    }
    
    public void setBadges(List<Badge> badges) {
        this.badges = badges;
    }
    
    public long getDailyStreak() {
        return this.dailyStreak;
    }
    
    public void setDailyStreak(long dailyStreak) {
        this.dailyStreak = dailyStreak;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public long getGamesWon() {
        return this.gamesWon;
    }
    
    public void setGamesWon(long gamesWon) {
        this.gamesWon = gamesWon;
    }
    
    public long getLastDailyAt() {
        return this.lastDailyAt;
    }
    
    public void setLastDailyAt(long lastDailyAt) {
        this.lastDailyAt = lastDailyAt;
    }
    
    public long getLockedUntil() {
        return this.lockedUntil;
    }
    
    public void setLockedUntil(long lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
    
    public Long getMarriedSince() {
        return this.marriedSince;
    }
    
    public void setMarriedSince(Long marriedSince) {
        this.marriedSince = marriedSince;
    }
    
    public String getMarriedWith() {
        return this.marriedWith;
    }
    
    public void setMarriedWith(String marriedWith) {
        this.marriedWith = marriedWith;
    }
    
    public long getMoneyOnBank() {
        return this.moneyOnBank;
    }
    
    public void setMoneyOnBank(long moneyOnBank) {
        this.moneyOnBank = moneyOnBank;
    }
    
    public Badge getMainBadge() {
        return this.mainBadge;
    }
    
    public void setMainBadge(Badge mainBadge) {
        this.mainBadge = mainBadge;
    }
    
    public long getMarketUsed() {
        return this.marketUsed;
    }
    
    public void setMarketUsed(long marketUsed) {
        this.marketUsed = marketUsed;
    }
    
    public boolean isShowBadge() {
        return this.showBadge;
    }
    
    public void setShowBadge(boolean showBadge) {
        this.showBadge = showBadge;
    }
    
    public PotionEffect getActivePotion() {
        return this.activePotion;
    }
    
    public void setActivePotion(PotionEffect activePotion) {
        this.activePotion = activePotion;
    }
    
    public PotionEffect getActiveBuff() {
        return this.activeBuff;
    }
    
    public void setActiveBuff(PotionEffect activeBuff) {
        this.activeBuff = activeBuff;
    }
    
    public long getWaifuCachedValue() {
        return this.waifuCachedValue;
    }
    
    public void setWaifuCachedValue(long waifuCachedValue) {
        this.waifuCachedValue = waifuCachedValue;
    }
    
    public List<ProfileComponent> getProfileComponents() {
        return this.profileComponents;
    }
    
    public void setProfileComponents(List<ProfileComponent> profileComponents) {
        this.profileComponents = profileComponents;
    }
    
    public List<Pet> getProfilePets() {
        return this.profilePets;
    }
    
    public void setProfilePets(List<Pet> profilePets) {
        this.profilePets = profilePets;
    }
    
    public long getPetSlots() {
        return this.petSlots;
    }
    
    public void setPetSlots(long petSlots) {
        this.petSlots = petSlots;
    }
    
    public Map<String, Pet> getPets() {
        return this.pets;
    }
    
    public void setPets(Map<String, Pet> pets) {
        this.pets = pets;
    }
    
    protected boolean canEqual(final Object other) {
        return other instanceof PlayerData;
    }
    
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $experience = this.getExperience();
        result = result * PRIME + (int) ($experience >>> 32 ^ $experience);
        final Object $badges = this.getBadges();
        result = result * PRIME + ($badges == null ? 43 : $badges.hashCode());
        final long $dailyStreak = this.getDailyStreak();
        result = result * PRIME + (int) ($dailyStreak >>> 32 ^ $dailyStreak);
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final long $gamesWon = this.getGamesWon();
        result = result * PRIME + (int) ($gamesWon >>> 32 ^ $gamesWon);
        final long $lastDailyAt = this.getLastDailyAt();
        result = result * PRIME + (int) ($lastDailyAt >>> 32 ^ $lastDailyAt);
        final long $lockedUntil = this.getLockedUntil();
        result = result * PRIME + (int) ($lockedUntil >>> 32 ^ $lockedUntil);
        final Object $marriedSince = this.getMarriedSince();
        result = result * PRIME + ($marriedSince == null ? 43 : $marriedSince.hashCode());
        final Object $marriedWith = this.getMarriedWith();
        result = result * PRIME + ($marriedWith == null ? 43 : $marriedWith.hashCode());
        final long $moneyOnBank = this.getMoneyOnBank();
        result = result * PRIME + (int) ($moneyOnBank >>> 32 ^ $moneyOnBank);
        final Object $mainBadge = this.getMainBadge();
        result = result * PRIME + ($mainBadge == null ? 43 : $mainBadge.hashCode());
        final long $marketUsed = this.getMarketUsed();
        result = result * PRIME + (int) ($marketUsed >>> 32 ^ $marketUsed);
        result = result * PRIME + (this.isShowBadge() ? 79 : 97);
        final Object $activePotion = this.getActivePotion();
        result = result * PRIME + ($activePotion == null ? 43 : $activePotion.hashCode());
        final Object $activeBuff = this.getActiveBuff();
        result = result * PRIME + ($activeBuff == null ? 43 : $activeBuff.hashCode());
        final long $waifuCachedValue = this.getWaifuCachedValue();
        result = result * PRIME + (int) ($waifuCachedValue >>> 32 ^ $waifuCachedValue);
        final Object $profileComponents = this.getProfileComponents();
        result = result * PRIME + ($profileComponents == null ? 43 : $profileComponents.hashCode());
        final Object $profilePets = this.getProfilePets();
        result = result * PRIME + ($profilePets == null ? 43 : $profilePets.hashCode());
        final long $petSlots = this.getPetSlots();
        result = result * PRIME + (int) ($petSlots >>> 32 ^ $petSlots);
        final Object $pets = this.getPets();
        result = result * PRIME + ($pets == null ? 43 : $pets.hashCode());
        return result;
    }
    
    public boolean equals(final Object o) {
        if(o == this) return true;
        if(!(o instanceof PlayerData)) return false;
        final PlayerData other = (PlayerData) o;
        if(!other.canEqual(this)) return false;
        if(this.getExperience() != other.getExperience()) return false;
        final Object this$badges = this.getBadges();
        final Object other$badges = other.getBadges();
        if(!Objects.equals(this$badges, other$badges)) return false;
        if(this.getDailyStreak() != other.getDailyStreak()) return false;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        if(!Objects.equals(this$description, other$description))
            return false;
        if(this.getGamesWon() != other.getGamesWon()) return false;
        if(this.getLastDailyAt() != other.getLastDailyAt()) return false;
        if(this.getLockedUntil() != other.getLockedUntil()) return false;
        final Object this$marriedSince = this.getMarriedSince();
        final Object other$marriedSince = other.getMarriedSince();
        if(!Objects.equals(this$marriedSince, other$marriedSince))
            return false;
        final Object this$marriedWith = this.getMarriedWith();
        final Object other$marriedWith = other.getMarriedWith();
        if(!Objects.equals(this$marriedWith, other$marriedWith))
            return false;
        if(this.getMoneyOnBank() != other.getMoneyOnBank()) return false;
        final Object this$mainBadge = this.getMainBadge();
        final Object other$mainBadge = other.getMainBadge();
        if(!Objects.equals(this$mainBadge, other$mainBadge)) return false;
        if(this.getMarketUsed() != other.getMarketUsed()) return false;
        if(this.isShowBadge() != other.isShowBadge()) return false;
        final Object this$activePotion = this.getActivePotion();
        final Object other$activePotion = other.getActivePotion();
        if(!Objects.equals(this$activePotion, other$activePotion))
            return false;
        final Object this$activeBuff = this.getActiveBuff();
        final Object other$activeBuff = other.getActiveBuff();
        if(!Objects.equals(this$activeBuff, other$activeBuff)) return false;
        if(this.getWaifuCachedValue() != other.getWaifuCachedValue()) return false;
        final Object this$profileComponents = this.getProfileComponents();
        final Object other$profileComponents = other.getProfileComponents();
        if(!Objects.equals(this$profileComponents, other$profileComponents))
            return false;
        final Object this$profilePets = this.getProfilePets();
        final Object other$profilePets = other.getProfilePets();
        if(!Objects.equals(this$profilePets, other$profilePets))
            return false;
        if(this.getPetSlots() != other.getPetSlots()) return false;
        final Object this$pets = this.getPets();
        final Object other$pets = other.getPets();
        return Objects.equals(this$pets, other$pets);
    }
    
    public String toString() {
        return "PlayerData(experience=" + this.getExperience() + ", badges=" + this.getBadges() + ", dailyStreak=" + this.getDailyStreak() + ", description=" + this.getDescription() + ", gamesWon=" + this.getGamesWon() + ", lastDailyAt=" + this.getLastDailyAt() + ", lockedUntil=" + this.getLockedUntil() + ", marriedSince=" + this.getMarriedSince() + ", marriedWith=" + this.getMarriedWith() + ", moneyOnBank=" + this.getMoneyOnBank() + ", mainBadge=" + this.getMainBadge() + ", marketUsed=" + this.getMarketUsed() + ", showBadge=" + this.isShowBadge() + ", activePotion=" + this.getActivePotion() + ", activeBuff=" + this.getActiveBuff() + ", waifuCachedValue=" + this.getWaifuCachedValue() + ", profileComponents=" + this.getProfileComponents() + ", profilePets=" + this.getProfilePets() + ", petSlots=" + this.getPetSlots() + ", pets=" + this.getPets() + ")";
    }
}
