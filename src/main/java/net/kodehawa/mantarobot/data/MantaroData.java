/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.data;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rethinkdb.net.Connection;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import net.kodehawa.mantarobot.utils.exporters.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.rethinkdb.RethinkDB.r;

public class MantaroData {
    private static final Logger log = LoggerFactory.getLogger(MantaroData.class);
    private static final ScheduledExecutorService exec = Executors.newScheduledThreadPool(
            1, new ThreadFactoryBuilder().setNameFormat("MantaroData-Executor Thread-%d").build()
    );

    private static JsonDataManager<Config> config;
    private static Connection connection;
    private static ManagedDatabase db;

    private static final JedisPool defaultJedisPool = new JedisPool(config().get().jedisPoolAddress, config().get().jedisPoolPort);

    static {
        Metrics.THREAD_POOL_COLLECTOR.add("mantaro-data", exec);
    }

    public static JsonDataManager<Config> config() {
        if (config == null) {
            config = new JsonDataManager<>(Config.class, "config.json", Config::new);
        }

        return config;
    }

    public static Connection conn() {
        var config = config().get();
        if (connection == null) {
            synchronized (MantaroData.class) {
                if (connection != null) {
                    return connection;
                }

                connection = r.connection()
                        .hostname(config.getDbHost())
                        .port(config.getDbPort())
                        .db(config.getDbDb())
                        .user(config.getDbUser(), config.getDbPassword())
                        .connect();

                log.info("Established first database connection to {}:{} ({})",
                        config.getDbHost(), config.getDbPort(), config.getDbUser()
                );
            }
        }

        return connection;
    }


    public static ManagedDatabase db() {
        if (db == null) {
            db = new ManagedDatabase(conn());
        }

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

    public static JedisPool getDefaultJedisPool() {
        return MantaroData.defaultJedisPool;
    }
}
