package com.rethinkdb.serialization;

import java.util.Map;

public interface UnserializationStrategy {
	Object unserialize(Map<String, Object> object);
}
