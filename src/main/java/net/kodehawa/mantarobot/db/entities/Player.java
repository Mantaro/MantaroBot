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
import net.kodehawa.mantarobot.commands.currency.item.PotionEffect;
import net.kodehawa.mantarobot.commands.currency.pets.HousePet;
import net.kodehawa.mantarobot.commands.currency.pets.PetChoice;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.profile.ProfileComponent;
import net.kodehawa.mantarobot.commands.currency.profile.inventory.InventorySortType;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedMongoObject;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.serialize;

public class Player implements ManagedMongoObject {
    @BsonIgnore
    private static final Config config = MantaroData.config().get();
    @BsonIgnore
    public static final String DB_TABLE = "players";
    @BsonIgnore
    private final transient Inventory inventoryObject = new Inventory(new HashMap<>());

    @BsonId
    private String id;
    private long level;
    private long oldMoney;
    private long reputation;
    private long experience = 0;
    private long newMoney = 0L;
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
    private boolean resetWarning = false;
    private InventorySortType inventorySortType = InventorySortType.AMOUNT;
    private boolean hiddenLegacy = false;
    private boolean newPlayerNotice = false;
    private long petSlots = 4;
    private PetChoice petChoice = null;
    private HousePet pet;
    private List<Badge> badges = new ArrayList<>();
    private List<ProfileComponent> profileComponents = new LinkedList<>();
    private Map<Integer, Integer> inventory = new HashMap<>();

    public Player() {}

    private Player(String id, Long level, Long oldMoney, Long reputation, Map<Integer, Integer> inventory) {
        this.id = id;
        this.level = level == null ? 0 : level;
        this.oldMoney = oldMoney == null ? 0 : oldMoney;
        this.reputation = reputation == null ? 0 : reputation;
        this.inventoryObject.replaceWith(Inventory.unserialize(inventory));
    }

    /**
     * The Player.of methods are for resetting players or creating new ones when they don't exist.
     *
     * @param user The user to create or reset.
     * @return The new Player.
     */
    public static Player of(User user) {
        return of(user.getId());
    }

    /**
     * The Player.of methods are for resetting players or creating new ones when they don't exist.
     *
     * @param member The user to create or reset.
     * @return The new Player.
     */
    public static Player of(Member member) {
        return of(member.getUser());
    }

    /**
     * The Player.of methods are for resetting players or creating new ones when they don't exist.
     *
     * @param userId The user to create or reset.
     * @return The new Player.
     */
    public static Player of(String userId) {
        return new Player(userId, 0L, 0L, 0L, new HashMap<>());
    }

    @BsonIgnore
    public boolean hasBadge(Badge b) {
        return badges.contains(b);
    }

    @BsonIgnore
    public boolean addBadgeIfAbsent(Badge b) {
        if (hasBadge(b)) {
            return false;
        }

        badges.add(b);
        return true;
    }

    @BsonIgnore
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

    public long getPetSlots() {
        return this.petSlots;
    }

    public void setPetSlots(long petSlots) {
        this.petSlots = petSlots;
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

    public void setInventory(Map<Integer, Integer> inventory) {
        this.inventory = inventory;
        this.inventoryObject.replaceWith(Inventory.unserialize(inventory));
        System.out.println("called set");
    }

    public Map<Integer, Integer> getInventory() {
        System.out.println("called get");
        return serialize(inventoryObject.asList());
    }

    @BsonIgnore
    public void incrementMiningExperience(Random random) {
        this.miningExperience = miningExperience + (random.nextInt(5) + 1);
    }

    @BsonIgnore
    public void incrementFishingExperience(Random random) {
        this.fishingExperience = fishingExperience + (random.nextInt(5) + 1);
    }

    @BsonIgnore
    public void incrementChopExperience(Random random) {
        this.chopExperience = chopExperience + (random.nextInt(5) + 1);
    }

    @BsonProperty("inventory")
    public Map<Integer, Integer> rawInventory() {
        return serialize(inventoryObject.asList());
    }

    @BsonIgnore
    public Inventory inventory() {
        return inventoryObject;
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

    public boolean isNewPlayerNotice() {
        return newPlayerNotice;
    }

    public void setNewPlayerNotice(boolean newPlayerNotice) {
        this.newPlayerNotice = newPlayerNotice;
    }

    public void setPet(HousePet pet) {
        this.pet = pet;
    }

    public HousePet getPet() {
        return pet;
    }

    public PetChoice getPetChoice() {
        return petChoice;
    }

    public void setPetChoice(PetChoice petChoice) {
        this.petChoice = petChoice;
    }

    public long getOldMoney() {
        return oldMoney;
    }

    public void setOldMoney(long newAmount) {
        this.oldMoney = newAmount;
    }

    public long getReputation() {
        return this.reputation;
    }

    public void setReputation(Long reputation) {
        this.reputation = reputation;
    }

    @BsonIgnore
    public PetChoice getActiveChoice(Marriage marriage) {
        if (getPetChoice() == null) {
            if (marriage == null || marriage.getPet() == null) {
                return PetChoice.PERSONAL;
            } else {
                return PetChoice.MARRIAGE;
            }
        } else {
            return getPetChoice();
        }
    }

    @BsonIgnore
    public boolean shouldSeeCampaign() {
        if (config.isPremiumBot())
            return false;

        return System.currentTimeMillis() > (getLastSeenCampaign() + TimeUnit.HOURS.toMillis(12));
    }

    @BsonIgnore
    public void markCampaignAsSeen() {
        this.lastSeenCampaign = System.currentTimeMillis();
    }

    /**
     * Adds x amount of money from the player.
     *
     * @param toAdd How much?
     * @return pls dont overflow.
     */
    @BsonIgnore
    public boolean addMoney(long toAdd) {
        boolean useOld = config.isPremiumBot() || config.isSelfHost();
        long money = useOld ? this.oldMoney : newMoney;
        if (toAdd < 0)
            return false;

        money = Math.addExact(money, toAdd);

        if (useOld) {
            setOldMoney(money);
        } else {
            setNewMoney(money);
        }

        return true;
    }

    /**
     * Adds x amount of reputation to a player. Normally 1.
     *
     * @param rep how much?
     */
    @BsonIgnore
    public void addReputation(long rep) {
        this.reputation += rep;
        this.setReputation(reputation);
    }

    /**
     * Removes x amount of money from the player. Only goes though if money removed sums more than zero (avoids negative values).
     *
     * @param toRemove How much?
     */
    public boolean removeMoney(long toRemove) {
        boolean useOld = config.isPremiumBot() || config.isSelfHost();
        long money = useOld ? this.oldMoney : newMoney;
        if (money - toRemove < 0) {
            return false;
        }

        money -= toRemove;

        if (useOld) {
            setOldMoney(money);
        } else {
            setNewMoney(money);
        }

        return true;
    }

    //it's 3am and i cba to replace usages of this so whatever
    @BsonIgnore
    public boolean isLocked() {
        return getLockedUntil() - System.currentTimeMillis() > 0;
    }

    @BsonIgnore
    public void setLocked(boolean locked) {
        setLockedUntil(locked ? System.currentTimeMillis() + 35000 : 0);
    }

    @Nonnull
    public String getId() {
        return this.id;
    }

    @BsonIgnore
    @Override
    @Nonnull
    public String getTableName() {
        return DB_TABLE;
    }

    @BsonIgnore
    @Nonnull
    @Override
    public String getDatabaseId() {
        return getId();
    }

    @Override
    public void save() {
        MantaroData.db().saveMongo(this, Player.class);
    }

    @Override
    public void delete() {
        MantaroData.db().deleteMongo(this, Player.class);
    }

    public Long getLevel() {
        return this.level;
    }

    public void setLevel(long level) {
        this.level = level;
    }

    @BsonIgnore
    public Long getCurrentMoney() {
        boolean useOld = config.isPremiumBot() || config.isSelfHost();
        if (useOld) {
            return oldMoney;
        } else {
            return newMoney;
        }
    }

    @BsonIgnore
    public void setCurrentMoney(long money) {
        boolean useOld = config.isPremiumBot() || config.isSelfHost();
        if (useOld) {
            setOldMoney(money < 0 ? 0 : money);
        } else {
            setNewMoney(money < 0 ? 0 : money);
        }
    }

    @BsonIgnore
    public PlayerStats getStats() {
        return MantaroData.db().getPlayerStats(getId());
    }
}
