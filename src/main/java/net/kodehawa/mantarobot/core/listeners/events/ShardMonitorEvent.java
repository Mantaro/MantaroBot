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

package net.kodehawa.mantarobot.core.listeners.events;

import net.dv8tion.jda.api.events.Event;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ShardMonitorEvent extends Event implements MantaroEvent {
    
    public static final int
            MANTARO_LISTENER = 0,
            COMMAND_LISTENER = 1;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ShardMonitorEvent.class);
    private final Set<ShardListeners> alive = new CopyOnWriteArraySet<>();
    private final Set<ShardListeners> dead = new CopyOnWriteArraySet<>();
    
    public ShardMonitorEvent(int shards) {
        super(null, 0);
        for(int i = 0; i < shards; i++)
            dead.add(new ShardListeners(i));
    }
    
    public void alive(int shard, int listener) {
        dead.stream().filter(s -> s.shardId == shard).forEach(s -> s.alive(listener));
    }
    
    public int[] getAliveShards() {
        return alive.stream().mapToInt(s -> s.shardId).toArray();
    }
    
    public int[] getDeadShards() {
        return dead.stream().mapToInt(s -> s.shardId).toArray();
    }
    
    public boolean isAlive(int shard) {
        return alive.stream().map(s -> s.shardId == shard).count() != 0;
    }
    
    public int totalAliveShards() {
        return alive.size();
    }
    
    @Override
    public void onPropagation() {
        log.debug("Sent event to check if shards are alive!");
    }
    
    private class ShardListeners {
        private final int shardId;
        private boolean commandListener = false;
        private boolean mantaroListener = false;
        
        private ShardListeners(int shardId) {
            this.shardId = shardId;
        }
        
        @Override
        public int hashCode() {
            return shardId;
        }
        
        @Override
        public boolean equals(Object other) {
            return other instanceof ShardListeners && ((ShardListeners) other).shardId == shardId;
        }
        
        private void alive(int listener) {
            switch(listener) {
                case MANTARO_LISTENER:
                    mantaroListener = true;
                    break;
                case COMMAND_LISTENER:
                    commandListener = true;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown listener id " + listener);
            }
            if(mantaroListener && commandListener) {
                log.debug("Shard " + shardId + " is alive");
                dead.remove(this);
                alive.add(this);
            }
        }
    }
}
