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

package net.kodehawa.mantarobot.utils;

import com.rethinkdb.net.Connection;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.annotations.ConfigName;
import net.kodehawa.mantarobot.utils.annotations.HiddenConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.LocaleUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.utils.commands.EmoteReference.BLUE_SMALL_MARKER;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    public static final OkHttpClient httpClient = new OkHttpClient();
    public static final Pattern mentionPattern = Pattern.compile("<(#|@|@&)?.[0-9]{17,21}>");

    private final static String BLOCK_INACTIVE = "\u25AC";
    private final static String BLOCK_ACTIVE = "\uD83D\uDD18";
    private static final int TOTAL_BLOCKS = 10;

    //The regex to filter discord invites.
    public static final Pattern DISCORD_INVITE = Pattern.compile(
            "(?:discord(?:(?:\\.|.?dot.?)gg|app(?:\\.|.?dot.?)com/invite)/(?<id>" +
                    "([\\w]{10,16}|[a-zA-Z0-9]{4,8})))");

    public static final Pattern DISCORD_INVITE_2 = Pattern.compile(
            "(?:https?://)?discord((?:app)?(?:\\.|\\s*?dot\\s*?)com\\s?/\\s*invite\\s*/\\s*|(?:\\.|\\s*dot\\s*)(?:gg|me|io)\\s*/\\s*)([a-zA-Z0-9\\-_]+)"
    );

    private static final char BACKTICK = '`';
    private static final char LEFT_TO_RIGHT_ISOLATE = '\u2066';
    private static final char POP_DIRECTIONAL_ISOLATE = '\u2069';
    private static final Pattern pattern = Pattern.compile("\\d+?[a-zA-Z]");
    private static final Config config = MantaroData.config().get();

    /**
     * Capitalizes the first letter of a string.
     *
     * @param s the string to capitalize
     * @return A string with the first letter capitalized.
     */
    public static String capitalize(String s) {
        if (s.length() == 0) {
            return s;
        }

        return s.substring(0, 1)
                .toUpperCase() + s.substring(1)
                .toLowerCase();
    }

    public static String formatDuration(long time) {
        if (time < 1000) {
            return "less than a second";
        }

        var days = TimeUnit.MILLISECONDS.toDays(time);
        var hours = TimeUnit.MILLISECONDS.toHours(time) % TimeUnit.DAYS.toHours(1);
        var minutes = TimeUnit.MILLISECONDS.toMinutes(time) % TimeUnit.HOURS.toMinutes(1);
        var seconds = TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1);

        var parts = Stream.of(
                formatUnit(days, "day"), formatUnit(hours, "hour"),
                formatUnit(minutes, "minute"), formatUnit(seconds, "second")
        ).filter(i -> !i.isEmpty()).iterator();

        var sb = new StringBuilder();
        var multiple = false;

        while(parts.hasNext()) {
            sb.append(parts.next());
            if (parts.hasNext()) {
                multiple = true;
                sb.append(", ");
            }
        }

        if (multiple) {
            var last = sb.lastIndexOf(", ");
            sb.replace(last, last + 2, " and ");
        }

        return sb.toString();
    }

    public static long parseTime(String toParse) {
        toParse = toParse.toLowerCase();
        long[] time = { 0 };

        iterate(pattern.matcher(toParse)).forEach(string -> {
            var l = string.substring(0, string.length() - 1);
            var unit = switch (string.charAt(string.length() - 1)) {
                case 'm' -> TimeUnit.MINUTES;
                case 'h' -> TimeUnit.HOURS;
                case 'd' -> TimeUnit.DAYS;
                default -> TimeUnit.SECONDS;
            };

            time[0] += unit.toMillis(Long.parseLong(l));
        });

        return time[0];
    }

    private static String formatUnit(long amount, String baseName) {
        if (amount == 0) {
            return "";
        }

        if (amount == 1) {
            return "1 " + baseName;
        }

        return amount + " " + baseName + "s";
    }

    public static Locale getLocaleFromLanguage(String language) {
        // No need to pass it to LocaleUtils if we pass nothing to this.
        if (language == null || language.isEmpty()) {
            return Locale.ENGLISH;
        }

        // Parse the user's language settings to attempt to get the locale.
        Locale locale = null;
        try {
            locale = LocaleUtils.toLocale(language);
        } catch (IllegalArgumentException ignore) { }

        if (locale == null) {
            locale = Locale.ENGLISH;
        }

        return locale;
    }

    public static String paste(String toSend) {
        try {
            var post = RequestBody.create(MediaType.parse("text/plain"), toSend);

            var toPost = new Request.Builder()
                    .url("https://hasteb.in/documents")
                    .header("User-Agent", MantaroInfo.USER_AGENT)
                    .header("Content-Type", "text/plain")
                    .post(post)
                    .build();

            try (var r = httpClient.newCall(toPost).execute()) {
                return "https://hasteb.in/" + new JSONObject(r.body().string()).getString("key");
            }
        } catch (Exception e) {
            return "cannot post data to hasteb.in";
        }
    }

    /**
     * Same than above, but using OkHttp. Way easier tbh.
     *
     * @param url The URL to get the object from.
     * @return The object as a parsed string.
     */
    public static String httpRequest(String url) {
        try {
            var req = new Request.Builder()
                    .url(url)
                    .header("User-Agent", MantaroInfo.USER_AGENT)
                    .build();

            try (var r = httpClient.newCall(req).execute()) {
                if (r.body() == null || r.code() / 100 != 2) {
                    if (r.code() != 404) {
                        log.warn("Non 404 code failure for {}: {}", url, r.code());
                    }

                    return null;
                }

                return r.body().string();
            }
        } catch (Exception e) {
            log.warn("Exception trying to fetch from URL {}", url, e);
            return null;
        }
    }

    public static String urlEncodeUTF8(Map<?, ?> map) {
        var sb = new StringBuilder();
        for (var entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }

            sb.append(String.format("%s=%s",
                    urlEncodeUTF8(entry.getKey().toString()),
                    urlEncodeUTF8(entry.getValue().toString())
            ));
        }
        return sb.toString();
    }

    private static String urlEncodeUTF8(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    public static Iterable<String> iterate(Pattern pattern, String string) {
        return () -> {
            var matcher = pattern.matcher(string);
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return matcher.find();
                }

                @Override
                public String next() {
                    return matcher.group();
                }
            };
        };
    }

    private static Iterable<String> iterate(Matcher matcher) {
        return new Iterable<>() {
            @NotNull
            @Override
            public Iterator<String> iterator() {
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return matcher.find();
                    }

                    @Override
                    public String next() {
                        return matcher.group();
                    }
                };
            }

            @Override
            public void forEach(Consumer<? super String> action) {
                while (matcher.find()) {
                    action.accept(matcher.group());
                }
            }
        };
    }

    public static Connection newDbConnection() {
        return r.connection()
                .hostname(config.getDbHost())
                .port(config.getDbPort())
                .db(config.getDbDb())
                .user(config.getDbUser(), config.getDbPassword())
                .connect();
    }

    public static String replaceArguments(Map<String, ?> args, String content, String... toReplace) {
        if (args == null || args.isEmpty()) {
            return content;
        }

        var contentReplaced = content;

        for (var s : toReplace) {
            if (args.containsKey(s)) {
                contentReplaced = contentReplaced
                        .replace(" -" + s, "")
                        .replace("-" + s, "");
            }
        }

        return contentReplaced;
    }

    public static boolean isValidTimeZone(final String timeZone) {
        if (timeZone.equals("GMT") || timeZone.equals("UTC")) {
            return true;
        } else {
            String id = TimeZone.getTimeZone(timeZone).getID();
            return !id.equals("GMT");
        }
    }

    public static ZoneId timezoneToZoneID(final String timeZone) {
        return TimeZone.getTimeZone(timeZone).toZoneId();
    }

    public static String prettyDisplay(String header, String body) {
        return BLUE_SMALL_MARKER + "**" + header + "**: " + body;
    }

    public static String prettyDisplayLine(String header, String body) {
        return BLUE_SMALL_MARKER + "**" + header + "**:\n" + body;
    }

    private static String formatMemoryHelper(long bytes, long unitSize, String unit) {
        if (bytes % unitSize == 0) {
            return String.format("%d %s", bytes / unitSize, unit);
        }

        return String.format("%.1f %s", bytes / (double) unitSize, unit);
    }

    public static String formatMemoryUsage(long used, long total) {
        return String.format("%s/%s", formatMemoryAmount(used), formatMemoryAmount(total));
    }

    public static String formatMemoryAmount(long bytes) {
        if (bytes > 1L << 30) {
            return formatMemoryHelper(bytes, 1L << 30, "GiB");
        }

        if (bytes > 1L << 20) {
            return formatMemoryHelper(bytes, 1L << 20, "MiB");
        }

        if (bytes > 1L << 10) {
            return formatMemoryHelper(bytes, 1L << 10, "KiB");
        }

        return String.format("%d B", bytes);
    }

    public static OffsetDateTime epochToDate(long epoch) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault());
    }

    public static String formatDate(OffsetDateTime date) {
        return date.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
    }

    public static String formatHours(OffsetDateTime date, String locale) {
        return date.format(DateTimeFormatter.ofPattern("HH:mm:ss").withLocale(getLocaleFromLanguage(locale)));
    }

    public static String formatHours(OffsetDateTime date) {
        return formatHours(date, "en_US");
    }

    public static String formatDate(long epoch, String lang) {
        return epochToDate(epoch)
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(getLocaleFromLanguage(lang)));
    }

    public static String formatDate(OffsetDateTime date, String lang) {
        return date.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(getLocaleFromLanguage(lang)));
    }

    public static String formatDate(LocalDateTime date, String lang) {
        return date.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(getLocaleFromLanguage(lang)));
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> LinkedList<T> createLinkedList(T... elements) {
        LinkedList<T> list = new LinkedList<>();
        Collections.addAll(list, elements);

        return list;
    }

    /**
     * Fixes the direction of the rendering of the text inside `inline codeblocks` to
     * be always left to right.
     *
     * @param src Source string.
     *
     * @return String with appropriate unicode direction modifier characters
     *         around code blocks.
     */
    @Nonnull
    @CheckReturnValue
    public static String fixInlineCodeblockDirection(@Nonnull String src) {
        //if there's no right to left override, there's nothing to do
        if (!isRtl(src)) {
            return src;
        }

        //no realloc unless we somehow have 5 codeblocks
        var sb = new StringBuilder(src.length() + 8);
        var inside = false;

        for (var i = 0; i < src.length(); i++) {
            var ch = src.charAt(i);
            if (ch == BACKTICK) {
                if (inside) {
                    sb.append(BACKTICK)
                            .append(POP_DIRECTIONAL_ISOLATE);
                } else {
                    sb.append(LEFT_TO_RIGHT_ISOLATE)
                            .append(BACKTICK);
                }
                inside = !inside;
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static boolean isRtl(String string) {
        if (string == null) {
            return false;
        }

        for (int i = 0, n = string.length(); i < n; ++i) {
            var d = Character.getDirectionality(string.charAt(i));

            switch (d) {
                case Character.DIRECTIONALITY_RIGHT_TO_LEFT:
                case Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC:
                case Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING:
                case Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE:
                    return true;

                case Character.DIRECTIONALITY_LEFT_TO_RIGHT:
                case Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING:
                case Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE:
                    return false;
            }
        }

        return false;
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (var entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * Retrieves a map of objects in a class and its respective values.
     * Yes, I'm too lazy to do it manually and it would make absolutely no sense to either.
     * <p>
     * Modified it a bit. (Original: https://narendrakadali.wordpress.com/2011/08/27/41/)
     *
     * @author Narendra
     * @since Aug 27, 2011 5:27:19 AM
     */
    public static HashMap<String, Pair<String, Object>> mapConfigObjects(Object valueObj) {
        try {
            var clazz = valueObj.getClass();
            var valueObjFields = clazz.getDeclaredFields();

            HashMap<String, Pair<String, Object>> fieldMap = new HashMap<>();

            for (var valueObjField : valueObjFields) {
                var fieldName = valueObjField.getName();
                var fieldDescription = "unknown";
                valueObjField.setAccessible(true);

                Object newObj = valueObjField.get(valueObj);

                if (valueObjField.getAnnotation(HiddenConfig.class) != null) {
                    continue;
                }

                if (valueObjField.getAnnotation(ConfigName.class) != null) {
                    fieldDescription = valueObjField
                            .getAnnotation(ConfigName.class)
                            .value();
                }

                fieldMap.put(fieldName, Pair.of(fieldDescription, newObj));
            }

            return fieldMap;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static String getProgressBar(long now, long total) {
        var activeBlocks = (int) ((float) now / total * TOTAL_BLOCKS);
        var builder = new StringBuilder();
        for (var i = 0; i < TOTAL_BLOCKS; i++)
            builder.append(activeBlocks == i ? BLOCK_ACTIVE : BLOCK_INACTIVE);

        return builder.append(BLOCK_INACTIVE).toString();
    }

    public static String getProgressBar(long now, long total, long blocks) {
        var activeBlocks = (int) ((float) now / total * blocks);
        var builder = new StringBuilder();

        for (var i = 0; i < blocks; i++)
            builder.append(
                    activeBlocks == i ? BLOCK_ACTIVE : BLOCK_INACTIVE
            );

        return builder.append(BLOCK_INACTIVE).toString();
    }

    public static <T extends Enum<T>> T lookupEnumString(String name, Class<T> enumType) {
        for (var t : enumType.getEnumConstants()) {
            if (t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }

        return null;
    }

    public enum HushType {
        ANIME, CHARACTER, MUSIC
    }
}
