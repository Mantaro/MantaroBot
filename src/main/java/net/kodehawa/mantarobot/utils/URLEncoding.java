package net.kodehawa.mantarobot.utils;

import lombok.SneakyThrows;

import java.net.URLDecoder;
import java.net.URLEncoder;

public class URLEncoding {
    @SneakyThrows
    public static String decode(String s) {
        return URLDecoder.decode(s, "UTF-8");
    }

    @SneakyThrows
    public static String encode(String s) {
        return URLEncoder.encode(s, "UTF-8");
    }
}
