package net.kodehawa.mantarobot.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.io.CharStreams;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import okhttp3.*;
import org.json.JSONObject;
import us.monoid.web.Resty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Utils {
    public static final OkHttpClient httpClient = new OkHttpClient();
    private static final Pattern pattern = Pattern.compile("\\d+?[a-zA-Z]");
    public static ObjectMapper XML_MAPPER = new XmlMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Capitalizes the first letter of a string.
     *
     * @param s the string to capitalize
     * @return A string with the first letter capitalized.
     */
    public static String capitalize(String s) {
        if(s.length() == 0) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    public static String getReadableTime(long millis) {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    public static String getVerboseTime(long millis) {
        return String.format("%02d hours, %02d minutes and %02d seconds",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    public static String getDurationMinutes(long length) {
        return String.format("%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(length),
                TimeUnit.MILLISECONDS.toSeconds(length) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(length))
        );
    }

    public static Iterable<String> iterate(Pattern pattern, String string) {
        return () -> {
            Matcher matcher = pattern.matcher(string);
            return new Iterator<String>() {
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

    public static String paste(String toSend) {
        try {
            RequestBody post = RequestBody.create(MediaType.parse("text/plain"), toSend);

            Request toPost = new Request.Builder()
                    .url("https://hastebin.com/documents")
                    .header("User-Agent", "Mantaro")
                    .header("Content-Type", "text/plain")
                    .post(post)
                    .build();

            Response r = httpClient.newCall(toPost).execute();
            JSONObject response = new JSONObject(r.body().string());
            r.close();
            return "https://hastebin.com/" + response.getString("key");
        } catch(Exception e) {
            e.printStackTrace();
            return "An error was encountered while trying to upload to hastebin.";
        }
    }

    public static Comparator<String> randomOrder() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int x = r.nextInt(), y = r.nextInt();
        boolean b = r.nextBoolean();
        return Comparator.comparingInt((String s) -> s.hashCode() ^ x)
                .thenComparingInt(s -> s.length() ^ y)
                .thenComparing(b ? Comparator.naturalOrder() : Comparator.reverseOrder());
    }

    public static String centerString(String text, int len) {
        String out = String.format("%" + len + "s%s%" + len + "s", "", text, "");
        float mid = (out.length() / 2);
        float start = mid - (len / 2);
        float end = start + len;
        return out.substring((int) start, (int) end);
    }

    public static String urlEncodeUTF8(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<?, ?> entry : map.entrySet()) {
            if(sb.length() > 0) {
                sb.append("&");
            }
            sb.append(String.format("%s=%s",
                    urlEncodeUTF8(entry.getKey().toString()),
                    urlEncodeUTF8(entry.getValue().toString())
            ));
        }
        return sb.toString();
    }

    /**
     * Fetches an Object from any given URL. Uses vanilla Java methods.
     * Can retrieve text, JSON Objects, XML and probably more.
     *
     * @param url   The URL to get the object from.
     * @param event guild event
     * @return The object as a parsed UTF-8 string.
     */
    public static String wget(String url, GuildMessageReceivedEvent event) {
        String webObject = null;
        try {
            URL ur1 = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) ur1.openConnection();
            conn.setRequestProperty("User-Agent", "Mantaro");
            InputStream ism = conn.getInputStream();
            webObject = CharStreams.toString(new InputStreamReader(ism, StandardCharsets.UTF_8));
        } catch(Exception e) {
            if(e instanceof java.io.FileNotFoundException) return null;

            log.warn(getFetchDataFailureResponse(url, null), e);
            Optional.ofNullable(event).ifPresent((w) -> w.getChannel().sendMessage("\u274C I got an error while retrieving data from " + url).queue());
        }

        return webObject;
    }

    /**
     * Same than above, but using resty. Way easier tbh.
     *
     * @param url   The URL to get the object from.
     * @param event JDA message event.
     * @return The object as a parsed string.
     */
    public static String wgetResty(String url, GuildMessageReceivedEvent event) {
        String url2 = null;
        Resty resty = new Resty().identifyAsMozilla();
        try {
            InputStream is = resty.text(url).stream();
            url2 = CharStreams.toString(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch(IOException e) {
            log.warn(getFetchDataFailureResponse(url, "Resty"), e);
            Optional.ofNullable(event).ifPresent((evt) -> evt.getChannel().sendMessage("\u274C Error retrieving data from URL [Resty]").queue());
        }

        return url2;
    }

    public static String pretty(int number) {
        String ugly = Integer.toString(number);

        char[] almostPretty = new char[ugly.length()];

        Arrays.fill(almostPretty, '0');

        if((almostPretty[0] = ugly.charAt(0)) == '-') almostPretty[1] = ugly.charAt(1);

        return new String(almostPretty);
    }

    /**
     * Get a data failure response, place in its own method due to redundancy
     *
     * @param url           The URL from which the data fetch failed
     * @param servicePrefix The prefix from a specific service
     * @return The formatted response string
     */
    private static String getFetchDataFailureResponse(String url, String servicePrefix) {
        StringBuilder response = new StringBuilder();
        if(servicePrefix != null) response.append("[").append(servicePrefix).append("]");
        else response.append("\u274C");
        return response.append(" ").append("Hmm, seems like I can't retrieve data from ").append(url).toString();
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
    public static HashMap<String, Object> mapObjects(Object valueObj) {
        try {
            Class c1 = valueObj.getClass();
            HashMap<String, Object> fieldMap = new HashMap<>();
            Field[] valueObjFields = c1.getDeclaredFields();

            for(int i = 0; i < valueObjFields.length; i++) {
                String fieldName = valueObjFields[i].getName();
                valueObjFields[i].setAccessible(true);
                Object newObj = valueObjFields[i].get(valueObj);

                fieldMap.put(fieldName, newObj);
            }

            return fieldMap;
        } catch(IllegalAccessException e) {
            return null;
        }
    }

    @SneakyThrows
    private static String urlEncodeUTF8(String s) {
        return URLEncoder.encode(s, "UTF-8");
    }


    private static Iterable<String> iterate(Matcher matcher) {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
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
                while(matcher.find()) {
                    action.accept(matcher.group());
                }
            }
        };
    }


    public static long parseTime(String s) {
        s = s.toLowerCase();
        long[] time = {0};
        iterate(pattern.matcher(s)).forEach(string -> {
            String l = string.substring(0, string.length() - 1);
            TimeUnit unit;
            switch(string.charAt(string.length() - 1)) {
                case 's':
                    unit = TimeUnit.SECONDS;
                    break;
                case 'm':
                    unit = TimeUnit.MINUTES;
                    break;
                case 'h':
                    unit = TimeUnit.HOURS;
                    break;
                case 'd':
                    unit = TimeUnit.DAYS;
                    break;
                default:
                    unit = TimeUnit.SECONDS;
                    break;
            }
            time[0] += unit.toMillis(Long.parseLong(l));
        });
        return time[0];
    }
}