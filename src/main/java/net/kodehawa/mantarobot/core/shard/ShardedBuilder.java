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

package net.kodehawa.mantarobot.core.shard;

import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;

/**
 * Sharded builder. Has all the necessary stuff to build a new {@link ShardedMantaro} instance to work with.
 */
public class ShardedBuilder {
    //How many shards
    private int amount = 1;
    
    //Start from shard x to shard x.
    private int fromShard;
    private int toShard;
    
    //Automatic sharding
    private boolean auto;
    private ICommandProcessor commandProcessor;
    private boolean debug;
    private String token;
    
    public ShardedBuilder amount(int shardAmount) {
        amount = shardAmount;
        return this;
    }
    
    public ShardedBuilder auto(boolean auto) {
        this.auto = auto;
        return this;
    }
    
    public ShardedBuilder debug(boolean debug) {
        this.debug = debug;
        return this;
    }
    
    public ShardedBuilder token(String token) {
        this.token = token;
        return this;
    }
    
    public ShardedBuilder amountNode(int from, int to) {
        this.fromShard = from;
        this.toShard = to;
        return this;
    }
    
    public ShardedBuilder commandProcessor(ICommandProcessor processor) {
        this.commandProcessor = processor;
        return this;
    }
    
    public ShardedMantaro build() {
        if(token == null)
            throw new IllegalArgumentException("Token cannot be null");
        if(commandProcessor == null)
            throw new IllegalArgumentException("H-How do you expect me to process commands!");
        
        return new ShardedMantaro(amount, debug, auto, token, commandProcessor, fromShard, toShard);
    }
}
