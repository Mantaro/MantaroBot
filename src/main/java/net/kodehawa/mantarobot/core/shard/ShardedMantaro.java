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

package net.kodehawa.mantarobot.core.shard;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.LoadState;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.SentryHelper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static net.kodehawa.mantarobot.utils.ShutdownCodes.SHARD_FETCH_FAILURE;

@Slf4j
public class ShardedMantaro {

    @Getter
    private final List<MantaroEventManager> managers = new ArrayList<>();
    @Getter
    private final MantaroShard[] shards;
    @Getter
    private final int totalShards;
    private final ICommandProcessor processor;

    public ShardedMantaro(int totalShards, boolean isDebug, boolean auto, String token, ICommandProcessor commandProcessor) {
        int shardAmount = totalShards;
        if(auto) shardAmount = getRecommendedShards(token);
        if(isDebug) shardAmount = 2;
        this.totalShards = shardAmount;
        processor = commandProcessor;
        shards = new MantaroShard[this.totalShards];
    }

    public void shard() {
        try{
            MantaroCore.setLoadState(LoadState.LOADING_SHARDS);
            log.info("Spawning shards...");
            for (int i = 0; i < totalShards; i++) {
                if(MantaroData.config().get().upToShard != 0 && i > MantaroData.config().get().upToShard) continue;

                log.info("Starting shard #" + i + " of " + totalShards);
                MantaroEventManager manager = new MantaroEventManager();
                managers.add(manager);
                shards[i] = new MantaroShard(i, totalShards, manager, processor);
                log.debug("Finished loading shard #" + i + ".");
            }
        } catch (Exception e) {
            e.printStackTrace();
            SentryHelper.captureExceptionContext("Shards failed to initialize!", e, this.getClass(), "Shard Loader");
        }
    }

    public void startUpdaters() {
        for(MantaroShard shard : getShards()) {
            shard.updateServerCount();
            shard.updateStatus();
        }
    }

    private static int getRecommendedShards(String token) {

        if(MantaroData.config().get().totalShards != 0){
            return MantaroData.config().get().totalShards;
        }

        try {
            OkHttpClient okHttp = new OkHttpClient();
            Request shards = new Request.Builder()
                    .url("https://discordapp.com/api/gateway/bot")
                    .header("Authorization", "Bot " + token)
                    .header("Content-Type", "application/json")
                    .build();

            Response response = okHttp.newCall(shards).execute();
            JSONObject shardObject = new JSONObject(response.body().string());
            response.close();
            return shardObject.getInt("shards");
        } catch (Exception e) {
            SentryHelper.captureExceptionContext(
                    "Exception thrown when trying to get shard count, discord isn't responding?", e, MantaroBot.class, "Shard Count Fetcher"
            );
            System.exit(SHARD_FETCH_FAILURE);
        }
        return 1;
    }
}
