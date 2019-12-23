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

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.cache.CacheView;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.utils.cache.CacheView.allSnowflakes;

public abstract class ShardedJDA implements UnifiedJDA {

    @Override
    public long getGatewayPing() {
        return ((long) stream().mapToLong(JDA::getGatewayPing).average().orElseThrow(() -> new IllegalStateException("no JDA instances")));
    }
    
    @Nonnull
    @Override
    public JDA awaitStatus(@Nonnull Status status, @Nonnull Status... failOn) {
        throw new UnsupportedOperationException();
    }
    
    public long[] getPings() {
        return stream().mapToLong(JDA::getGatewayPing).toArray();
    }

    @Override
    public void setEventManager(IEventManager manager) {
        forEach(jda -> jda.setEventManager(manager));
    }

    @Override
    public void addEventListener(@Nonnull Object... listeners) {
        forEach(jda -> jda.addEventListener(listeners));
    }

    @Override
    public void removeEventListener(@Nonnull Object... listeners) {
        forEach(jda -> jda.removeEventListener(listeners));
    }

    @Override
    @Nonnull
    public List<User> getUsers() {
        return stream().flatMap(j -> j.getUsers().stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public User getUserById(@Nonnull String id) {
        return stream().map(jda -> jda.getUserById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public User getUserById(long id) {
        return stream().map(jda -> jda.getUserById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    @Nonnull
    public List<Guild> getMutualGuilds(@Nonnull User... users) {
        return stream().flatMap(jda -> jda.getMutualGuilds(users).stream()).collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public List<Guild> getMutualGuilds(@Nonnull Collection<User> users) {
        return stream().flatMap(jda -> jda.getMutualGuilds(users).stream()).collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public List<User> getUsersByName(@Nonnull String name, boolean ignoreCase) {
        return stream().flatMap(jda -> jda.getUsersByName(name, ignoreCase).stream()).distinct().collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public RestAction<User> retrieveUserById(long id) {
        return stream().map(jda -> jda.retrieveUserById(id)).findFirst().orElseThrow();
    }

    @Override
    @Nonnull
    public List<Guild> getGuilds() {
        return stream().map(JDA::getGuilds).flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public Guild getGuildById(@NotNull String id) {
        return MantaroBot.getInstance().getShard((int) (Long.parseLong(id) >> 22 % MantaroBot.getInstance().getShardList().size())).getGuildById(id);
    }

    @Override
    public Guild getGuildById(long id) {
        return MantaroBot.getInstance().getShard((int) (id >> 22 % MantaroBot.getInstance().getShardList().size())).getGuildById(id);
    }

    @Override
    @Nonnull
    public List<Guild> getGuildsByName(@Nonnull String name, boolean ignoreCase) {
        return stream().flatMap(jda -> jda.getGuildsByName(name, ignoreCase).stream()).collect(Collectors.toList());
    }
    
    @Nonnull
    @Override
    public Set<String> getUnavailableGuilds() {
        return stream().flatMap(j -> j.getUnavailableGuilds().stream()).collect(Collectors.toSet());
    }
    
    @Override
    @Nonnull
    public List<Role> getRoles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Role getRoleById(@Nonnull String id) {
        return stream().map(jda -> jda.getRoleById(id)).filter(Objects::nonNull).findFirst().orElseThrow();
    }

    @Override
    public Role getRoleById(long id) {
        return stream().map(jda -> jda.getRoleById(id)).filter(Objects::nonNull).findFirst().orElseThrow();
    }

    @Override
    @Nonnull
    public List<Role> getRolesByName(@Nonnull String name, boolean ignoreCase) {
        return stream().map(jda -> jda.getRolesByName(name, ignoreCase)).findFirst().orElseThrow();
    }

    @Override
    @Nonnull
    public List<TextChannel> getTextChannels() {
        return stream().map(JDA::getTextChannels).flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public TextChannel getTextChannelById(@Nonnull String id) {
        return stream().map(jda -> jda.getTextChannelById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public TextChannel getTextChannelById(long id) {
        return stream().map(jda -> jda.getTextChannelById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    @Nonnull
    public List<TextChannel> getTextChannelsByName(@Nonnull String name, boolean ignoreCase) {
        return stream().flatMap(jda -> jda.getTextChannelsByName(name, ignoreCase).stream()).collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public List<VoiceChannel> getVoiceChannels() {
        return stream().map(JDA::getVoiceChannels).flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public VoiceChannel getVoiceChannelById(@Nonnull String id) {
        return stream().map(jda -> jda.getVoiceChannelById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public VoiceChannel getVoiceChannelById(long id) {
        return stream().map(jda -> jda.getVoiceChannelById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Deprecated
    @Override
    @Nonnull
    public List<PrivateChannel> getPrivateChannels() {
        return stream().map(JDA::getPrivateChannels).flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public PrivateChannel getPrivateChannelById(@Nonnull String id) {
        return stream().map(jda -> jda.getPrivateChannelById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public PrivateChannel getPrivateChannelById(long id) {
        return stream().map(jda -> jda.getPrivateChannelById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    @Nonnull
    public List<Emote> getEmotes() {
        return stream().map(JDA::getEmotes).flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public Emote getEmoteById(@Nonnull String id) {
        return stream().map(jda -> jda.getEmoteById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public Emote getEmoteById(long id) {
        return stream().map(jda -> jda.getEmoteById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    @Nonnull
    public List<Emote> getEmotesByName(@Nonnull String name, boolean ignoreCase) {
        return stream().flatMap(jda -> jda.getEmotesByName(name, ignoreCase).stream()).collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public List<Category> getCategoriesByName(@Nonnull String name, boolean ignoreCase) {
        return stream().flatMap(jda -> jda.getCategoriesByName(name, ignoreCase).stream()).collect(Collectors.toList());
    }

    @Override
    public Category getCategoryById(@Nonnull String id) {
        return stream().map(jda -> jda.getCategoryById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public Category getCategoryById(long id) {
        return stream().map(jda -> jda.getCategoryById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    @Nonnull
    public List<Category> getCategories() {
        return stream().flatMap(jda -> jda.getCategories().stream()).collect(Collectors.toList());
    }

    @Override
    public long getResponseTotal() {
        return stream().mapToLong(JDA::getResponseTotal).sum();
    }

    @Override
    public void setAutoReconnect(boolean reconnect) {
        forEach(jda -> jda.setAutoReconnect(reconnect));
    }

    @Override
    public void shutdown() {
        forEach(JDA::shutdown);
    }

    @Override
    public void shutdownNow() {
        forEach(JDA::shutdownNow);
    }

    @Override
    public Status[] getShardStatus() {
        return stream().map(JDA::getStatus).toArray(Status[]::new);
    }

    @Override
    public int getMaxReconnectDelay() {
        return MantaroData.config().get().maxJdaReconnectDelay;
    }

    @NotNull
    @Override
    public SnowflakeCacheView<User> getUserCache() {
        return allSnowflakes(() -> stream().map(JDA::getUserCache));
    }

    @NotNull
    @Override
    public SnowflakeCacheView<Guild> getGuildCache() {
        return allSnowflakes(() -> stream().map(JDA::getGuildCache));
    }

    @NotNull
    @Override
    public SnowflakeCacheView<Category> getCategoryCache() {
        return allSnowflakes(() -> stream().map(JDA::getCategoryCache));
    }

    @NotNull
    @Override
    public SnowflakeCacheView<TextChannel> getTextChannelCache() {
        return allSnowflakes(() -> stream().map(JDA::getTextChannelCache));
    }

    @NotNull
    @Override
    public SnowflakeCacheView<VoiceChannel> getVoiceChannelCache() {
        return allSnowflakes(() -> stream().map(JDA::getVoiceChannelCache));
    }

    @NotNull
    @Override
    public SnowflakeCacheView<PrivateChannel> getPrivateChannelCache() {
        return allSnowflakes(() -> stream().map(JDA::getPrivateChannelCache));
    }

    @NotNull
    @Override
    public SnowflakeCacheView<Role> getRoleCache() {
        return allSnowflakes(() -> stream().map(JDA::getRoleCache));
    }

    @NotNull
    @Override
    public SnowflakeCacheView<Emote> getEmoteCache() {
        return allSnowflakes(() -> stream().map(JDA::getEmoteCache));
    }

    @NotNull
    @Override
    public CacheView<AudioManager> getAudioManagerCache() {
        return CacheView.all(() -> stream().map(JDA::getAudioManagerCache));
    }

    public abstract void restartShard(int shardId, boolean force);
}
