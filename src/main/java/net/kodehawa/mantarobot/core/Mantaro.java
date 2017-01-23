package net.kodehawa.mantarobot.core;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.kodehawa.mantarobot.config.Config;
import net.kodehawa.mantarobot.listeners.BirthdayListener;
import net.kodehawa.mantarobot.listeners.CommandListener;
import net.kodehawa.mantarobot.listeners.LogListener;
import net.kodehawa.mantarobot.log.DiscordLogBack;
import net.kodehawa.mantarobot.log.SimpleLogToSLF4JAdapter;
import net.kodehawa.mantarobot.module.Loader;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.module.Parser;
import net.kodehawa.mantarobot.thread.ThreadPoolHelper;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public final class Mantaro {
	//Who is maintaining this?
	public static final String OWNER_ID = "155867458203287552";
	private static final Logger LOGGER = LoggerFactory.getLogger("Mantaro");
	//Bot data. Will be used in About command.
	//In that command it returns it as data[0] + data[1]. Will be displayed as 1.1.1a2-0001.26112016, for example.
	//The data after the dash is the hour (4 numbers) and the date.
	private static final String[] data = {"22012017", "1.2.0a1-0001"};
	private static final Parser parser = new Parser();
	public static Set<Class<? extends Module>> classes = null; //A Set of classes, which will be later on loaded on Loader.
	//Am I debugging this?
	public static boolean isDebugEnabled = false;
	//Gets in what OS the bot is running. Useful because my machine is running Windows 10, but the server is running Linux.
	private static String OS = System.getProperty("os.name").toLowerCase();
	private static Config cl;
	private static Game game = Game.of("~>help | " + "It's not a bug, it's a feature!");
	//JDA and Loader. We need this and they're extremely important.
	private static JDA jda;
	private static List<Runnable> runnables = new ArrayList<>();
	private static State status = State.PRELOAD;

	private static synchronized void addClasses() {
		Runnable classThr = () -> {
			//Adds all the Classes extending Module to the classes HashMap. They will be later loaded in Loader.
			Reflections reflections = new Reflections("net.kodehawa.mantarobot.cmd");
			classes = reflections.getSubTypesOf(Module.class);
		};
		ThreadPoolHelper.DEFAULT().startThread("Load", classThr);
	}

	public static Config getConfig() {
		return cl;
	}

	public static String getMetadata(String s) {
		int i = -1;
		if (s.equals("date")) {
			i = 0;
		}
		if (s.equals("build")) {
			i = 1;
		}

		return data[i];
	}

	public static Parser getParser() {
		return parser;
	}

	public static JDA getSelf() {
		return jda;
	}

	public static State getState() {
		return status;
	}

	public static boolean isUnix() {
		return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
	}

	public static boolean isWindows() {
		return (OS.contains("win"));
	}

	private static synchronized void loadClasses() {
		try {
			new Loader();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to initialize commands.", e);
		}
	}

	public static void main(String[] args) {
		SimpleLogToSLF4JAdapter.install();

		LOGGER.info("MantaroBot starting...");

		cl = Config.load();
		addClasses();

		String botToken = getConfig().values().get("token").toString();
		isDebugEnabled = (Boolean) getConfig().values().get("debug");

		try {
			status = State.LOADING;
			jda = new JDABuilder(AccountType.BOT)
				.setToken(botToken)
				.addListener(new CommandListener(), new LogListener(), new BirthdayListener())
				.setAudioSendFactory(new NativeAudioSendFactory())
				.setAutoReconnect(true)
				.setGame(game)
				.buildBlocking();
			DiscordLogBack.enable();
			status = State.LOADED;
			LOGGER.info("--------------------");
			LOGGER.info("Started bot instance.");
		} catch (LoginException | InterruptedException | RateLimitedException e) {
			e.printStackTrace();
			DiscordLogBack.disable();
			LOGGER.error("Cannot build JDA instance! This is normally very bad. Error: " + e.getCause());
			LOGGER.error("Exiting program...");
			System.exit(-1);
		}

		loadClasses();

		LOGGER.info("Started MantaroBot " + data[1] + " on JDA " + JDAInfo.VERSION);
	}

	public static void runScheduled() {
		runnables.remove(null);
		runnables.forEach(Runnable::run);
	}

	public static void schedule(Runnable runnable) {
		runnables.add(runnable);
	}

	public static void setStatus(State state) {
		status = state;
	}

}