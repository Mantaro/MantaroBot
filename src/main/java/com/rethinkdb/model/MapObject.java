package com.rethinkdb.model;

import java.util.HashMap;

public class MapObject<K, V> extends HashMap<K, V> {

	public MapObject() {
	}

	public MapObject with(K key, V value) {
		put(key, value);
		return this;
	}
}