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
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.PotionEffect;
import net.kodehawa.mantarobot.commands.currency.pets.HousePet;
import net.kodehawa.mantarobot.commands.currency.pets.PetChoice;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.profile.ProfileComponent;
import net.kodehawa.mantarobot.commands.currency.profile.inventory.InventorySortType;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedMongoObject;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.db.entities.Inventory.serialize;

public class Player implements ManagedMongoObject {
    @BsonIgnore
    private static final Config config = MantaroData.config().get();
    @BsonIgnore
    public static final String DB_TABLE = "players";
    @BsonIgnore
    private final Inventory inventoryObject = new Inventory();
    @BsonIgnore
    public Map<String, Object> fieldTracker = new HashMap<>();

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
    //null = most important badge shows.
    private Badge mainBadge = null;
    private long marketUsed;
    private boolean showBadge = true;
    private PotionEffect activePotion;
    private PotionEffect activeBuff;
    private long waifuCachedValue;
    private boolean claimLocked = false;
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
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private Map<String, Integer> inventory = new HashMap<>();

    public Player() {}

    @SuppressWarnings("SameParameterValue")
    private Player(String id, Long level, Long oldMoney, Long reputation, Map<String, Integer> inventory) {
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
    @SuppressWarnings("unused")
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

    public boolean isClaimLocked() {
        return claimLocked;
    }

    public long getExperience() {
        return this.experience;
    }

    public List<Badge> getBadges() {
        return this.badges;
    }

    public long getDailyStreak() {
        return this.dailyStreak;
    }

    public String getDescription() {
        return this.description;
    }

    public long getGamesWon() {
        return this.gamesWon;
    }

    public long getLastDailyAt() {
        return this.lastDailyAt;
    }

    public long getLockedUntil() {
        return this.lockedUntil;
    }

    public Badge getMainBadge() {
        return this.mainBadge;
    }

    public long getMarketUsed() {
        return this.marketUsed;
    }

    public boolean isShowBadge() {
        return this.showBadge;
    }

    @SuppressWarnings("unused")
    public PotionEffect getActivePotion() {
        return this.activePotion;
    }

    @SuppressWarnings("unused")
    public PotionEffect getActiveBuff() {
        return this.activeBuff;
    }

    public long getWaifuCachedValue() {
        return this.waifuCachedValue;
    }

    public List<ProfileComponent> getProfileComponents() {
        return this.profileComponents;
    }

    @SuppressWarnings("unused")
    public long getPetSlots() {
        return this.petSlots;
    }

    public long getMiningExperience() {
        return miningExperience;
    }

    public long getFishingExperience() {
        return fishingExperience;
    }

    public long getTimesMopped() {
        return timesMopped;
    }

    public long getCratesOpened() {
        return cratesOpened;
    }

    public long getSharksCaught() {
        return sharksCaught;
    }

    public boolean isWaifuout() {
        return waifuout;
    }

    public int getLastCrateGiven() {
        return lastCrateGiven;
    }

    public long getChopExperience() {
        return chopExperience;
    }

    public Map<String, Integer> getInventory() {
        return serialize(inventoryObject.asList());
    }

    // -- Setters (protected if possible)
    @SuppressWarnings("unused")
    protected void setClaimLocked(boolean claimLocked) {
        this.claimLocked = claimLocked;
    }

    // Unused, only used for migration
    public void setExperience(long experience) {
        this.experience = experience;
    }

    // Unused, only used for migration
    public void setBadges(List<Badge> badges) {
        this.badges = badges;
    }

    // Unused, only used for migration
    public void setMiningExperience(long miningExperience) {
        this.miningExperience = miningExperience;
    }

    // Unused, only used for migration
    public void setFishingExperience(long fishingExperience) {
        this.fishingExperience = fishingExperience;
    }

    @SuppressWarnings("unused")
    protected void setDailyStreak(long dailyStreak) {
        this.dailyStreak = dailyStreak;
    }

    @SuppressWarnings("unused")
    protected void setDescription(String description) {
        this.description = description;
    }

    @SuppressWarnings("unused")
    protected void setGamesWon(long gamesWon) {
        this.gamesWon = gamesWon;
    }

    @SuppressWarnings("unused")
    protected void setLastDailyAt(long lastDailyAt) {
        this.lastDailyAt = lastDailyAt;
    }

    protected void setLockedUntil(long lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    @SuppressWarnings("unused")
    protected void setMainBadge(Badge mainBadge) {
        this.mainBadge = mainBadge;
    }

    @SuppressWarnings("unused")
    protected void setShowBadge(boolean showBadge) {
        this.showBadge = showBadge;
    }

    @SuppressWarnings("unused")
    protected void setMarketUsed(long marketUsed) {
        this.marketUsed = marketUsed;
    }

    @SuppressWarnings("unused")
    protected void setActivePotion(PotionEffect activePotion) {
        this.activePotion = activePotion;
    }

    @SuppressWarnings("unused")
    protected void setActiveBuff(PotionEffect activeBuff) {
        this.activeBuff = activeBuff;
    }

    @SuppressWarnings("unused")
    protected void setWaifuCachedValue(long waifuCachedValue) {
        this.waifuCachedValue = waifuCachedValue;
    }

    @SuppressWarnings("unused")
    protected void setProfileComponents(List<ProfileComponent> profileComponents) {
        this.profileComponents = profileComponents;
    }

    @SuppressWarnings("unused")
    protected void setPetSlots(long petSlots) {
        this.petSlots = petSlots;
    }

    @SuppressWarnings("unused")
    protected void setTimesMopped(long timesMopped) {
        this.timesMopped = timesMopped;
    }

    @SuppressWarnings("unused")
    protected void setCratesOpened(long cratesOpened) {
        this.cratesOpened = cratesOpened;
    }

    @SuppressWarnings("unused")
    protected void setSharksCaught(long sharksCaught) {
        this.sharksCaught = sharksCaught;
    }

    @SuppressWarnings("unused")
    protected void setWaifuout(boolean waifuout) {
        this.waifuout = waifuout;
    }

    @SuppressWarnings("unused")
    protected void setLastCrateGiven(int lastCrateGiven) {
        this.lastCrateGiven = lastCrateGiven;
    }

    protected void setNewMoney(long newMoney) {
        this.newMoney = newMoney;
    }

    @SuppressWarnings("unused")
    protected void setInventorySortType(InventorySortType inventorySortType) {
        this.inventorySortType = inventorySortType;
    }

    @SuppressWarnings("unused")
    protected void setHiddenLegacy(boolean hiddenLegacy) {
        this.hiddenLegacy = hiddenLegacy;
    }

    @SuppressWarnings("unused")
    protected void setNewPlayerNotice(boolean newPlayerNotice) {
        this.newPlayerNotice = newPlayerNotice;
    }

    protected void setOldMoney(long newAmount) {
        this.oldMoney = newAmount;
    }

    protected void setReputation(Long reputation) {
        this.reputation = reputation;
    }

    @SuppressWarnings("unused")
    protected void setLevel(long level) {
        this.level = level;
    }

    @SuppressWarnings("unused")
    protected void setChopExperience(long chopExperience) {
        this.chopExperience = chopExperience;
    }

    @SuppressWarnings("unused")
    protected void setLastSeenCampaign(long lastSeenCampaign) {
        this.lastSeenCampaign = lastSeenCampaign;
    }

    @SuppressWarnings("unused")
    protected void setPetChoice(PetChoice petChoice) {
        this.petChoice = petChoice;
    }

    @SuppressWarnings("unused")
    public void setInventory(Map<String, Integer> inventory) {
        this.inventory = inventory;
        this.inventoryObject.replaceWith(Inventory.unserialize(inventory));
    }

    // -- Tracking setters (always public)
    @BsonIgnore
    public void timesMopped(long timesMopped) {
        this.timesMopped = timesMopped;
        fieldTracker.put("timesMopped", this.timesMopped);
    }

    @BsonIgnore
    public void sharksCaught(long sharksCaught) {
        this.sharksCaught = sharksCaught;
        fieldTracker.put("sharksCaught", this.sharksCaught);
    }

    @BsonIgnore
    public void waifuout(boolean waifuout) {
        this.waifuout = waifuout;
        fieldTracker.put("waifuout", this.waifuout);
    }

    @BsonIgnore
    public void lastCrateGiven(int lastCrateGiven) {
        this.lastCrateGiven = lastCrateGiven;
        fieldTracker.put("lastCrateGiven", this.lastCrateGiven);
    }

    @BsonIgnore
    public void inventorySortType(InventorySortType inventorySortType) {
        this.inventorySortType = inventorySortType;
        fieldTracker.put("inventorySortType", this.inventorySortType);
    }

    @BsonIgnore
    public void hiddenLegacy(boolean hiddenLegacy) {
        this.hiddenLegacy = hiddenLegacy;
        fieldTracker.put("hiddenLegacy", this.hiddenLegacy);
    }

    @BsonIgnore
    public void newPlayerNotice(boolean newPlayerNotice) {
        this.newPlayerNotice = newPlayerNotice;
        fieldTracker.put("newPlayerNotice", this.newPlayerNotice);
    }

    @BsonIgnore
    public void reputation(Long reputation) {
        this.reputation = reputation;
        fieldTracker.put("reputation", this.reputation);
    }

    @BsonIgnore
    public void level(long level) {
        this.level = level;
        fieldTracker.put("level", this.level);
    }

    @BsonIgnore
    public void cratesOpened(long cratesOpened) {
        this.cratesOpened = cratesOpened;
        fieldTracker.put("cratesOpened", this.cratesOpened);
    }

    @BsonIgnore
    public void waifuCachedValue(long waifuCachedValue) {
        this.waifuCachedValue = waifuCachedValue;
        fieldTracker.put("waifuCachedValue", this.waifuCachedValue);
    }

    @BsonIgnore
    public void profileComponents(List<ProfileComponent> profileComponents) {
        this.profileComponents = profileComponents;
        fieldTracker.put("profileComponents", this.profileComponents);
    }

    @BsonIgnore
    public void showBadge(boolean showBadge) {
        this.showBadge = showBadge;
        fieldTracker.put("showBadge", this.showBadge);
    }

    @BsonIgnore
    public void mainBadge(Badge mainBadge) {
        this.mainBadge = mainBadge;
        fieldTracker.put("mainBadge", this.mainBadge);
    }

    @BsonIgnore
    public void gamesWon(long gamesWon) {
        this.gamesWon = gamesWon;
        fieldTracker.put("gamesWon", this.gamesWon);
    }

    @BsonIgnore
    public void petChoice(PetChoice petChoice) {
        this.petChoice = petChoice;
        fieldTracker.put("petChoice", this.petChoice);
    }

    @BsonIgnore
    public void lastDailyAt(long lastDailyAt) {
        this.lastDailyAt = lastDailyAt;
        fieldTracker.put("lastDailyAt", this.lastDailyAt);
    }

    @BsonIgnore
    public void marketUsed(long marketUsed) {
        this.marketUsed = marketUsed;
        fieldTracker.put("marketUsed", this.marketUsed);
    }

    @BsonIgnore
    public void description(String description) {
        this.description = description;
        fieldTracker.put("description", this.description);
    }

    @BsonIgnore
    public void dailyStreak(long dailyStreak) {
        this.dailyStreak = dailyStreak;
        fieldTracker.put("dailyStreak", this.dailyStreak);
    }

    @BsonIgnore
    public void claimLocked(boolean claimLocked) {
        this.claimLocked = claimLocked;
        fieldTracker.put("claimLocked", this.claimLocked);
    }

    // -- Helpers
    @BsonIgnore
    public void resetProfileComponents() {
        profileComponents.clear();
        fieldTracker.put("profileComponents", profileComponents);
    }

    @BsonIgnore
    public void incrementMiningExperience(Random random) {
        this.miningExperience = miningExperience + (random.nextInt(5) + 1);
        fieldTracker.put("miningExperience", miningExperience);
    }

    @BsonIgnore
    public void incrementFishingExperience(Random random) {
        this.fishingExperience = fishingExperience + (random.nextInt(5) + 1);
        fieldTracker.put("fishingExperience", fishingExperience);
    }

    @BsonIgnore
    public void incrementChopExperience(Random random) {
        this.chopExperience = chopExperience + (random.nextInt(5) + 1);
        fieldTracker.put("chopExperience", chopExperience);
    }

    @SuppressWarnings("unused")
    @BsonProperty("inventory")
    public Map<String, Integer> rawInventory() {
        return serialize(inventoryObject.asList());
    }

    public long getNewMoney() {
        return newMoney;
    }

    public long getLastSeenCampaign() {
        return lastSeenCampaign;
    }

    @SuppressWarnings("unused")
    public boolean isResetWarning() {
        return resetWarning;
    }

    @SuppressWarnings("unused")
    public void setResetWarning(boolean resetWarning) {
        this.resetWarning = resetWarning;
    }

    public InventorySortType getInventorySortType() {
        return inventorySortType;
    }

    public boolean isHiddenLegacy() {
        return hiddenLegacy;
    }

    public boolean isNewPlayerNotice() {
        return newPlayerNotice;
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

    public long getOldMoney() {
        return oldMoney;
    }

    public long getReputation() {
        return this.reputation;
    }

    public Long getLevel() {
        return this.level;
    }

    @BsonIgnore
    public int getItemAmount(Item item) {
        return inventoryObject.getAmount(item);
    }

    @BsonIgnore
    public boolean fitsItemAmount(Item item, int amount) {
        return getItemAmount(item) + amount <= ItemStack.MAX_STACK_SIZE;
    }

    @BsonIgnore
    public boolean canFitItem(Item item) {
        return fitsItemAmount(item, 1);
    }

    @BsonIgnore
    public boolean fitsItemStack(ItemStack it) {
        return fitsItemAmount(it.getItem(), it.getAmount());
    }

    @BsonIgnore
    public void processItem(Item item, int amount) {
        inventoryObject.process(new ItemStack(item, amount));
        fieldTracker.put("inventory", getInventory());
    }

    @BsonIgnore
    public void processItem(ItemStack stack) {
        inventoryObject.process(stack);
        fieldTracker.put("inventory", getInventory());
    }

    @BsonIgnore
    public void processItems(List<ItemStack> stack) {
        inventoryObject.process(stack);
        fieldTracker.put("inventory", getInventory());
    }

    @BsonIgnore
    public boolean mergeInventory(List<ItemStack> stack) {
        var merge = inventoryObject.merge(stack);
        fieldTracker.put("inventory", getInventory());
        return merge;
    }

    @BsonIgnore
    public boolean containsItem(Item item) {
        return inventoryObject.containsItem(item);
    }

    @BsonIgnore
    public List<ItemStack> getInventoryList() {
        return inventoryObject.asList();
    }

    @BsonIgnore
    public void markPetChange() {
        fieldTracker.put("pet", this.pet);
    }

    @BsonIgnore
    public boolean addBadgeIfAbsent(Badge b) {
        if (hasBadge(b)) {
            return false;
        }

        badges.add(b);
        fieldTracker.put("badges", this.badges);
        return true;
    }

    @BsonIgnore
    public boolean removeBadge(Badge b) {
        if (!hasBadge(b)) {
            return false;
        }

        badges.remove(b);
        fieldTracker.put("badges", this.badges);
        return true;
    }

    @BsonIgnore
    public PetChoice getActivePetChoice(Marriage marriage) {
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
            fieldTracker.put("oldMoney", money);
        } else {
            setNewMoney(money);
            fieldTracker.put("newMoney", money);
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
        fieldTracker.put("reputation", this.reputation);
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
            fieldTracker.put("oldMoney", money);
        } else {
            setNewMoney(money);
            fieldTracker.put("newMoney", money);
        }

        return true;
    }

    //it's 3am and i cba to replace usages of this so whatever
    @BsonIgnore
    public boolean isLocked() {
        return getLockedUntil() - System.currentTimeMillis() > 0;
    }

    @BsonIgnore
    public void locked(boolean locked) {
        setLockedUntil(locked ? System.currentTimeMillis() + 35000 : 0);
        fieldTracker.put("lockedUntil", lockedUntil);
    }

    @Override
    @Nonnull
    public String getId() {
        return this.id;
    }

    @SuppressWarnings("unused")
    @BsonIgnore
    @Override
    @Nonnull
    public String getTableName() {
        return DB_TABLE;
    }

    @SuppressWarnings("unused")
    @BsonIgnore
    @Nonnull
    @Override
    public String getDatabaseId() {
        return getId();
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
    public void currentMoney(long money) {
        boolean useOld = config.isPremiumBot() || config.isSelfHost();
        if (useOld) {
            setOldMoney(money < 0 ? 0 : money);
            fieldTracker.put("oldMoney", this.oldMoney);
        } else {
            setNewMoney(money < 0 ? 0 : money);
            fieldTracker.put("newMoney", this.newMoney);
        }
    }

    @BsonIgnore
    @Override
    public void updateAllChanged() {
        MantaroData.db().updateFieldValues(this, fieldTracker);
    }

    @Override
    public void insertOrReplace() {
        MantaroData.db().saveMongo(this, Player.class);
    }

    @SuppressWarnings("unused")
    @Override
    public void delete() {
        MantaroData.db().deleteMongo(this, Player.class);
    }

    @BsonIgnore
    public PlayerStats getStats() {
        return MantaroData.db().getPlayerStats(getId());
    }
}
