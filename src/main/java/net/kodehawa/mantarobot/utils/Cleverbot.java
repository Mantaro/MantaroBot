package net.kodehawa.mantarobot.utils;

import com.rethinkdb.model.MapObject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.action.ImageActionCmd;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static br.com.brjdevs.java.utils.extensions.CollectionUtils.random;
import static br.com.brjdevs.java.utils.strings.StringUtils.splitArgs;
import static net.kodehawa.mantarobot.data.MantaroData.config;

@Slf4j
public class Cleverbot {
	private static final Map<Predicate<String>, Consumer<GuildMessageReceivedEvent>> OVERRIDES =
		new MapObject<Predicate<String>, Consumer<GuildMessageReceivedEvent>>()
			.with(
				s -> s.trim().isEmpty(),
				event -> event.getChannel().sendMessage("Oh, hi! I'm Mantaro! Type ``" + config().get().prefix + "help`` to get started!").queue()
			)
			.with(
				Pattern.compile("help\\s*?(meh?)?[.!?~]*?", Pattern.CASE_INSENSITIVE).asPredicate(),
				event -> event.getChannel().sendMessage("Oh, hi! Type ``" + config().get().prefix + "help`` to get started!").queue()
			)
			.with(
				Pattern.compile("awo+?[.!?~]*?", Pattern.CASE_INSENSITIVE).asPredicate(),
				event -> event.getChannel().sendMessage(
					"Awoo" + IntStream.range(0, random(10)).mapToObj(i -> "o").collect(Collectors.joining()) + "!"
				).queue()
			)
			.with(
				Pattern.compile("(hi+?)|(hey(a*?|y+?))|(hello+?)[.!?~]*?", Pattern.CASE_INSENSITIVE).asPredicate(),
				event -> event.getChannel().sendMessage(
					random(new String[]{"Hi", "Hello", "Heya"}) + " " + event.getMember().getEffectiveName() + "!"
				).queue()
			)
			.with(
				//The lewdness is strong with this one
				Pattern.compile("(s+?e+?x+?)|(p+?o+?r+?n+?)|(h+?e+?n+?t+?a+?i+?)|(e+?c+?h+?i+?)|(xxx)", Pattern.CASE_INSENSITIVE).asPredicate(),
				event -> event.getChannel().sendFile(
					ImageActionCmd.CACHE.getInput("http://imgur.com/LJfZYau.png"), "lewd.png",
					new MessageBuilder().append("Y-You lewdie!").build()
				).queue()
			)
			.with(
				//The lewdness is also strong with this one
				Pattern.compile("l((e+?w+?)|(oo+?))d", Pattern.CASE_INSENSITIVE).asPredicate(),
				event -> event.getChannel().sendMessage("I-I was Lood?").queue()
			)
			.with(
				Pattern.compile("ha+?n+?s+?[.!?~]*?", Pattern.CASE_INSENSITIVE).asPredicate(),
				event -> event.getChannel().sendMessage("*grabs ze flamethrower*").queue()
			)
			.immutable();

	public static void handle(GuildMessageReceivedEvent event) {
		String input = splitArgs(event.getMessage().getStrippedContent(), 2)[1];

		for (Entry<Predicate<String>, Consumer<GuildMessageReceivedEvent>> override : OVERRIDES.entrySet()) {
			if (override.getKey().test(input)) {
				override.getValue().accept(event);
				return;
			}
		}

		try {
			if (MantaroBot.CLEVERBOT == null) throw new UnsupportedOperationException("exploiting a try-catch");

			event.getChannel().sendMessage(MantaroBot.CLEVERBOT.getResponse(input)).queue();
		} catch (Exception e) {
			if (!(e instanceof UnsupportedOperationException)) {
				log.error("Seems that something on Cleverbot API broke...", e);
			}
			event.getChannel().sendMessage(EmoteReference.CRYING + "I-I don't know what to say! P-please forgive me. (Cleverbot API isn't enabled or it broke)").queue();
		}
	}
}
