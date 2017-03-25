package net.kodehawa.mantarobot.commands.custom.compiler;

import br.com.brjdevs.java.utils.extensions.CollectionUtils;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.custom.Mapifier.dynamicResolve;
import static net.kodehawa.mantarobot.commands.custom.Mapifier.map;

public class CustomCommandCompiler {
	private static BiFunction<String, GuildMessageReceivedEvent, String> resolver = (s, e) -> {
		if (s.contains("$(")) {
			Map<String, String> dynamicMap = new HashMap<>();
			map("event", dynamicMap, e);
			s = dynamicResolve(s, dynamicMap);
		}

		return s;
	};

	public static Consumer<GuildMessageReceivedEvent> compile(String string) {
		if (string.startsWith("play:")) {
			String v = string.substring(5);

			return event -> {
				String s = resolver.apply(v, event);
				try {
					new URL(s);
				} catch (Exception e) {
					s = "ytsearch: " + s;
				}
				MantaroBot.getInstance().getAudioManager().loadAndPlay(event, s);
			};
		}

//		if (string.startsWith("embed:")) {
//			event.getChannel().sendMessage(GsonDataManager.gson(false).fromJson('{' + response.substring(6) + '}', EmbedJSON.class).gen()).queue();
//			return;
//		}
//
//		if (string.startsWith("imgembed:")) {
//			event.getChannel().sendMessage(new EmbedBuilder().setImage(response.substring(9)).setTitle(cmdName, null).setColor(event.getMember().getColor()).build()).queue();
//			return;
//		}
		//default mode
		return event -> event.getChannel().sendMessage(resolver.apply(string, event)).queue();
	}

	public static CompiledCustomCommand compile(List<String> values) {
		ArrayList<String> list = new ArrayList<>(values);

		Consumer<GuildMessageReceivedEvent> consumer = random(list.stream().map(CustomCommandCompiler::compile).collect(Collectors.toList()));

		return new CompiledCustomCommand() {
			@Override
			public void call(GuildMessageReceivedEvent event) {
				consumer.accept(event);
			}

			@Override
			public List<String> source() {
				return list;
			}
		};
	}

	public static Consumer<GuildMessageReceivedEvent> random(List<Consumer<GuildMessageReceivedEvent>> consumers) {
		return event -> CollectionUtils.random(consumers).accept(event);
	}
}
