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

import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.rethinkdb.net.Connection;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.annotations.ConfigName;
import net.kodehawa.mantarobot.utils.annotations.UnusedConfig;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.RateLimit;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
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
import java.util.stream.Stream;

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

    private static final char BACKTICK = '`';
    private static final char LEFT_TO_RIGHT_ISOLATE = '\u2066';
    private static final char POP_DIRECTIONAL_ISOLATE = '\u2069';
    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    private static final Pattern pattern = Pattern.compile("\\d+?[a-zA-Z]");
    private static final Config config = MantaroData.config().get();
    private static final Set<String> loggedSpambotUsers = ConcurrentHashMap.newKeySet();
    private static final Set<String> loggedAttemptUsers = ConcurrentHashMap.newKeySet();

    /**
     * Capitalizes the first letter of a string.
     *
     * @param s the string to capitalize
     * @return A string with the first letter capitalized.
     */
    public static String capitalize(String s) {
        if (s.length() == 0)
            return s;

        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    public static String getReadableTime(long millis) {
        return String.format("%02d:%02d:%02d",
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

    public static String formatDuration(long time) {
        if(time < 1000) {
            return "less than a second";
        }

        long days = TimeUnit.MILLISECONDS.toDays(time);
        long hours = TimeUnit.MILLISECONDS.toHours(time) % TimeUnit.DAYS.toHours(1);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time) % TimeUnit.HOURS.toMinutes(1);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1);
        var parts = Stream.of(
                formatUnit(days, "day"), formatUnit(hours, "hour"),
                formatUnit(minutes, "minute"), formatUnit(seconds, "second")
        ).filter(i -> !i.isEmpty()).iterator();
        var sb = new StringBuilder();
        var multiple = false;
        while(parts.hasNext()) {
            sb.append(parts.next());
            if(parts.hasNext()) {
                multiple = true;
                sb.append(", ");
            }
        }
        if(multiple) {
            var last = sb.lastIndexOf(", ");
            sb.replace(last, last + 2, " and ");
        }
        return sb.toString();
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
     * Same than above, but using OkHttp. Way easier tbh.
     *
     * @param url The URL to get the object from.
     * @return The object as a parsed string.
     */
    public static String wget(String url) {
        try {
            Request req = new Request.Builder()
                    .url(url)
                    .header("User-Agent", MantaroInfo.USER_AGENT)
                    .build();

            try (Response r = httpClient.newCall(req).execute()) {
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

    //Localized + no default.
    public static Member findMember(GuildMessageReceivedEvent event, I18nContext lang, String content) {
        List<Member> members = FinderUtil.findMembers(content, event.getGuild());
        if (members.isEmpty()) {
            event.getChannel().sendMessageFormat(lang.get("general.find_members_failure"), EmoteReference.ERROR).queue();
            return null;
        }

        if (members.size() > 1) {
            event.getChannel().sendMessageFormat(lang.get("general.too_many_members"), EmoteReference.THINKING, members.stream().limit(7).map(m -> String.format("%s#%s", m.getUser().getName(), m.getUser().getDiscriminator())).collect(Collectors.joining(", "))).queue();
            return null;
        }

        return members.get(0);
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

        if ((almostPretty[0] = ugly.charAt(0)) == '-')
            almostPretty[1] = ugly.charAt(1);

        return new String(almostPretty);
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

    private static String formatUnit(long amount, String baseName) {
        if(amount == 0) return "";
        if(amount == 1) return "1 " + baseName;
        return amount + " " + baseName + "s";
    }

    public static Connection newDbConnection() {
        return r.connection().hostname(config.dbHost).port(config.dbPort).db(config.dbDb).user(config.dbUser, config.dbPassword).connect();
    }

    public static boolean handleIncreasingRatelimit(IncreasingRateLimiter rateLimiter, String u, GuildMessageReceivedEvent event, I18nContext context, boolean spamAware) {
        if (context == null) {
            //en_US
            context = new I18nContext();
        }

        RateLimit rateLimit = rateLimiter.limit(u);
        if (rateLimit.getTriesLeft() < 1) {
            event.getChannel().sendMessage(
                    String.format(context.get("general.ratelimit.header"),
                            EmoteReference.STOPWATCH, context.get("general.ratelimit_quotes"),
                            Utils.formatDuration(rateLimit.getCooldown()))
                            + ((rateLimit.getSpamAttempts() > 2 && spamAware) ? "\n\n" + EmoteReference.STOP + context.get("general.ratelimit.spam_1") : "")
                            + ((rateLimit.getSpamAttempts() > 4 && spamAware) ? context.get("general.ratelimit.spam_2") : "")
                            + ((rateLimit.getSpamAttempts() > 10 && spamAware) ? context.get("general.ratelimit.spam_3") : "")
                            + ((rateLimit.getSpamAttempts() > 15 && spamAware) ? context.get("general.ratelimit.spam_4") : "")
            ).queue();

            //Assuming it's an user RL if it can parse a long since we use UUIDs for other RLs.
            try {
                //noinspection ResultOfMethodCallIgnored
                Long.parseUnsignedLong(u);
                User user = MantaroBot.getInstance().getShardManager().getUserById(u);

                //Why would ANYONE go over 20 attempts?
                if (rateLimit.getSpamAttempts() > 20 && spamAware && user != null && !loggedAttemptUsers.contains(user.getId())) {
                    loggedAttemptUsers.add(user.getId());
                    LogUtils.spambot(user, LogUtils.SpamType.OVER_SPAM_LIMIT);
                }

                onRateLimit(user);
            } catch (Exception ignored) {}

            return false;
        }

        return true;
    }

    public static boolean handleIncreasingRatelimit(IncreasingRateLimiter rateLimiter, User u, GuildMessageReceivedEvent event, I18nContext context, boolean spamAware) {
        return handleIncreasingRatelimit(rateLimiter, u.getId(), event, context, spamAware);
    }

    public static boolean handleIncreasingRatelimit(IncreasingRateLimiter rateLimiter, User u, GuildMessageReceivedEvent event, I18nContext context) {
        return handleIncreasingRatelimit(rateLimiter, u.getId(), event, context, true);
    }

    public static boolean handleIncreasingRatelimit(IncreasingRateLimiter rateLimiter, User u, Context ctx) {
        return handleIncreasingRatelimit(rateLimiter, u.getId(), ctx.getEvent(), ctx.getLanguageContext(), true);
    }

    private static void onRateLimit(User user) {
        int ratelimitedTimes = ratelimitedUsers.computeIfAbsent(user.getIdLong(), __ -> new AtomicInteger()).incrementAndGet();
        if (ratelimitedTimes > 800 && !loggedSpambotUsers.contains(user.getId())) {
            loggedSpambotUsers.add(user.getId());
            LogUtils.spambot(user, LogUtils.SpamType.BLATANT);
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
        if(!isRtl(src)) {
            return src;
        }

        //no realloc unless we somehow have 5 codeblocks
        var sb = new StringBuilder(src.length() + 8);
        var inside = false;
        for(var i = 0; i < src.length(); i++) {
            var ch = src.charAt(i);
            if(ch == BACKTICK) {
                if(inside) {
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
            byte d = Character.getDirectionality(string.charAt(i));

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
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    public enum HushType {
        ANIME, CHARACTER, MUSIC
    }
}
