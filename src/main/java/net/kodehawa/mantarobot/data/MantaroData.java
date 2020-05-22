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
import net.kodehawa.mantarobot.utils.Prometheus;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.rethinkdb.RethinkDB.r;

public class MantaroData {
    private static final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("MantaroData-Executor Thread-%d").build());
    private static final Logger log = LoggerFactory.getLogger(MantaroData.class);
    private static GsonDataManager<Config> config;
    private static Connection conn;
    private static ManagedDatabase db;

    private static final JedisPool defaultJedisPool = new JedisPool(config().get().jedisPoolAddress, config().get().jedisPoolPort);
    //private static final org.redisson.config.Config redissonConfig = new org.redisson.config.Config();
    //private static RedissonClient redisson;

    static {
        Prometheus.THREAD_POOL_COLLECTOR.add("mantaro-data", exec);
        //redissonConfig.useSingleServer().setAddress("redis://127.0.0.1:" + config().get().jedisPoolPort);
        //redisson = Redisson.create(redissonConfig);
    }

    public static GsonDataManager<Config> config() {
        if (config == null)
            config = new GsonDataManager<>(Config.class, "config.json", Config::new);

        return config;
    }

    public static Connection conn() {
        Config c = config().get();
        if (conn == null) {
            synchronized (MantaroData.class) {
                if (conn != null)
                    return conn;

                conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect();
                log.info("Established first database connection to {}:{} ({})", c.dbHost, c.dbPort, c.dbUser);
            }
        }
        return conn;
    }


    public static ManagedDatabase db() {
        if (db == null) {
            db = new ManagedDatabase(conn());
        }
        return db;
    }

    // public static RedissonClient redisson() {
    //     return redisson;
    // }

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
