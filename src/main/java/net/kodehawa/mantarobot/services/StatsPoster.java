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

import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.JDA;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
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
                    ))
                    .build();

            Response response = httpClient.newCall(request).execute();
            response.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ShardStats getStatsForShard(int shardId) throws IOException {
        Request request = new Request.Builder()
                .url(config.apiTwoUrl + "/mantaroapi/bot/stats/shards/specific")
                .addHeader("Authorization", config.getApiAuthKey())
                .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                .post(RequestBody.create(
                        okhttp3.MediaType.parse("application/json"),
                        new JSONObject()
                                .put("bot_id", botId)
                                .put("shard_id", shardId)
                                .toString()
                ))
                .build();

        Response response = httpClient.newCall(request).execute();
        String body = response.body().string();
        response.close();

        return GsonDataManager.GSON_PRETTY.fromJson(body, ShardStats.class);
    }

    public String getStatsForShardRaw(int shardId) throws IOException {
        Request request = new Request.Builder()
                .url(config.apiTwoUrl + "/mantaroapi/bot/stats/shards/specific")
                .addHeader("Authorization", config.getApiAuthKey())
                .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                .post(RequestBody.create(
                        okhttp3.MediaType.parse("application/json"),
                        new JSONObject()
                                .put("bot_id", botId)
                                .put("shard_id", shardId)
                                .toString()
                ))
                .build();

        Response response = httpClient.newCall(request).execute();
        String body = response.body().string();
        response.close();

        return body;
    }

    public Map<Integer, ShardStats> getShardStats() throws IOException {
        Request request = new Request.Builder()
                .url(config.apiTwoUrl + "/mantaroapi/bot/stats/shards/bot/all")
                .addHeader("Authorization", config.getApiAuthKey())
                .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                .post(RequestBody.create(
                        okhttp3.MediaType.parse("application/json"),
                        new JSONObject()
                                .put("bot_id", botId)
                                .toString()
                ))
                .build();

        Response response = httpClient.newCall(request).execute();
        String body = response.body().string();
        response.close();

        return GsonDataManager.GSON_PRETTY.fromJson(body, new TypeToken<Map<Integer, ShardStats>>(){}.getType());
    }

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
                ))
                .build();

        Response response = httpClient.newCall(request).execute();
        String body = response.body().string();
        response.close();

        return body;
    }

}
