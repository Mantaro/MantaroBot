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
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.InterfacedEventManager;
import net.kodehawa.mantarobot.commands.game.listener.GameListener;
import net.kodehawa.mantarobot.commands.music.listener.VoiceChannelListener;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.List;
import java.util.Random;

import static br.com.brjdevs.java.utils.extensions.CollectionUtils.random;
import static net.kodehawa.mantarobot.data.MantaroData.config;

public class MantaroShard implements JDA {
	public static final DataManager<List<String>> SPLASHES = new SimpleFileDataManager("assets/mantaro/texts/splashes.txt");
	private static final Random RANDOM = new Random();

	static {
		if (SPLASHES.get().removeIf(s -> s == null || s.isEmpty())) SPLASHES.save();
	}

	private final Logger LOGGER;
	private final int shardId;
	private final int totalShards;
	@Delegate
	private JDA jda;

	public MantaroShard(int shardId, int totalShards) throws RateLimitedException, LoginException, InterruptedException {
		this.shardId = shardId;
		this.totalShards = totalShards;
		LOGGER = LoggerFactory.getLogger("MantaroShard-" + shardId);
		restartJDA();
	}

	@Override
	public String toString() {
		return "Shard [" + getId() + "/" + totalShards + " ]";
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
		jda.getRegisteredListeners().forEach(listener -> jda.removeEventListener(listener));
	}

	public void restartJDA() throws RateLimitedException, LoginException, InterruptedException {
		JDABuilder jdaBuilder = new JDABuilder(AccountType.BOT)
			.setToken(config().get().token)
			.setAudioSendFactory(new NativeAudioSendFactory())
			.setEventManager(new InterfacedEventManager() {
				@Override
				public void handle(Event event) {
					Async.thread("Async EventHandling", () -> super.handle(event));
				}
			})
			.setAutoReconnect(true)
			.setGame(Game.of("Hold on to your seatbelts!"));
		if (totalShards > 1)
			jdaBuilder.useSharding(shardId, totalShards);
		if (jda != null) {
			prepareShutdown();
			jda.shutdown(false);
		}
		jda = jdaBuilder.buildBlocking();
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

					try {
						if (dbotsToken != null) {
							LOGGER.debug("Successfully posted the botdata to bots.discord.pw: " + Unirest.post("https://bots.discord.pw/api/bots/" + jda.getSelfUser().getId() + "/stats")
								.header("Authorization", dbotsToken)
								.header("Content-Type", "application/json")
								.body(new JSONObject().put("server_count", newC).put("shard_id", getId()).put("shard_count", totalShards).toString())
								.asString().getBody());
						}

						if (carbonToken != null) {
							LOGGER.debug("Successfully posted the botdata to carbonitex.com: " +
								Unirest.post("https://www.carbonitex.net/discord/data/botdata.php")
									.field("key", carbonToken)
									.field("servercount", newC)
									.field("shardid", getId())
									.field("shardcount", totalShards)
									.asString().getBody());
						}

						if (dbotsorgToken != null) {
							LOGGER.debug("Successfully posted the botdata to discordbots.org: " +
								Unirest.post("https://discordbots.org/api/bots/" + jda.getSelfUser().getId() + "/stats")
									.header("Authorization", dbotsorgToken)
									.header("Content-Type", "application/json")
									.body(new JSONObject().put("server_count", newC).put("shard_id", getId()).put("shard_count", totalShards).toString())
									.asString().getBody());
						}

					} catch (Exception e) {
						LOGGER.error("An error occured while posting the botdata to discord lists (DBots/Carbonitex/DBots.org) - Shard " + getId(), e);
					}

					LOGGER.info("Updated discord lists guild count: " + newC + " guilds");
				} catch (Exception e) {
					LOGGER.warn("An error occured while posting the botdata to discord lists (DBots/Carbonitex/DBots.org)", e);
				}
			}, 3600);
		}
	}

	void updateStatus() {
		Runnable changeStatus = () -> {
			String newStatus = random(SPLASHES.get(), RANDOM);
			getJDA().getPresence().setGame(Game.of(config().get().prefix + "help | " + newStatus + " | [" + getId() + "]"));
			LOGGER.debug("Changed status to: " + newStatus);
		};

		changeStatus.run();
		Async.task("Splash Thread", changeStatus, 600);
	}

	public void readdListeners(){
		jda.addEventListener(new MantaroListener(), new VoiceChannelListener(), InteractiveOperations.listener(), new GameListener());
	}
}
