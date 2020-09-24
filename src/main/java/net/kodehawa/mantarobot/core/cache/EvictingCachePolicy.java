package net.kodehawa.mantarobot.core.cache;

import net.dv8tion.jda.api.entities.GuildVoiceState;
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
        var s = new EvictionStrategy[Collections.max(shardIds) + 1];
        for(var id : shardIds) {
            s[id] = strategySupplier.get();
        }

        this.strategies = s;
    }
    
    @Override
    public boolean cacheMember(@NotNull Member member) {
        GuildVoiceState voiceState = member.getVoiceState();
        if(voiceState != null && voiceState.getChannel() != null) {
            return true;
        }

        long evict;
        //this can be called from ws threads or requester threads
        var shard = member.getJDA().getShardInfo().getShardId();
        var strategy = strategies[shard];

        if(strategy == null) {
            log.error("Null strategy for shard {}", shard);
            return true;
        }

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized(strategy) {
            evict = strategy.cache(member.getIdLong());
        }

        //the strategy contains only members that were added to this shard
        //so removing shouldn't fail
        if(evict != EvictionStrategy.NO_REMOVAL_NEEDED) {
            member.getJDA().getGuildCache().forEach(g -> {
                var m = g.getMemberById(evict);
                if(m == null)
                    return;

                // Only remove if voice state is null, or channel in the voice state is null.
                if(m.getVoiceState() == null || m.getVoiceState().getChannel() == null) {
                    g.unloadMember(evict);
                }
            });
        }

        return true;
    }
}
