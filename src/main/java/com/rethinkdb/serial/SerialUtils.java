package com.rethinkdb.serial;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SerialUtils {
    public static ObjectMapper getMapper() {
        SimpleModule m = new SimpleModule(
                "RethinkDB ObjectMapper Module",
                new Version(1, 0, 0, null, null, null)
        );

        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

        m.addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime val, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(val.format(fmt.withZone(ZoneId.systemDefault())));
            }
        });

        m.addSerializer(ZonedDateTime.class, new JsonSerializer<ZonedDateTime>() {
            @Override
            public void serialize(ZonedDateTime val, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(val.format(fmt));
            }
        });

        m.addSerializer(OffsetDateTime.class, new JsonSerializer<OffsetDateTime>() {
            @Override
            public void serialize(OffsetDateTime val, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(val.format(fmt));
            }
        });

        m.addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return LocalDateTime.from(fmt.parse(p.getValueAsString()));
            }
        });

        m.addDeserializer(ZonedDateTime.class, new JsonDeserializer<ZonedDateTime>() {
            @Override
            public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return ZonedDateTime.from(fmt.parse(p.getValueAsString()));
            }
        });

        m.addDeserializer(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
            @Override
            public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return OffsetDateTime.from(fmt.parse(p.getValueAsString()));
            }
        });

        return new ObjectMapper().registerModule(m);
    }
}
