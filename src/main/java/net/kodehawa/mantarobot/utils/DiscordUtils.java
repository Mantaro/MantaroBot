package net.kodehawa.mantarobot.utils;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.listeners.FunctionListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class DiscordUtils {
	public static Future<Integer> selectInt(GuildMessageReceivedEvent event, int max) {
		CompletableFuture<Integer> complete = new CompletableFuture<>();

		FunctionListener functionListener = new FunctionListener(event.getChannel().getId(), (l, e) -> {
			if (!e.getAuthor().equals(event.getAuthor())) return false;

			try {
				int choose = Integer.parseInt(e.getMessage().getContent());
				if (choose < 1 || choose >= max) return false;
				complete.complete(choose);
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
				complete.cancel(true);
			}
		}).run();

		return complete;
	}
}
