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

package net.kodehawa.mantarobot.services;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.JDA;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.services.entities.BotStats;
import net.kodehawa.mantarobot.services.entities.ShardStats;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import static net.kodehawa.mantarobot.utils.Utils.httpClient;

public class StatsPoster {
    private final Config config = MantaroData.config().get();
    private final long botId;

    public StatsPoster(long botId) {
        this.botId = botId;
    }

    /**
     * Post the current shard information to the API.
     *
     * This can later be retrieved using {@link StatsPoster#getStatsForShard(int shardId)}
     *
     * @param shardId The current shard id to post.
     * @param status The {@link JDA.Status} object. This usually tells if the shard is connected, loading or otherwise.
     * @param guilds The amount of guilds the shard sees.
     * @param users The amount of *cached* users the shard sees.
     * @param ping The current gateway ping (in ms).
     * @param eventTime The difference between time when the last JDA event was received and the current time (in ms).
     */
    public void postForShard(int shardId, JDA.Status status, long guilds, long users, long ping, long eventTime) {
        try {
            Request request = new Request.Builder()
                    .url(config.apiTwoUrl + "/mantaroapi/bot/stats/shards")
                    .addHeader("Authorization", config.getApiAuthKey())
                    .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                    .post(RequestBody.create(
                            okhttp3.MediaType.parse("application/json"),
                            new JSONObject()
                                    .put("bot_id", botId)
                                    .put("shard_id", shardId)
                                    .put("shard_info", new JSONObject()
                                            .put("status", status.toString())
                                            .put("guilds", guilds)
                                            .put("users", users)
                                            .put("ping", ping)
                                            .put("event_time", eventTime)
                                    )
                                    .toString()
                    )).build();

            Response response = httpClient.newCall(request).execute();
            response.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the statistics for a single bot shard.
     * This includes stuff like guild count, user count, ping, last event time, etc.
     * @param shardId The id of the shard to look up.
     * @return A JSON object that contains the requested information.
     * @throws IOException If it can't reach the API.
     */
    public String getStatsForShardRaw(int shardId) throws IOException {
        Request request = new Request.Builder()
                .url(config.apiTwoUrl + "/mantaroapi/bot/stats/shards/bot/specific")
                .addHeader("Authorization", config.getApiAuthKey())
                .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                .post(RequestBody.create(
                        okhttp3.MediaType.parse("application/json"),
                        new JSONObject()
                                .put("bot_id", botId)
                                .put("shard_id", shardId)
                                .toString()
                )).build();

        Response response = httpClient.newCall(request).execute();
        String body = response.body().string();
        response.close();

        return body;
    }

    /**
     * Gets the statistics for a single bot shard.
     * This includes stuff like guild count, user count, ping, last event time, etc.
     *
     * To get the raw JSON, use {@link StatsPoster#getStatsForShardRaw(int)}
     *
     * @param shardId The id of the shard to look up.
     * @return A ShardStats object that contains the requested information.
     * @throws IOException If it can't reach the API.
     */
    public ShardStats getStatsForShard(int shardId) throws IOException, JsonSyntaxException {
        return GsonDataManager.GSON_PRETTY.fromJson(getStatsForShardRaw(shardId), ShardStats.class);
    }

    /**
     * Gets the statistics for all of the bot shards.
     * This includes stuff like guild count, user count, ping, last event time, etc.
     * @return A JSON object that contains the requested information.
     * @throws IOException If it can't reach the API.
     */
    public String getShardStatsRaw() throws IOException {
        Request request = new Request.Builder()
                .url(config.apiTwoUrl + "/mantaroapi/bot/stats/shards/bot/all")
                .addHeader("Authorization", config.getApiAuthKey())
                .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                .post(RequestBody.create(
                        okhttp3.MediaType.parse("application/json"),
                        new JSONObject()
                                .put("bot_id", botId)
                                .toString()
                )).build();

        Response response = httpClient.newCall(request).execute();
        String body = response.body().string();
        response.close();

        return body;
    }

    /**
     * Gets the statistics for all of the bot shards.
     * This includes stuff like guild count, user count, ping, last event time, etc.
     *
     * To get the raw JSON, use {@link StatsPoster#getShardStatsRaw()}
     *
     * @return A Map of shardId -> ShardStats object that contains the requested information.
     * @throws IOException If it can't reach the API.
     */
    public Map<Integer, ShardStats> getShardStats() throws IOException, JsonSyntaxException {
        return GsonDataManager.GSON_PRETTY.fromJson(getShardStatsRaw(), new TypeToken<Map<Integer, ShardStats>>(){}.getType());
    }

    /**
     * Gets the combined guild and user count for all shards on a determined bot id. This uses the current bot id to determine it.
     * @return A JSON contained the requested information.
     * @throws IOException If it can't reach the API.
     */
    public String getCombinedInfoRaw() throws IOException {
        Request request = new Request.Builder()
                .url(config.apiTwoUrl + "/mantaroapi/bot/stats/shards/combined")
                .addHeader("Authorization", config.getApiAuthKey())
                .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                .post(RequestBody.create(
                        okhttp3.MediaType.parse("application/json"),
                        new JSONObject()
                                .put("bot_id", botId)
                                .toString()
                )).build();

        Response response = httpClient.newCall(request).execute();
        String body = response.body().string();
        response.close();

        return body;
    }

    /**
     * Gets the combined guild and user count for all shards on a determined bot id. This uses the current bot id to determine it.
     *
     * To get the raw JSON, use {@link StatsPoster#getCombinedInfoRaw()}
     *
     * @return A BotStats object contained the requested information.
     * @throws IOException If it can't reach the API.
     */
    public BotStats getCombinedInfo() throws IOException, JsonSyntaxException {
        return GsonDataManager.GSON_PRETTY.fromJson(getCombinedInfoRaw(), BotStats.class);
    }
}
