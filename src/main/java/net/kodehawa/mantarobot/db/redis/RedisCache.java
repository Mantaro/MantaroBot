package net.kodehawa.mantarobot.db.redis;

import net.kodehawa.mantarobot.db.ManagedObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RedisCache {
    private final JedisPool pool;

    public RedisCache(JedisPool pool) {
        this.pool = pool;
    }

    public <T> T run(Function<Jedis, T> function) {
        if(pool == null) return null;
        try(Jedis jedis = pool.getResource()) {
            return function.apply(jedis);
        }
    }

    public void runNoReply(Consumer<Jedis> consumer) {
        if(pool == null) return;
        try(Jedis jedis = pool.getResource()) {
            consumer.accept(jedis);
        }
    }

    public void invalidate(String key) {
        runNoReply(j -> j.del(key));
    }

    @SuppressWarnings("unchecked")
    public <T extends ManagedObject> T get(String key) {
        if(pool == null) return null;
        return run(j -> (T) ManagedObject.fromBase64(j.get(key)));
    }

    @SuppressWarnings("unchecked")
    public <T extends ManagedObject> T getOrElse(String key, Supplier<T> supplier) {
        if(pool == null) return supplier.get();
        return run(j -> {
            String s = j.get(key);
            if(s == null || s.isEmpty()) {
                T t = supplier.get();
                j.set(key, t.toBase64());
                return t;
            }
            ManagedObject o = ManagedObject.fromBase64(s);
            return (T) o;
        });
    }

    public <T extends ManagedObject> void set(String key, T t) {
        if(pool == null) return;
        runNoReply(j -> j.set(key, t.toBase64()));
    }
}
