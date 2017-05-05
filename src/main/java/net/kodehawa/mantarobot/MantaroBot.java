package net.kodehawa.mantarobot;

import br.com.brjdevs.java.utils.extensions.Async;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Guild;
import net.kodehawa.mantarobot.commands.moderation.TempBanManager;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.commands.music.listener.VoiceLeaveTimer;
import net.kodehawa.mantarobot.core.CommandProcessor;
import net.kodehawa.mantarobot.core.LoadState;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.ShardMonitorEvent;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.DiscordLogBack;
import net.kodehawa.mantarobot.log.SimpleLogToSLF4JAdapter;
import net.kodehawa.mantarobot.modules.Event;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.events.EventDispatcher;
import net.kodehawa.mantarobot.modules.events.PostLoadEvent;
import net.kodehawa.mantarobot.utils.CompactPrintStream;
import net.kodehawa.mantarobot.utils.jda.ShardedJDA;
import org.apache.commons.collections4.iterators.ArrayIterator;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.MantaroInfo.VERSION;
import static net.kodehawa.mantarobot.core.LoadState.*;

@Slf4j
public class MantaroBot extends ShardedJDA {
	public static int cwport;
	@Getter
	private static MantaroBot instance;
	@Getter
	public static LoadState loadState = PRELOAD;
	@Getter
	private static TempBanManager tempBanManager;
	public static void main(String[] args) {
		if (System.getProperty("mantaro.verbose", null) != null) {
			System.setOut(new CompactPrintStream(System.out));
			System.setErr(new CompactPrintStream(System.err));
		}

		if (args.length > 0) {
			try {
				cwport = Integer.parseInt(args[0]);
			} catch (Exception e) {
				log.info("Invalid connection watcher port specified in arguments, using value in config");
				cwport = MantaroData.config().get().connectionWatcherPort;
			}
		} else {
			log.info("No connection watcher port specified, using value in config");
			cwport = MantaroData.config().get().connectionWatcherPort;
		}

		log.info("Using port " + cwport + " to communicate with connection watcher");

		if (cwport > 0) {
			new Thread(() -> {
				try {
					MantaroData.connectionWatcher();
				} catch (Exception e) {
					log.error("Error connecting to Connection Watcher", e);
				}
			});
		}

		try {
			new MantaroBot();
		} catch (Exception e) {
			DiscordLogBack.disable();
			log.error("Could not complete Main Thread routine!", e);
			log.error("Cannot continue! Exiting program...");
			System.exit(-1);
		}
	}

	private static int getRecommendedShards(String token) {
		try {
			HttpResponse<JsonNode> shards = Unirest.get("https://discordapp.com/api/gateway/bot")
				.header("Authorization", "Bot " + token)
				.header("Content-Type", "application/json")
				.asJson();
			return shards.getBody().getObject().getInt("shards");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 1;
	}

	@Getter
	private MantaroAudioManager audioManager;
	private List<MantaroEventManager> managers = new ArrayList<>();
	@Getter
	private MantaroShard[] shards;
	private int totalShards;
	@Getter
	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
	private MantaroBot() throws Exception {
		instance = this;

		SimpleLogToSLF4JAdapter.install();
		log.info("MantaroBot starting...");

		Config config = MantaroData.config().get();

		//Let's see if we find a class.
		Future<Set<Class<?>>> classes = Async.future("Classes Lookup", () ->
			new Reflections(
				"net.kodehawa.mantarobot.commands",
				new MethodAnnotationsScanner(),
				new TypeAnnotationsScanner(),
				new SubTypesScanner())
				.getTypesAnnotatedWith(Module.class)
		);

		totalShards = getRecommendedShards(config.token);
		shards = new MantaroShard[totalShards];
		loadState = LOADING;

		for (int i = 0; i < totalShards; i++) {
			log.info("Starting shard #" + i + " of " + totalShards);
			MantaroEventManager manager = new MantaroEventManager();
			managers.add(manager);
			shards[i] = new MantaroShard(i, totalShards, manager);
			log.debug("Finished loading shard #" + i + ".");
			if (i + 1 < totalShards) {
				log.info("Waiting for cooldown...");
				Thread.sleep(5000);
			}
		}

		new Thread(() -> {
			log.info("ShardWatcherThread started");
			final int wait = MantaroData.config().get().shardWatcherWait;
			while (true) {
				try {
					Thread.sleep(wait);
					MantaroEventManager.getLog().info("Checking shards...");
					ShardMonitorEvent sme = new ShardMonitorEvent(totalShards);
					managers.forEach(manager -> manager.handle(sme));
					int[] dead = sme.getDeadShards();
					if (dead.length != 0) {
						MantaroEventManager.getLog().error("Dead shards found: {}", Arrays.toString(dead));
						Arrays.stream(dead).forEach(id -> getShard(id).readdListeners());
					} else {
						MantaroEventManager.getLog().info("No dead shards found");
					}
				} catch (InterruptedException e) {
					log.error("ShardWatcher interrupted, stopping...");
					return;
				}
			}
		}, "ShardWatcherThread").start();

		DiscordLogBack.enable();
		loadState = LOADED;
		log.info("[-=-=-=-=-=- MANTARO STARTED -=-=-=-=-=-]");
		log.info("Started bot instance.");
		log.info("Started MantaroBot " + VERSION + " on JDA " + JDAInfo.VERSION);

		audioManager = new MantaroAudioManager();
		tempBanManager = new TempBanManager(MantaroData.db().getMantaroData().getTempBans());

		log.info("Starting update managers.");
		Arrays.stream(shards).forEach(MantaroShard::updateServerCount);
		Arrays.stream(shards).forEach(MantaroShard::updateStatus);

		MantaroData.config().save();

		Set<Method> events = new Reflections(
			classes.get(),
			new MethodAnnotationsScanner())
			.getMethodsAnnotatedWith(Event.class);

		EventDispatcher.dispatch(events, CommandProcessor.REGISTRY);

		loadState = POSTLOAD;
		log.info("Finished loading basic components. Status is now set to POSTLOAD");

		EventDispatcher.dispatch(events, new PostLoadEvent());

		log.info("Loaded " + CommandProcessor.REGISTRY.commands().size() + " commands in " + totalShards + " shards.");

		//Free Instances
		EventDispatcher.instances.clear();

		executorService.scheduleWithFixedDelay(new VoiceLeaveTimer(), 1, 3, TimeUnit.MINUTES);
	}

	public Guild getGuildById(String guildId) {
		return getShardForGuild(guildId).getGuildById(guildId);
	}

	public MantaroShard getShard(int id) {
		return Arrays.stream(shards).filter(shard -> shard.getId() == id).findFirst().orElse(null);
	}

	@Override
	public int getShardAmount() {
		return totalShards;
	}

	@Nonnull
	@Override
	public Iterator<JDA> iterator() {
		return new ArrayIterator<>(shards);
	}

	public int getId(JDA jda) {
		return jda.getShardInfo() == null ? 0 : jda.getShardInfo().getShardId();
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
}
