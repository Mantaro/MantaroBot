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

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ConnectionPoolSettings;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.codecs.MapCodecProvider;
import net.kodehawa.mantarobot.utils.ShutdownCodes;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MantaroData {
    private static final Logger log = LoggerFactory.getLogger(MantaroData.class);
    private static JsonDataManager<Config> config;
    private static ManagedDatabase db;
    private static MongoClient mongoClient;
    private static final CodecProvider pojoCodecProvider = PojoCodecProvider.builder()
            .automatic(true)
            .register(new MapCodecProvider())
            .conventions(Arrays.asList(Conventions.CLASS_AND_PROPERTY_CONVENTION, Conventions.ANNOTATION_CONVENTION, Conventions.OBJECT_ID_GENERATORS, Conventions.SET_PRIVATE_FIELDS_CONVENTION))
            .build();

    private static final CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));
    private static final JedisPool defaultJedisPool = new JedisPool(config().get().jedisPoolAddress, config().get().jedisPoolPort);

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
                try {
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
                } catch (Exception e) {
                    log.error("Cannot connect to database! Bailing out", e);
                    System.exit(ShutdownCodes.FATAL_FAILURE);
                }
            }
        }

        return mongoClient;
    }

    public static ManagedDatabase db() {
        if (db == null) {
            db = new ManagedDatabase(mongoConnection());
        }

        return db;
    }

    public static JedisPool getDefaultJedisPool() {
        return MantaroData.defaultJedisPool;
    }
}
