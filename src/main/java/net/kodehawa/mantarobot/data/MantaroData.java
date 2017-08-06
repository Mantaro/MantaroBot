package net.kodehawa.mantarobot.data;

import com.rethinkdb.net.Connection;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.redis.RedisCache;
import net.kodehawa.mantarobot.utils.data.ConnectionWatcherDataManager;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.rethinkdb.RethinkDB.r;

public class MantaroData {
    private static final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private static RedisCache cache;
    private static GsonDataManager<Config> config;
    private static Connection conn;
    private static ConnectionWatcherDataManager connectionWatcher;
    private static ManagedDatabase db;

    public static GsonDataManager<Config> config() {
        if(config == null) config = new GsonDataManager<>(Config.class, "config.json", Config::new);
        return config;
    }

    public static Connection conn() {
        Config c = config().get();
        if(conn == null)
            conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect();
        return conn;
    }

    public static ConnectionWatcherDataManager connectionWatcher() {
        if(connectionWatcher == null) {
            connectionWatcher = new ConnectionWatcherDataManager(MantaroBot.cwport);
        }
        return connectionWatcher;
    }

    public static ManagedDatabase db() {
        if(db == null) db = new ManagedDatabase(conn(), cache());
        return db;
    }

    public static RedisCache cache() {
        if(cache == null) {
            int port = config().get().redisPort;
            if(port == -1) {
                return cache = new RedisCache(null);
            }
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal(16);
            config.setMinIdle(1);
            cache = new RedisCache(new JedisPool(config, "localhost", config().get().redisPort));
        }
        return cache;
    }

    public static ScheduledExecutorService getExecutor() {
        return exec;
    }

    public static void queue(Callable<?> action) {
        exec.submit(action);
    }

    public static void queue(Runnable runnable) {
        exec.submit(runnable);
    }
}
