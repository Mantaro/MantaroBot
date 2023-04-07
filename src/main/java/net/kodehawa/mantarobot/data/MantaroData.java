/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.data;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ConnectionPoolSettings;
import com.rethinkdb.net.Connection;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import net.kodehawa.mantarobot.utils.exporters.Metrics;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.mongodb.MongoClientSettings.builder;
import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static com.rethinkdb.RethinkDB.r;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MantaroData {
    private static final Logger log = LoggerFactory.getLogger(MantaroData.class);
    private static final ScheduledExecutorService exec = Executors.newScheduledThreadPool(
            1, new ThreadFactoryBuilder().setNameFormat("MantaroData-Executor Thread-%d").build()
    );

    private static JsonDataManager<Config> config;
    private static Connection connection;
    private static ManagedDatabase db;
    private static MongoClient mongoClient;
    private static final CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
    private static final CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));
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

    public static MongoClient mongoConnection() {
        var config = config().get();
        if (mongoClient == null) {
            synchronized (MantaroData.class) {
                ConnectionString connectionString = new ConnectionString(config.getMongoUri());
                ConnectionPoolSettings connectionPoolSettings = ConnectionPoolSettings.builder()
                        .minSize(2)
                        .maxSize(30)
                        .maxConnectionIdleTime(0, TimeUnit.MILLISECONDS)
                        .maxConnectionLifeTime(120, TimeUnit.SECONDS)
                        .build();

                MongoClientSettings clientSettings = MongoClientSettings.builder()
                        .applyConnectionString(connectionString)
                        .applyToConnectionPoolSettings(builder -> builder.applySettings(connectionPoolSettings))
                        .codecRegistry(pojoCodecRegistry)
                        .build();

                mongoClient = MongoClients.create(clientSettings);
                log.info("Established first MongoDB connection.");
            }
        }

        return mongoClient;
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

                log.info("Established first RethinkDB connection to {}:{} ({})",
                        config.getDbHost(), config.getDbPort(), config.getDbUser()
                );
            }
        }

        return connection;
    }

    public static ManagedDatabase db() {
        if (db == null) {
            db = new ManagedDatabase(conn(), mongoConnection());
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
