package net.kodehawa.mantarobot.shard;

import br.com.brjdevs.java.utils.async.Async;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.experimental.Delegate;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.listener.VoiceChannelListener;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.core.listeners.operations.old.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.old.ReactionOperations;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.services.Carbonitex;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;
import static net.kodehawa.mantarobot.data.MantaroData.config;
import static net.kodehawa.mantarobot.utils.Utils.pretty;

public class MantaroShard implements JDA {
    public static final DataManager<List<String>> SPLASHES = new SimpleFileDataManager("assets/mantaro/texts/splashes.txt");
    public static final VoiceChannelListener VOICE_CHANNEL_LISTENER = new VoiceChannelListener();
    private static final Random RANDOM = new Random();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    static {
        if(SPLASHES.get().removeIf(s -> s == null || s.isEmpty())) SPLASHES.save();
    }

    @Getter
    public final MantaroEventManager manager;
    @Getter
    private final ExecutorService threadPool;
    @Getter
    private final ExecutorService commandPool;
    private final CommandListener commandListener;
    private final Logger log;
    private final MantaroListener mantaroListener;
    private final int shardId;
    private final int totalShards;
    @Delegate
    private JDA jda;

    public MantaroShard(int shardId, int totalShards, MantaroEventManager manager) throws RateLimitedException, LoginException, InterruptedException {
        this.shardId = shardId;
        this.totalShards = totalShards;
        this.manager = manager;

        ThreadFactory normalTPNamedFactory =
                new ThreadFactoryBuilder()
                        .setNameFormat("MantaroShard-Executor[" + shardId + "/" + totalShards + "] Thread-%d")
                        .build();

        ThreadFactory commandTPNamedFactory =
                new ThreadFactoryBuilder()
                        .setNameFormat("MantaroShard-Command[" + shardId + "/" + totalShards + "] Thread-%d")
                        .build();

        threadPool = Executors.newCachedThreadPool(normalTPNamedFactory);
        commandPool = Executors.newCachedThreadPool(commandTPNamedFactory);

        log = LoggerFactory.getLogger("MantaroShard-" + shardId);
        mantaroListener = new MantaroListener(shardId, this);
        commandListener = new CommandListener(shardId, this);

        restartJDA(false);
    }

    public void restartJDA(boolean force) throws RateLimitedException, LoginException, InterruptedException {
        if(jda != null) {
            log.info("Attempting to drop shard #" + shardId);
            if(!force) prepareShutdown();
            jda.shutdown();
            log.info("Dropped shard #" + shardId);
        }

        JDABuilder jdaBuilder = new JDABuilder(AccountType.BOT)
                .setToken(config().get().token)
                .setEventManager(manager)
                //.setAudioSendFactory(new NativeAudioSendFactory())
                .setAutoReconnect(true)
                .setCorePoolSize(15)
                .setHttpClientBuilder(
                        new OkHttpClient.Builder()
                                .connectTimeout(30, TimeUnit.SECONDS)
                                .readTimeout(30, TimeUnit.SECONDS)
                                .writeTimeout(30, TimeUnit.SECONDS)
                )
                .setGame(Game.of("Hold on to your seatbelts!"));
        if(totalShards > 1)
            jdaBuilder.useSharding(shardId, totalShards);

        jda = jdaBuilder.buildAsync();
        Thread.sleep(5000);
        readdListeners();
    }

    public void updateServerCount() {
        OkHttpClient httpClient = new OkHttpClient();
        Config config = config().get();

        String dbotsToken = config.dbotsToken;
        String dbotsorgToken = config.dbotsorgToken;

        if(dbotsToken != null || dbotsorgToken != null) {
            Async.task("Botlist API update Thread", () -> {
                int count = jda.getGuilds().size();

                try {
                    RequestBody body = RequestBody.create(
                            JSON,
                            new JSONObject().put("server_count", count).put("shard_id", getId()).put("shard_count", totalShards).toString()
                    );


                    if(dbotsToken != null) {
                        Request request = new Request.Builder()
                                .url("https://bots.discord.pw/api/bots/" + jda.getSelfUser().getId() + "/stats")
                                .addHeader("Authorization", dbotsToken)
                                .addHeader("Content-Type", "application/json")
                                .post(body)
                                .build();
                        httpClient.newCall(request).execute().close();
                    }

                    if(dbotsorgToken != null) {
                        Request request = new Request.Builder()
                                .url("https://discordbots.org/api/bots/" + jda.getSelfUser().getId() + "/stats")
                                .addHeader("Authorization", dbotsorgToken)
                                .addHeader("Content-Type", "application/json")
                                .post(body)
                                .build();
                        httpClient.newCall(request).execute().close();
                    }
                } catch(Exception ignored) {
                }
            }, 1, TimeUnit.HOURS);
        }

        Async.task(new Carbonitex(jda, getId(), getTotalShards()), 30, TimeUnit.MINUTES); //Carbon is special now.
    }

    public void updateStatus() {
        Runnable changeStatus = () -> {
            AtomicInteger users = new AtomicInteger(0), guilds = new AtomicInteger(0);
            if(MantaroBot.getInstance() != null) {
                Arrays.stream(MantaroBot.getInstance().getShardedMantaro().getShards()).map(MantaroShard::getJDA).forEach(jda -> {
                    users.addAndGet(jda.getUsers().size());
                    guilds.addAndGet(jda.getGuilds().size());
                });
            }
            String newStatus = random(SPLASHES.get(), RANDOM)
                    .replace("%ramgb%", String.valueOf(((long) (Runtime.getRuntime().maxMemory() * 1.2D)) >> 30L))
                    .replace("%usercount%", users.toString())
                    .replace("%guildcount%", guilds.toString())
                    .replace("%shardcount%", String.valueOf(getTotalShards()))
                    .replace("%prettyusercount%", pretty(users.get()))
                    .replace("%prettyguildcount%", pretty(guilds.get()));

            getJDA().getPresence().setGame(Game.of(config().get().prefix[0] + "help | " + newStatus + " | [" + getId() + "]"));
            log.debug("Changed status to: " + newStatus);
        };

        changeStatus.run();
        Async.task("Splash Thread", changeStatus, 600, TimeUnit.SECONDS);
    }

    public MantaroEventManager getEventManager() {
        return manager;
    }

    public int getId() {
        return shardId;
    }

    public JDA getJDA() {
        return jda;
    }

    public int getTotalShards() {
        return totalShards;
    }

    public void prepareShutdown() {
        jda.removeEventListener(jda.getRegisteredListeners().toArray());
    }

    public void readdListeners() {
        jda.removeEventListener(mantaroListener, commandListener, VOICE_CHANNEL_LISTENER, InteractiveOperations.listener(), ReactionOperations.listener());
        jda.addEventListener(mantaroListener, commandListener, VOICE_CHANNEL_LISTENER, InteractiveOperations.listener(), ReactionOperations.listener());
    }

    @Override
    public String toString() {
        return "MantaroShard [" + (getId() + 1) + "/" + totalShards + " ]";
    }
}
