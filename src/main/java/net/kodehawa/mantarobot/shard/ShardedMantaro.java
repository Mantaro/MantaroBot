package net.kodehawa.mantarobot.shard;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.MantaroEventManager;
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
    private List<MantaroEventManager> managers = new ArrayList<>();
    @Getter
    private MantaroShard[] shards;
    @Getter
    private int totalShards;

    public ShardedMantaro(int totalShards, boolean isDebug, boolean auto, String token) {
        if(isDebug) totalShards = 2;
        if(auto) totalShards = getRecommendedShards(token);
        this.totalShards = totalShards;
        shards = new MantaroShard[totalShards];
    }

    private static int getRecommendedShards(String token) {
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
        } catch(Exception e) {
            SentryHelper.captureExceptionContext(
                    "Exception thrown when trying to get shard count, discord isn't responding?", e, MantaroBot.class, "Shard Count Fetcher"
            );
            System.exit(SHARD_FETCH_FAILURE);
        }
        return 1;
    }

    public void shard() {
        try {
            for(int i = 0; i < totalShards; i++) {
                log.info("Starting shard #" + i + " of " + totalShards);
                MantaroEventManager manager = new MantaroEventManager();
                managers.add(manager);
                shards[i] = new MantaroShard(i, totalShards, manager);
                log.debug("Finished loading shard #" + i + ".");
            }
        } catch(Exception e) {
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
}
