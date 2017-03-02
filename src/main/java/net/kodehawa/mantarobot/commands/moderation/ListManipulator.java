package net.kodehawa.mantarobot.commands.moderation;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

public class ListManipulator<T> implements Callable {
	private final BiFunction<GuildMessageReceivedEvent, String, T> addFunction;
	private final Function<GuildMessageReceivedEvent, List<T>> listFunction;
	private final BiConsumer<GuildMessageReceivedEvent, T> postAdded;
	private final BiConsumer<GuildMessageReceivedEvent, List<T>> postCleared;
	private final BiConsumer<GuildMessageReceivedEvent, T> postRemove;
	private final BiFunction<GuildMessageReceivedEvent, String, T> removeFunction;

	public ListManipulator(
		BiFunction<GuildMessageReceivedEvent, String, T> addFunction,
		BiFunction<GuildMessageReceivedEvent, String, T> removeFunction,
		Function<GuildMessageReceivedEvent, List<T>> listFunction,
		BiConsumer<GuildMessageReceivedEvent, T> postRemove,
		BiConsumer<GuildMessageReceivedEvent, T> postAdded,
		BiConsumer<GuildMessageReceivedEvent, List<T>> postCleared
	) {
		this.addFunction = addFunction;
		this.removeFunction = removeFunction;
		this.listFunction = listFunction;
		this.postRemove = postRemove;
		this.postAdded = postAdded;
		this.postCleared = postCleared;
	}

	@Override
	public boolean call(GuildMessageReceivedEvent event, String value) {
		if (value == null) return false;
		String[] args = SPLIT_PATTERN.split(value, 2);
		if (args.length != 2) return false;
		String op = args[0], v = args[1];

		List<T> list = listFunction.apply(event);

		if (op.equals("remove") || op.equals("rm")) {
			postRemove.accept(event, removeFunction.apply(event, v));
			return true;
		}

		if (op.equals("add")) {
			postAdded.accept(event, addFunction.apply(event, v));
			return true;
		}

		if (op.equals("clear")) {
			ArrayList<T> clone = new ArrayList<>(list);
			list.clear();
			postCleared.accept(event, clone);
			return true;
		}

		return false;
	}
}

