/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.data;

import com.rethinkdb.net.Connection;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.redis.RedisCachedDatabase;
import net.kodehawa.mantarobot.utils.data.ConnectionWatcherDataManager;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.rethinkdb.RethinkDB.r;

@Slf4j
public class MantaroData {
    private static final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private static GsonDataManager<Config> config;
    private static Connection conn;
    private static ConnectionWatcherDataManager connectionWatcher;
    private static ManagedDatabase db;
    private static RedissonClient redisson;

    public static GsonDataManager<Config> config() {
        if(config == null) config = new GsonDataManager<>(Config.class, "config.json", Config::new);
        return config;
    }

    public static Connection conn() {
        Config c = config().get();
        if(conn == null) {
            conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).user(c.dbUser, c.dbPassword).connect();
            log.info("Establishing first database connection to {}:{} ({})...", c.dbHost, c.dbPort, c.dbUser);
        }
        return conn;
    }

    public static RedissonClient redisson() {
        if(redisson == null) {
            Config.RedisInfo i = config().get().redis;
            if(i.enabled) {
                org.redisson.config.Config cfg = new org.redisson.config.Config();
                cfg.useSingleServer().setAddress("redis://" + i.host + ":" + i.port);
                redisson = Redisson.create(cfg);
            }
        }
        return redisson;
    }

    public static ConnectionWatcherDataManager connectionWatcher() {
        if(connectionWatcher == null) {
            connectionWatcher = new ConnectionWatcherDataManager(MantaroBot.cwport);
        }
        return connectionWatcher;
    }

    public static ManagedDatabase db() {
        if(db == null) {
            Config.RedisInfo i = config().get().redis;
            if(i.enabled) {
                RedissonClient client = redisson();

                db = new RedisCachedDatabase(conn(),
                        client.getMap("custom-commands"),
                        client.getMap("guilds"),
                        client.getMap("players"),
                        client.getMap("users"),
                        client.getMap("premium-keys"),
                        client.getBucket("mantaro")
                );
            } else {
                db = new ManagedDatabase(conn());
            }
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
}
