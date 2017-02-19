package net.kodehawa.mantarobot;

import com.mashape.unirest.http.Unirest;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Game;
import net.kodehawa.mantarobot.commands.audio.MantaroAudioManager;
import net.kodehawa.mantarobot.commands.custom.Holder;
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
import static net.kodehawa.mantarobot.commands.audio.MantaroAudioManager.closeConnection;
import static net.kodehawa.mantarobot.core.LoadState.*;

public class MantaroBot {
	private static final Logger LOGGER = LoggerFactory.getLogger("MantaroBot");
	private static JDA jda;
	private static LoadState status = PRELOAD;

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
			.addListener(new MantaroListener())
			//.setAudioSendFactory(new NativeAudioSendFactory())
			.setAutoReconnect(true)
			.setGame(Game.of("Hold your seatbelts!"))
			.buildBlocking();
		DiscordLogBack.enable();
		status = LOADED;
		LOGGER.info("[-=-=-=-=-=- MANTARO STARTED -=-=-=-=-=-]");
		LOGGER.info("Started bot instance.");
		LOGGER.info("Started MantaroBot " + VERSION + " on JDA " + JDAInfo.VERSION);

		Data data = MantaroData.getData().get();
		Random r = new Random();

		List<String> splashes = MantaroData.getSplashes().get();
		if (splashes.removeIf(s -> s == null || s.isEmpty())) MantaroData.getSplashes().update();

		Runnable changeStatus = () -> {
			String newStatus = splashes.get(r.nextInt(splashes.size()));
			jda.getPresence().setGame(Game.of(data.defaultPrefix + "help | " + newStatus));
			LOGGER.info("Changed status to: " + newStatus);
		};

		changeStatus.run();

		Async.startAsyncTask("Splash Thread", changeStatus, 600);

		Holder<Integer> guildCount = new Holder<>(jda.getGuilds().size());
		Async.startAsyncTask("DBots Thread", () -> {
			int newC = jda.getGuilds().size();
			if (newC != guildCount.get()) {
				guildCount.accept(newC);

				Unirest.post("https://bots.discord.pw/api/bots/" + jda.getSelfUser().getId() + "/stats")
					.header("Authorization", MantaroData.getConfig().get().dbotsToken)
					.header("Content-Type", "application/json")
					.body(new JSONObject().put("server_count", newC).toString())
					.asJsonAsync();

				LOGGER.info("Updated DBots Guild Count: " + newC + " guilds");
			}
		}, 3600);

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

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			MantaroData.getData().update();
			MantaroBot.getJDA().getRegisteredListeners().forEach(listener -> MantaroBot.getJDA().removeEventListener(listener));
			MantaroAudioManager.getMusicManagers().forEach((s, musicManager) -> {
				if(musicManager.getScheduler().getPlayer() != null)
					musicManager.getScheduler().getPlayer().getPlayingTrack().stop();
				musicManager.getScheduler().getQueue().clear();
				closeConnection(
						musicManager, musicManager.getScheduler().channel().getGuild().getAudioManager(), musicManager.getScheduler().channel()
				);
			});

			MantaroBot.getJDA().getTextChannelById("266231083341840385")
					.sendMessage("<@155867458203287552> Something made mantaro finish unexpectedly.").queue();
			MantaroBot.getJDA().shutdown();
		}));
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
}
