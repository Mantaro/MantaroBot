/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core.shard.jda;

import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.SessionControllerAdapter;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public class BucketedController extends SessionControllerAdapter {
    private final SessionController[] shardControllers;
    
    public BucketedController(@Nonnegative int bucketFactor, long homeGuildId) {
        if(bucketFactor < 1) {
            throw new IllegalArgumentException("Bucket factor must be at least 1");
        }
        this.shardControllers = new SessionController[bucketFactor];
        for(int i = 0; i < bucketFactor; i++) {
            this.shardControllers[i] = new PrioritizingSessionController(homeGuildId);
        }
    }
    
    public BucketedController(long homeGuildId) {
        this(16, homeGuildId);
    }
    
    @Override
    public void appendSession(@Nonnull SessionConnectNode node) {
        controllerFor(node).appendSession(node);
    }
    
    @Override
    public void removeSession(@Nonnull SessionConnectNode node) {
        controllerFor(node).removeSession(node);
    }
    
    @Nonnull
    @CheckReturnValue
    private SessionController controllerFor(@Nonnull SessionConnectNode node) {
        return shardControllers[node.getShardInfo().getShardId() % shardControllers.length];
    }
}
