package net.kodehawa.mantarobot.data;

import com.rethinkdb.net.Connection;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.db.ManagedDatabase;
import net.kodehawa.mantarobot.utils.data.ConnectionWatcherDataManager;
import net.kodehawa.mantarobot.utils.data.CrossBotDataManager;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.rethinkdb.RethinkDB.r;

public class MantaroData {
	private static GsonDataManager<Config> config;
	private static Connection conn;
	private static CrossBotDataManager crossBot;
	private static ManagedDatabase db;
	private static ConnectionWatcherDataManager connectionWatcher;
	private static ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

	public static GsonDataManager<Config> config() {
		if (config == null) config = new GsonDataManager<>(Config.class, "config.json", Config::new);
		return config;
	}

	public static Connection conn() {
		Config c = config().get();
		if (conn == null) conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).connect();
		return conn;
	}

	public static CrossBotDataManager crossBot() {
		if (crossBot == null) {
			Config config = config().get();
			CrossBotDataManager.Builder builder;
			if (config.crossBotServer) {
				builder = new CrossBotDataManager.Builder(CrossBotDataManager.Builder.Type.SERVER);
			} else {
				builder = new CrossBotDataManager.Builder(CrossBotDataManager.Builder.Type.CLIENT).name("Mantaro").host(config.crossBotHost);
			}
			crossBot = builder
				.async(true, 10) //try to send everything on the queue every 10ms
				.port(config.crossBotPort)
				.build();
		}

		return crossBot;
	}

	public static ConnectionWatcherDataManager connectionWatcher() {
	    if(connectionWatcher == null) {
	        connectionWatcher = new ConnectionWatcherDataManager(MantaroBot.cwport);
        }
        return connectionWatcher;
    }

	public static ManagedDatabase db() {
		if (db == null) db = new ManagedDatabase(conn());
		return db;
	}

	public static ScheduledExecutorService getExecutor() {
		return exec;
	}

	public static void queue(Callable<?> action) {
		getExecutor().submit(action);
	}

	public static void queue(Runnable runnable) {
		getExecutor().submit(runnable);
	}
}
