package net.kodehawa.mantarobot;

import br.com.brjdevs.java.utils.Holder;
import com.mashape.unirest.http.Unirest;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Game;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.commands.music.VoiceChannelListener;
import net.kodehawa.mantarobot.core.LoadState;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.Data;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.DiscordLogBack;
import net.kodehawa.mantarobot.log.SimpleLogToSLF4JAdapter;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.utils.Async;
import net.kodehawa.mantarobot.utils.ThreadPoolHelper;
import org.json.JSONObject;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Future;

import static net.kodehawa.mantarobot.MantaroInfo.VERSION;
import static net.kodehawa.mantarobot.core.LoadState.*;

public class MantaroBot {
	private static final Logger LOGGER = LoggerFactory.getLogger("MantaroBot");
	private static MantaroAudioManager audioManager;
	private static JDA jda;
	private static LoadState status = PRELOAD;
	private static final RethinkDB database = RethinkDB.r;
	//TODO actual db
	//private static final Connection conn = database.connection().hostname("localhost").port(28015).connect();

	public static MantaroAudioManager getAudioManager() {
		return audioManager;
	}

	public static JDA getJDA() {
		return jda;
	}

	public static LoadState getStatus() {
		return status;
	}

	private static void init() throws Exception {
		SimpleLogToSLF4JAdapter.install();
		LOGGER.info("MantaroBot starting...");

		Config config = MantaroData.getConfig().get();

		Future<Set<Class<? extends Module>>> classesAsync = ThreadPoolHelper.defaultPool().getThreadPool()
			.submit(() -> new Reflections("net.kodehawa.mantarobot.commands").getSubTypesOf(Module.class));

		status = LOADING;
		jda = new JDABuilder(AccountType.BOT)
			.setToken(config.token)
			.addListener(new MantaroListener(), new VoiceChannelListener())
			.setAudioSendFactory(new NativeAudioSendFactory())
			.setAutoReconnect(true)
			.setGame(Game.of("Hold your seatbelts!"))
			.buildBlocking();
		DiscordLogBack.enable();
		status = LOADED;
		LOGGER.info("[-=-=-=-=-=- MANTARO STARTED -=-=-=-=-=-]");
		LOGGER.info("Started bot instance.");
		LOGGER.info("Started MantaroBot " + VERSION + " on JDA " + JDAInfo.VERSION);
		//LOGGER.info("Started RethinkDB on " + conn.hostname + " successfully.");
		Data data = MantaroData.getData().get();
		Random r = new Random();
		audioManager = new MantaroAudioManager();
		List<String> splashes = MantaroData.getSplashes().get();
		if (splashes.removeIf(s -> s == null || s.isEmpty())) MantaroData.getSplashes().save();

		Runnable changeStatus = () -> {
			String newStatus = splashes.get(r.nextInt(splashes.size()));
			jda.getPresence().setGame(Game.of(data.defaultPrefix + "help | " + newStatus));
			LOGGER.info("Changed status to: " + newStatus);
		};

		changeStatus.run();

		Async.startAsyncTask("Splash Thread", changeStatus, 600);

		Holder<Integer> guildCount = new Holder<>(jda.getGuilds().size());

		String dbotsToken = config.dbotsToken;
		String carbonToken = config.carbonToken;
		String dbotsorgToken = config.dbotsorgToken;

		if (dbotsToken != null) {
			Async.startAsyncTask("List API update Thread", () -> {
				int newC = jda.getGuilds().size();
				if (newC != guildCount.get()) {
					try {
					guildCount.accept(newC);
					//Unirest.post intensifies

					Unirest.post("https://bots.discord.pw/api/bots/" + jda.getSelfUser().getId() + "/stats")
						.header("Authorization", dbotsToken)
						.header("Content-Type", "application/json")
						.body(new JSONObject().put("server_count", newC).toString())
						.asJsonAsync();

					LOGGER.info("Successfully posted the botdata to carbonitex.com: " +
							Unirest.post("https://www.carbonitex.net/discord/data/botdata.php")
							.field("key", carbonToken)
							.field("servercount", newC)
							.asString().getBody());

					Unirest.post("https://discordbots.org/api/bots/" + jda.getSelfUser().getId() + "/stats")
							.header("Authorization", dbotsorgToken)
							.header("Content-Type", "application/json")
							.body(new JSONObject().put("server_count", newC).toString())
							.asJsonAsync();

					LOGGER.info("Updated discord lists Guild Count: " + newC + " guilds");
					} catch (Exception e) {
						LOGGER.error("An error occured while posting the botdata to discord lists (DBots/Carbonitex/DBots.org", e);
					}
				}
			}, 1800);
		}

		MantaroData.getConfig().save();

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
		LOGGER.info("Loaded " + Module.Manager.commands.size() + " commands");

		modules.forEach(Module::onPostLoad);
	}

	public static void main(String[] args) {
		try {
			init();
		} catch (Exception e) {
			DiscordLogBack.disable();
			LOGGER.error("Could not complete Main Thread Routine!", e);
			LOGGER.error("Cannot continue! Exiting program...");
			System.exit(-1);
		}
	}

	public static RethinkDB database(){
		return database;
	}

	/*public static Connection databaseConnection(){
		return conn;
	}*/
}
