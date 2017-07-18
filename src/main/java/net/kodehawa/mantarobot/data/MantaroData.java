package net.kodehawa.mantarobot.data;

import com.rethinkdb.net.Connection;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.utils.data.ConnectionWatcherDataManager;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.rethinkdb.RethinkDB.r;

public class MantaroData {
    private static GsonDataManager<Config> config;
    private static Connection conn;
    private static ConnectionWatcherDataManager connectionWatcher;
    private static ManagedDatabase db;
    private static ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

    public static GsonDataManager<Config> config() {
        if(config == null) config = new GsonDataManager<>(Config.class, "config.json", Config::new);
        return config;
    }

    public static Connection conn() {
        Config c = config().get();
        if(conn == null) conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).connect();
        return conn;
    }

    public static ConnectionWatcherDataManager connectionWatcher() {
        if(connectionWatcher == null) {
            connectionWatcher = new ConnectionWatcherDataManager(MantaroBot.cwport);
        }
        return connectionWatcher;
    }

    public static ManagedDatabase db() {
        if(db == null) db = new ManagedDatabase(conn());
        return db;
    }

    public static ScheduledExecutorService getExecutor() {
        return exec;
    }

    public static void queue(Callable<?> action) {
        MantaroBot.getInstance().getStatsClient().increment("database_hits");
        getExecutor().submit(action);
    }

    public static void queue(Runnable runnable) {
        MantaroBot.getInstance().getStatsClient().increment("database_hits");
        getExecutor().submit(runnable);
    }
}
