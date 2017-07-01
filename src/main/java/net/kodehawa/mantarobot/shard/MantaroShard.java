package net.kodehawa.mantarobot.shard;

import br.com.brjdevs.java.utils.async.Async;
import br.com.brjdevs.java.utils.holding.objects.Holder;
import lombok.Getter;
import lombok.experimental.Delegate;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.VoiceChannelListener;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.ReactionOperations;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.services.Carbonitex;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import okhttp3.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;
import static net.kodehawa.mantarobot.data.MantaroData.config;
import static net.kodehawa.mantarobot.utils.Utils.pretty;

public class MantaroShard implements JDA {
	public static final DataManager<List<String>> SPLASHES = new SimpleFileDataManager("assets/mantaro/texts/splashes.txt");
	public static final VoiceChannelListener VOICE_CHANNEL_LISTENER = new VoiceChannelListener();
	private static final Random RANDOM = new Random();

	static {
		if (SPLASHES.get().removeIf(s -> s == null || s.isEmpty())) SPLASHES.save();
	}

	@Getter
	public final MantaroEventManager manager;
	private final CommandListener commandListener;
	private final Logger log;
	private final MantaroListener mantaroListener;
	private final int shardId;
	private final int totalShards;
	@Delegate
	private JDA jda;
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	public MantaroShard(int shardId, int totalShards, MantaroEventManager manager) throws RateLimitedException, LoginException, InterruptedException {
		this.shardId = shardId;
		this.totalShards = totalShards;
		this.manager = manager;

		log = LoggerFactory.getLogger("MantaroShard-" + shardId);
		mantaroListener = new MantaroListener(shardId);
		commandListener = new CommandListener(shardId);

		restartJDA(false);
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
			.setEventManager(manager)
			//Keep this disabled until they fix the audio issues with it enabled (aka no audio coming out at all)
			//.setAudioSendFactory(new NativeAudioSendFactory())
			.setAutoReconnect(true)
			.setCorePoolSize(12)
			.setGame(Game.of("Hold on to your seatbelts!"));
		if (totalShards > 1)
			jdaBuilder.useSharding(shardId, totalShards);

		jda = jdaBuilder.buildBlocking();
		readdListeners();
	}

	public void updateServerCount() {
		OkHttpClient httpClient = new OkHttpClient();
		Config config = config().get();
		Holder<Integer> guildCount = new Holder<>(jda.getGuilds().size());

		String dbotsToken = config.dbotsToken;
		String dbotsorgToken = config.dbotsorgToken;

		if (dbotsToken != null || dbotsorgToken != null) {
			Async.task("Botlist API update Thread", () -> {
				int newC = jda.getGuilds().size();
				guildCount.accept(newC);

				try {
					RequestBody body = RequestBody.create(
							JSON,
							new JSONObject().put("server_count", guildCount).put("shard_id", getId()).put("shard_count", totalShards).toString()
					);


					if (dbotsToken != null) {
						Request request = new Request.Builder()
								.url("https://bots.discord.pw/api/bots/" + jda.getSelfUser().getId() + "/stats")
								.addHeader("Authorization", dbotsToken)
								.addHeader("Content-Type", "application/json")
								.post(body)
								.build();
						Response response = httpClient.newCall(request).execute();
						response.close();
					}

					if (dbotsorgToken != null) {
						Request request = new Request.Builder()
								.url("https://discordbots.org/api/bots/" + jda.getSelfUser().getId() + "/stats")
								.addHeader("Authorization", dbotsorgToken)
								.addHeader("Content-Type", "application/json")
								.post(body)
								.build();
						Response response = httpClient.newCall(request).execute();
						response.close();
					}
				} catch (Exception ignored) {}
			}, 1, TimeUnit.HOURS);
		}

		Async.task(new Carbonitex(jda, getId(), getTotalShards()), 30, TimeUnit.MINUTES); //Carbon is special now.
	}

	public void updateStatus() {
		Runnable changeStatus = () -> {
			AtomicInteger users = new AtomicInteger(0), guilds = new AtomicInteger(0);
			if (MantaroBot.getInstance() != null) {
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
		jda.removeEventListener(mantaroListener, commandListener, /*VOICE_CHANNEL_LISTENER,*/ InteractiveOperations.listener(), ReactionOperations.listener());
		jda.addEventListener(mantaroListener, commandListener, /*VOICE_CHANNEL_LISTENER,*/ InteractiveOperations.listener(), ReactionOperations.listener());
	}

	@Override
	public String toString() {
		return "MantaroShard [" + (getId() + 1) + "/" + totalShards + " ]";
	}
}
