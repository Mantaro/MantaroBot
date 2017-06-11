package net.kodehawa.mantarobot;

import br.com.brjdevs.java.utils.async.Async;
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
import net.kodehawa.mantarobot.core.CommandProcessor;
import net.kodehawa.mantarobot.core.LoadState;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.ShardMonitorEvent;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.DiscordLogBack;
import net.kodehawa.mantarobot.log.SimpleLogToSLF4JAdapter;
import net.kodehawa.mantarobot.modules.Command;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.events.EventDispatcher;
import net.kodehawa.mantarobot.modules.events.PostLoadEvent;
import net.kodehawa.mantarobot.web.service.MantaroAPIChecker;
import net.kodehawa.mantarobot.web.service.MantaroAPISender;
import net.kodehawa.mantarobot.services.VoiceLeave;
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
import java.util.concurrent.*;

import static net.kodehawa.mantarobot.core.LoadState.*;

/**
 * <pre>Main class for MantaroBot.</pre>
 *
 * <pre>This class could be considered a wrapper and a main initializer if you would like.</pre>
 *
 * This class contains all the methods and variables necessary for the main component of the bot.
 * Mantaro is modular, which means you technically could add more modules to /commands without the necessity to even touch this class. This also means you can remove modules
 * without major problems.
 *
 * <pre>This class and most classes check for a status of {@link LoadState#POSTLOAD} to start doing any JDA-related work, to avoid stacktraces and unwanted results.</pre>
 *
 * A instance of this class contains most of the necessary wrappers to make a command and JDA lookups. (see ShardedJDA and UnifiedJDA). All shards come to an unifying point
 * in this class, meaning that doing {@link MantaroBot#getUserById(String)} is completely valid and so it will look for all users in all shards, without duplicates (distinct).
 *
 * After JDA startup, the internal {@link EventDispatcher} will attempt to dispatch {@link PostLoadEvent} to all the Module classes which contain a onPostLoad method, with a
 * {@link Command} annotation on it.
 *
 * Mantaro's version is determined, for now, on the data set in build.gradle and the date of build.
 *
 * This bot contains some mechanisms to prevent clones, such as some triggers to avoid bot start on incorrect settings, or just no timeout on database connection.
 * If you know about coding, I'm sure you could setup a instance of this bot without any problems and play around with it, but I would appreciate if you could keep all exact
 * or close clones of Mantaro outside of bot listing sites, since it will just get deleted from there (as in for clones of any other bot).
 * Thanks.
 *
 * <pr>This bot is a copyrighted work of Kodehawa and is the result a collaborative effort with AdrianTodt and many others,
 * This program is licensed under GPLv3, which summarized legal notice can be found down there.</pr>
 *
 * <pr>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.</pr>
 *
 * @see ShardedJDA
 * @see net.kodehawa.mantarobot.utils.jda.UnifiedJDA
 * @see Module
 * @since 16/08/2016
 * @author Kodehawa, AdrianTodt
 */
@Slf4j
public class MantaroBot extends ShardedJDA {

	private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();
	public static int cwport;
	@Getter
	public static LoadState loadState = PRELOAD;
	private static boolean DEBUG = false;
	@Getter
	private static MantaroBot instance;
	@Getter
	private static TempBanManager tempBanManager;
	@Getter
	private MantaroAPIChecker mantaroAPIChecker = new MantaroAPIChecker();

	public static void main(String[] args) {
		if (System.getProperty("mantaro.verbose") != null) {
			System.setOut(new CompactPrintStream(System.out));
			System.setErr(new CompactPrintStream(System.err));
		}

		if(System.getProperty("mantaro.debug") != null){
			DEBUG = true;
			System.out.println("Running in debug mode!");
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
	@Getter
	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
	private List<MantaroEventManager> managers = new ArrayList<>();
	@Getter
	private MantaroShard[] shards;
	private int totalShards;

	private MantaroBot() throws Exception {
		long start = System.currentTimeMillis();
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

		totalShards = DEBUG ? 2 : getRecommendedShards(config.token);
		shards = new MantaroShard[totalShards];
		loadState = LOADING;

		for (int i = 0; i < totalShards; i++) {
			log.info("Starting shard #" + i + " of " + totalShards);
			MantaroEventManager manager = new MantaroEventManager();
			managers.add(manager);
			shards[i] = new MantaroShard(i, totalShards, manager);
			log.debug("Finished loading shard #" + i + ".");
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
						for(int id : dead){
							try{
								FutureTask<Integer> restartJDA = new FutureTask<>(() -> {
									try {
										log.info("Starting automatic shard restart on shard {} due to it being inactive for longer than 2 minutes.", id);
										getShard(id).restartJDA(true);
										Thread.sleep(1000);
										return 1;
									} catch (Exception e) {
										log.warn("Cannot restart shard #{} <@155867458203287552> try to do it manually.", id);
										return 0;
									}
								});
								THREAD_POOL.execute(restartJDA);
								restartJDA.get(2, TimeUnit.MINUTES);
							}
							catch (Exception e){
								log.warn("Cannot restart shard #{} <@155867458203287552> try to do it manually.", id);
							}
						}
					}
					else {
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
		log.info("Started MantaroBot {} on JDA {}", MantaroInfo.VERSION, JDAInfo.VERSION);

		audioManager = new MantaroAudioManager();
		tempBanManager = new TempBanManager(MantaroData.db().getMantaroData().getTempBans());

		log.info("Starting update managers.");
			for(MantaroShard shard : shards) {
				shard.updateServerCount();
				shard.updateStatus();
			}

		MantaroData.config().save();

		Set<Method> events = new Reflections(
			classes.get(),
			new MethodAnnotationsScanner())
			.getMethodsAnnotatedWith(Command.class);

		EventDispatcher.dispatch(events, CommandProcessor.REGISTRY);

		loadState = POSTLOAD;
		log.info("Finished loading basic components. Status is now set to POSTLOAD");

		EventDispatcher.dispatch(events, new PostLoadEvent());

		log.info("Loaded " + CommandProcessor.REGISTRY.commands().size() + " commands in " + totalShards + " shards.");

		//Free Instances
		EventDispatcher.instances.clear();
		executorService.scheduleWithFixedDelay(new VoiceLeave(), 1, 3, TimeUnit.MINUTES);
		long end = System.currentTimeMillis();

		log.info("Succesfully started MantaroBot in {} seconds.", (end - start) / 1000);

		if(!MantaroData.config().get().isPremiumBot() || !MantaroData.config().get().isBeta()){
			mantaroAPIChecker.startService();
			MantaroAPISender.startService();
		}
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
