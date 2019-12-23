/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
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
 *
 */

package net.kodehawa.mantarobot.core.shard.jda;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.StoreChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.managers.DirectAudioController;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.GuildAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import okhttp3.OkHttpClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface UnifiedJDA extends JDA, Iterable<JDA> {
    JDA getShard(int shard);
    
    int getShardAmount();
    
    Status[] getShardStatus();
    
    @Nonnull
    @Override
    default Status getStatus() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default JDA awaitStatus(@Nonnull Status status) {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default JDA awaitReady() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default ScheduledExecutorService getRateLimitPool() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default ScheduledExecutorService getGatewayPool() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default ExecutorService getCallbackPool() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default OkHttpClient getHttpClient() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default DirectAudioController getDirectAudioController() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default List<Object> getRegisteredListeners() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default GuildAction createGuild(@Nonnull String name) {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default List<AudioManager> getAudioManagers() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default RestAction<User> retrieveUserById(@Nonnull String id) {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default SnowflakeCacheView<StoreChannel> getStoreChannelCache() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default IEventManager getEventManager() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default SelfUser getSelfUser() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default Presence getPresence() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default ShardInfo getShardInfo() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default String getToken() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    default int getMaxReconnectDelay() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    default void setRequestTimeoutRetry(boolean retry) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    default boolean isAutoReconnect() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    default boolean isBulkDeleteSplittingEnabled() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default AccountType getAccountType() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default RestAction<ApplicationInfo> retrieveApplicationInfo() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default String getInviteUrl(@Nullable Permission... permissions) {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default String getInviteUrl(@Nullable Collection<Permission> permissions) {
        throw new UnsupportedOperationException();
    }
    
    @Nullable
    @Override
    //We're the shard manager...
    default ShardManager getShardManager() {
        throw new UnsupportedOperationException();
    }
    
    @Nonnull
    @Override
    default RestAction<Webhook> retrieveWebhookById(@Nonnull String webhookId) {
        throw new UnsupportedOperationException();
    }
    
    default Stream<JDA> stream() {
        return StreamSupport.stream(spliterator(), false).filter(Objects::nonNull).sorted(Comparator.comparingInt(jda -> jda.getShardInfo().getShardId()));
    }
}
