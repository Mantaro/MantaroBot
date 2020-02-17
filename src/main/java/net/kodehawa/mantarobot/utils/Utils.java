/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.utils;

import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.rethinkdb.net.Connection;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.annotations.ConfigName;
import net.kodehawa.mantarobot.utils.annotations.UnusedConfig;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.RateLimit;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.commands.OptsCmd.optsCmd;
import static net.kodehawa.mantarobot.utils.commands.EmoteReference.BLUE_SMALL_MARKER;

public class Utils {
    public static final Map<Long, AtomicInteger> ratelimitedUsers = new ConcurrentHashMap<>();
    public static final OkHttpClient httpClient = new OkHttpClient();
    public static final Pattern mentionPattern = Pattern.compile("<(#|@|@&)?.[0-9]{17,21}>");
    //The regex to filter discord invites.
    public static final Pattern DISCORD_INVITE = Pattern.compile(
            "(?:discord(?:(?:\\.|.?dot.?)gg|app(?:\\.|.?dot.?)com/invite)/(?<id>" +
                    "([\\w]{10,16}|[a-zA-Z0-9]{4,8})))");
    public static final Pattern DISCORD_INVITE_2 = Pattern.compile(
            "(https?://)?discord(app(\\.|\\s*?dot\\s*?)com\\s+?/\\s+?invite\\s*?/\\s*?|(\\.|\\s*?dot\\s*?)(gg|me|io)\\s*?/\\s*?)([a-zA-Z0-9\\-_]+)"
    );
    public static final Pattern THIRD_PARTY_INVITE = Pattern.compile(
            "(https?://)?discord(\\.|\\s*?dot\\s*?)(me|io)\\s*?/\\s*?([a-zA-Z0-9\\-_]+)"
    );
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Utils.class);
    private static final Pattern pattern = Pattern.compile("\\d+?[a-zA-Z]");
    private static final Config config = MantaroData.config().get();
    private static final Random random = new Random();
    private static Set<String> loggedUsers = ConcurrentHashMap.newKeySet();

    /**
     * Capitalizes the first letter of a string.
     *
     * @param s the string to capitalize
     * @return A string with the first letter capitalized.
     */
    public static String capitalize(String s) {
        if (s.length() == 0) return s;
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

        if (days > 0) {
            output.append(days).append(" ").append((days > 1 ? "days" : "day"));
            leading = true;
        }

        if (hours > 0) {
            //If we have a leading day, and minutes after, append a comma
            if (leading && (minutes != 0 || seconds != 0)) {
                output.append(", ");
            }

            if (!output.toString().isEmpty() && (minutes == 0 && seconds == 0)) { //else, append "and", since it's the end.
                output.append(" and ");
            }

            output.append(hours).append(" ").append((hours > 1 ? "hours" : "hour"));
            leading = true;
        }

        if (minutes > 0) {
            //If we have a leading hour, and seconds after, append a comma
            if (leading && seconds != 0) {
                output.append(", ");
            }

            if (!output.toString().isEmpty() && seconds == 0) { //else, append "and", since it's the end.
                output.append(" and ");
            }

            //Re-assign, in case we didn't get hours at all.
            leading = true;

            output.append(minutes).append(" ").append((minutes > 1 ? "minutes" : "minute"));
        }

        if (seconds > 0) {
            if (leading) {
                //We reach our destiny...
                output.append(" and ");
            }

            output.append(seconds).append(" ").append((seconds > 1 ? "seconds" : "second"));
        }

        if (output.toString().isEmpty() && !leading) {
            output.append("0 seconds (about now)..");
        }

        return output.toString();
    }

    public static Iterable<String> iterate(Pattern pattern, String string) {
        return () -> {
            Matcher matcher = pattern.matcher(string);
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
        } catch (Exception e) {
            return "cannot post data to hastebin";
        }
    }

    public static String paste2(String toSend) {
        try {
            RequestBody body = new FormBody.Builder()
                    .add("text", toSend)
                    .build();

            Request request = new Request.Builder()
                    .url("https://hastepaste.com/api/create")
                    .header("User-Agent", MantaroInfo.USER_AGENT)
                    .post(body)
                    .build();

            try (Response r = httpClient.newCall(request).execute()) {
                return r.body().string();
            }
        } catch (Exception e) {
            return "cannot post data to hastepaste";
        }
    }

    public static String paste3(String toSend) {
        try {
            RequestBody post = RequestBody.create(MediaType.parse("text/plain"), toSend);

            Request toPost = new Request.Builder()
                    .url("https://hasteb.in/documents")
                    .header("User-Agent", MantaroInfo.USER_AGENT)
                    .header("Content-Type", "text/plain")
                    .post(post)
                    .build();

            try (Response r = httpClient.newCall(toPost).execute()) {
                return "https://hasteb.in/" + new JSONObject(r.body().string()).getString("key");
            }

        } catch (Exception e) {
            return "cannot post data to hasteb.in";
        }
    }


    /**
     * DEPRECATED - Redirects to wgetOkHttp.
     * Fetches an Object from any given URL. Uses vanilla Java methods.
     * Can retrieve text, JSON Objects, XML and probably more.
     *
     * @param url The URL to get the object from.
     * @return The object as a parsed UTF-8 string.
     */
    @Deprecated
    public static String wget(String url) {
        return wgetOkHttp(url);
    }

    /**
     * Same than above, but using OkHttp. Way easier tbh.
     *
     * @param url The URL to get the object from.
     * @return The object as a parsed string.
     */
    public static String wgetOkHttp(String url) {
        try {
            Request req = new Request.Builder()
                    .url(url)
                    .header("User-Agent", MantaroInfo.USER_AGENT)
                    .build();

            try (Response r = httpClient.newCall(req).execute()) {
                if (r.body() == null || r.code() / 100 != 2) {
                    if (r.code() != 404) {
                        log.warn(getFetchDataFailureResponse(url, "HTTP"));
                    }
                    return null;
                }
                return r.body().string();
            }
        } catch (Exception e) {
            log.warn(getFetchDataFailureResponse(url, "HTTP"), e);
            return null;
        }
    }

    public static String urlEncodeUTF8(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
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

    public static Member findMember(GuildMessageReceivedEvent event, Member first, String content) {
        List<Member> found = FinderUtil.findMembers(content, event.getGuild());
        if (found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot find any member with that name :(").queue();
            return null;
        }

        if (found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format("%sToo many users found, maybe refine your search? (ex. use name#discriminator)\n**Users found:** %s",
                    EmoteReference.THINKING, found.stream().limit(7).map(m -> m.getUser().getName() + "#" + m.getUser().getDiscriminator()).collect(Collectors.joining(", "))))
                    .queue();

            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        }

        return first;
    }

    public static Role findRole(GuildMessageReceivedEvent event, String content) {
        List<Role> found = FinderUtil.findRoles(content, event.getGuild());
        if (found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot find any role with that name :( -if the role has spaces try wrapping it in quotes \"like this\"-").queue();
            return null;
        }

        if (found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format("%sToo many roles found, maybe refine your search?\n**Roles found:** %s",
                    EmoteReference.THINKING, found.stream().limit(7).map(Role::getName).collect(Collectors.joining(", ")))).queue();

            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        }

        return event.getMember().getRoles().get(0);
    }

    public static Role findRoleSelect(GuildMessageReceivedEvent event, String content, Consumer<Role> consumer) {
        List<Role> found = FinderUtil.findRoles(content, event.getGuild());
        if (found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot find any roles with that name :( -if the role has spaces try wrapping it in quotes \"like this\"-").queue();
            return null;
        }

        if (found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format("%sToo many roles found, maybe refine your search?\n**Roles found:** %s\n" +
                            "If the role you're trying to search contain spaces, wrap it in quotes `\"like this\"`",
                    EmoteReference.THINKING, found.stream().limit(7).map(Role::getName).collect(Collectors.joining(", ")))).queue();

            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        } else {
            DiscordUtils.selectList(event, found,
                    role -> String.format("%s (ID: %s)", role.getName(), role.getId()),
                    s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Role:").setDescription(s).build(), consumer
            );
        }

        return null;
    }

    public static TextChannel findChannel(GuildMessageReceivedEvent event, String content) {
        List<TextChannel> found = FinderUtil.findTextChannels(content, event.getGuild());
        if (found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot find any text channel with that name :(").queue();
            return null;
        }

        if (found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format("%sToo many channels found, maybe refine your search?\n**Text Channel found:** %s",
                    EmoteReference.THINKING, found.stream().map(TextChannel::getName).collect(Collectors.joining(", ")))).queue();

            return null;
        }

        if (found.size() == 1) {
            return found.get(0);
        }

        return null;
    }

    public static TextChannel findChannelSelect(GuildMessageReceivedEvent event, String content, Consumer<TextChannel> consumer) {
        List<TextChannel> found = FinderUtil.findTextChannels(content, event.getGuild());
        if (found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot find any text channel with that name :(").queue();
            return null;
        }

        if (found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format("%sToo many channels found, maybe refine your search?\n**Text Channel found:** %s",
                    EmoteReference.THINKING, found.stream().map(TextChannel::getName).collect(Collectors.joining(", ")))).queue();

            return null;
        }

        if (found.size() == 1) {
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
        if (found.isEmpty() && !content.isEmpty()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot find any voice channel with that name :(").queue();
            return null;
        }

        if (found.size() > 1 && !content.isEmpty()) {
            event.getChannel().sendMessage(String.format("%sToo many channels found, maybe refine your search?\n**Voice Channels found:** %s",
                    EmoteReference.THINKING, found.stream().limit(7).map(VoiceChannel::getName).collect(Collectors.joining(", ")))).queue();

            return null;
        }

        if (found.size() == 1) {
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

        if ((almostPretty[0] = ugly.charAt(0)) == '-') almostPretty[1] = ugly.charAt(1);

        return new String(almostPretty);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> map(Object... mappings) {
        if (mappings.length % 2 == 1) throw new IllegalArgumentException("mappings.length must be even");
        Map<K, V> map = new HashMap<>();

        for (int i = 0; i < mappings.length; i += 2) {
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
        if (servicePrefix != null) response.append("[").append(servicePrefix).append("]");
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
    public static HashMap<String, Pair<String, Object>> mapConfigObjects(Object valueObj) {
        try {
            Class<?> c1 = valueObj.getClass();
            HashMap<String, Pair<String, Object>> fieldMap = new HashMap<>();
            Field[] valueObjFields = c1.getDeclaredFields();

            for (Field valueObjField : valueObjFields) {
                String fieldName = valueObjField.getName();
                String fieldDescription = "unknown";
                valueObjField.setAccessible(true);
                Object newObj = valueObjField.get(valueObj);

                if (valueObjField.getAnnotation(UnusedConfig.class) != null) {
                    continue;
                }

                if (valueObjField.getAnnotation(ConfigName.class) != null) {
                    fieldDescription = valueObjField.getAnnotation(ConfigName.class).value();
                }

                fieldMap.put(fieldName, Pair.of(fieldDescription, newObj));
            }

            return fieldMap;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static String urlEncodeUTF8(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
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


    public static long parseTime(String s) {
        s = s.toLowerCase();
        long[] time = {0};
        iterate(pattern.matcher(s)).forEach(string -> {
            String l = string.substring(0, string.length() - 1);
            TimeUnit unit;
            switch (string.charAt(string.length() - 1)) {
                case 'm':
                    unit = TimeUnit.MINUTES;
                    break;
                case 'h':
                    unit = TimeUnit.HOURS;
                    break;
                case 'd':
                    unit = TimeUnit.DAYS;
                    break;
                case 's':
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

    public static Connection newDbConnection() {
        return r.connection().hostname(config.dbHost).port(config.dbPort).db(config.dbDb).user(config.dbUser, config.dbPassword).connect();
    }

    public static boolean handleDefaultRatelimit(RateLimiter rateLimiter, User u, GuildMessageReceivedEvent event, I18nContext context) {
        if (context == null) {
            //en_US
            context = new I18nContext(null, null);
        }

        if (!rateLimiter.process(u.getId())) {
            event.getChannel().sendMessageFormat(context.get("general.ratelimit.header"),
                    EmoteReference.STOPWATCH, context.get("general.ratelimit_quotes"), Utils.getHumanizedTime(rateLimiter.tryAgainIn(event.getAuthor()))
            ).queue();

            onRateLimit(u);
            return false;
        }

        return true;
    }

    public static boolean handleDefaultIncreasingRatelimit(IncreasingRateLimiter rateLimiter, String u, GuildMessageReceivedEvent event, I18nContext context, boolean spamAware) {
        if (context == null) {
            //en_US
            context = new I18nContext(null, null);
        }

        RateLimit rateLimit = rateLimiter.limit(u);
        if (rateLimit.getTriesLeft() < 1) {
            event.getChannel().sendMessage(
                    String.format(context.get("general.ratelimit.header"),
                            EmoteReference.STOPWATCH, context.get("general.ratelimit_quotes"), Utils.getHumanizedTime(rateLimit.getCooldown()))
                            + ((rateLimit.getSpamAttempts() > 2 && spamAware) ? "\n\n" + EmoteReference.STOP + context.get("general.ratelimit.spam_1") : "")
                            + ((rateLimit.getSpamAttempts() > 4 && spamAware) ? context.get("general.ratelimit.spam_2") : "")
            ).queue();

            //Assuming it's an user RL if it can parse a long since we use UUIDs for other RLs.
            try {
                //noinspection ResultOfMethodCallIgnored
                Long.parseUnsignedLong(u);
                User user = MantaroBot.getInstance().getShardManager().getUserById(u);
                onRateLimit(user);
            } catch (Exception ignored) {
            }

            return false;
        }

        return true;
    }

    public static boolean handleDefaultIncreasingRatelimit(IncreasingRateLimiter rateLimiter, User u, GuildMessageReceivedEvent event, I18nContext context, boolean spamAware) {
        return handleDefaultIncreasingRatelimit(rateLimiter, u.getId(), event, context, spamAware);
    }

    public static boolean handleDefaultIncreasingRatelimit(IncreasingRateLimiter rateLimiter, User u, GuildMessageReceivedEvent event, I18nContext context) {
        return handleDefaultIncreasingRatelimit(rateLimiter, u.getId(), event, context, true);
    }

    private static void onRateLimit(User user) {
        int ratelimitedTimes = ratelimitedUsers.computeIfAbsent(user.getIdLong(), __ -> new AtomicInteger()).incrementAndGet();
        if (ratelimitedTimes > 800 && !loggedUsers.contains(user.getId())) {
            loggedUsers.add(user.getId());
            LogUtils.spambot(user);
        }
    }

    public static String replaceArguments(Map<String, ?> args, String content, String... toReplace) {
        if (args == null || args.isEmpty()) {
            return content;
        }

        String contentReplaced = content;

        for (String s : toReplace) {
            if (args.containsKey(s)) {
                contentReplaced = contentReplaced.replace(" -" + s, "").replace("-" + s, "");
            }
        }

        return contentReplaced;
    }

    @Nullable
    public static Badge getHushBadge(String name, HushType type) {
        if (!config.needApi)
            return null; //nothing to query on.

        try {
            Request request = new Request.Builder()
                    .url(config.apiTwoUrl + "/mantaroapi/bot/hush")
                    .addHeader("Authorization", config.getApiAuthKey())
                    .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                    .post(RequestBody.create(
                            okhttp3.MediaType.parse("application/json"),
                            new JSONObject()
                                    .put("type", type) //lowercase -> subcat in json
                                    .put("name", name) //key, will return result from type.name
                                    .toString()
                    ))
                    .build();

            Response response = httpClient.newCall(request).execute();
            String body = response.body().string();
            response.close();

            return Badge.lookupFromString(new JSONObject(body).getString("hush"));
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Pair<Boolean, String> getPledgeInformation(String user) {
        if (!config.needApi)
            return null; //nothing to query on.

        try {
            Request request = new Request.Builder()
                    .url(config.apiTwoUrl + "/mantaroapi/bot/patreon/check")
                    .addHeader("Authorization", config.getApiAuthKey())
                    .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                    .post(RequestBody.create(
                            okhttp3.MediaType.parse("application/json"),
                            new JSONObject()
                                    .put("id", user)
                                    .put("context", config.isPremiumBot())
                                    .toString()
                    ))
                    .build();

            Response response = httpClient.newCall(request).execute();
            String body = response.body().string();
            response.close();

            JSONObject reply = new JSONObject(body);

            return new Pair<>(reply.getBoolean("active"), reply.getString("amount"));
        } catch (Exception ex) {
            //don't disable premium if the api is wonky, no need to be a meanie.
            ex.printStackTrace();
            return null;
        }
    }


    public static boolean isValidTimeZone(final String timeZone) {
        final String DEFAULT_GMT_TIMEZONE = "GMT";
        if (timeZone.equals(DEFAULT_GMT_TIMEZONE)) {
            return true;
        } else {
            String id = TimeZone.getTimeZone(timeZone).getID();
            return !id.equals(DEFAULT_GMT_TIMEZONE);
        }

    }

    public static String prettyDisplay(String header, String body) {
        return BLUE_SMALL_MARKER + "**" + header + "**: " + body;
    }

    public static String prettyDisplayLine(String header, String body) {
        return BLUE_SMALL_MARKER + "**" + header + "**:\n" + body;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> LinkedList<T> createLinkedList(T... elements) {
        LinkedList<T> list = new LinkedList<>();
        Collections.addAll(list, elements);
        return list;
    }

    private static String formatMemoryHelper(long bytes, long unitSize, String unit) {
        if (bytes % unitSize == 0) {
            return String.format("%d %s", bytes / unitSize, unit);
        }
        return String.format("%.1f %s", bytes / (double) unitSize, unit);
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

    public static String formatMemoryUsage(long used, long total) {
        return String.format("%s/%s", formatMemoryAmount(used), formatMemoryAmount(total));
    }

    public enum HushType {
        ANIME, CHARACTER, MUSIC
    }
}
