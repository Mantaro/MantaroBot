package net.kodehawa.mantarobot.utils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.listeners.FunctionListener;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

public class DiscordUtils {
	public static <T> Pair<String, Integer> embedList(List<T> list, Function<T, String> toString) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			String s = toString.apply(list.get(i));
			if (b.length() + s.length() + 5 > EmbedBuilder.TEXT_MAX_LENGTH) return Pair.of(b.toString(), i);
			b.append('[').append(i + 1).append("] ");
			b.append(s);
			b.append("\n");
		}

		return Pair.of(b.toString(), list.size());
	}

	public static void selectInt(GuildMessageReceivedEvent event, int max, IntConsumer valueConsumer) {
		FunctionListener functionListener = new FunctionListener(event.getChannel().getId(), (l, e) -> {
			if (!e.getAuthor().equals(event.getAuthor())) return false;

			try {
				int choose = Integer.parseInt(e.getMessage().getContent());
				if (choose < 1 || choose >= max) return false;
				valueConsumer.accept(choose);
				return true;
			} catch (Exception ignored) {
			}
			return false;
		});

		MantaroBot.getJDA().addEventListener(functionListener);
		Async.asyncSleepThen(10000, () -> {
			if (!functionListener.isDone()) {
				MantaroBot.getJDA().removeEventListener(functionListener);
				event.getChannel().sendMessage("\u274C Timeout: No reply in 10 seconds").queue();
			}
		}).run();
	}

	public static <T> void selectList(GuildMessageReceivedEvent event, List<T> list, Function<T, String> toString, Function<String, MessageEmbed> toEmbed, Consumer<T> valueConsumer) {
		Pair<String, Integer> r = embedList(list, toString);
		event.getChannel().sendMessage(toEmbed.apply(r.getLeft())).queue();
		selectInt(event, r.getRight() + 1, i -> valueConsumer.accept(list.get(i - 1)));
	}

	public static <T> T selectListSync(GuildMessageReceivedEvent event, List<T> list, Function<T, String> toString, Function<String, MessageEmbed> toEmbed) {
		CompletableFuture<T> future = new CompletableFuture<T>();
		Object lock = new Object();
		selectList(event, list, toString, toEmbed, (value) -> {
			future.complete(value);
			synchronized (lock) {
				lock.notify();
			}
		});

		synchronized (lock) {
			try {
				lock.wait(10000);
			} catch (InterruptedException ignored) {

			}
		}

		if (!future.isDone()) future.complete(null);

		try {
			return future.get();
		} catch (InterruptedException | ExecutionException ignored) {
			return null;
		}
	}
}
