/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.core.shard.jda;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.requests.RestAction;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;

public class ShardedUser implements User {
    private final ShardedJDA jda;
    private final List<User> users;

    public ShardedUser(List<User> users, ShardedJDA jda) {
        this.users = new ArrayList<>(users);
        this.jda = jda;
    }

    @Override
    public String getAsMention() {
        return random(users).getAsMention();
    }

    @Override
    public String getId() {
        return random(users).getId();
    }

    @Override
    public long getIdLong() {
        return random(users).getIdLong();
    }

    @Override
    public OffsetDateTime getCreationTime() {
        return random(users).getCreationTime();
    }

    @Override
    public String getName() {
        return random(users).getName();
    }

    @Override
    public String getDiscriminator() {
        return random(users).getDiscriminator();
    }

    @Override
    public String getAvatarId() {
        return random(users).getAvatarId();
    }

    @Override
    public String getAvatarUrl() {
        return random(users).getAvatarUrl();
    }

    @Override
    public String getDefaultAvatarId() {
        return random(users).getDefaultAvatarId();
    }

    @Override
    public String getDefaultAvatarUrl() {
        return random(users).getDefaultAvatarUrl();
    }

    @Override
    public String getEffectiveAvatarUrl() {
        return random(users).getEffectiveAvatarUrl();
    }

    @Override
    public boolean hasPrivateChannel() {
        return random(users).isBot();
    }

    @Override
    public RestAction<PrivateChannel> openPrivateChannel() {
        return random(users).openPrivateChannel();
    }

    @Override
    public List<Guild> getMutualGuilds() {
        return random(users).getMutualGuilds();
    }

    @Override
    public boolean isBot() {
        return random(users).isBot();
    }

    @Override
    public JDA getJDA() {
        return jda;
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof User)) {
            return false;
        } else {
            User oUser = (User) o;
            return this == oUser || this.getId().equals(oUser.getId());
        }
    }

    @Override
    public String toString() {
        return "U:" + this.getName() + '(' + this.getId() + ')';
    }

    @Override
    public boolean isFake() {
        return random(users).isFake();
    }

    public int getShardsIn() {
        return users.size();
    }
}
