package net.kodehawa.mantarobot.utils.data;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import net.kodehawa.mantarobot.data.JedisData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Closeable;
import java.util.function.Consumer;

public class JedisSerializatorDataManager implements Closeable, DataManager<JedisData> {
    private final JedisPool jedisPool;
    private final KryoPool kryoPool;
    private final JedisData data;

    public JedisSerializatorDataManager(String host, int port, JedisPoolConfig config, KryoFactory factory) {
        jedisPool = new JedisPool(config, host, port);
        kryoPool = new KryoPool.Builder(factory).build();
        data = new JedisData(jedisPool, kryoPool);
    }

    public JedisSerializatorDataManager(String host, int port, JedisPoolConfig config) {
        this(host, port, config, Kryo::new);
    }

    public JedisSerializatorDataManager(String host, int port, KryoFactory factory) {
        this(host, port, new JedisPoolConfig(), factory);
    }

    public JedisSerializatorDataManager(String host, int port) {
        this(host, port, new JedisPoolConfig(), Kryo::new);
    }

    @Override
    public void close() {
        jedisPool.destroy();
    }

    @Override
    public void save() {
        run(Jedis::save);
    }

    @Override
    public JedisData get() {
        return data;
    }

    public <T> T run(KryoCallback<T> callback) {
        return kryoPool.run(callback);
    }

    public void run(Consumer<? super Jedis> consumer) {
        try(Jedis jedis = jedisPool.getResource()) {
            consumer.accept(jedis);
        }
    }

    public String get(String key) {
        try(Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }

    public String set(String key, String value) {
        try(Jedis jedis = jedisPool.getResource()) {
            return jedis.set(key, value);
        }
    }
}
