package net.kodehawa.mantarobot.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class YoutubeMp3Info {
	private static final Logger LOGGER = LoggerFactory.getLogger("YoutubeInMP3");

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

	public static YoutubeMp3Info forLink(String youtubeLink, GuildMessageReceivedEvent event) {
		String finalLink;

		if (validTrackPatterns.stream().noneMatch(p -> p.test(youtubeLink))) {
			UnaryOperator<String> op = partialPatterns.entrySet().stream().filter(entry -> entry.getKey().test(youtubeLink)).map(Entry::getValue).findFirst().orElse(null);

			if (op == null) {
				event.getChannel().sendMessage(":heavy_multiplication_x: Link seems to be invalid.").queue();
				return null;
			}

			finalLink = op.apply(youtubeLink);
		} else {
			finalLink = youtubeLink;
		}

		String link = "https://www.youtubeinmp3.com/fetch/?format=JSON&video=" + finalLink;

		String s = Utils.wgetResty(link, event);

		if (s == null) {
			LOGGER.warn("YTMP3 returned null link: " + link);
			return null;
		}

		try {
			return GsonDataManager.GSON_PRETTY.fromJson(s, YoutubeMp3Info.class);
		} catch (Exception e) {
			LOGGER.warn("``" + e.getClass().getSimpleName() + "`` thrown while deserializing JSON.", e);
		}
		return null;
	}

	public String error, title, length, link;
}
