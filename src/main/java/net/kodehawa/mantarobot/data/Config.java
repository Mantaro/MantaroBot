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

package net.kodehawa.mantarobot.data;

import lombok.Data;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.redisson.api.LocalCachedMapOptions;

import java.util.ArrayList;
import java.util.List;

@Data
public class Config {
    public String alClient;
    public String alsecret;
    public String apiLoginCreds;
    public String apiUrl = "127.0.0.1:4454";
    public String bugreportChannel;
    public boolean cacheGames = false;
    public String carbonToken;
    public String cleverbotKey;
    public String cleverbotUser;
    public int connectionWatcherPort = 26000;
    public String consoleChannel = "266231083341840385";
    public String crossBotHost;
    public int crossBotPort;
    public boolean crossBotServer = false;
    public String dbDb = "mantaro";
    public String dbHost = "localhost";
    public String dbPassword;
    public int dbPort = 28015;
    public String dbUser;
    public String dbotsToken;
    public String dbotsorgToken;
    public boolean isBeta = false;
    public boolean isPremiumBot = false;
    public int maxJdaReconnectDelay = 3; //3 seconds
    public String osuApiKey;
    public List<String> owners = new ArrayList<>();
    public String[] prefix = {"~>", "->"};
    public String rMQIP;
    public String rMQPassword;
    public String rMQUser;
    public RedisInfo redis = new RedisInfo();
    public String remoteNode;
    public String sentryDSN;
    public int shardWatcherTimeout = 1500; //wait 1500ms for the handlers to run
    public int shardWatcherWait = 600000; //run once every 600 seconds (10 minutes)
    public String shardWebhookUrl;
    public String sqlPassword;
    public String token;
    public int totalMusicNodes = 1;
    public int totalNodes = 1;
    public int totalShards = 0;
    public int upToShard = 0;
    public String weatherAppId;
    public String webhookUrl;
    public String weebapiKey;
    public String apiTwoUrl = "http://127.0.0.1:5874";
    public boolean needApi = true;

    public boolean isOwner(Member member) {
        return isOwner(member.getUser());
    }

    public boolean isOwner(User user) {
        return isOwner(user.getId());
    }

    public boolean isOwner(String id) {
        return owners.contains(id);
    }

    public static class RedisInfo {
        public CacheInfo customCommands = new CacheInfo();
        public boolean enabled = true;
        public CacheInfo guilds = new CacheInfo();
        public String host = "localhost";
        public CacheInfo players = new CacheInfo();
        public int port = 6379;
        public CacheInfo premiumKeys = new CacheInfo();
        public CacheInfo users = new CacheInfo();

        public static class CacheInfo {
            public boolean enabled = false;
            public LocalCachedMapOptions.EvictionPolicy evictionPolicy = LocalCachedMapOptions.EvictionPolicy.LFU;
            public LocalCachedMapOptions.InvalidationPolicy invalidationPolicy = LocalCachedMapOptions.InvalidationPolicy.ON_CHANGE;
            public long maxIdleMs = 180000;
            public int maxSize = 1000;
            public long ttlMs = 180000;
        }
    }
}
