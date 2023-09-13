/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core.shard.discord;

import com.fasterxml.jackson.annotation.JsonProperty;

// The *sheer* amount of boilerplate here, holy shit.
public class BotGateway {
    private String url;
    private int shards;
    @JsonProperty("session_start_limit")
    private SessionStartLimit sessionStartLimit;

    @SuppressWarnings("unused")
    public String getUrl() {
        return url;
    }

    @SuppressWarnings("unused")
    public int getShards() {
        return shards;
    }

    @JsonProperty("session_start_limit")
    public SessionStartLimit getSession() {
        return sessionStartLimit;
    }

    @SuppressWarnings("unused")
    public void setUrl(String url) {
        this.url = url;
    }

    @SuppressWarnings("unused")
    public void setShards(int shards) {
        this.shards = shards;
    }

    @SuppressWarnings("unused")
    public void setSessionStartLimit(SessionStartLimit sessionStartLimit) {
        this.sessionStartLimit = sessionStartLimit;
    }

    @SuppressWarnings("unused")
    public static class SessionStartLimit {
        private int total;
        private int remaining;
        private int reset_after;
        private int max_concurrency;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getRemaining() {
            return remaining;
        }

        public void setRemaining(int remaining) {
            this.remaining = remaining;
        }

        @JsonProperty("reset_after")
        public int getResetAfter() {
            return reset_after;
        }

        public void setReset_after(int reset_after) {
            this.reset_after = reset_after;
        }

        @JsonProperty("max_concurrency")
        public int getMaxConcurrency() {
            return max_concurrency;
        }

        public void setMax_concurrency(int max_concurrency) {
            this.max_concurrency = max_concurrency;
        }
    }
}
