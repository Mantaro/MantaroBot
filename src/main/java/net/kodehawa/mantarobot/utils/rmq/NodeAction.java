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

package net.kodehawa.mantarobot.utils.rmq;

public enum NodeAction {
    //RIP.
    SHUTDOWN,
    //Something did a boom or it has been running for too long.
    RESTART,
    //Should give a restart_shard signal to the node to restart X shard. Shard should be specified in the JSON payload.
    RESTART_SHARD,
    //meme, kappa. Should restart all node shards in a rolling fashion.
    ROLLING_RESTART
}