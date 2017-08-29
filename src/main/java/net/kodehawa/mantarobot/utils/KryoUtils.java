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

package net.kodehawa.mantarobot.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class KryoUtils {
    public static final KryoPool POOL = new KryoPool.Builder(Kryo::new).build();

    private KryoUtils() {
    }

    public static <T> T copy(T t) {
        if(t == null) return null;
        return POOL.run(kryo -> kryo.copy(t));
    }

    public static byte[] serialize(Kryo kryo, Object obj) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output out = new Output(baos);
        checkNotNull(kryo, "kryo").writeClassAndObject(out, obj);
        out.close();
        return baos.toByteArray();
    }

    public static byte[] serialize(KryoPool pool, Object obj) {
        return checkNotNull(pool, "pool").run(kryo -> serialize(kryo, obj));
    }

    public static byte[] serialize(Object obj) {
        return serialize(POOL, obj);
    }

    public static Object unserialize(Kryo kryo, byte[] data) {
        Input in = new Input(new ByteArrayInputStream(checkNotNull(data, "data")));
        Object o = checkNotNull(kryo, "kryo").readClassAndObject(in);
        in.close();
        return o;
    }

    public static <T> T unserialize(Kryo kryo, byte[] data, Class<T> clazz) {
        return checkNotNull(clazz, "clazz").cast(unserialize(kryo, data));
    }

    public static Object unserialize(KryoPool pool, byte[] data) {
        return checkNotNull(pool, "pool").run(kryo -> unserialize(kryo, data));
    }

    public static <T> T unserialize(KryoPool pool, byte[] data, Class<T> clazz) {
        return checkNotNull(clazz, "clazz").cast(unserialize(pool, data));
    }

    public static Object unserialize(byte[] data) {
        return POOL.run(kryo -> unserialize(kryo, data));
    }

    public static <T> T unserialize(byte[] data, Class<T> clazz) {
        return checkNotNull(clazz, "clazz").cast(unserialize(POOL, data));
    }
}
