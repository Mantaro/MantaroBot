package net.kodehawa.mantarobot.db.codecs;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.json.JsonReader;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MapCodec<K, T> implements Codec<Map<K, T>> {
    private final Class<Map<K, T>> encoderClass;
    private final Codec<K> keyCodec;
    private final Codec<T> valueCodec;

    MapCodec(final Class<Map<K, T>> encoderClass, final Codec<K> keyCodec, final Codec<T> valueCodec) {
        this.encoderClass = encoderClass;
        this.keyCodec = keyCodec;
        this.valueCodec = valueCodec;
    }

    @Override
    public void encode(final BsonWriter writer, final Map<K, T> map, final EncoderContext encoderContext) {
        try (var dummyWriter = new BsonDocumentWriter(new BsonDocument())) {
            dummyWriter.writeStartDocument();
            writer.writeStartDocument();
            for (final Map.Entry<K, T> entry : map.entrySet()) {
                var dummyId = UUID.randomUUID().toString();
                dummyWriter.writeName(dummyId);
                keyCodec.encode(dummyWriter, entry.getKey(), encoderContext);

                var documentValue = dummyWriter.getDocument().asDocument().get(dummyId);
                if (documentValue.isString()) {
                    writer.writeName(documentValue.asString().getValue());
                } else if (documentValue.isInt64() || documentValue.isInt32()) { // This is hilariously hacky.
                    writer.writeName(String.valueOf(documentValue.asNumber().longValue()));
                } else {
                    throw new IllegalArgumentException("Invalid document type! Expected String or Number, got: " + documentValue);
                }

                valueCodec.encode(writer, entry.getValue(), encoderContext);
            }

            dummyWriter.writeEndDocument();
        }

        writer.writeEndDocument();
    }

    @Override
    public Map<K, T> decode(final BsonReader reader, final DecoderContext context) {
        reader.readStartDocument();
        Map<K, T> map = getInstance();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            var nameReader = new JsonReader("{\"key:\":\"" + reader.readName() + "\"}");
            nameReader.readStartDocument();
            nameReader.readBsonType();

            if (reader.getCurrentBsonType() == BsonType.NULL) {
                map.put(keyCodec.decode(nameReader, context), null);
                reader.readNull();
            } else {
                map.put(keyCodec.decode(nameReader, context), valueCodec.decode(reader, context));
            }

            nameReader.readEndDocument();
        }

        reader.readEndDocument();
        return map;
    }

    @Override
    public Class<Map<K, T>> getEncoderClass() {
        return encoderClass;
    }

    private Map<K, T> getInstance() {
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
