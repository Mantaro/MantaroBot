package com.rethinkdb.serialization;

import java.util.Map;

public interface SerializationStrategy {
	Map<String, Object> serialize(Object object);
}
