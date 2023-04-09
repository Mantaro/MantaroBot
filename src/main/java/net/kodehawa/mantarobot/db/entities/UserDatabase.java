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

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.item.PlayerEquipment;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedMongoObject;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.Pair;
import net.kodehawa.mantarobot.utils.Utils;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

public class UserDatabase implements ManagedMongoObject {
    @BsonIgnore
    public static final String DB_TABLE = "users";
    @BsonIgnore
    private final Config config = MantaroData.config().get();

    @BsonId
    private String id;
    private long premiumUntil;
    private String birthday;
    private boolean receivedFirstKey;
    private String premiumKey;
    private int remindedTimes;
    private String timezone;
    private String lang;
    private int dustLevel; //percentage
    private int equippedPick; //item id, 0 = nothing (even tho in theory 0 its headphones...)
    private int equippedRod; //item id, 0 = nothing
    private PlayerEquipment equippedItems = new PlayerEquipment(new HashMap<>(), new HashMap<>(), new HashMap<>()); //hashmap is type -> itemId
    private boolean receivedExpirationWarning = false; //premium key about to expire!
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
    private boolean autoEquip = false;
    private boolean actionsDisabled = false;

    // Mongo serialization
    public UserDatabase() { }
    
    protected UserDatabase(String id, long premiumUntil) {
        this.id = id;
        this.premiumUntil = premiumUntil;
    }

    public static UserDatabase of(String id) {
        return new UserDatabase(id, 0);
    }

    public UserDatabase incrementPremium(long milliseconds) {
        if (isPremium()) {
            this.premiumUntil += milliseconds;
        } else {
            this.premiumUntil = currentTimeMillis() + milliseconds;
        }
        return this;
    }

    public String getBirthday() {
        return this.birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getPremiumKey() {
        return this.premiumKey;
    }

    public void setPremiumKey(String premiumKey) {
        this.premiumKey = premiumKey;
    }

    public int getRemindedTimes() {
        return this.remindedTimes;
    }

    public void setRemindedTimes(int remindedTimes) {
        this.remindedTimes = remindedTimes;
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

    public boolean isAutoEquip() {
        return autoEquip;
    }

    public void setAutoEquip(boolean autoEquip) {
        this.autoEquip = autoEquip;
    }

    public boolean isActionsDisabled() {
        return actionsDisabled;
    }

    public void setActionsDisabled(boolean actionsDisabled) {
        this.actionsDisabled = actionsDisabled;
    }

    public void setReceivedFirstKey(boolean hasReceivedFirstKey) {
        this.receivedFirstKey = hasReceivedFirstKey;
    }

    public void setReceivedExpirationWarning(boolean receivedExpirationWarning) {
        this.receivedExpirationWarning = receivedExpirationWarning;
    }

    public boolean getReceivedFirstKey() {
        return this.receivedFirstKey;
    }

    public boolean getReceivedExpirationWarning() {
        return this.receivedExpirationWarning;
    }

    @BsonIgnore
    public User getUser(JDA jda) {
        return jda.retrieveUserById(getId()).complete();
    }

    @BsonIgnore
    public User getUser() {
        return MantaroBot.getInstance().getShardManager().retrieveUserById(getId()).complete();
    }

    @BsonIgnore
    public long getPremiumLeft() {
        return isPremium() ? this.premiumUntil - currentTimeMillis() : 0;
    }

    @BsonIgnore
    public Marriage getMarriage() {
        //we're going full round trip here
        return MantaroData.db().getMarriage(marriageId);
    }

    @BsonIgnore
    public int increaseDustLevel(int by) {
        int increased = dustLevel + Math.min(1, by);
        if (increased >= 100) {
            this.setDustLevel(100);
            return dustLevel; //same as before, cap at 100.
        }

        this.setDustLevel(increased);
        return this.dustLevel;
    }

    @BsonIgnore
    public boolean hasReceivedFirstKey() {
        return this.receivedFirstKey;
    }

    @BsonIgnore
    public void incrementReminders() {
        remindedTimes += 1;
    }

    @BsonIgnore
    public boolean hasReceivedExpirationWarning() {
        return this.receivedExpirationWarning;
    }

    @BsonIgnore
    //Slowly convert old key system to new key system (link old accounts).
    public boolean isPremium() {
        //Return true if this is running in MP, as all users are considered Premium on it.
        if (config.isPremiumBot())
            return true;

        PremiumKey key = MantaroData.db().getPremiumKey(getPremiumKey());
        boolean isActive = false;

        if (key != null) {
            //Check for this because there's no need to check if this key is active.
            boolean isKeyActive = currentTimeMillis() < key.getExpiration();
            if (!isKeyActive && LocalDate.now(ZoneId.of("America/Chicago")).getDayOfMonth() > 5) {
                UserDatabase owner = MantaroData.db().getUser(key.getOwner());
                //Remove from owner's key ownership storage if key owner != key holder.
                if (!key.getOwner().equals(getId())) {
                    owner.getKeysClaimed().remove(getId());
                    owner.save();
                }

                //Handle this so we don't go over this check again. Remove premium key from user object.
                removePremiumKey(key.getId());

                // Send a message if the user was premium but the key expired.
                // This has a 5-day leeway. This means it won't kill your key if the day of the month is before or the 5th of X month.
                // This is because Patreon can take up to the 5th to process pledges.
                if (key.getOwner().equals(getId())) {
                    MantaroBot.getInstance().getShardManager()
                            .retrieveUserById(key.getOwner())
                            .flatMap(User::openPrivateChannel)
                            .flatMap(privateChannel ->
                                    privateChannel.sendMessage("""
                                            Hello! Your key(s) seems to have expired, this usually only happens when your Patreon subscription is over (aka you cancelled it). If you didn't cancel your Patreon subscription, please check Patreon to see if your pledge went through.
                                            If you bought this key via PayPal, you can ignore this message.
                                            Thanks you for supporting Mantaro and I hope you have a good day! :heart:."""
                                    )
                            ).queue();

                }

                // Delete key.
                key.delete();

                // User is not premium.
                return false;
            }

            //Link key to owner if key == owner and key holder is on patreon.
            //Sadly gotta skip of holder isnt patron here bc there are some bought keys (paypal) which I can't convert without invalidating
            Pair<Boolean, String> pledgeInfo = APIUtils.getPledgeInformation(key.getOwner());
            if (pledgeInfo != null && pledgeInfo.left()) {
                key.setLinkedTo(key.getOwner());
                key.save(); //doesn't matter if it doesnt save immediately, will do later anyway (key is usually immutable in db)
            }

            //If the receipt is not the owner, account them to the keys the owner has claimed.
            //This has usage later when seeing how many keys can they take. The second/third check is kind of redundant, but necessary anyway to see if it works.
            String keyLinkedTo = key.getLinkedTo();
            if (!getId().equals(key.getOwner()) && keyLinkedTo != null && keyLinkedTo.equals(key.getOwner())) {
                UserDatabase owner = MantaroData.db().getUser(key.getOwner());
                if (!owner.getKeysClaimed().containsKey(getId())) {
                    owner.getKeysClaimed().put(getId(), key.getId());
                    owner.save();
                }
            }

            isActive = key.getLinkedTo() == null || (pledgeInfo != null ? pledgeInfo.left() : true); //default to true if no link
        }

        if (!isActive && key != null && LocalDate.now(ZoneId.of("America/Chicago")).getDayOfMonth() > 5) {
            //Handle this so we don't go over this check again. Remove premium key from user object.
            removePremiumKey(key.getId());
            key.delete();
        }

        return key != null && currentTimeMillis() < key.getExpiration() && key.getParsedType().equals(PremiumKey.Type.USER) && isActive;
    }

    @BsonIgnore
    public PremiumKey generateAndApplyPremiumKey(int days, String owner) {
        String premiumId = UUID.randomUUID().toString();
        PremiumKey newKey = new PremiumKey(premiumId, TimeUnit.DAYS.toMillis(days), currentTimeMillis() + TimeUnit.DAYS.toMillis(days), PremiumKey.Type.USER, true, owner, null);
        setPremiumKey(premiumId);
        newKey.save();
        save();
        return newKey;
    }

    @BsonIgnore
    public void removePremiumKey(String originalKey) {
        setPremiumKey(null);
        getKeysClaimed().remove(Utils.getKeyByValue(getKeysClaimed(), originalKey));
        setReceivedFirstKey(false);
        save();
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

    @Override
    public void save() {
        MantaroData.db().saveMongo(this, UserDatabase.class);
    }

    @Override
    public void delete() {
        MantaroData.db().deleteMongo(this, UserDatabase.class);
    }

    public long getPremiumUntil() {
        return this.premiumUntil;
    }

    public Config getConfig() {
        return this.config;
    }
}
