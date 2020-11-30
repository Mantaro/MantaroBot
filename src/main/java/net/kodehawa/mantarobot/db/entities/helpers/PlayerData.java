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

package net.kodehawa.mantarobot.db.entities.helpers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kodehawa.mantarobot.commands.currency.item.PotionEffect;
import net.kodehawa.mantarobot.commands.currency.pets.global.Pet;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.profile.ProfileComponent;
import net.kodehawa.mantarobot.commands.currency.profile.inventory.InventorySortType;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.helpers.quests.Quest;
import net.kodehawa.mantarobot.db.entities.helpers.quests.QuestTracker;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerData {
    @JsonIgnore
    private static final Config config = MantaroData.config().get();

    public long experience = 0;
    public long newMoney = 0L;
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
    private boolean isClaimLocked = false;
    private long miningExperience;
    private long fishingExperience;
    private long chopExperience;
    private long timesMopped;
    private long cratesOpened;
    private long sharksCaught;
    private boolean waifuout;
    private int lastCrateGiven = 69;
    private long lastSeenCampaign;
    private QuestTracker quests = new QuestTracker();
    private int questQuota = 3;
    private boolean resetWarning = false;
    private InventorySortType inventorySortType = InventorySortType.AMOUNT;
    private boolean hiddenLegacy = false;

    //lol?
    //this is needed so it actually works, even though it does absolutely nothing
    //thanks rethonkdb
    private List<Pet> profilePets = new LinkedList<>();

    private long petSlots = 4;
    private Map<String, Pet> pets = new HashMap<>();

    public PlayerData() { }

    @JsonIgnore
    public boolean hasBadge(Badge b) {
        return badges.contains(b);
    }

    @JsonIgnore
    public boolean addBadgeIfAbsent(Badge b) {
        if (hasBadge(b)) {
            return false;
        }

        badges.add(b);
        return true;
    }

    @JsonIgnore
    public boolean removeBadge(Badge b) {
        if (!hasBadge(b)) {
            return false;
        }

        badges.remove(b);
        return true;
    }

    public boolean isClaimLocked() {
        return isClaimLocked;
    }

    public void setClaimLocked(boolean claimLocked) {
        isClaimLocked = claimLocked;
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

    public long getMiningExperience() {
        return miningExperience;
    }

    public void setMiningExperience(long miningExperience) {
        this.miningExperience = miningExperience;
    }

    public long getFishingExperience() {
        return fishingExperience;
    }

    public void setFishingExperience(long fishingExperience) {
        this.fishingExperience = fishingExperience;
    }

    public long getTimesMopped() {
        return timesMopped;
    }

    public void setTimesMopped(long timesMopped) {
        this.timesMopped = timesMopped;
    }

    public long getCratesOpened() {
        return cratesOpened;
    }

    public void setCratesOpened(long cratesOpened) {
        this.cratesOpened = cratesOpened;
    }

    public long getSharksCaught() {
        return sharksCaught;
    }

    public void setSharksCaught(long sharksCaught) {
        this.sharksCaught = sharksCaught;
    }

    public boolean isWaifuout() {
        return waifuout;
    }

    public void setWaifuout(boolean waifuout) {
        this.waifuout = waifuout;
    }

    public int getLastCrateGiven() {
        return lastCrateGiven;
    }

    public void setLastCrateGiven(int lastCrateGiven) {
        this.lastCrateGiven = lastCrateGiven;
    }

    public long getChopExperience() {
        return chopExperience;
    }

    public void setChopExperience(long chopExperience) {
        this.chopExperience = chopExperience;
    }

    public QuestTracker getQuests() {
        return quests;
    }

    public void setQuests(QuestTracker quests) {
        this.quests = quests;
    }

    public int getQuestQuota() {
        return questQuota;
    }

    public void setQuestQuota(int questQuota) {
        this.questQuota = questQuota;
    }

    @JsonIgnore
    public void incrementMiningExperience(Random random) {
        this.miningExperience = miningExperience + (random.nextInt(5) + 1);
    }

    @JsonIgnore
    public void incrementFishingExperience(Random random) {
        this.fishingExperience = fishingExperience + (random.nextInt(5) + 1);
    }

    @JsonIgnore
    public void incrementChopExperience(Random random) {
        this.chopExperience = chopExperience + (random.nextInt(5) + 1);
    }

    public long getNewMoney() {
        return newMoney;
    }

    public void setNewMoney(long newMoney) {
        this.newMoney = newMoney;
    }

    public long getLastSeenCampaign() {
        return lastSeenCampaign;
    }

    public void setLastSeenCampaign(long lastSeenCampaign) {
        this.lastSeenCampaign = lastSeenCampaign;
    }

    public boolean isResetWarning() {
        return resetWarning;
    }

    public void setResetWarning(boolean resetWarning) {
        this.resetWarning = resetWarning;
    }

    public InventorySortType getInventorySortType() {
        return inventorySortType;
    }

    public void setInventorySortType(InventorySortType inventorySortType) {
        this.inventorySortType = inventorySortType;
    }

    public void setHiddenLegacy(boolean hiddenLegacy) {
        this.hiddenLegacy = hiddenLegacy;
    }

    public boolean isHiddenLegacy() {
        return hiddenLegacy;
    }

    @JsonIgnore
    public boolean shouldSeeCampaign() {
        if (config.isPremiumBot())
            return false;

        return System.currentTimeMillis() > (getLastSeenCampaign() + TimeUnit.DAYS.toMillis(1));
    }

    @JsonIgnore
    public void markCampaignAsSeen() {
        this.lastSeenCampaign = System.currentTimeMillis();
    }

    @JsonIgnore
    public Quest startQuest(SecureRandom random) {
        if (quests.getCurrentActiveQuests().size() > getQuestQuota()) {
            return null;
        }

        return quests.startRandomQuest(random);
    }
}
