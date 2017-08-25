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
		if (value == null) return null;
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
