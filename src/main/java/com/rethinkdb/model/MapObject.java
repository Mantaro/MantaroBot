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

package com.rethinkdb.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapObject<K, V> extends LinkedHashMap<K, V> {
	public MapObject() {}

	public MapObject(Map<? extends K, ? extends V> m) {
		super(m);
	}

	@SafeVarargs
	public MapObject(Map<? extends K, ? extends V>... maps) {
		for (Map<? extends K, ? extends V> m : maps) putAll(m);
	}

	public MapObject(K key, V value) {
		put(key, value);
	}

	public MapObject(K key1, V value1, K key2, V value2) {
		this(key1, value1);
		put(key2, value2);
	}

	public MapObject(K key1, V value1, K key2, V value2, K key3, V value3) {
		this(key1, value1, key2, value2);
		put(key3, value3);
	}

	public Map<K, V> immutable() {
		return Collections.unmodifiableMap(this);
	}

	/**
	 * Associates the specified value with the specified key in this map.
	 * If the map previously contained a mapping for the key, the old
	 * value is replaced.
	 *
	 * @param key   key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 * @return itself for chaining
	 */
	public MapObject<K, V> with(K key, V value) {
		put(key, value);
		return this;
	}
}