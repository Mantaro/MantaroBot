package net.kodehawa.mantarobot;

import br.com.brjdevs.java.utils.extensions.Async;
import br.com.brjdevs.java.utils.holding.Holder;
import com.mashape.unirest.http.Unirest;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import lombok.experimental.Delegate;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.kodehawa.mantarobot.commands.music.listener.VoiceChannelListener;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static br.com.brjdevs.java.utils.extensions.CollectionUtils.random;
import static net.kodehawa.mantarobot.data.MantaroData.config;

public class MantaroShard implements JDA {
	public static final DataManager<List<String>> SPLASHES = new SimpleFileDataManager("assets/mantaro/texts/splashes.txt");
	private static final Random RANDOM = new Random();

	static {
		if (SPLASHES.get().removeIf(s -> s == null || s.isEmpty())) SPLASHES.save();
	}

	public static String pretty(int number) {
		String ugly = Integer.toString(number);

		char[] almostPretty = new char[ugly.length()];

		Arrays.fill(almostPretty, '0');

		if ((almostPretty[0] = ugly.charAt(0)) == '-') almostPretty[1] = ugly.charAt(1);

		return new String(almostPretty);
	}

	public final MantaroEventManager manager;
	private final CommandListener commandListener;
	private final Logger log;
	private final MantaroListener mantaroListener;
	private final int shardId;
	private final int totalShards;
	private final VoiceChannelListener voiceChannelListener;
	@Delegate //I love Lombok so much
	private JDA jda;

	public MantaroShard(int shardId, int totalShards, MantaroEventManager manager) throws RateLimitedException, LoginException, InterruptedException {
		this.shardId = shardId;
		this.totalShards = totalShards;
		this.manager = manager;

		log = LoggerFactory.getLogger("MantaroShard-" + shardId);
		mantaroListener = new MantaroListener(shardId);
		commandListener = new CommandListener(shardId);
		voiceChannelListener = new VoiceChannelListener(shardId);

		restartJDA(false);
	}

	@Override
	public String toString() {
		return "MantaroShard [" + getId() + "/" + totalShards + " ]";
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
		jda.removeEventListener(mantaroListener, commandListener, voiceChannelListener, InteractiveOperations.listener());
		jda.addEventListener(mantaroListener, commandListener, voiceChannelListener, InteractiveOperations.listener());
	}

	public void restartJDA(boolean force) throws RateLimitedException, LoginException, InterruptedException {
		if (jda != null) {
			log.info("Attempting to drop shard #" + shardId);
			if (!force) prepareShutdown();
			jda.shutdown(false);
			log.info("Dropped shard #" + shardId);
		}

		JDABuilder jdaBuilder = new JDABuilder(AccountType.BOT)
			.setToken(config().get().token)
			//.setAudioSendFactory(new NativeAudioSendFactory())
			.setEventManager(manager)
			.setAutoReconnect(true)
			.setCorePoolSize(5)
			.setGame(Game.of("Hold on to your seatbelts!"));
		if (totalShards > 1)
			jdaBuilder.useSharding(shardId, totalShards);

		jda = jdaBuilder.buildBlocking();
		readdListeners();
	}

	public void updateServerCount() {
		Config config = config().get();
		Holder<Integer> guildCount = new Holder<>(jda.getGuilds().size());

		String dbotsToken = config.dbotsToken;
		String carbonToken = config.carbonToken;
		String dbotsorgToken = config.dbotsorgToken;

		if (dbotsToken != null || carbonToken != null || dbotsorgToken != null) {
			Async.task("Botlist API update Thread", () -> {
				int newC = jda.getGuilds().size();
				try {
					guildCount.accept(newC);
					//Unirest.post intensifies

					if (dbotsToken != null) {
						log.debug("Successfully posted the botdata to bots.discord.pw: " + Unirest.post("https://bots.discord.pw/api/bots/" + jda.getSelfUser().getId() + "/stats")
							.header("Authorization", dbotsToken)
							.header("Content-Type", "application/json")
							.body(new JSONObject().put("server_count", newC).put("shard_id", getId()).put("shard_count", totalShards).toString())
							.asString().getBody());
					}

					if (carbonToken != null) {
						log.debug("Successfully posted the botdata to carbonitex.com: " +
							Unirest.post("https://www.carbonitex.net/discord/data/botdata.php")
								.field("key", carbonToken)
								.field("servercount", newC)
								.field("shardid", getId())
								.field("shardcount", totalShards)
								.asString().getBody());
					}

					if (dbotsorgToken != null) {
						log.debug("Successfully posted the botdata to discordbots.org: " +
							Unirest.post("https://discordbots.org/api/bots/" + jda.getSelfUser().getId() + "/stats")
								.header("Authorization", dbotsorgToken)
								.header("Content-Type", "application/json")
								.body(new JSONObject().put("server_count", newC).put("shard_id", getId()).put("shard_count", totalShards).toString())
								.asString().getBody());
					}
				} catch (Exception e) {
					log.warn("An error occured while posting the botdata to discord lists (DBots/Carbonitex/DBots.org)", e);
				}
			}, 3600);
		}
	}

	void updateStatus() {
		Runnable changeStatus = () -> {
			AtomicInteger users = new AtomicInteger(0), guilds = new AtomicInteger(0);
			if (MantaroBot.getInstance() != null) {
				Arrays.stream(MantaroBot.getInstance().getShards()).map(MantaroShard::getJDA).forEach(jda -> {
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

			getJDA().getPresence().setGame(Game.of(config().get().prefix + "help | " + newStatus + " | [" + getId() + "]"));
			log.debug("Changed status to: " + newStatus);
		};

		changeStatus.run();
		Async.task("Splash Thread", changeStatus, 600);
	}
}
