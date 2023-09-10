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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedMongoObject;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.Pair;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PremiumKey implements ManagedMongoObject {
    @BsonIgnore
    public static final String DB_TABLE = "keys";

    @BsonId
    private String id;
    private long duration;
    private boolean enabled;
    private long expiration;
    private String owner;
    private int type;
    private String linkedTo;

    // Serialization constructor
    @SuppressWarnings("unused")
    public PremiumKey() {}

    public PremiumKey(String id, long duration, long expiration, Type type, boolean enabled, String owner, String linkedTo) {
        this.id = id;
        this.duration = duration;
        this.expiration = expiration;
        this.type = type.ordinal();
        this.enabled = enabled;
        this.owner = owner;
        this.linkedTo = linkedTo;
    }

    @BsonIgnore
    public static PremiumKey generatePremiumKey(String owner, Type type, boolean linked) {
        String premiumId = UUID.randomUUID().toString();
        PremiumKey newKey = new PremiumKey(premiumId, -1, -1, type, false, owner, null);
        if (linked)
            newKey.setLinkedTo(owner); //used for patreon checks in newly-activated keys (if applicable)

        newKey.insertOrReplace();
        return newKey;
    }

    @SuppressWarnings("unused")
    @BsonIgnore
    public static PremiumKey generatePremiumKeyTimed(String owner, Type type, int days, boolean linked) {
        String premiumId = UUID.randomUUID().toString();
        PremiumKey newKey = new PremiumKey(premiumId, TimeUnit.DAYS.toMillis(days), currentTimeMillis() + TimeUnit.DAYS.toMillis(days), type, false, owner, null);
        if (linked)
            newKey.setLinkedTo(owner); //used for patreon checks in newly-activated keys (if applicable)

        newKey.insertOrReplace();
        return newKey;
    }

    @BsonIgnore
    public Type getParsedType() {
        return Type.values()[type];
    }

    @BsonIgnore
    public long getDurationDays() {
        return TimeUnit.MILLISECONDS.toDays(duration);
    }

    @BsonIgnore
    public long validFor() {
        return TimeUnit.MILLISECONDS.toDays(getExpiration() - currentTimeMillis());
    }

    @SuppressWarnings("unused")
    @BsonIgnore
    public long validForMs() {
        return getExpiration() - currentTimeMillis();
    }

    @BsonIgnore
    public void activate(int days) {
        this.enabled = true;
        this.duration = TimeUnit.DAYS.toMillis(days);
        this.expiration = currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
        insertOrReplace();
    }

    @BsonIgnore
    public boolean renew() {
        if (getLinkedTo() != null && !getLinkedTo().isEmpty()) {
            Pair<Boolean, String> pledgeInfo = APIUtils.getPledgeInformation(getLinkedTo());
            if (pledgeInfo != null && pledgeInfo.left()) {
                switch (type) {
                    //user
                    case 1 -> this.activate(365);
                    //server
                    case 2 -> this.activate(180);
                    default -> this.activate(60);
                }

                return true;
            }
        }

        return false;
    }

    public String getLinkedTo() {
        return this.linkedTo;
    }

    public void setLinkedTo(String linkedTo) {
        this.linkedTo = linkedTo;
    }

    @SuppressWarnings("unused")
    public long getDuration() {
        return this.duration;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public long getExpiration() {
        return this.expiration;
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

    public String getOwner() {
        return this.owner;
    }

    @SuppressWarnings("unused")
    public int getType() {
        return this.type;
    }

    public enum Type {
        MASTER, USER, GUILD
    }

    @Override
    public void insertOrReplace() {
        MantaroData.db().saveMongo(this, PremiumKey.class);
    }

    @Override
    public void delete() {
        MantaroData.db().deleteMongo(this, PremiumKey.class);
    }
}
