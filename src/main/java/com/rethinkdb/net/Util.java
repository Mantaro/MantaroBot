/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package com.rethinkdb.net;

import com.rethinkdb.gen.exc.ReqlDriverError;
import net.kodehawa.mantarobot.utils.Mapifier;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class Util {
	public static String bufferToString(ByteBuffer buf) {
		// This should only be used on ByteBuffers we've created by
		// wrapping an array
		return new String(buf.array(), StandardCharsets.UTF_8);
	}

	public static <T, P> T convertToPojo(Object value, Optional<Class<P>> pojoClass) {
		return !pojoClass.isPresent() || !(value instanceof Map)
			? (T) value
			: (T) toPojo(pojoClass.get(), (Map<String, Object>) value);
	}

	public static long deadline(long timeout) {
		return System.currentTimeMillis() + timeout;
	}

	public static String fromUTF8(byte[] ba) {
		return new String(ba, StandardCharsets.UTF_8);
	}

	public static ByteBuffer leByteBuffer(int capacity) {
		// Creating the ByteBuffer over an underlying array makes
		// it easier to turn into a string later.
		byte[] underlying = new byte[capacity];
		return ByteBuffer.wrap(underlying)
			.order(ByteOrder.LITTLE_ENDIAN);
	}

	public static JSONObject toJSON(String str) {
		return (JSONObject) JSONValue.parse(str);
	}

	public static JSONObject toJSON(ByteBuffer buf) {
		InputStreamReader codepointReader =
			new InputStreamReader(new ByteArrayInputStream(buf.array()));
		return (JSONObject) JSONValue.parse(codepointReader);
	}

	public static byte[] toUTF8(String s) {
		return s.getBytes(StandardCharsets.UTF_8);
	}

	private static <T> T toPojo(Class<T> pojoClass, Map<String, Object> map) {
		try {
			if (map == null) {
				return null;
			}

			return Mapifier.fromMap(pojoClass, map);
		} catch (IllegalArgumentException e) {
			throw new ReqlDriverError("Can't convert %s to a POJO: %s", map, e.getMessage());
		}
	}
}
