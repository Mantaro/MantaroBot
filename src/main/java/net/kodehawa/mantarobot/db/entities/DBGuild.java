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

package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.db.entities.helpers.PremiumKeyData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.Pair;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DBGuild implements ManagedObject {
    public static final String DB_TABLE = "guilds";
    private final GuildData data;
    private final String id;
    private long premiumUntil;

    @JsonIgnore
    private Config config = MantaroData.config().get();

    @JsonCreator
    @ConstructorProperties({"id", "premiumUntil", "data"})
    public DBGuild(@JsonProperty("id") String id, @JsonProperty("premiumUntil") long premiumUntil, @JsonProperty("data") GuildData data) {
        this.id = id;
        this.premiumUntil = premiumUntil;
        this.data = data;
    }

    public static DBGuild of(String id) {
        return new DBGuild(id, 0, new GuildData());
    }

    public static DBGuild of(String id, long premiumUntil) {
        return new DBGuild(id, premiumUntil, new GuildData());
    }

    public Guild getGuild(JDA jda) {
        return jda.getGuildById(getId());
    }

    @JsonIgnore
    public long getPremiumLeft() {
        return isPremium() ? this.premiumUntil - currentTimeMillis() : 0;
    }

    public void incrementPremium(long milliseconds) {
        if (isPremium()) {
            this.premiumUntil += milliseconds;
        } else {
            this.premiumUntil = currentTimeMillis() + milliseconds;
        }
    }

    @JsonIgnore
    public boolean isPremium() {
        PremiumKey key = MantaroData.db().getPremiumKey(data.getPremiumKey());
        //Key validation check (is it still active? delete otherwise)
        if (key != null) {
            boolean isKeyActive = currentTimeMillis() < key.getExpiration();
            if (!isKeyActive) {
                DBUser owner = MantaroData.db().getUser(key.getOwner());
                UserData ownerData = owner.getData();
                ownerData.getKeysClaimed().remove(getId());
                owner.save();

                key.delete();
                return false;
            }

            //Link key to owner if key == owner and key holder is on patreon.
            //Sadly gotta skip of holder isn't patron here bc there are some bought keys (paypal) which I can't convert without invalidating
            Pair<Boolean, String> pledgeInfo = APIUtils.getPledgeInformation(key.getOwner());
            if (pledgeInfo != null && pledgeInfo.getLeft()) {
                key.getData().setLinkedTo(key.getOwner());
                key.save(); //doesn't matter if it doesn't save immediately, will do later anyway (key is usually immutable in db)
            }

            //If the receipt is not the owner, account them to the keys the owner has claimed.
            //This has usage later when seeing how many keys can they take. The second/third check is kind of redundant, but necessary anyway to see if it works.
            String keyLinkedTo = key.getData().getLinkedTo();
            if (keyLinkedTo != null) {
                DBUser owner = MantaroData.db().getUser(keyLinkedTo);
                UserData ownerData = owner.getData();
                if (!ownerData.getKeysClaimed().containsKey(getId())) {
                    ownerData.getKeysClaimed().put(getId(), key.getId());
                    owner.save();
                }
            }
        }

        //Patreon bot link check.
        String linkedTo = getData().getMpLinkedTo();
        if (config.isPremiumBot() && linkedTo != null && key == null) { //Key should always be null in MP anyway.
            Pair<Boolean, String> pledgeInfo = APIUtils.getPledgeInformation(linkedTo);
            if (pledgeInfo != null && pledgeInfo.getLeft() && Double.parseDouble(pledgeInfo.getRight()) >= 4) {
                //Subscribed to MP properly, return true.
                return true;
            }
        }

        //MP uses the old premium system for some guilds: keep it here.
        return currentTimeMillis() < premiumUntil || (key != null && currentTimeMillis() < key.getExpiration() && key.getParsedType().equals(PremiumKey.Type.GUILD));
    }

    @JsonIgnore
    public PremiumKey generateAndApplyPremiumKey(int days) {
        String premiumId = UUID.randomUUID().toString();
        PremiumKey newKey = new PremiumKey(premiumId, TimeUnit.DAYS.toMillis(days),
                currentTimeMillis() + TimeUnit.DAYS.toMillis(days), PremiumKey.Type.GUILD, true, id, new PremiumKeyData());
        data.setPremiumKey(premiumId);
        newKey.saveAsync();
        saveAsync();
        return newKey;
    }

    @JsonIgnore
    public void removePremiumKey() {
        data.setPremiumKey(null);
        saveAsync();
    }

    public GuildData getData() {
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
