package net.kodehawa.mantarobot.core.cache;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;

public class EvictingCachePolicy implements MemberCachePolicy {
    private final EvictionStrategy strategy;
    
    public EvictingCachePolicy(EvictionStrategy strategy) {
        this.strategy = strategy;
    }
    
    @Override
    public boolean cacheMember(@NotNull Member member) {
        long evict;
        //this can be called from ws threads or requester threads
        //but also from any shard, so we need to synchronize here
        //todo: per-shard strategy? (var strategy = strategies[shard_id])
        synchronized(strategy) {
            evict = strategy.cache(member.getIdLong());
        }
        if(evict != EvictionStrategy.NO_REMOVAL_NEEDED) {
            member.getGuild().unloadMember(evict);
        }
        return true;
    }
}
