package net.kodehawa.mantarobot;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.rethinkdb.RethinkDB;
import frederikam.jca.JCA;
import frederikam.jca.JCABuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.kodehawa.mantarobot.commands.game.listener.GameListener;
import net.kodehawa.mantarobot.commands.moderation.TempBanManager;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.commands.music.listener.VoiceChannelListener;
import net.kodehawa.mantarobot.core.LoadState;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.DiscordLogBack;
import net.kodehawa.mantarobot.log.SimpleLogToSLF4JAdapter;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.utils.ThreadPoolHelper;
import net.kodehawa.mantarobot.utils.jda.ShardedJDA;
import org.apache.commons.collections4.iterators.ArrayIterator;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;

import static net.kodehawa.mantarobot.MantaroInfo.VERSION;
import static net.kodehawa.mantarobot.core.LoadState.*;

public class MantaroBot extends ShardedJDA {
	private static final Logger LOGGER = LoggerFactory.getLogger("MantaroBot");
	private static MantaroBot instance;
	public static JCA CLEVERBOT;
	private static TempBanManager tempBanManager;

	public static MantaroBot getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		try {
			instance = new MantaroBot();
		} catch (Exception e) {
			DiscordLogBack.disable();
			LOGGER.error("Could not complete Main Thread routine!", e);
			LOGGER.error("Cannot continue! Exiting program...");
			System.exit(-1);
		}
	}

	private MantaroAudioManager audioManager;
	private MantaroShard[] shards;
	private LoadState status = PRELOAD;
	private int totalShards;
	public MantaroEventManager manager;

	private MantaroBot() throws Exception {
		SimpleLogToSLF4JAdapter.install();
		LOGGER.info("MantaroBot starting...");

		Config config = MantaroData.config().get();

		Future<Set<Class<? extends Module>>> classesAsync = ThreadPoolHelper.defaultPool().getThreadPool()
			.submit(() -> new Reflections("net.kodehawa.mantarobot.commands").getSubTypesOf(Module.class));
		//CLEVERBOT = new JCABuilder().setUser(config.getCleverbotUser()).setKey(config.getCleverbotKey()).buildBlocking();

		totalShards = getRecommendedShards(config);
		shards = new MantaroShard[totalShards];
		status = LOADING;

		manager = new MantaroEventManager(totalShards);
		manager.register(InteractiveOperations.listener());
		new Thread(()->{
		    LOGGER.info("ShardWatcherThread started");
		    int timeout = MantaroData.config().get().shardWatcherTimeout;
		    int wait = MantaroData.config().get().shardWatcherWait;
		    while(true) {
		        try {
		            Thread.sleep(wait);
                    manager.checkShards(timeout);
                } catch(InterruptedException e) {
		            LOGGER.error("ShardWatcher interrupted, stopping...");
		            return;
                }
            }
        }, "ShardWatcherThread").start();
		for (int i = 0; i < totalShards; i++) {
			LOGGER.info("Starting shard #" + i + " of " + totalShards);
			shards[i] = new MantaroShard(i, totalShards, manager);
			LOGGER.debug("Finished loading shard #" + i + ".");
			if (i + 1 < totalShards) {
				LOGGER.info("Waiting for cooldown...");
				Thread.sleep(5000);
			}
		}

		/*Arrays.stream(shards).forEach(mantaroShard -> mantaroShard.getJDA()
			.addEventListener(new MantaroListener(), new CommandListener(), new VoiceChannelListener(),
					InteractiveOperations.listener(), new GameListener()));*/
		DiscordLogBack.enable();
		status = LOADED;
		LOGGER.info("[-=-=-=-=-=- MANTARO STARTED -=-=-=-=-=-]");
		LOGGER.info("Started bot instance.");
		LOGGER.info("Started MantaroBot " + VERSION + " on JDA " + JDAInfo.VERSION);
		audioManager = new MantaroAudioManager();
		tempBanManager = new TempBanManager(MantaroData.db().getMantaroData().getTempBans());

		LOGGER.info("Starting update managers.");
		Arrays.stream(shards).forEach(MantaroShard::updateServerCount);
		Arrays.stream(shards).forEach(MantaroShard::updateStatus);

		MantaroData.config().save();

		Set<Module> modules = new HashSet<>();
		for (Class<? extends Module> c : classesAsync.get()) {
			try {
				modules.add(c.newInstance());
			} catch (InstantiationException e) {
				LOGGER.error("Cannot initialize a command", e);
			} catch (IllegalAccessException e) {
				LOGGER.error("Cannot access a command class!", e);
			}
		}

		status = POSTLOAD;
		LOGGER.info("Finished loading basic components. Status is now set to POSTLOAD");
		LOGGER.info("Loaded " + Module.Manager.commands.size() + " commands in " + totalShards + " shards.");

        RethinkDB r = RethinkDB.r;
        //r.db("mantaro").tableCreate("players").run(MantaroData.conn());

		modules.forEach(Module::onPostLoad);
	}

	public MantaroShard getShard(int id) {
		return Arrays.stream(shards).filter(shard -> shard.getId() == id).findFirst().orElse(null);
	}

	@Override
	public int getShardAmount() {
		return totalShards;
	}

	@Override
	public Iterator<JDA> iterator() {
		return new ArrayIterator<>(shards);
	}

	public MantaroAudioManager getAudioManager() {
		return audioManager;
	}

	public int getId(JDA jda) {
		return jda.getShardInfo() == null ? 0 : jda.getShardInfo().getShardId();
	}

	public LoadState getLoadStatus() {
		return status;
	}

	private int getRecommendedShards(Config config) {
		try {
			HttpResponse<JsonNode> shards = Unirest.get("https://discordapp.com/api/gateway/bot")
				.header("Authorization", "Bot " + config.token)
				.header("Content-Type", "application/json")
				.asJson();
			return shards.getBody().getObject().getInt("shards");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 1;
	}

	public MantaroShard getShardBy(JDA jda) {
		if (jda.getShardInfo() == null) return shards[0];
		return Arrays.stream(shards).filter(shard -> shard.getId() == jda.getShardInfo().getShardId()).findFirst().orElse(null);
	}

	public MantaroShard getShardForGuild(String guildId) {
		return getShardForGuild(Long.parseLong(guildId));
	}

	public MantaroShard getShardForGuild(long guildId) {
		return getShard((int) ((guildId >> 22) % totalShards));
	}

	public List<MantaroShard> getShardList() {
		return Arrays.asList(shards);
	}

	public MantaroShard[] getShards() {
		return shards;
	}

	public TempBanManager getTempBanManager(){
		return tempBanManager;
	}
}
