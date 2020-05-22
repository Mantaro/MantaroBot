/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.utils.data.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;

public class StringLongPairDeserializator extends JsonDeserializer<Pair<String, Long>> {
    @Override
    public Pair<String, Long> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        jp.getText(); // {
        jp.nextToken();
        String key = jp.getText();
        jp.nextToken();
        Long value = Long.parseLong(jp.getText());
        jp.nextToken();
        jp.getText(); // }

        return Pair.of(key, value);
    }
}
