/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.core.cache;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class EvictingCachePolicy implements MemberCachePolicy {
    private static final Logger log = LoggerFactory.getLogger(EvictingCachePolicy.class);
    
    private final EvictionStrategy[] strategies;
    
    public EvictingCachePolicy(List<Integer> shardIds, Supplier<EvictionStrategy> strategySupplier) {
        var strategy = new EvictionStrategy[Collections.max(shardIds) + 1];

        for (var id : shardIds) {
            strategy[id] = strategySupplier.get();
        }

        this.strategies = strategy;
    }
    
    @Override
    public boolean cacheMember(@NotNull Member member) {
        var voiceState = member.getVoiceState();
        if (voiceState != null && voiceState.getChannel() != null) {
            return true;
        }

        long evict;
        // This can be called from ws threads or requester threads
        var shard = member.getJDA().getShardInfo().getShardId();
        var strategy = strategies[shard];

        if (strategy == null) {
            log.error("Null strategy for shard {}", shard);
            return true;
        }

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized(strategy) {
            evict = strategy.cache(member.getIdLong());
        }

        // The strategy contains only members that were added to this shard
        // So removing shouldn't fail
        if (evict != EvictionStrategy.NO_REMOVAL_NEEDED) {
            member.getJDA().getGuildCache().forEach(g -> {
                var evicted = g.getMemberById(evict);
                if (evicted == null) {
                    return;
                }

                // Only remove if voice state is null, or channel in the voice state is null.
                if (evicted.getVoiceState() == null || evicted.getVoiceState().getChannel() == null) {
                    g.unloadMember(evict);
                }
            });
        }

        return true;
    }
}
