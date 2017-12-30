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

package net.kodehawa.mantarobot.utils;

import com.google.common.io.CharStreams;
import com.jagrosh.jdautilities.utils.FinderUtil;
import com.rethinkdb.net.Connection;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.NewRateLimiter;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;
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
import java.util.stream.Collectors;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.commands.OptsCmd.optsCmd;

@Slf4j
public class Utils {
    public static final OkHttpClient httpClient = new OkHttpClient();
    private static final Pattern pattern = Pattern.compile("\\d+?[a-zA-Z]");
    private static final Config config = MantaroData.config().get();

    private static final String[] ratelimitQuotes = {
            "Woah... you're calling me a bit too fast... I might get dizzy!", "Don't be greedy!", "Y-You're calling me so fast that I'm getting dizzy...",
            "Halt in there buddy!", "Wait just a tiiiiny bit more uwu", "Seems like we're gonna get a speed ticket if we continue going this fast!",
            "I wanna do this... but halt for a bit please."
    };

    private static final Random random = new Random();

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

    /**
     * @param millis How many ms to convert.
     * @return The humanized time (for example, 2 hours and 3 minutes, or 24 seconds).
     */
    public static String getHumanizedTime(long millis) {
        //What we're dealing with.
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(millis));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));

        StringBuilder output = new StringBuilder();
        //Marks whether it's just one value or more after.
        boolean leading = false;

        if(days > 0) {
            output.append(days).append(" ").append((days > 1 ? "days" : "day"));
            leading = true;
        }

        if(hours > 0) {
            //If we have a leading day, and minutes after, append a comma
            if(leading && (minutes != 0 || seconds != 0)) {
                output.append(", ");
            }

            if(!output.toString().isEmpty() && (minutes == 0 && seconds == 0)) { //else, append "and", since it's the end.
                output.append(" and ");
            }

            output.append(hours).append(" ").append((hours > 1 ? "hours" : "hour"));
            leading = true;
        }

        if(minutes > 0) {
            //If we have a leading hour, and seconds after, append a comma
            if(leading && seconds != 0) {
                output.append(", ");
            }

            if(!output.toString().isEmpty() && seconds == 0) { //else, append "and", since it's the end.
                output.append(" and ");
            }

            //Re-assign, in case we didn't get hours at all.
            leading = true;

            output.append(minutes).append(" ").append((minutes > 1 ? "minutes" : "minute"));
        }

        if(seconds > 0) {
            if(leading) {
                //We reach our destiny...
                output.append(" and ");
            }

            output.append(seconds).append(" ").append((seconds > 1 ? "seconds" : "second"));
        }

        if(output.toString().isEmpty() && !leading) {
            output.append("0 seconds (about now)..");
        }

        return output.toString();
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
                    .header("User-Agent", MantaroInfo.USER_AGENT)
                    .header("Content-Type", "text/plain")
                    .post(post)
                    .build();

            Response r = httpClient.newCall(toPost).execute();
            JSONObject response = new JSONObject(r.body().string());
            r.close();
            return "https://hastebin.com/" + response.getString("key");
        } catch(Exception e) {
            return "cannot post data to hastebin";
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
            conn.setRequestProperty("User-Agent", MantaroInfo.USER_AGENT);
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
        Resty resty = new Resty();

        try {
            resty.withHeader("User-Agent", MantaroInfo.USER_AGENT);
            InputStream is = resty.text(url).stream();
            url2 = CharStreams.toString(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch(IOException e) {
            log.warn(getFetchDataFailureResponse(url, "Resty"), e);
            Optional.ofNullable(event).ifPresent((evt) -> evt.getChannel().sendMessage("\u274C Error retrieving data from URL [Resty]").queue());
        }

        return url2;
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

    public static Member findMember(GuildMessageReceivedEvent event, Member first, String content) {
        List<Member> found = FinderUtil.findMembers(content, event.getGuild());
        if(found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Your search yielded no results :(").queue();
            return null;
        }

        if(found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format("%sToo many users found, maybe refine your search? (ex. use name#discriminator)\n**Users found:** %s",
                    EmoteReference.THINKING, found.stream().map(m -> m.getUser().getName() + "#" + m.getUser().getDiscriminator()).collect(Collectors.joining(", "))))
                    .queue();

            return null;
        }

        if(found.size() == 1) {
            return found.get(0);
        }

        return first;
    }

    public static Role findRole(GuildMessageReceivedEvent event, String content) {
        List<Role> found = FinderUtil.findRoles(content, event.getGuild());
        if(found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Your search yielded no results :(").queue();
            return null;
        }

        if(found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format("%sToo many roles found, maybe refine your search?\n**Roles found:** %s",
                    EmoteReference.THINKING, found.stream().map(Role::getName).collect(Collectors.joining(", ")))).queue();

            return null;
        }

        if(found.size() == 1) {
            return found.get(0);
        }

        return event.getMember().getRoles().get(0);
    }

    public static TextChannel findChannel(GuildMessageReceivedEvent event, String content) {
        List<TextChannel> found = FinderUtil.findTextChannels(content, event.getGuild());
        if(found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Your search yielded no results :(").queue();
            return null;
        }

        if(found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format("%sToo many channels found, maybe refine your search?\n**Text Channel found:** %s",
                    EmoteReference.THINKING, found.stream().map(TextChannel::getName).collect(Collectors.joining(", ")))).queue();

            return null;
        }

        if(found.size() == 1) {
            return found.get(0);
        }

        return null;
    }

    public static TextChannel findChannelSelect(GuildMessageReceivedEvent event, String content, Consumer<TextChannel> consumer) {
        List<TextChannel> found = FinderUtil.findTextChannels(content, event.getGuild());
        if(found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Your search yielded no results :(").queue();
            return null;
        }

        if(found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format("%sToo many channels found, maybe refine your search?\n**Text Channel found:** %s",
                    EmoteReference.THINKING, found.stream().map(TextChannel::getName).collect(Collectors.joining(", ")))).queue();

            return null;
        }

        if(found.size() == 1) {
            return found.get(0);
        } else {
            DiscordUtils.selectList(event, found,
                    textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                    s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Channel:").setDescription(s).build(), consumer
            );
        }

        return null;
    }

    public static VoiceChannel findVoiceChannelSelect(GuildMessageReceivedEvent event, String content, Consumer<VoiceChannel> consumer) {
        List<VoiceChannel> found = FinderUtil.findVoiceChannels(content, event.getGuild());
        if(found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Your search yielded no results :(").queue();
            return null;
        }

        if(found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format("%sToo many channels found, maybe refine your search?\n**Voice Channels found:** %s",
                    EmoteReference.THINKING, found.stream().map(VoiceChannel::getName).collect(Collectors.joining(", ")))).queue();

            return null;
        }

        if(found.size() == 1) {
            return found.get(0);
        } else {
            DiscordUtils.selectList(event, found,
                    voiceChannel -> String.format("%s (ID: %s)", voiceChannel.getName(), voiceChannel.getId()),
                    s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Channel:").setDescription(s).build(), consumer
            );
        }

        return null;
    }

    public static String pretty(int number) {
        String ugly = Integer.toString(number);

        char[] almostPretty = new char[ugly.length()];

        Arrays.fill(almostPretty, '0');

        if((almostPretty[0] = ugly.charAt(0)) == '-') almostPretty[1] = ugly.charAt(1);

        return new String(almostPretty);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> map(Object... mappings) {
        if(mappings.length % 2 == 1) throw new IllegalArgumentException("mappings.length must be even");
        Map<K, V> map = new HashMap<>();

        for(int i = 0; i < mappings.length; i += 2) {
            map.put((K) mappings[i], (V) mappings[i + 1]);
        }

        return map;
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
            Class<?> c1 = valueObj.getClass();
            HashMap<String, Object> fieldMap = new HashMap<>();
            Field[] valueObjFields = c1.getDeclaredFields();

            for(Field valueObjField : valueObjFields) {
                String fieldName = valueObjField.getName();
                valueObjField.setAccessible(true);
                Object newObj = valueObjField.get(valueObj);

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

    public static String formatDuration(long time) {
        long days = TimeUnit.MILLISECONDS.toDays(time);
        long hours = TimeUnit.MILLISECONDS.toHours(time) % TimeUnit.DAYS.toHours(1);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time) % TimeUnit.HOURS.toMinutes(1);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1);
        return ((days == 0 ? "" : days + " day" + (days == 1 ? "" : "s") + ", ") +
                (hours == 0 ? "" : hours + " hour" + (hours == 1 ? "" : "s") + ", ") +
                (minutes == 0 ? "" : minutes + " minute" + (minutes == 1 ? "" : "s") + ", ") +
                (seconds == 0 ? "" : seconds + " second" + (seconds == 1 ? "" : "s"))).replaceAll(", (\\d{1,2} \\S+)$", " and $1");
    }

    public static void dbConnection(Consumer<Connection> consumer) {
        try(Connection conn = r.connection().hostname(config.dbHost).port(config.dbPort).db(config.dbDb).user(config.dbUser, config.dbPassword).connect()) {
            consumer.accept(conn);
        }
    }

    public static Connection newDbConnection() {
        return r.connection().hostname(config.dbHost).port(config.dbPort).db(config.dbDb).user(config.dbUser, config.dbPassword).connect();
    }

    public static boolean handleDefaultRatelimit(RateLimiter rateLimiter, User u, GuildMessageReceivedEvent event) {
        if(!rateLimiter.process(u.getId())) {
            event.getChannel().sendMessage(
                    EmoteReference.STOPWATCH +
                            ratelimitQuotes[random.nextInt(ratelimitQuotes.length)] + " (Ratelimited)" +
                            "\n **You'll be able to use this command again in " + Utils.getHumanizedTime(rateLimiter.tryAgainIn(event.getAuthor()))
                            + ".**"
            ).queue();

            MantaroBot.getInstance().getStatsClient().increment("ratelimits");

            return false;
        }

        return true;
    }

    public static boolean handleDefaultNewRatelimit(NewRateLimiter rateLimiter, User u, GuildMessageReceivedEvent event) {
        if(!rateLimiter.test(u.getId())) {
            event.getChannel().sendMessage(
                    String.format("%s%s (Ratelimited)\n **You'll be able to use this command again in %s.**",
                            EmoteReference.STOPWATCH, ratelimitQuotes[random.nextInt(ratelimitQuotes.length)], Utils.getHumanizedTime(rateLimiter.tryAgainIn(event.getAuthor())))
            ).queue();

            MantaroBot.getInstance().getStatsClient().increment("ratelimits");

            return false;
        }

        return true;
    }

    public static String replaceArguments(Map<String, Optional<String>> args, String content, String... toReplace) {
        if(args == null || args.isEmpty()) {
            return content;
        }

        String contentReplaced = content;

        for(String s : toReplace) {
            if(args.containsKey(s)) {
                contentReplaced = contentReplaced.replace(" -" + s, "").replace("-" + s, "");
            }
        }

        return contentReplaced;
    }
}
