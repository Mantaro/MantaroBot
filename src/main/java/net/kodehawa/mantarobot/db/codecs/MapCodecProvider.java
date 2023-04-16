package net.kodehawa.mantarobot.db.codecs;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;

import java.util.HashMap;
import java.util.Map;

public class MapCodecProvider implements PropertyCodecProvider {
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> Codec<T> get(final TypeWithTypeParameters<T> type, final PropertyCodecRegistry registry) {
        if (Map.class.isAssignableFrom(type.getType()) && type.getTypeParameters().size() == 2) {
            return new net.kodehawa.mantarobot.db.codecs.MapCodec(type.getType(), registry.get(type.getTypeParameters().get(0)), registry.get(type.getTypeParameters().get(1)));
        } else {
            return null;
        }
    }

    private static class MapCodec<T> implements Codec<Map<String, T>> {
        private final Class<Map<String, T>> encoderClass;
        private final Codec<T> codec;

        MapCodec(final Class<Map<String, T>> encoderClass, final Codec<T> codec) {
            this.encoderClass = encoderClass;
            this.codec = codec;
        }

        @Override
        public void encode(final BsonWriter writer, final Map<String, T> map, final EncoderContext encoderContext) {
            writer.writeStartDocument();
            for (final Map.Entry<String, T> entry : map.entrySet()) {
                writer.writeName(entry.getKey());
                if (entry.getValue() == null) {
                    writer.writeNull();
                } else {
                    codec.encode(writer, entry.getValue(), encoderContext);
                }
            }
            writer.writeEndDocument();
        }

        @Override
        public Map<String, T> decode(final BsonReader reader, final DecoderContext context) {
            reader.readStartDocument();
            Map<String, T> map = getInstance();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (reader.getCurrentBsonType() == BsonType.NULL) {
                    map.put(reader.readName(), null);
                    reader.readNull();
                } else {
                    map.put(reader.readName(), codec.decode(reader, context));
                }
            }
            reader.readEndDocument();
            return map;
        }

        @Override
        public Class<Map<String, T>> getEncoderClass() {
            return encoderClass;
        }

        private Map<String, T> getInstance() {
            if (encoderClass.isInterface()) {
                return new HashMap<>();
            }
            try {
                return encoderClass.getDeclaredConstructor().newInstance();
            } catch (final Exception e) {
                throw new CodecConfigurationException(e.getMessage(), e);
            }
        }
    }
}

