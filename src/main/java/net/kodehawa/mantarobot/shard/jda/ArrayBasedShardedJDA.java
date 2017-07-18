package net.kodehawa.mantarobot.shard.jda;

import net.dv8tion.jda.core.JDA;
import org.apache.commons.collections4.iterators.ArrayIterator;

import javax.annotation.Nonnull;
import java.util.Iterator;

public class ArrayBasedShardedJDA extends ShardedJDA {
    private final JDA[] shards;

    public ArrayBasedShardedJDA(JDA... shards) {
        this.shards = shards;
    }

    @Override
    public JDA getShard(int shard) {
        return shards[shard];
    }

    @Override
    public int getShardAmount() {
        return shards.length;
    }

    @Nonnull
    @Override
    public Iterator<JDA> iterator() {
        return new ArrayIterator<>(shards);
    }
}
