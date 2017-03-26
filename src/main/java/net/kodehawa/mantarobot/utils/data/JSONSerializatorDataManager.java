package net.kodehawa.mantarobot.utils.data;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import net.kodehawa.mantarobot.data.SerializedData;
import net.kodehawa.mantarobot.utils.FileIOUtils;
import net.kodehawa.mantarobot.utils.UnsafeUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JSONSerializatorDataManager implements DataManager<SerializedData> {
    private static final Logger LOGGER = LoggerFactory.getLogger("JSONSerializatorDataManager");

    private final Path path;
    private final KryoPool kryoPool;
    private final SerializedData data;
    private final JSONObject json;

    public JSONSerializatorDataManager(String file, KryoFactory factory) {
        this.path = Paths.get(file);
        File fl = path.toFile();
        if(!fl.exists()) {
            try {
                if(!fl.createNewFile()) {
                    UnsafeUtils.throwException(new IOException("Error creating file"));
                }
            } catch(IOException e) {
                UnsafeUtils.throwException(e);
            }
            json = new JSONObject();
        } else {
            JSONObject j;
            try {
                String contents = FileIOUtils.read(path);
                j = new JSONObject(contents);
            } catch(IOException e) {
                UnsafeUtils.throwException(e);
                j = null; //not really called
            }
            json = j;
        }
        kryoPool = new KryoPool.Builder(factory).build();
        data = new SerializedData(kryoPool, this::set, this::get);
    }

    public JSONSerializatorDataManager(String path) {
        this(path, Kryo::new);
    }

    @Override
    public void save() {
        try {
            FileIOUtils.write(path, json.toString(4));
        } catch(IOException e) {
            UnsafeUtils.throwException(e);
        }
    }

    @Override
    public SerializedData get() {
        return data;
    }

    public void set(String key, String value) {
        if(value == null) json.remove(key);
        else json.put(key, value);
    }

    public String get(String key) {
        return json.optString(key, null);
    }

    public <T> T run(KryoCallback<T> callback) {
        return kryoPool.run(callback);
    }
}
