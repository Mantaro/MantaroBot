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

package net.kodehawa.mantarobot.data;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.currency.seasons.Season;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Config {
    public boolean cacheGames = false;
    public String carbonToken;
    public String dbDb = "mantaro";
    public String dbHost = "localhost";
    public String dbPassword;
    public int dbPort = 28015;
    public String dbUser;
    public String dbotsToken;
    public String dbotsorgToken;
    public boolean isPremiumBot = false;
    public int maxJdaReconnectDelay = 3; //3 seconds
    public String osuApiKey;
    public List<String> owners = new ArrayList<>();
    public String[] prefix = {"~>", "->"};
    public String sentryDSN;
    public int shardWatcherWait = 600000; //run once every 600 seconds (10 minutes)
    public String shardWebhookUrl;
    public String token;
    public int totalShards = 0;
    public String weatherAppId;
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
    public String cacheClientEndpoint;
    public String cacheClientToken;
    public String ipv6Block = "";
    public String excludeAddress = "";
    public int bucketFactor = 4;
    public long daily_maxPeriod_millis = TimeUnit.HOURS.toMillis(50);

    public Config() {
    }

    public boolean isOwner(Member member) {
        return isOwner(member.getUser());
    }

    public boolean isOwner(User user) {
        return isOwner(user.getId());
    }

    public boolean isOwner(String id) {
        return owners.contains(id);
    }

    public boolean isCacheGames() {
        return this.cacheGames;
    }

    public void setCacheGames(boolean cacheGames) {
        this.cacheGames = cacheGames;
    }

    public String getCarbonToken() {
        return this.carbonToken;
    }

    public void setCarbonToken(String carbonToken) {
        this.carbonToken = carbonToken;
    }

    public String getDbDb() {
        return this.dbDb;
    }

    public void setDbDb(String dbDb) {
        this.dbDb = dbDb;
    }

    public String getDbHost() {
        return this.dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public String getDbPassword() {
        return this.dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public int getDbPort() {
        return this.dbPort;
    }

    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbUser() {
        return this.dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbotsToken() {
        return this.dbotsToken;
    }

    public void setDbotsToken(String dbotsToken) {
        this.dbotsToken = dbotsToken;
    }

    public String getDbotsorgToken() {
        return this.dbotsorgToken;
    }

    public void setDbotsorgToken(String dbotsorgToken) {
        this.dbotsorgToken = dbotsorgToken;
    }

    public boolean isPremiumBot() {
        return this.isPremiumBot;
    }

    public void setPremiumBot(boolean isPremiumBot) {
        this.isPremiumBot = isPremiumBot;
    }

    public int getMaxJdaReconnectDelay() {
        return this.maxJdaReconnectDelay;
    }

    public void setMaxJdaReconnectDelay(int maxJdaReconnectDelay) {
        this.maxJdaReconnectDelay = maxJdaReconnectDelay;
    }

    public String getOsuApiKey() {
        return this.osuApiKey;
    }

    public void setOsuApiKey(String osuApiKey) {
        this.osuApiKey = osuApiKey;
    }

    public List<String> getOwners() {
        return this.owners;
    }

    public void setOwners(List<String> owners) {
        this.owners = owners;
    }

    public String[] getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String[] prefix) {
        this.prefix = prefix;
    }

    public String getSentryDSN() {
        return this.sentryDSN;
    }

    public void setSentryDSN(String sentryDSN) {
        this.sentryDSN = sentryDSN;
    }

    public int getShardWatcherWait() {
        return this.shardWatcherWait;
    }

    public void setShardWatcherWait(int shardWatcherWait) {
        this.shardWatcherWait = shardWatcherWait;
    }

    public String getShardWebhookUrl() {
        return this.shardWebhookUrl;
    }

    public void setShardWebhookUrl(String shardWebhookUrl) {
        this.shardWebhookUrl = shardWebhookUrl;
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

    public void setTotalShards(int totalShards) {
        this.totalShards = totalShards;
    }

    public String getWeatherAppId() {
        return this.weatherAppId;
    }

    public void setWeatherAppId(String weatherAppId) {
        this.weatherAppId = weatherAppId;
    }

    public String getWebhookUrl() {
        return this.webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getSpambotUrl() {
        return this.spambotUrl;
    }

    public void setSpambotUrl(String spambotUrl) {
        this.spambotUrl = spambotUrl;
    }

    public String getWeebapiKey() {
        return this.weebapiKey;
    }

    public void setWeebapiKey(String weebapiKey) {
        this.weebapiKey = weebapiKey;
    }

    public String getApiTwoUrl() {
        return this.apiTwoUrl;
    }

    public void setApiTwoUrl(String apiTwoUrl) {
        this.apiTwoUrl = apiTwoUrl;
    }

    public boolean isNeedApi() {
        return this.needApi;
    }

    public void setNeedApi(boolean needApi) {
        this.needApi = needApi;
    }

    public int getPrometheusPort() {
        return this.prometheusPort;
    }

    public void setPrometheusPort(int prometheusPort) {
        this.prometheusPort = prometheusPort;
    }

    public String getApiAuthKey() {
        return this.apiAuthKey;
    }

    public void setApiAuthKey(String apiAuthKey) {
        this.apiAuthKey = apiAuthKey;
    }

    public Season getCurrentSeason() {
        return this.currentSeason;
    }

    public void setCurrentSeason(Season currentSeason) {
        this.currentSeason = currentSeason;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getJedisPoolAddress() {
        return this.jedisPoolAddress;
    }

    public void setJedisPoolAddress(String jedisPoolAddress) {
        this.jedisPoolAddress = jedisPoolAddress;
    }

    public int getJedisPoolPort() {
        return this.jedisPoolPort;
    }

    public void setJedisPoolPort(int jedisPoolPort) {
        this.jedisPoolPort = jedisPoolPort;
    }

    public List<String> getLavalinkNodes() {
        return this.lavalinkNodes;
    }

    public void setLavalinkNodes(List<String> lavalinkNodes) {
        this.lavalinkNodes = lavalinkNodes;
    }

    public String getLavalinkPass() {
        return this.lavalinkPass;
    }

    public void setLavalinkPass(String lavalinkPass) {
        this.lavalinkPass = lavalinkPass;
    }

    public String getCacheClientEndpoint() {
        return this.cacheClientEndpoint;
    }

    public void setCacheClientEndpoint(String cacheClientEndpoint) {
        this.cacheClientEndpoint = cacheClientEndpoint;
    }

    public String getCacheClientToken() {
        return this.cacheClientToken;
    }

    public void setCacheClientToken(String cacheClientToken) {
        this.cacheClientToken = cacheClientToken;
    }

    public String getIpv6Block() {
        return this.ipv6Block;
    }

    public void setIpv6Block(String ipv6Block) {
        this.ipv6Block = ipv6Block;
    }

    public String getExcludeAddress() {
        return this.excludeAddress;
    }

    public void setExcludeAddress(String excludeAddress) {
        this.excludeAddress = excludeAddress;
    }

    public int getBucketFactor() {
        return this.bucketFactor;
    }

    public void setBucketFactor(int bucketFactor) {
        this.bucketFactor = bucketFactor;
    }

    public long getDaily_maxPeriod_millis(){
        return this.daily_maxPeriod_millis;
    }

    public void setDaily_maxPeriod_millis(long daily_maxPeriod_millis){
        this.daily_maxPeriod_millis = daily_maxPeriod_millis;
    }
}
