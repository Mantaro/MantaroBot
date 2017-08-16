package net.kodehawa.mantarobot.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.kodehawa.mantarobot.utils.data.deserialize.StringLongPairDeserializator;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Supplier;

public class Mapifier {
	public enum Mode {
		SOFT(() -> {
			ObjectMapper m = new ObjectMapper();
			SimpleModule pair = new SimpleModule("Pair", new Version(1, 0, 0, null, null, null));
			pair.addDeserializer(Pair.class, new StringLongPairDeserializator());
			m.registerModule(pair);
			return m;
		}),
		RAW(() -> {
			ObjectMapper m = new ObjectMapper();
			m.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
			m.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
			SimpleModule pair = new SimpleModule("Pair", new Version(1, 0, 0, null, null, null));
			pair.addDeserializer(Pair.class, new StringLongPairDeserializator());
			m.registerModule(pair);
			return m;
		});

		private final ObjectMapper mapper;

		Mode(Supplier<ObjectMapper> mapper) {
			this.mapper = mapper.get();
		}
	}

	private static Logger logger = LoggerFactory.getLogger("Mapifier");

	public static <T> T fromMap(Mode mode, Class<T> c, Map<String, Object> map) {
		return mode.mapper.convertValue(map, c);
	}

	public static <T> T fromMap(Class<T> c, Map<String, Object> map) {
		return fromMap(Mode.SOFT, c, map);
	}

	public static Map<String, Object> toMap(Object object) {
		return toMap(Mode.SOFT, object);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> toMap(Mode mode, Object object) {
		return (Map<String, Object>) mode.mapper.convertValue(object, Map.class);
	}
}