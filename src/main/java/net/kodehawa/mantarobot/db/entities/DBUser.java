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

package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.PremiumKeyData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.Pair;
import net.kodehawa.mantarobot.utils.Utils;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

public class DBUser implements ManagedObject {
    public static final String DB_TABLE = "users";
    private final UserData data;
    private final String id;
    private long premiumUntil;
    
    @JsonIgnore
    private Config config = MantaroData.config().get();
    
    @JsonCreator
    @ConstructorProperties({"id", "premiumUntil", "data"})
    public DBUser(@JsonProperty("id") String id, @JsonProperty("premiumUntil") long premiumUntil, @JsonProperty("data") UserData data) {
        this.id = id;
        this.premiumUntil = premiumUntil;
        this.data = data;
    }
    
    public static DBUser of(String id) {
        return new DBUser(id, 0, new UserData());
    }
    
    @JsonIgnore
    public User getUser(JDA jda) {
        return jda.getUserById(getId());
    }
    
    @JsonIgnore
    public User getUser() {
        return MantaroBot.getInstance().getShardManager().getUserById(getId());
    }
    
    @JsonIgnore
    public long getPremiumLeft() {
        return isPremium() ? this.premiumUntil - currentTimeMillis() : 0;
    }
    
    public DBUser incrementPremium(long milliseconds) {
        if(isPremium()) {
            this.premiumUntil += milliseconds;
        } else {
            this.premiumUntil = currentTimeMillis() + milliseconds;
        }
        return this;
    }
    
    @JsonIgnore
    //Slowly convert old key system to new key system (link old accounts).
    public boolean isPremium() {
        //Return true if this is running in MP, as all users are considered Premium on it.
        if(config.isPremiumBot())
            return true;
        
        PremiumKey key = MantaroData.db().getPremiumKey(data.getPremiumKey());
        boolean isActive = false;
        
        if(key != null) {
            //Check for this because there's no need to check if this key is active.
            boolean isKeyActive = currentTimeMillis() < key.getExpiration();
            if(!isKeyActive) {
                DBUser owner = MantaroData.db().getUser(key.getOwner());
                UserData ownerData = owner.getData();
                
                //Remove from owner's key ownership storage if key owner != key holder.
                if(!key.getOwner().equals(getId())) {
                    ownerData.getKeysClaimed().remove(getId());
                    owner.save();
                }
                
                //Handle this so we don't go over this check again. Remove premium key from user object.
                key.delete();
                removePremiumKey();
                
                //User is not premium.
                return false;
            }
            
            //Link key to owner if key == owner and key holder is on patreon.
            //Sadly gotta skip of holder isnt patron here bc there are some bought keys (paypal) which I can't convert without invalidating
            Pair<Boolean, String> pledgeInfo = Utils.getPledgeInformation(key.getOwner());
            if(pledgeInfo != null && pledgeInfo.getLeft()) {
                key.getData().setLinkedTo(key.getOwner());
                key.save(); //doesn't matter if it doesnt save immediately, will do later anyway (key is usually immutable in db)
            }
            
            //If the receipt is not the owner, account them to the keys the owner has claimed.
            //This has usage later when seeing how many keys can they take. The second/third check is kind of redundant, but necessary anyway to see if it works.
            String keyLinkedTo = key.getData().getLinkedTo();
            if(!getId().equals(key.getOwner()) && keyLinkedTo != null && keyLinkedTo.equals(key.getOwner())) {
                DBUser owner = MantaroData.db().getUser(key.getOwner());
                UserData ownerData = owner.getData();
                if(!ownerData.getKeysClaimed().containsKey(getId())) {
                    ownerData.getKeysClaimed().put(getId(), key.getId());
                    owner.save();
                }
            }
            
            isActive = key.getData().getLinkedTo() == null || (pledgeInfo != null ? pledgeInfo.getLeft() : true); //default to true if no link
        }
        
        if(!isActive && key != null) {
            //Handle this so we don't go over this check again. Remove premium key from user object.
            key.delete();
            removePremiumKey();
        }
        
        //TODO remove old system check.
        return  //old system, deprecated, maybe remove later?
                currentTimeMillis() < premiumUntil ||
                        //Key parsing
                        (key != null && currentTimeMillis() < key.getExpiration() && key.getParsedType().equals(PremiumKey.Type.USER) && isActive);
    }
    
    @JsonIgnore
    public PremiumKey generateAndApplyPremiumKey(int days, String owner) {
        String premiumId = UUID.randomUUID().toString();
        PremiumKey newKey = new PremiumKey(premiumId, TimeUnit.DAYS.toMillis(days), currentTimeMillis() + TimeUnit.DAYS.toMillis(days), PremiumKey.Type.USER, true, owner, new PremiumKeyData());
        data.setPremiumKey(premiumId);
        newKey.saveAsync();
        save();
        return newKey;
    }
    
    @JsonIgnore
    public void removePremiumKey() {
        data.setPremiumKey(null);
        data.setHasReceivedFirstKey(false);
        save();
    }
    
    public UserData getData() {
        return this.data;
    }
    
    public String getId() {
        return this.id;
    }
    
    @JsonIgnore
    @Override
    @Nonnull
    public String getTableName() {
        return DB_TABLE;
    }
    
    public long getPremiumUntil() {
        return this.premiumUntil;
    }
    
    public Config getConfig() {
        return this.config;
    }
}
