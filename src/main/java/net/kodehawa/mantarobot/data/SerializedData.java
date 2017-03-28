package net.kodehawa.mantarobot.data;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SerializedData {
    private final KryoPool kryoPool;
    private final BiConsumer<String, String> set;
    private final Function<String, String> get;

    public SerializedData(KryoPool kryoPool, BiConsumer<String, String> set, Function<String, String> get) {
        this.kryoPool = kryoPool;
        this.set = set;
        this.get = get;
    }

    public void set(String key, Object object) {
        kryoPool.run((kryo)->{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output out = new Output(baos);
            kryo.writeClassAndObject(out, object);
            out.close();
            set.accept(key, Base64.getEncoder().encodeToString(baos.toByteArray()));
            return null;
        });
    }

    public void setString(String key, String value) {
        set.accept(key, value);
    }

    public Object get(String key) {
        String value = get.apply(key);
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
        return get.apply(key);
    }
}
