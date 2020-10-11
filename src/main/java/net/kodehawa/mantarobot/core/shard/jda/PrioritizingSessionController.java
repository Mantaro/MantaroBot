package net.kodehawa.mantarobot.core.shard.jda;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.SessionControllerAdapter;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

//created by napster (https://github.com/napstr)
public class PrioritizingSessionController extends SessionControllerAdapter
        implements Comparator<SessionController.SessionConnectNode> {

    private final long homeGuildId;

    public PrioritizingSessionController(long homeGuildId) {
        this.homeGuildId = homeGuildId;
        this.connectQueue = new PriorityBlockingQueue<>(1, this);
    }

    @Override
    public int compare(SessionController.SessionConnectNode s1, SessionController.SessionConnectNode s2) {
        //if one of the shards is containing the home guild, do it first always
        if (isHomeShard(s1)) {
            return -1;
        }

        if (isHomeShard(s2)) {
            return 1;
        }

        //if both or none are reconnecting, order by their shard ids
        if ((s1.isReconnect() && s2.isReconnect())
                || (!s1.isReconnect() && !s2.isReconnect())) {
            return s1.getShardInfo().getShardId() - s2.getShardInfo().getShardId();
        }

        //otherwise prefer the one that is reconnecting
        return s1.isReconnect() ? -1 : 1;
    }

    private long getHomeShardId(int shardTotal) {
        return (this.homeGuildId >> 22) % shardTotal;
    }

    private boolean isHomeShard(SessionController.SessionConnectNode node) {
        return homeGuildId != -1 &&
                getHomeShardId(node.getShardInfo().getShardTotal())
                        == node.getShardInfo().getShardId();
    }

    @Nonnull
    @Override
    public ShardedGateway getShardedGateway(@Nonnull JDA api) {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public String getGateway(@Nonnull JDA api) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getGlobalRatelimit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setGlobalRatelimit(long ratelimit) {
        throw new UnsupportedOperationException();
    }
}
