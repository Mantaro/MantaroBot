package net.kodehawa.mantarobot.services;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import okhttp3.*;

import static net.kodehawa.mantarobot.data.MantaroData.config;

@Slf4j
public class Carbonitex implements Runnable {
    private final String carbonToken = config().get().carbonToken;
    private final OkHttpClient httpClient = new OkHttpClient();
    private int shardId, totalShards;
    private JDA jda;

    public Carbonitex(JDA jda, int shardId, int totalShards) {
        this.shardId = shardId;
        this.totalShards = totalShards;
        this.jda = jda;
    }

    @Override
    public void run() {
        if(carbonToken != null) {
            int newC = jda.getGuilds().size();
            try {
                RequestBody body = new FormBody.Builder()
                        .add("key", carbonToken)
                        .add("servercount", String.valueOf(newC))
                        .add("shardid", String.valueOf(shardId))
                        .add("shardcount", String.valueOf(totalShards))
                        .build();

                Request request = new Request.Builder()
                        .url("https://www.carbonitex.net/discord/data/botdata.php")
                        .post(body)
                        .build();

                Response response = httpClient.newCall(request).execute();
                response.close();
            } catch(Exception ignored) {
            }
        }
    }
}
