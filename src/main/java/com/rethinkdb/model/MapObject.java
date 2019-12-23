package com.rethinkdb.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapObject<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 1;
    
    public MapObject() {
    }
    
    public MapObject(Map<? extends K, ? extends V> m) {
        super(m);
    }
    
    @SafeVarargs
    public MapObject(Map<? extends K, ? extends V>... maps) {
        for(Map<? extends K, ? extends V> m : maps) putAll(m);
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
