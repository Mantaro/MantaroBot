package net.kodehawa.mantarobot.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Supplier;

public class Mapifier {
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

    public enum Mode {
        SOFT(ObjectMapper::new),
        RAW(() -> {
            ObjectMapper m = new ObjectMapper();
            m.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
            m.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
            return m;
        });

        private final ObjectMapper mapper;

        Mode(Supplier<ObjectMapper> mapper) {
            this.mapper = mapper.get();
        }
    }
}