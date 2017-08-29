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
