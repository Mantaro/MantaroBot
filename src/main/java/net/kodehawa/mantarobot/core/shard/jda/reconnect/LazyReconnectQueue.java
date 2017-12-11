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

package net.kodehawa.mantarobot.core.shard.jda.reconnect;

import net.dv8tion.jda.core.requests.SessionReconnectQueue;
import net.dv8tion.jda.core.requests.WebSocketClient;

public class LazyReconnectQueue extends SessionReconnectQueue {
    private volatile boolean ready;

    public LazyReconnectQueue() {
        super();
    }

    @Override
    public void appendSession(WebSocketClient client) {
        if(!reconnectQueue.offer(client))
            throw new IllegalStateException("The queue rejected this session");
        if(ready)
            runWorker();
    }

    public void ready() {
        ready = true;
        if(!reconnectQueue.isEmpty())
            runWorker();
    }
}
