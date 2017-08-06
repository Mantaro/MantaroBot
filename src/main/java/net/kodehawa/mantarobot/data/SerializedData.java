package net.kodehawa.mantarobot.data;

import com.esotericsoftware.kryo.pool.KryoPool;

import java.util.Base64;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.kodehawa.mantarobot.utils.KryoUtils.*;

public class SerializedData {
    private final Function<String, String> get;
    private final KryoPool kryoPool;
    private final BiConsumer<String, String> set;

    public SerializedData(KryoPool kryoPool, BiConsumer<String, String> set, Function<String, String> get) {
        this.kryoPool = kryoPool == null ? POOL : kryoPool;
        this.set = checkNotNull(set, "set");
        this.get = checkNotNull(get, "get");
    }

    public Object get(String key) {
        String value = get.apply(key);
        if(value == null) return null;
        return unserialize(kryoPool, Base64.getDecoder().decode(value));
    }

    public String getString(String key) {
        return get.apply(key);
    }

    public void set(String key, Object object) {
        set.accept(key, Base64.getEncoder().encodeToString(serialize(kryoPool, object)));
    }

    public void setString(String key, String value) {
        set.accept(key, value);
    }
}
