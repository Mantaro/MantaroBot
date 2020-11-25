/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.data;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.currency.seasons.Season;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("CanBeFinal")
public class Config {
    public String dbDb = "mantaro";
    public String dbHost = "localhost";
    public String dbPassword;
    public int dbPort = 28015;
    public String dbUser;
    public String dbotsorgToken;
    public String botsOnDiscordToken;
    public String discordBoatsToken;
    public String dblToken;
    public boolean isPremiumBot = false;
    public List<String> owners = new ArrayList<>();
    public String[] prefix = {"~>", "->"};
    public String shardWebhookUrl;
    public String token;
    public int totalShards = 0;
    public String webhookUrl;
    public String spambotUrl;
    public String weebapiKey;
    public String apiTwoUrl = "http://127.0.0.1:5874";
    public boolean needApi = true;
    public int prometheusPort = 9091;
    public String apiAuthKey;
    public Season currentSeason = Season.FIRST;
    public String clientId; //why not ig.
    public String jedisPoolAddress = "127.0.0.1";
    public int jedisPoolPort = 6379;
    public List<String> lavalinkNodes = new ArrayList<>();
    public String lavalinkPass;
    public String ipv6Block = "";
    public String excludeAddress = "";
    public int bucketFactor = 4;
    public long dailyMaxPeriodMilliseconds = TimeUnit.HOURS.toMillis(50);
    public boolean isSelfHost = false;
    public int memberCacheSize = 10_000;
    public boolean handleRatelimits = true;

    public Config() { }

    public boolean isOwner(Member member) {
        return isOwner(member.getUser());
    }

    public boolean isOwner(User user) {
        return isOwner(user.getId());
    }

    public boolean isOwner(String id) {
        return owners.contains(id);
    }

    public String getDbDb() {
        return this.dbDb;
    }

    public String getDbHost() {
        return this.dbHost;
    }

    public String getDbPassword() {
        return this.dbPassword;
    }

    public int getDbPort() {
        return this.dbPort;
    }

    public String getDbUser() {
        return this.dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbotsorgToken() {
        return this.dbotsorgToken;
    }

    public String getBotsOnDiscordToken() {
        return botsOnDiscordToken;
    }

    public String getDiscordBoatsToken() {
        return discordBoatsToken;
    }

    public String getDblToken() {
        return dblToken;
    }

    public boolean isPremiumBot() {
        return this.isPremiumBot;
    }

    public List<String> getOwners() {
        return this.owners;
    }

    public String[] getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String[] prefix) {
        this.prefix = prefix;
    }

    public String getShardWebhookUrl() {
        return this.shardWebhookUrl;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getTotalShards() {
        return this.totalShards;
    }

    public String getWebhookUrl() {
        return this.webhookUrl;
    }

    public String getSpambotUrl() {
        return this.spambotUrl;
    }

    public String getWeebapiKey() {
        return this.weebapiKey;
    }

    public String getApiTwoUrl() {
        return this.apiTwoUrl;
    }

    public boolean isNeedApi() {
        return this.needApi;
    }

    public int getPrometheusPort() {
        return this.prometheusPort;
    }

    public String getApiAuthKey() {
        return this.apiAuthKey;
    }

    public Season getCurrentSeason() {
        return this.currentSeason;
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getJedisPoolAddress() {
        return this.jedisPoolAddress;
    }

    public int getJedisPoolPort() {
        return this.jedisPoolPort;
    }

    public List<String> getLavalinkNodes() {
        return this.lavalinkNodes;
    }

    public String getLavalinkPass() {
        return this.lavalinkPass;
    }

    public String getIpv6Block() {
        return this.ipv6Block;
    }

    public String getExcludeAddress() {
        return this.excludeAddress;
    }

    public int getBucketFactor() {
        return this.bucketFactor;
    }

    public long getDailyMaxPeriodMilliseconds(){
        return this.dailyMaxPeriodMilliseconds;
    }

    public boolean isSelfHost() {
        return isSelfHost;
    }

    public void setSelfHost(boolean selfHost) {
        isSelfHost = selfHost;
    }

    public boolean isHandleRatelimits() {
        return handleRatelimits;
    }
}
