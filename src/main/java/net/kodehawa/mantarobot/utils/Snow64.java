package net.kodehawa.mantarobot.utils;

import com.google.common.primitives.Longs;

import java.util.Base64;

public class Snow64 {
    public static long fromSnow64(String snow64) {
        return (Longs.fromByteArray(Base64.getDecoder().decode(snow64.replace('-', '/'))));
    }

    public static String toSnow64(long snowflake) {
        return Base64.getEncoder().encodeToString(Longs.toByteArray((snowflake))).replace('/', '-').replace('=', ' ').trim();
    }
}