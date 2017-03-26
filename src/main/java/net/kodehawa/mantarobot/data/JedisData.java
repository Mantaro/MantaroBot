package net.kodehawa.mantarobot.data;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class JedisData {
    private final JedisPool jedisPool;
    private final KryoPool kryoPool;

    public JedisData(JedisPool jedisPool, KryoPool kryoPool) {
        this.jedisPool = jedisPool;
        this.kryoPool = kryoPool;
    }

    public void set(String key, Object object) {
        kryoPool.run((kryo)->{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output out = new Output(baos);
            kryo.writeClassAndObject(out, object);
            out.close();
            try(Jedis jedis = jedisPool.getResource()) {
                jedis.set(key, Base64.getEncoder().encodeToString(baos.toByteArray()));
            }
            return null;
        });
    }

    public String setString(String key, String value) {
        try(Jedis jedis = jedisPool.getResource()) {
            return jedis.set(key, value);
        }
    }

    public Object get(String key) {
        String value;
        try(Jedis jedis = jedisPool.getResource()) {
            value = jedis.get(key);
        }
        if(value == null) return null;
        return kryoPool.run((kryo)->{
            ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(value));
            Input in = new Input(bais);
            Object o = kryo.readClassAndObject(in);
            in.close();
            return o;
        });
    }

    public String getString(String key) {
        try(Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }
}
