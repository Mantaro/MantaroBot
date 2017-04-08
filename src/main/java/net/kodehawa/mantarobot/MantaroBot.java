package net.kodehawa.mantarobot;

import br.com.brjdevs.java.utils.extensions.Async;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import frederikam.jca.JCA;
import frederikam.jca.JCABuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.kodehawa.mantarobot.commands.moderation.TempBanManager;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.core.LoadState;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.ShardMonitorEvent;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.DiscordLogBack;
import net.kodehawa.mantarobot.log.SimpleLogToSLF4JAdapter;
import net.kodehawa.mantarobot.modules.Module;
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
	public static final boolean DEBUG = System.getProperty("mantaro.debug", null) != null;

	private static final Logger LOGGER = LoggerFactory.getLogger("MantaroBot");
	public static JCA CLEVERBOT;
	public static int cwport;
	private static MantaroBot instance;
	private static LoadState status = PRELOAD;
	private static TempBanManager tempBanManager;

	public static MantaroBot getInstance() {
		return instance;
	}

	public static LoadState getLoadStatus() {
		return status;
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			try {
				cwport = Integer.parseInt(args[0]);
			} catch (Exception e) {
				LOGGER.info("Invalid connection watcher port specified in arguments, using value in config");
				cwport = MantaroData.config().get().connectionWatcherPort;
			}
		} else {
			LOGGER.info("No connection watcher port specified, using value in config");
			cwport = MantaroData.config().get().connectionWatcherPort;
		}
		LOGGER.info("Using port " + cwport + " to communicate with connection watcher");
		if (cwport > 0) {
			new Thread(() -> {
				try {
					MantaroData.connectionWatcher();
				} catch (Exception e) {
					LOGGER.error("Error connecting to Connection Watcher", e);
				}
			});
		}
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
	private List<MantaroEventManager> managers = new ArrayList<>();
	private MantaroShard[] shards;
	private int totalShards;

	private MantaroBot() throws Exception {
		SimpleLogToSLF4JAdapter.install();
		LOGGER.info("MantaroBot starting...");

		Config config = MantaroData.config().get();
		Future<Set<Class<? extends Module>>> classesAsync = Async.future(() -> new Reflections("net.kodehawa.mantarobot.commands").getSubTypesOf(Module.class));
		Async.thread("CleverBot Builder", () -> CLEVERBOT = new JCABuilder().setUser(config.getCleverbotUser()).setKey(config.getCleverbotKey()).buildBlocking());

		totalShards = getRecommendedShards(config);
		shards = new MantaroShard[totalShards];
		status = LOADING;

		for (int i = 0; i < totalShards; i++) {
			LOGGER.info("Starting shard #" + i + " of " + totalShards);
			MantaroEventManager manager = new MantaroEventManager();
			managers.add(manager);
			shards[i] = new MantaroShard(i, totalShards, manager);
			LOGGER.debug("Finished loading shard #" + i + ".");
			if (i + 1 < totalShards) {
				LOGGER.info("Waiting for cooldown...");
				Thread.sleep(5000);
			}
		}
		new Thread(() -> {
			LOGGER.info("ShardWatcherThread started");
			final int wait = MantaroData.config().get().shardWatcherWait;
			while (true) {
				try {
					Thread.sleep(wait);
					MantaroEventManager.LOGGER.info("Checking shards...");
					ShardMonitorEvent sme = new ShardMonitorEvent(totalShards);
					managers.forEach(manager -> manager.handleSync(sme));
					int[] dead = sme.getDeadShards();
					if (dead.length != 0) {
						MantaroEventManager.LOGGER.error("Dead shards found: " + Arrays.toString(dead));
						Arrays.stream(dead).forEach(id -> getShard(id).readdListeners());
					} else {
						MantaroEventManager.LOGGER.info("No dead shards found");
					}
				} catch (InterruptedException e) {
					LOGGER.error("ShardWatcher interrupted, stopping...");
					return;
				}
			}
		}, "ShardWatcherThread").start();

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
		modules.forEach(Module::onPostLoad);

		LOGGER.info("Loaded " + Module.Manager.commands.size() + " commands in " + totalShards + " shards.");
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

	private int getRecommendedShards(Config config) {
		if (DEBUG) return 2;
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

	public TempBanManager getTempBanManager() {
		return tempBanManager;
	}
}
