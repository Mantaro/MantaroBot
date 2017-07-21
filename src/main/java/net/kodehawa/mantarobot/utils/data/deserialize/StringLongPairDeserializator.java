package net.kodehawa.mantarobot.utils.data.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;

public class StringLongPairDeserializator extends JsonDeserializer<Pair> {
    @Override
    public Pair deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String tmp = jp.getText(); // {
        jp.nextToken();
        String key = jp.getText();
        jp.nextToken();
        Object value = jp.getText() ;
        jp.nextToken();
        tmp = jp.getText(); // }

        return Pair.of(key, value);
    }
}