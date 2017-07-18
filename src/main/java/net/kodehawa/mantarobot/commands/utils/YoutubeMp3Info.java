package net.kodehawa.mantarobot.commands.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class YoutubeMp3Info {
    private static final String PROTOCOL_REGEX = "(?:http://|https://|)";
    private static final String SUFFIX_REGEX = "(?:\\?.*|&.*|)";
    private static final String VIDEO_ID_REGEX = "([a-zA-Z0-9_-]{11})";
    private static final Map<Predicate<String>, UnaryOperator<String>> partialPatterns = new ImmutableMap.Builder<Predicate<String>, UnaryOperator<String>>()
            .put(Pattern.compile("^" + VIDEO_ID_REGEX + "$").asPredicate(), "https://youtu.be"::concat)
            .put(Pattern.compile("^\\?v=" + VIDEO_ID_REGEX + "$").asPredicate(), "https://youtube.com/watch"::concat)
            .build();

    private static final List<Predicate<String>> validTrackPatterns = new ImmutableList.Builder<Predicate<String>>()
            .add(Pattern.compile("^" + PROTOCOL_REGEX + "(?:www\\.|)youtube.com/watch\\?v=" + VIDEO_ID_REGEX + SUFFIX_REGEX + "$").asPredicate())
            .add(Pattern.compile("^" + PROTOCOL_REGEX + "(?:www\\.|)youtu.be/" + VIDEO_ID_REGEX + SUFFIX_REGEX + "$").asPredicate())
            .build();
    public String error, title, length, link;

    public static YoutubeMp3Info forLink(String youtubeLink) {
        String finalLink;

        if(validTrackPatterns.stream().noneMatch(p -> p.test(youtubeLink))) {
            UnaryOperator<String> op = partialPatterns.entrySet().stream().filter(entry -> entry.getKey().test(youtubeLink)).map(Entry::getValue).findFirst().orElse(null);

            if(op == null) {
                return null;
            }

            finalLink = op.apply(youtubeLink);
        } else {
            finalLink = youtubeLink;
        }

        String link = "https://www.youtubeinmp3.com/fetch/?format=JSON&video=" + finalLink;

        String s = Utils.wgetResty(link, null);

        if(s == null) return null;

        try {
            return GsonDataManager.GSON_PRETTY.fromJson(s, YoutubeMp3Info.class);
        } catch(Exception e) {
        }
        return null;
    }
}
