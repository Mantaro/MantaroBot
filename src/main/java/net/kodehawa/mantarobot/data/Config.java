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

package net.kodehawa.mantarobot.data;

import lombok.Data;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.currency.seasons.Season;

import java.util.ArrayList;
import java.util.List;

@Data
public class Config {
    public String alClient;
    public String alsecret;
    public String apiLoginCreds;
    public boolean cacheGames = false;
    public String carbonToken;
    public String consoleChannel = "266231083341840385";
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
    public String sentryDSN;
    public int shardWatcherTimeout = 1500; //wait 1500ms for the handlers to run
    public int shardWatcherWait = 600000; //run once every 600 seconds (10 minutes)
    public String shardWebhookUrl;
    public String token;
    public int totalShards = 0;
    public int fromShard = 0;
    public int upToShard = 0;
    public String weatherAppId;
    public String webhookUrl;
    public String spambotUrl;
    public String weebapiKey;
    public String apiTwoUrl = "http://127.0.0.1:5874";
    public boolean needApi = true;
    public int prometheusPort = 9091;
    public int ratelimitPoolSize = 4;
    public String apiAuthKey;
    public Season currentSeason = Season.FIRST;
    public String clientId; //why not ig.
    public String lavalinkNode;
    public String lavalinkPass;
    public String jedisPoolAddress = "127.0.0.1";
    public int jedisPoolPort = 6379;

    public boolean isOwner(Member member) {
        return isOwner(member.getUser());
    }

    public boolean isOwner(User user) {
        return isOwner(user.getId());
    }

    public boolean isOwner(String id) {
        return owners.contains(id);
    }
}
