/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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

import net.dv8tion.jda.bot.JDABot;
import net.dv8tion.jda.client.JDAClient;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.Webhook;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.managers.Presence;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.restaction.GuildAction;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface UnifiedJDA extends JDA, Iterable<JDA> {
    JDA getShard(int shard);

    int getShardAmount();

    Status[] getShardStatus();

    @Override
    default Status getStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    default List<Object> getRegisteredListeners() {
        throw new UnsupportedOperationException();
    }

    @Override
    default RestAction<User> retrieveUserById(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    default SelfUser getSelfUser() {
        throw new UnsupportedOperationException();
    }

    @Override
    default Presence getPresence() {
        throw new UnsupportedOperationException();
    }

    @Override
    default ShardInfo getShardInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    default String getToken() {
        throw new UnsupportedOperationException();
    }

    @Override
    default int getMaxReconnectDelay() {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean isAutoReconnect() {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean isAudioEnabled() {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean isBulkDeleteSplittingEnabled() {
        throw new UnsupportedOperationException();
    }

    @Override
    default AccountType getAccountType() {
        throw new UnsupportedOperationException();
    }

    @Override
    default JDAClient asClient() {
        throw new UnsupportedOperationException();
    }

    @Override
    default JDABot asBot() {
        throw new UnsupportedOperationException();
    }

    @Override
    default void setRequestTimeoutRetry(boolean retry) {
        throw new UnsupportedOperationException();
    }

    @Override
    default GuildAction createGuild(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    default RestAction<Webhook> getWebhookById(String webhook) {
        throw new UnsupportedOperationException();
    }

    @Override
    default JDA awaitStatus(Status status) {
        throw new UnsupportedOperationException();
    }

    @Override
    default JDA awaitReady() {
        throw new UnsupportedOperationException();
    }

    @Override
    default List<AudioManager> getAudioManagers() {
        throw new UnsupportedOperationException();
    }


    default Stream<JDA> stream() {
        return StreamSupport.stream(spliterator(), false).filter(Objects::nonNull).sorted(Comparator.comparingInt(jda -> jda.getShardInfo().getShardId()));
    }
}
