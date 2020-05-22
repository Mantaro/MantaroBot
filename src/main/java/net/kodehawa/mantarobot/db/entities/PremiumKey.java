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
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.PremiumKeyData;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.Pair;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PremiumKey implements ManagedObject {
    public static final String DB_TABLE = "keys";
    private long duration;
    private boolean enabled;
    private long expiration;
    private String id;
    private String owner;
    private int type;
    //Setting a default to avoid backwards compat issues.
    private PremiumKeyData data = new PremiumKeyData();

    @JsonCreator
    @ConstructorProperties({"id", "duration", "expiration", "type", "enabled", "owner"})
    public PremiumKey(@JsonProperty("id") String id, @JsonProperty("duration") long duration,
                      @JsonProperty("expiration") long expiration, @JsonProperty("type") Type type,
                      @JsonProperty("enabled") boolean enabled, @JsonProperty("owner") String owner, @JsonProperty("data") PremiumKeyData data) {
        this.id = id;
        this.duration = duration;
        this.expiration = expiration;
        this.type = type.ordinal();
        this.enabled = enabled;
        this.owner = owner;
        if (data != null)
            this.data = data;
    }

    @JsonIgnore
    public PremiumKey() {
    }

    @JsonIgnore
    public static PremiumKey generatePremiumKey(String owner, Type type, boolean linked) {
        String premiumId = UUID.randomUUID().toString();
        PremiumKey newKey = new PremiumKey(premiumId, -1, -1, type, false, owner, new PremiumKeyData());
        if (linked)
            newKey.data.setLinkedTo(owner); //used for patreon checks in newly-activated keys (if applicable)

        newKey.save();
        return newKey;
    }

    @JsonIgnore
    public Type getParsedType() {
        return Type.values()[type];
    }

    @JsonIgnore
    public long getDurationDays() {
        return TimeUnit.MILLISECONDS.toDays(duration);
    }

    @JsonIgnore
    public long validFor() {
        return TimeUnit.MILLISECONDS.toDays(getExpiration() - currentTimeMillis());
    }

    @JsonIgnore
    public long validForMs() {
        return getExpiration() - currentTimeMillis();
    }

    @JsonIgnore
    public void activate(int days) {
        this.enabled = true;
        this.duration = TimeUnit.DAYS.toMillis(days);
        this.expiration = currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
        save();
    }

    @JsonIgnore
    public boolean renew() {
        if (data.getLinkedTo() != null && !data.getLinkedTo().isEmpty()) {
            Pair<Boolean, String> pledgeInfo = APIUtils.getPledgeInformation(data.getLinkedTo());
            if (pledgeInfo != null && pledgeInfo.getLeft()) {
                switch (type) {
                    case 1: //user
                        this.activate(365);
                        break;
                    case 2: //server
                        this.activate(180);
                        break;
                    default:
                        this.activate(60);
                }

                return true;
            }
        }

        return false;
    }

    public long getDuration() {
        return this.duration;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public long getExpiration() {
        return this.expiration;
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

    public String getOwner() {
        return this.owner;
    }

    public int getType() {
        return this.type;
    }

    public PremiumKeyData getData() {
        return this.data;
    }

    public enum Type {
        MASTER, USER, GUILD
    }
}
