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

package net.kodehawa.mantarobot.commands.utils.leaderboards;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.beans.ConstructorProperties;
import java.util.concurrent.TimeUnit;

public class CachedLeaderboardMember {
    @JsonProperty("id")
    private final long id;
    @JsonProperty("name")
    private final String name;
    @JsonProperty("discriminator")
    private final String disriminator;
    @JsonProperty("lastCachedAt")
    private final long lastCachedAt;

    @JsonCreator
    @ConstructorProperties({"id", "name", "discriminator", "lastCachedAt"})
    public CachedLeaderboardMember(long id, String name, String discriminator, long lastCachedAt) {
        this.id = id;
        this.name = name;
        this.disriminator = discriminator;
        this.lastCachedAt = lastCachedAt;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDiscriminator() {
        return disriminator;
    }

    public long getLastCachedAt() {
        return lastCachedAt;
    }

    public long getLastCachedAtHours() {
        return TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - lastCachedAt);
    }

    @JsonIgnore
    public String getTag() {
        return getName() + getDiscriminator();
    }
}
