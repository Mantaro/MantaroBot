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

package net.kodehawa.mantarobot.utils.data;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import lombok.SneakyThrows;
import net.kodehawa.mantarobot.data.SerializedData;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JSONSerializatorDataManager implements DataManager<SerializedData> {
    private final SerializedData data;
    private final JSONObject json;
    private final KryoPool kryoPool;
    private final Path path;

    @SneakyThrows
    public JSONSerializatorDataManager(String file, KryoFactory factory) {
        this.path = Paths.get(file);
        File fl = path.toFile();
        if(!fl.exists()) {
            if(!fl.createNewFile()) throw new IOException("Error creating file");
            json = new JSONObject();
        } else {
            json = new JSONObject(FileIOUtils.read(path));
        }

        kryoPool = new KryoPool.Builder(factory).build();
        data = new SerializedData(kryoPool, this::set, this::get);
    }

    public JSONSerializatorDataManager(String path) {
        this(path, Kryo::new);
    }

    @Override
    public SerializedData get() {
        return data;
    }

    @Override
    @SneakyThrows
    public void save() {
        FileIOUtils.write(path, json.toString(4));
    }

    public String get(String key) {
        return json.optString(key, null);
    }

    public <T> T run(KryoCallback<T> callback) {
        return kryoPool.run(callback);
    }

    public void set(String key, String value) {
        if(value == null) json.remove(key);
        else json.put(key, value);
    }
}
