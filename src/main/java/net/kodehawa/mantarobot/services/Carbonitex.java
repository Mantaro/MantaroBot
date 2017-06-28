package net.kodehawa.mantarobot.services;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.kodehawa.mantarobot.data.Config;

import static net.kodehawa.mantarobot.data.MantaroData.config;

@Slf4j
public class Carbonitex implements Runnable {
    private final String carbonToken = config().get().carbonToken;
    private int shardId, totalShards;
    private JDA jda;


    public Carbonitex(JDA jda, int shardId, int totalShards){
        this.shardId = shardId;
        this.totalShards = totalShards;
        this.jda = jda;
    }

    @Override
    public void run() {
        if (carbonToken != null) {
            int newC = jda.getGuilds().size();
            try{
                log.debug("Successfully posted the botdata to carbonitex.com: " +
                        Unirest.post("https://www.carbonitex.net/discord/data/botdata.php")
                                .field("key", carbonToken)
                                .field("servercount", newC)
                                .field("shardid", shardId)
                                .field("shardcount", totalShards)
                                .asString().getBody());
            } catch (UnirestException ignored){}
        }
    }
}
